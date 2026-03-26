package com.questlearn;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class QrOverlayView extends View {

    // Frame dimensions
    private static final float FRAME_SIZE_RATIO = 0.62f;  // fraction of the smaller dimension
    private static final float CORNER_LENGTH_DP  = 40f;
    private static final float CORNER_STROKE_DP  = 4f;

    private final Paint dimPaint    = new Paint();
    private final Paint cornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint clearPaint  = new Paint();

    private RectF frameRect = new RectF();
    private float cornerLen;
    private float cornerStroke;

    private float scanLineY = 0f;
    private ValueAnimator scanAnimator;

    public QrOverlayView(Context context) {
        super(context);
        init();
    }

    public QrOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;
        cornerLen    = CORNER_LENGTH_DP  * density;
        cornerStroke = CORNER_STROKE_DP  * density;

        // Semi-transparent dark overlay
        dimPaint.setColor(Color.argb(140, 0, 0, 0));

        // Blue corners
        cornerPaint.setColor(Color.parseColor("#2196F3"));
        cornerPaint.setStyle(Paint.Style.STROKE);
        cornerPaint.setStrokeWidth(cornerStroke);
        cornerPaint.setStrokeCap(Paint.Cap.SQUARE);

        // Scan-line gradient
        linePaint.setStyle(Paint.Style.FILL);

        // Clear paint to punch hole in dim overlay
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float frameSize = Math.min(w, h) * FRAME_SIZE_RATIO;
        float cx = w / 2f;
        float cy = h / 2f;
        frameRect.set(cx - frameSize / 2f, cy - frameSize / 2f,
                      cx + frameSize / 2f, cy + frameSize / 2f);
        scanLineY = frameRect.top;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 1. Dark overlay
        canvas.drawRect(0, 0, getWidth(), getHeight(), dimPaint);

        // 2. Punch out the QR frame area
        canvas.drawRect(frameRect, clearPaint);

        // 3. Corner brackets
        float l = frameRect.left, t = frameRect.top,
              r = frameRect.right, b = frameRect.bottom;

        // Top-left
        canvas.drawLine(l, t, l + cornerLen, t, cornerPaint);
        canvas.drawLine(l, t, l, t + cornerLen, cornerPaint);
        // Top-right
        canvas.drawLine(r - cornerLen, t, r, t, cornerPaint);
        canvas.drawLine(r, t, r, t + cornerLen, cornerPaint);
        // Bottom-left
        canvas.drawLine(l, b - cornerLen, l, b, cornerPaint);
        canvas.drawLine(l, b, l + cornerLen, b, cornerPaint);
        // Bottom-right
        canvas.drawLine(r - cornerLen, b, r, b, cornerPaint);
        canvas.drawLine(r, b - cornerLen, r, b, cornerPaint);

        // 4. Animated scan line (gradient)
        if (scanLineY > 0) {
            float lineH = 3 * getResources().getDisplayMetrics().density;
            linePaint.setShader(new LinearGradient(
                    l, scanLineY, r, scanLineY,
                    new int[]{Color.TRANSPARENT,
                              Color.parseColor("#882196F3"),
                              Color.parseColor("#2196F3"),
                              Color.parseColor("#882196F3"),
                              Color.TRANSPARENT},
                    null,
                    Shader.TileMode.CLAMP));
            canvas.drawRect(l, scanLineY, r, scanLineY + lineH, linePaint);
        }
    }

    /** Start the repeating scan-line animation. */
    public void startScanAnimation() {
        if (scanAnimator != null) scanAnimator.cancel();
        scanAnimator = ValueAnimator.ofFloat(0f, 1f);
        scanAnimator.setDuration(2000);
        scanAnimator.setRepeatCount(ValueAnimator.INFINITE);
        scanAnimator.setRepeatMode(ValueAnimator.REVERSE);
        scanAnimator.setInterpolator(new LinearInterpolator());
        scanAnimator.addUpdateListener(anim -> {
            float fraction = (float) anim.getAnimatedValue();
            scanLineY = frameRect.top + frameRect.height() * fraction;
            invalidate();
        });
        scanAnimator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (scanAnimator != null) scanAnimator.cancel();
    }
}
