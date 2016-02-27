package in.omerjerk.libscreenshotter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;

/**
 * Created by omerjerk on 17/2/16.
 */
public class Screenshotter implements CodecCallback {

    private static final String TAG = "LibScreenshotter";

    private VirtualDisplay virtualDisplay;
    private Surface mSurface;

    private int width;
    private int height;

    private Context context;

    private int resultCode;
    private Intent data;
    private ScreenshotCallback cb;

    private static Screenshotter mInstance;

    private MediaProjection mMediaProjection;
    private EncoderDecoder codec;

    private int frameCount = 0;

    public static Screenshotter getInstance() {
        if (mInstance == null) {
            mInstance = new Screenshotter();
        }
        return mInstance;
    }

    private Screenshotter() {}

    /**
     * Takes the screenshot of whatever currently is on the default display.
     * @param resultCode The result code returned by the request for accessing MediaProjection permission
     * @param data The intent returned by the same request
     */
    public Screenshotter takeScreenshot(Context context, int resultCode, Intent data, ScreenshotCallback cb) {
        this.context = context;
        this.cb = cb;
        this.resultCode = resultCode;
        this.data = data;

        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) context
                .getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mMediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
        try {
            codec = new EncoderDecoder(width, height, this);
            mSurface = codec.createDisplaySurface();
            virtualDisplay = mMediaProjection.createVirtualDisplay("Screenshotter",
                    width, height, 50,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    mSurface, null, null);
            frameCount = 0;
            codec.run();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return this;
    }

    public Screenshotter setSize(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    @Override
    public void onBitmap(Bitmap b) {
        cb.onScreenshot(b);
        codec.stop();
    }

    private Bitmap createBitmap(byte[] data) {
        int nrOfPixels = data.length / 3; // Three bytes per pixel.
        int pixels[] = new int[nrOfPixels];
        for(int i = 0; i < nrOfPixels; i++) {
            int r = data[3*i];
            int g = data[3*i + 1];
            int b = data[3*i + 2];
            pixels[i] = Color.rgb(r, g, b);
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
    }
}
