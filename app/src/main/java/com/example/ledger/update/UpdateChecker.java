package com.example.ledger.update;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class UpdateChecker {
    public static final String LATEST_URL = "https://api.github.com/repos/giao-sakura/ledger/releases/latest";

    private static final ExecutorService IO = Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "app-update"));

    private UpdateChecker() {
    }

    public static void check(Callback callback) {
        IO.execute(() -> {
            Result result = fetch();
            new Handler(Looper.getMainLooper()).post(() -> callback.onResult(result));
        });
    }

    public static void download(String url, File destination, Progress progress, DownloadCallback callback) {
        IO.execute(() -> {
            boolean ok = save(url, destination, progress);
            new Handler(Looper.getMainLooper()).post(() -> callback.onComplete(ok));
        });
    }

    public static int compareVersions(String local, String remote) {
        int[] left = parse(local);
        int[] right = parse(remote);
        int length = Math.max(left.length, right.length);
        for (int i = 0; i < length; i++) {
            int a = i < left.length ? left[i] : 0;
            int b = i < right.length ? right[i] : 0;
            if (a != b) {
                return Integer.compare(a, b);
            }
        }
        return 0;
    }

    private static Result fetch() {
        HttpURLConnection connection = null;
        try {
            connection = open(LATEST_URL);
            int code = connection.getResponseCode();
            if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                return Result.latest();
            }
            if (code != HttpURLConnection.HTTP_OK) {
                return Result.failed();
            }
            JSONObject json = new JSONObject(readText(connection));
            String tag = json.optString("tag_name", "");
            String version = tag.startsWith("v") || tag.startsWith("V") ? tag.substring(1) : tag;
            if (parse(version).length == 0) {
                return Result.failed();
            }
            String notes = json.optString("body", "").trim();
            String page = json.optString("html_url", "");
            String apk = apkUrl(json.optJSONArray("assets"));
            return Result.update(version, notes, apk, page);
        } catch (Exception ignored) {
            return Result.failed();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String apkUrl(JSONArray assets) {
        if (assets == null) {
            return "";
        }
        String fallback = "";
        for (int i = 0; i < assets.length(); i++) {
            JSONObject asset = assets.optJSONObject(i);
            if (asset == null) {
                continue;
            }
            String name = asset.optString("name", "");
            String url = asset.optString("browser_download_url", "");
            if (!name.endsWith(".apk") || url.isEmpty()) {
                continue;
            }
            if ("ledger.apk".equals(name)) {
                return url;
            }
            if (fallback.isEmpty()) {
                fallback = url;
            }
        }
        return fallback;
    }

    private static boolean save(String url, File destination, Progress progress) {
        HttpURLConnection connection = null;
        try {
            File parent = destination.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                return false;
            }
            File temporary = new File(destination.getAbsolutePath() + ".part");
            connection = open(url);
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return false;
            }
            long total = connection.getContentLengthLong();
            if (total <= 0L) {
                progress.onProgress(-1);
            }
            try (InputStream input = connection.getInputStream();
                 FileOutputStream output = new FileOutputStream(temporary)) {
                byte[] buffer = new byte[8192];
                long read = 0L;
                int count;
                while ((count = input.read(buffer)) != -1) {
                    if (progress.cancelled()) {
                        temporary.delete();
                        return false;
                    }
                    output.write(buffer, 0, count);
                    read += count;
                    if (total > 0L) {
                        int percent = (int) Math.min(100L, read * 100L / total);
                        progress.onProgress(percent);
                    }
                }
            }
            if (temporary.length() < 4L || !looksLikeZip(temporary)) {
                temporary.delete();
                return false;
            }
            if (destination.exists() && !destination.delete()) {
                temporary.delete();
                return false;
            }
            return temporary.renameTo(destination);
        } catch (Exception ignored) {
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static boolean looksLikeZip(File file) {
        try (InputStream input = new java.io.FileInputStream(file)) {
            return input.read() == 'P' && input.read() == 'K';
        } catch (Exception ignored) {
            return false;
        }
    }

    private static HttpURLConnection open(String address) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setInstanceFollowRedirects(true);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(60000);
        connection.setRequestProperty("User-Agent", "ledger-android");
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        return connection;
    }

    private static String readText(HttpURLConnection connection) throws Exception {
        try (InputStream input = connection.getInputStream()) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            return new String(output.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    private static int[] parse(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException();
        }
        String value = raw.trim();
        if (value.startsWith("v") || value.startsWith("V")) {
            value = value.substring(1);
        }
        if (value.isEmpty()) {
            throw new IllegalArgumentException();
        }
        String[] parts = value.split("\\.");
        int[] numbers = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty() || !parts[i].chars().allMatch(Character::isDigit)) {
                throw new IllegalArgumentException();
            }
            numbers[i] = Integer.parseInt(parts[i]);
        }
        return numbers;
    }

    public interface Callback {
        void onResult(Result result);
    }

    public interface DownloadCallback {
        void onComplete(boolean success);
    }

    public interface Progress {
        void onProgress(int percent);

        boolean cancelled();
    }

    public static final class Result {
        public enum Kind {
            LATEST,
            UPDATE,
            FAILED
        }

        public final Kind kind;
        public final String versionName;
        public final String notes;
        public final String apkUrl;
        public final String pageUrl;

        private Result(Kind kind, String versionName, String notes, String apkUrl, String pageUrl) {
            this.kind = kind;
            this.versionName = versionName;
            this.notes = notes;
            this.apkUrl = apkUrl;
            this.pageUrl = pageUrl;
        }

        static Result latest() {
            return new Result(Kind.LATEST, "", "", "", "");
        }

        static Result failed() {
            return new Result(Kind.FAILED, "", "", "", "");
        }

        static Result update(String versionName, String notes, String apkUrl, String pageUrl) {
            return new Result(Kind.UPDATE, versionName, notes, apkUrl, pageUrl);
        }
    }
}
