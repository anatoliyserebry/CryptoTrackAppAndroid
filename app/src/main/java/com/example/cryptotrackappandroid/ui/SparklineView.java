package com.example.cryptotrackappandroid.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.cryptotrackappandroid.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SparklineView extends View {
    public interface OnSelectionChangeListener {
        void onSelectionChanged(int index, long timestamp, double price);
    }

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bubblePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bubbleBounds = new RectF();

    private double[] values = new double[0];
    private long[] timestamps = new long[0];
    private boolean positive = true;
    private boolean detailed = false;
    private int selectedIndex = -1;
    private OnSelectionChangeListener selectionChangeListener;

    public SparklineView(Context context) {
        super(context);
        init();
    }

    public SparklineView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SparklineView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setStrokeWidth(2.6f * density);

        fillPaint.setStyle(Paint.Style.FILL);

        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f * density);
        gridPaint.setColor(ContextCompat.getColor(getContext(), R.color.divider));

        selectorPaint.setStyle(Paint.Style.STROKE);
        selectorPaint.setStrokeWidth(1.1f * density);
        selectorPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_secondary));

        pointPaint.setStyle(Paint.Style.FILL);

        textPaint.setTextSize(11f * getResources().getDisplayMetrics().scaledDensity);
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_secondary));

        bubblePaint.setStyle(Paint.Style.FILL);
        setFocusable(true);
    }

    public void setDetailed(boolean detailed) {
        this.detailed = detailed;
        invalidate();
    }

    public void setOnSelectionChangeListener(OnSelectionChangeListener listener) {
        this.selectionChangeListener = listener;
    }

    public void setValues(double[] values, boolean positive) {
        setSeries(null, values, positive);
    }

    public void setSeries(long[] timestamps, double[] values, boolean positive) {
        this.values = values != null ? values : new double[0];
        this.timestamps = normalizedTimestamps(timestamps, this.values.length);
        this.positive = positive;
        if (detailed && this.values.length > 0) {
            selectedIndex = this.values.length - 1;
            notifySelection();
        } else {
            selectedIndex = -1;
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (values.length < 2 || getWidth() == 0 || getHeight() == 0) {
            return;
        }

        int color = positive
                ? ContextCompat.getColor(getContext(), R.color.positive)
                : ContextCompat.getColor(getContext(), R.color.negative);
        linePaint.setColor(color);
        pointPaint.setColor(color);

        ChartBounds bounds = chartBounds();
        double min = minValue();
        double max = maxValue();
        if (Math.abs(max - min) < 0.0000001) {
            max += 1;
            min -= 1;
        }

        drawGrid(canvas, bounds, min, max);

        Path linePath = new Path();
        Path fillPath = new Path();
        for (int i = 0; i < values.length; i++) {
            float x = xForIndex(i, bounds);
            float y = yForValue(values[i], bounds, min, max);
            if (i == 0) {
                linePath.moveTo(x, y);
                fillPath.moveTo(x, y);
            } else {
                linePath.lineTo(x, y);
                fillPath.lineTo(x, y);
            }
        }

        fillPath.lineTo(xForIndex(values.length - 1, bounds), bounds.bottom);
        fillPath.lineTo(xForIndex(0, bounds), bounds.bottom);
        fillPath.close();
        fillPaint.setShader(new LinearGradient(
                0,
                bounds.top,
                0,
                bounds.bottom,
                alphaColor(color, detailed ? 54 : 34),
                alphaColor(color, 0),
                Shader.TileMode.CLAMP
        ));
        canvas.drawPath(fillPath, fillPaint);
        fillPaint.setShader(null);
        canvas.drawPath(linePath, linePaint);

        if (detailed && selectedIndex >= 0 && selectedIndex < values.length) {
            drawSelection(canvas, bounds, min, max, color);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!detailed || values.length < 2) {
            return super.onTouchEvent(event);
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                updateSelection(event.getX());
                return true;
            case MotionEvent.ACTION_UP:
                updateSelection(event.getX());
                performClick();
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                return true;
            default:
                return true;
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private void drawGrid(Canvas canvas, ChartBounds bounds, double min, double max) {
        int lines = detailed ? 4 : 1;
        for (int i = 0; i <= lines; i++) {
            float y = bounds.top + ((bounds.bottom - bounds.top) * i / (float) lines);
            canvas.drawLine(bounds.left, y, bounds.right, y, gridPaint);
        }

        if (!detailed) {
            return;
        }

        for (int i = 1; i < 4; i++) {
            float x = bounds.left + ((bounds.right - bounds.left) * i / 4f);
            canvas.drawLine(x, bounds.top, x, bounds.bottom, gridPaint);
        }

        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
        canvas.drawText(compactPrice(max), bounds.right + dp(8), bounds.top + dp(4), textPaint);
        canvas.drawText(compactPrice((min + max) / 2.0), bounds.right + dp(8), bounds.top + ((bounds.bottom - bounds.top) / 2f) + dp(4), textPaint);
        canvas.drawText(compactPrice(min), bounds.right + dp(8), bounds.bottom, textPaint);
    }

    private void drawSelection(Canvas canvas, ChartBounds bounds, double min, double max, int color) {
        float x = xForIndex(selectedIndex, bounds);
        float y = yForValue(values[selectedIndex], bounds, min, max);

        selectorPaint.setColor(alphaColor(ContextCompat.getColor(getContext(), R.color.text_primary), 108));
        canvas.drawLine(x, bounds.top, x, bounds.bottom, selectorPaint);

        pointPaint.setColor(ContextCompat.getColor(getContext(), R.color.surface));
        canvas.drawCircle(x, y, dp(6), pointPaint);
        pointPaint.setColor(color);
        canvas.drawCircle(x, y, dp(4), pointPaint);

        String price = Formatters.price(values[selectedIndex]);
        String time = formatTimestamp(timestamps[selectedIndex]);
        float priceWidth = textPaint.measureText(price);
        float timeWidth = textPaint.measureText(time);
        float bubbleWidth = Math.max(dp(88), Math.max(priceWidth, timeWidth) + dp(18));
        float bubbleHeight = dp(42);
        float bubbleLeft = clamp(x - bubbleWidth / 2f, dp(4), getWidth() - bubbleWidth - dp(4));
        float bubbleTop = bounds.top + dp(6);

        bubbleBounds.set(bubbleLeft, bubbleTop, bubbleLeft + bubbleWidth, bubbleTop + bubbleHeight);
        bubblePaint.setColor(ContextCompat.getColor(getContext(), R.color.text_primary));
        canvas.drawRoundRect(bubbleBounds, dp(8), dp(8), bubblePaint);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_inverse));
        textPaint.setFakeBoldText(true);
        canvas.drawText(price, bubbleBounds.centerX(), bubbleBounds.top + dp(17), textPaint);
        textPaint.setFakeBoldText(false);
        textPaint.setColor(alphaColor(ContextCompat.getColor(getContext(), R.color.text_inverse), 205));
        canvas.drawText(time, bubbleBounds.centerX(), bubbleBounds.top + dp(33), textPaint);
    }

    private void updateSelection(float touchX) {
        ChartBounds bounds = chartBounds();
        int nearestIndex = 0;
        float nearestDistance = Float.MAX_VALUE;
        for (int i = 0; i < values.length; i++) {
            float distance = Math.abs(touchX - xForIndex(i, bounds));
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestIndex = i;
            }
        }

        if (selectedIndex != nearestIndex) {
            selectedIndex = nearestIndex;
            notifySelection();
            invalidate();
        }
    }

    private void notifySelection() {
        if (selectionChangeListener != null && selectedIndex >= 0 && selectedIndex < values.length) {
            selectionChangeListener.onSelectionChanged(selectedIndex, timestamps[selectedIndex], values[selectedIndex]);
        }
    }

    private ChartBounds chartBounds() {
        float left = getPaddingStart() + (detailed ? dp(4) : 0f);
        float right = getWidth() - getPaddingEnd() - (detailed ? dp(64) : 0f);
        float top = getPaddingTop() + (detailed ? dp(8) : 0f);
        float bottom = getHeight() - getPaddingBottom() - (detailed ? dp(12) : 0f);
        if (right <= left) {
            right = left + 1f;
        }
        if (bottom <= top) {
            bottom = top + 1f;
        }
        return new ChartBounds(left, top, right, bottom);
    }

    private float xForIndex(int index, ChartBounds bounds) {
        long firstTime = timestamps.length > 0 ? timestamps[0] : 0L;
        long lastTime = timestamps.length > 0 ? timestamps[timestamps.length - 1] : 0L;
        if (firstTime > 0L && lastTime > firstTime && index < timestamps.length) {
            float progress = (float) (timestamps[index] - firstTime) / (float) (lastTime - firstTime);
            return bounds.left + ((bounds.right - bounds.left) * progress);
        }
        return bounds.left + ((bounds.right - bounds.left) * index / (float) (values.length - 1));
    }

    private float yForValue(double value, ChartBounds bounds, double min, double max) {
        float normalized = (float) ((value - min) / (max - min));
        return bounds.bottom - (normalized * (bounds.bottom - bounds.top));
    }

    private long[] normalizedTimestamps(long[] input, int size) {
        long[] result = new long[size];
        if (input != null && input.length == size) {
            System.arraycopy(input, 0, result, 0, size);
            return result;
        }

        long end = System.currentTimeMillis();
        long start = end - Math.max(1, size - 1) * 60L * 60L * 1000L;
        for (int i = 0; i < size; i++) {
            result[i] = start + Math.round((end - start) * (i / (double) Math.max(1, size - 1)));
        }
        return result;
    }

    private double minValue() {
        double min = values[0];
        for (double value : values) {
            min = Math.min(min, value);
        }
        return min;
    }

    private double maxValue() {
        double max = values[0];
        for (double value : values) {
            max = Math.max(max, value);
        }
        return max;
    }

    private String compactPrice(double value) {
        double abs = Math.abs(value);
        if (abs >= 1_000_000.0) {
            return String.format(Locale.US, "$%.1fM", value / 1_000_000.0);
        }
        if (abs >= 1_000.0) {
            return String.format(Locale.US, "$%.1fK", value / 1_000.0);
        }
        if (abs >= 1.0) {
            return String.format(Locale.US, "$%.2f", value);
        }
        return String.format(Locale.US, "$%.4f", value);
    }

    private String formatTimestamp(long timestamp) {
        if (timestamps.length > 1 && timestamps[timestamps.length - 1] - timestamps[0] <= 2L * 24L * 60L * 60L * 1000L) {
            return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
        }
        return new SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(new Date(timestamp));
    }

    private int alphaColor(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private static class ChartBounds {
        final float left;
        final float top;
        final float right;
        final float bottom;

        ChartBounds(float left, float top, float right, float bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
    }
}
