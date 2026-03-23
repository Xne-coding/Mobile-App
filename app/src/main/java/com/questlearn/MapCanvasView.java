package com.questlearn;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.List;

public class MapCanvasView extends View {

    public static final int PIN_BLUE   = 0;
    public static final int PIN_GREEN  = 1;
    public static final int PIN_ORANGE = 2;
    public static final int PIN_DONE   = 3;
    public static final int PIN_LOCKED = 4;

    public interface OnPinTappedListener {
        void onPinTapped(int pinType, String label);
    }

    private OnPinTappedListener pinTappedListener;

    // Paints
    private final Paint bgPaint    = new Paint();
    private final Paint gridPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint roadPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bldgPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bldgStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bldgText   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pinFill    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pinShadow  = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint userDot    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pulsePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Pin data class
    private static class MapPin {
        float cx, cy;
        int type;
        String label;
        MapPin(float cx, float cy, int type, String label) {
            this.cx = cx; this.cy = cy;
            this.type = type; this.label = label;
        }
    }

    private final List<MapPin> pins = new ArrayList<>();
    private float pulseRadius = 0f;
    private ValueAnimator pulseAnim;
    private float dp;

    public MapCanvasView(Context context) {
        super(context); init();
    }
    public MapCanvasView(Context context, AttributeSet attrs) {
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
        bldgStroke.setColor(Color.parseColor("#B0C8B0"));
        bldgStroke.setStyle(Paint.Style.STROKE);
        bldgStroke.setStrokeWidth(1.5f);

        bldgText.setColor(Color.parseColor("#4A6E4A"));
        bldgText.setTextSize(9 * dp);
        bldgText.setTextAlign(Paint.Align.CENTER);
        bldgText.setAntiAlias(true);

        pinFill.setAntiAlias(true);
        pinShadow.setAntiAlias(true);
        pinShadow.setMaskFilter(new BlurMaskFilter(4 * dp, BlurMaskFilter.Blur.NORMAL));

        userDot.setColor(Color.parseColor("#2196F3"));
        userDot.setStyle(Paint.Style.FILL);

        pulsePaint.setColor(Color.parseColor("#2196F3"));
        pulsePaint.setStyle(Paint.Style.STROKE);
        pulsePaint.setStrokeWidth(2f);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        buildPins(w, h);
        startPulseAnimation();
    }

    private void buildPins(int w, int h) {
        pins.clear();
        // Positions as fractions of view size
        pins.add(new MapPin(w * 0.38f, h * 0.25f, PIN_GREEN,  "Library QR"));
        pins.add(new MapPin(w * 0.71f, h * 0.23f, PIN_ORANGE, "Physics Beacon"));
        pins.add(new MapPin(w * 0.38f, h * 0.54f, PIN_BLUE,   "Trent GPS"));
        pins.add(new MapPin(w * 0.68f, h * 0.74f, PIN_LOCKED, "Locked"));
        pins.add(new MapPin(w * 0.30f, h * 0.80f, PIN_DONE,   "Portland Done"));
    }

    private void startPulseAnimation() {
        if (pulseAnim != null) pulseAnim.cancel();
        pulseAnim = ValueAnimator.ofFloat(0f, 30 * dp);
        pulseAnim.setDuration(2000);
        pulseAnim.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnim.setRepeatMode(ValueAnimator.RESTART);
        pulseAnim.setInterpolator(new LinearInterpolator());
        pulseAnim.addUpdateListener(anim -> {
            pulseRadius = (float) anim.getAnimatedValue();
            float fraction = pulseRadius / (30 * dp);
            pulsePaint.setAlpha((int) (200 * (1f - fraction)));
            invalidate();
        });
        pulseAnim.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth(), h = getHeight();

        // Background
        canvas.drawRect(0, 0, w, h, bgPaint);

        // Grid
        float gridSize = 30 * dp;
        for (float x = 0; x < w; x += gridSize)
            canvas.drawLine(x, 0, x, h, gridPaint);
        for (float y = 0; y < h; y += gridSize)
            canvas.drawLine(0, y, w, y, gridPaint);

        // Roads (horizontal)
        float roadW = 18 * dp;
        float[] hRoads = {h * 0.22f, h * 0.46f, h * 0.68f};
        for (float ry : hRoads)
            canvas.drawRect(0, ry, w, ry + roadW, roadPaint);

        // Roads (vertical)
        float[] vRoads = {w * 0.22f, w * 0.60f, w * 0.87f};
        for (float rx : vRoads)
            canvas.drawRect(rx, 0, rx + roadW, h, roadPaint);

        // Buildings
        drawBuilding(canvas, w * 0.26f, h * 0.25f, w * 0.30f, h * 0.21f, "Hallward\nLibrary");
        drawBuilding(canvas, w * 0.66f, h * 0.25f, w * 0.19f, h * 0.18f, "Physics\nBldg");
        drawBuilding(canvas, w * 0.27f, h * 0.50f, w * 0.27f, h * 0.15f, "Trent\nBuilding");
        drawBuilding(canvas, w * 0.66f, h * 0.50f, w * 0.19f, h * 0.14f, "SU");
        drawBuilding(canvas, w * 0.26f, h * 0.06f, w * 0.31f, h * 0.14f, "Portland\nBuilding");
        drawBuilding(canvas, w * 0.07f, h * 0.28f, w * 0.12f, h * 0.15f, "Arts");
        drawBuilding(canvas, w * 0.07f, h * 0.50f, w * 0.12f, h * 0.15f, "Sport");

        // User location pulse ring
        float userX = w * 0.46f, userY = h * 0.36f;
        if (pulseRadius > 0) {
            canvas.drawCircle(userX, userY, pulseRadius, pulsePaint);
        }

        // User dot
        canvas.drawCircle(userX, userY, 10 * dp, userDot);
        Paint whiteBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        whiteBorder.setColor(Color.WHITE);
        whiteBorder.setStyle(Paint.Style.STROKE);
        whiteBorder.setStrokeWidth(3 * dp);
        canvas.drawCircle(userX, userY, 10 * dp, whiteBorder);

        // Map pins
        for (MapPin pin : pins) {
            drawPin(canvas, pin);
        }
    }

