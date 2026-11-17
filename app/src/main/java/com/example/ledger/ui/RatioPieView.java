package com.example.ledger.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.ledger.R;

public class RatioPieView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private final int trackColor;
    private final int gapColor;
    private final float gapWidth;
    private int[] colors = new int[0];
    private long[] amounts = new long[0];

    public RatioPieView(Context context) {
        this(context, null);
    }

    public RatioPieView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        trackColor = ContextCompat.getColor(context, R.color.ink_soft);
        gapColor = ContextCompat.getColor(context, R.color.paper);
        gapWidth = 2f * getResources().getDisplayMetrics().density;
        paint.setAntiAlias(true);
    }

    public void setSlices(int[] nextColors, long[] nextAmounts) {
        int[] safeColors = nextColors == null ? new int[0] : nextColors;
        long[] safeAmounts = nextAmounts == null ? new long[0] : nextAmounts;
        if (same(safeColors, safeAmounts)) {
            return;
        }
        colors = safeColors.clone();
        amounts = safeAmounts.clone();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float padding = 4f * getResources().getDisplayMetrics().density;
        float size = Math.min(getWidth(), getHeight()) - padding * 2f;
        if (size <= 0f) {
            return;
        }
        float left = (getWidth() - size) / 2f;
        float top = (getHeight() - size) / 2f;
        oval.set(left, top, left + size, top + size);

        long total = 0L;
        int last = -1;
        for (int i = 0; i < amounts.length; i++) {
            if (amounts[i] > 0L) {
                total += amounts[i];
                last = i;
            }
        }
        if (total <= 0L) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(size * 0.12f);
            paint.setColor(trackColor);
            canvas.drawCircle(oval.centerX(), oval.centerY(), (size - size * 0.12f) / 2f, paint);
            return;
        }

        float start = -90f;
        int positive = 0;
        for (int i = 0; i < amounts.length; i++) {
            if (amounts[i] > 0L) {
                positive++;
            }
        }
        for (int i = 0; i < amounts.length; i++) {
            if (amounts[i] <= 0L || i >= colors.length) {
                continue;
            }
            float sweep = i == last ? 270f - start : amounts[i] * 360f / total;
            if (sweep <= 0f) {
                continue;
            }
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(colors[i]);
            canvas.drawArc(oval, start, sweep, true, paint);
            if (positive > 1) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(gapWidth);
                paint.setColor(gapColor);
                canvas.drawArc(oval, start, sweep, true, paint);
            }
            start += sweep;
        }
    }

    private boolean same(int[] nextColors, long[] nextAmounts) {
        if (colors.length != nextColors.length || amounts.length != nextAmounts.length) {
            return false;
        }
        for (int i = 0; i < amounts.length; i++) {
            if (amounts[i] != nextAmounts[i] || colors[i] != nextColors[i]) {
                return false;
            }
        }
        return true;
    }
}
