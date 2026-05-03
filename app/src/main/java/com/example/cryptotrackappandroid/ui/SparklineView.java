package com.example.cryptotrackappandroid.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.cryptotrackappandroid.R;

public class SparklineView extends View {
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private double[] values = new double[0];
    private boolean positive = true;

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
        linePaint.setStrokeWidth(2.4f * density);

        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f * density);
        gridPaint.setColor(ContextCompat.getColor(getContext(), R.color.divider));
    }

    public void setValues(double[] values, boolean positive) {
        this.values = values != null ? values : new double[0];
        this.positive = positive;
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

        float width = getWidth() - getPaddingStart() - getPaddingEnd();
        float height = getHeight() - getPaddingTop() - getPaddingBottom();
        float left = getPaddingStart();
        float top = getPaddingTop();
        float midY = top + height / 2f;
        canvas.drawLine(left, midY, left + width, midY, gridPaint);

        double min = values[0];
        double max = values[0];
        for (double value : values) {
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        if (Math.abs(max - min) < 0.0000001) {
            max += 1;
            min -= 1;
        }

        Path path = new Path();
        for (int i = 0; i < values.length; i++) {
            float x = left + (width * i / (values.length - 1));
            float normalized = (float) ((values[i] - min) / (max - min));
            float y = top + height - (normalized * height);
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        canvas.drawPath(path, linePaint);
    }
}
