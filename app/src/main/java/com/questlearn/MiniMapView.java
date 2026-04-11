package com.questlearn;

/*
 * MiniMapView — decorative fake map on challenge detail (not real Google Maps).
 * Draws blocks, grid, and an animated pin so the screen feels alive.
 */

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.OvershootInterpolator;

public class MiniMapView extends View {

    private final Paint bgPaint    = new Paint();
    private final Paint gridPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint roadPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bldgPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pinFill    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pulsePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float pinScale = 0f;
    private float pulseRadius = 0f;
    private ValueAnimator pinAnim, pulseAnim;
    private float dp;

    public MiniMapView(Context context) {
        super(context); init();
    }
    public MiniMapView(Context context, AttributeSet attrs) {
        super(context, attrs); init();
    }

    private void init() {
        dp = getResources().getDisplayMetrics().density;
        bgPaint.setColor(Color.parseColor("#DCE8DC"));
        gridPaint.setColor(Color.parseColor("#C8E6C8"));
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f);
        roadPaint.setColor(Color.parseColor("#F5F0E8"));
        bldgPaint.setColor(Color.parseColor("#C8D8C8"));
        pinFill.setColor(Color.parseColor("#2196F3"));
        pulsePaint.setColor(Color.parseColor("#2196F3"));
        pulsePaint.setStyle(Paint.Style.STROKE);
        pulsePaint.setStrokeWidth(2f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth(), h = getHeight();

        // BG + grid
        canvas.drawRect(0, 0, w, h, bgPaint);
        float gs = 20 * dp;
        for (float x = 0; x < w; x += gs)
            canvas.drawLine(x, 0, x, h, gridPaint);
        for (float y = 0; y < h; y += gs)
            canvas.drawLine(0, y, w, y, gridPaint);

        // Road
        canvas.drawRect(0, h * 0.45f, w, h * 0.45f + 14 * dp, roadPaint);
        canvas.drawRect(w * 0.45f, 0, w * 0.45f + 14 * dp, h, roadPaint);

        // Simple buildings
        canvas.drawRoundRect(new RectF(w*0.08f, h*0.05f, w*0.38f, h*0.38f), 4*dp,4*dp, bldgPaint);
        canvas.drawRoundRect(new RectF(w*0.60f, h*0.05f, w*0.90f, h*0.38f), 4*dp,4*dp, bldgPaint);
        canvas.drawRoundRect(new RectF(w*0.08f, h*0.55f, w*0.38f, h*0.90f), 4*dp,4*dp, bldgPaint);

        // Pulse ring around pin
        float pinX = w * 0.5f, pinY = h * 0.35f;
        if (pulseRadius > 0) {
            canvas.drawCircle(pinX, pinY, pulseRadius, pulsePaint);
        }

        // Pin (animated scale)
        if (pinScale > 0) {
            canvas.save();
            canvas.scale(pinScale, pinScale, pinX, pinY + 18 * dp);

            float headR = 14 * dp;
            pinFill.setColor(Color.parseColor("#2196F3"));
            canvas.drawCircle(pinX, pinY, headR, pinFill);

            Path point = new Path();
            point.moveTo(pinX - headR * 0.5f, pinY + headR * 0.5f);
            point.lineTo(pinX + headR * 0.5f, pinY + headR * 0.5f);
            point.lineTo(pinX, pinY + headR * 1.8f);
            point.close();
            canvas.drawPath(point, pinFill);

            // White emoji in pin
            Paint emojiPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            emojiPaint.setTextSize(13 * dp);
            emojiPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("📚", pinX, pinY + 5 * dp, emojiPaint);

            canvas.restore();
        }
    }

    public void startPinAnimation() {
        // Drop-in pin
        if (pinAnim != null) pinAnim.cancel();
        pinAnim = ValueAnimator.ofFloat(0f, 1f);
        pinAnim.setDuration(500);
        pinAnim.setStartDelay(200);
        pinAnim.setInterpolator(new OvershootInterpolator(2f));
        pinAnim.addUpdateListener(a -> {
            pinScale = (float) a.getAnimatedValue();
            invalidate();
        });
        pinAnim.start();

        // Pulse ring
        if (pulseAnim != null) pulseAnim.cancel();
        pulseAnim = ValueAnimator.ofFloat(0f, 30 * dp);
        pulseAnim.setDuration(1800);
        pulseAnim.setStartDelay(600);
        pulseAnim.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnim.setRepeatMode(ValueAnimator.RESTART);
        pulseAnim.addUpdateListener(a -> {
            pulseRadius = (float) a.getAnimatedValue();
            float frac = pulseRadius / (30 * dp);
            pulsePaint.setAlpha((int)(180 * (1f - frac)));
            invalidate();
        });
        pulseAnim.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (pinAnim   != null) pinAnim.cancel();
        if (pulseAnim != null) pulseAnim.cancel();
    }
}
