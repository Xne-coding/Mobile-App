package com.questlearn;

/*
 * QrCodeHelper — builds a square QR bitmap from a string (used for voucher display).
 * All drawing is done with ZXing; no camera involved here.
 */

import android.graphics.Bitmap;
import android.graphics.Color;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.util.EnumMap;
import java.util.Map;

public final class QrCodeHelper {

    private QrCodeHelper() {}

    public static Bitmap generate(String data, int sizePx) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN, 1);
            BitMatrix matrix = new QRCodeWriter()
                    .encode(data, BarcodeFormat.QR_CODE, sizePx, sizePx, hints);
            Bitmap bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565);
            for (int x = 0; x < sizePx; x++) {
                for (int y = 0; y < sizePx; y++) {
                    bmp.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bmp;
        } catch (Exception e) {
            return null;
        }
    }
}
