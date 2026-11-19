package com.example.ledger.wardrobe;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ClothingImages {
    private static final ExecutorService IO = Executors.newFixedThreadPool(2, runnable -> new Thread(runnable, "wardrobe-image"));
    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(12 * 1024) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount() / 1024;
        }
    };

    private ClothingImages() {
    }

    public static String importUri(Context context, Uri uri) throws java.io.IOException {
        File dir = new File(context.getFilesDir(), "wardrobe");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new java.io.IOException("mkdir");
        }
        File dest = new File(dir, System.currentTimeMillis() + ".jpg");
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            if (input == null) {
                throw new java.io.IOException("open");
            }
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, bounds);
        }
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            if (input == null) {
                throw new java.io.IOException("open");
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sample(boundsOf(context, uri), 1280);
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, options);
            if (bitmap == null) {
                throw new java.io.IOException("decode");
            }
            try (FileOutputStream output = new FileOutputStream(dest)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 86, output);
            }
            bitmap.recycle();
        }
        return dest.getAbsolutePath();
    }

    public static void delete(String path) {
        if (path == null || path.isEmpty()) {
            return;
        }
        CACHE.remove(path);
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
    }

    public static void load(ImageView view, String path, int fallback) {
        view.setTag(path == null ? "" : path);
        if (path == null || path.isEmpty()) {
            view.setImageBitmap(null);
            view.setBackgroundColor(fallback);
            return;
        }
        Bitmap cached = CACHE.get(path);
        if (cached != null) {
            view.setBackgroundColor(Color.TRANSPARENT);
            view.setImageBitmap(cached);
            return;
        }
        view.setImageBitmap(null);
        view.setBackgroundColor(fallback);
        IO.execute(() -> {
            Bitmap bitmap = decode(path, 720);
            if (bitmap == null) {
                return;
            }
            CACHE.put(path, bitmap);
            view.post(() -> {
                if (path.equals(view.getTag())) {
                    view.setBackgroundColor(Color.TRANSPARENT);
                    view.setImageBitmap(bitmap);
                }
            });
        });
    }

    private static int boundsOf(Context context, Uri uri) {
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, bounds);
            return Math.max(bounds.outWidth, bounds.outHeight);
        } catch (Exception ignored) {
            return 1280;
        }
    }

    private static Bitmap decode(String path, int maxSide) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, bounds);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample(Math.max(bounds.outWidth, bounds.outHeight), maxSide);
        return BitmapFactory.decodeFile(path, options);
    }

    private static int sample(int size, int maxSide) {
        int sample = 1;
        while (size / sample > maxSide * 2) {
            sample *= 2;
        }
        return Math.max(1, sample);
    }
}