    private void drawBuilding(Canvas canvas, float x, float y, float w, float h, String label) {
        RectF rect = new RectF(x, y, x + w, y + h);
        canvas.drawRoundRect(rect, 4 * dp, 4 * dp, bldgPaint);
        canvas.drawRoundRect(rect, 4 * dp, 4 * dp, bldgStroke);

        // Multi-line label
        String[] lines = label.split("\n");
        float lineH = bldgText.getFontSpacing();
        float startY = y + h / 2f - (lines.length - 1) * lineH / 2f + bldgText.getTextSize() / 3f;
        for (String line : lines) {
            canvas.drawText(line, x + w / 2f, startY, bldgText);
            startY += lineH;
        }
    }

    private void drawPin(Canvas canvas, MapPin pin) {
        float pinSize = 16 * dp;
        float headR   = pinSize;

        // Resolve colour
        int color;
        switch (pin.type) {
            case PIN_GREEN:  color = Color.parseColor("#4CAF50"); break;
            case PIN_ORANGE: color = Color.parseColor("#FF9800"); break;
            case PIN_DONE:   color = Color.parseColor("#2E7D32"); break;
            case PIN_LOCKED: color = Color.parseColor("#9E9E9E"); break;
            default:         color = Color.parseColor("#2196F3"); break;
        }

        pinFill.setColor(color);
        pinFill.setStyle(Paint.Style.FILL);

        // Shadow
        pinShadow.setColor(Color.argb(60, 0, 0, 0));
        canvas.drawCircle(pin.cx + 2 * dp, pin.cy + 4 * dp, headR, pinShadow);

        // Drop shape: circle + downward point
        canvas.drawCircle(pin.cx, pin.cy, headR, pinFill);
        Path point = new Path();
        point.moveTo(pin.cx - headR * 0.5f, pin.cy + headR * 0.5f);
        point.lineTo(pin.cx + headR * 0.5f, pin.cy + headR * 0.5f);
        point.lineTo(pin.cx, pin.cy + headR * 1.8f);
        point.close();
        canvas.drawPath(point, pinFill);

        // Label badge
        Paint badgeBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        badgeBg.setColor(Color.WHITE);
        Paint badgeText = new Paint(Paint.ANTI_ALIAS_FLAG);
        badgeText.setColor(Color.parseColor("#616161"));
        badgeText.setTextSize(8 * dp);
        badgeText.setTextAlign(Paint.Align.CENTER);
        badgeText.setTypeface(Typeface.DEFAULT_BOLD);

        float textW = badgeText.measureText(pin.label);
        float bx = pin.cx - textW / 2f - 6 * dp;
        float by = pin.cy + headR * 2.2f;
        RectF badgeRect = new RectF(bx, by, bx + textW + 12 * dp, by + 14 * dp);
        canvas.drawRoundRect(badgeRect, 7 * dp, 7 * dp, badgeBg);
        canvas.drawText(pin.label, pin.cx, by + 10 * dp, badgeText);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP && pinTappedListener != null) {
            float tx = event.getX(), ty = event.getY();
            float tapRadius = 28 * dp;
            for (MapPin pin : pins) {
                double dist = Math.hypot(tx - pin.cx, ty - pin.cy);
                if (dist <= tapRadius) {
                    pinTappedListener.onPinTapped(pin.type, pin.label);
                    // Brief scale animation
                    animate().scaleX(0.97f).scaleY(0.97f).setDuration(80)
                            .withEndAction(() -> animate().scaleX(1f).scaleY(1f).setDuration(120).start())
                            .start();
                    return true;
                }
            }
        }
        return super.onTouchEvent(event);
    }

    public void setOnPinTappedListener(OnPinTappedListener listener) {
        this.pinTappedListener = listener;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (pulseAnim != null) pulseAnim.cancel();
    }
}
