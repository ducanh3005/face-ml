
package com.ynsuper.arfacesohasdk.camera;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLException;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;

import com.gravity.face.core.ResultListener;
import com.gravity.face.landmark.FaceMeshDetection;
import com.gravity.face.landmark.models.FaceMeshOptions;
import com.gravity.face.landmark.models.FaceMeshResult;
import com.ynsuper.arfacesohasdk.facefilter.BaseFilter;
import com.ynsuper.arfacesohasdk.facefilter.CameraFilter;
import com.ynsuper.arfacesohasdk.facefilter.FacePointsFilter;
import com.ynsuper.arfacesohasdk.facefilter.FaceStickerFilter;
import com.ynsuper.arfacesohasdk.util.OpenGLUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class CameraGLSurfaceView extends GLSurfaceView
        implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {
    private static final String TAG = "CameraGLSurfaceView";
    private final FaceMeshDetection faceMeshDetection;

    // Whether to display face key points
    public boolean drawFacePoints;

    protected float[] cubeVertices = {
            -1.0f, -1.0f,  // 0 bottom left
            1.0f, -1.0f,  // 1 bottom right
            -1.0f, 1.0f,  // 2 top left
            1.0f, 1.0f,  // 3 top right
    };

    protected float[] textureVertices = {
            0.0f, 0.0f,     // 0 left bottom
            1.0f, 0.0f,     // 1 right bottom
            0.0f, 1.0f,     // 2 left top
            1.0f, 1.0f      // 3 right top
    };

    // Matrix for screen display
    private final float[] mMatrix = new float[16];

    private Context context;
    private EGLCamera mEGLCamera;
    private SurfaceTexture mSurfaceTexture;

    private int mRatioWidth = 0;
    private int mRatioHeight = 0;
    private float mOldDistance;

    private int[] mTextures;
    // Used to display clipped texture vertex buffers
    private FloatBuffer mDisplayVertexBuffer;
    private FloatBuffer mDisplayTextureBuffer;
    private FloatBuffer mVertexBuffer;
    private FloatBuffer mTextureBuffer;

    private BaseFilter screenFilter;
    private CameraFilter cameraFilter;
    private FaceStickerFilter stickerFilter;
    private FacePointsFilter facePointsFilter;

    public CameraGLSurfaceView(Context context) {
        this(context, null);
    }

    public CameraGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        if (context instanceof Activity) {
            mEGLCamera = new EGLCamera((Activity) context);
        }
        setEGLContextClientVersion(3);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        mDisplayVertexBuffer = OpenGLUtils.createFloatBuffer(cubeVertices);
        mDisplayTextureBuffer = OpenGLUtils.createFloatBuffer(textureVertices);
        mVertexBuffer = OpenGLUtils.createFloatBuffer(cubeVertices);
        mTextureBuffer = OpenGLUtils.createFloatBuffer(textureVertices);
         faceMeshDetection = new FaceMeshDetection(context,new FaceMeshOptions.Builder().build());
        faceMeshDetection.setResultListener(new ResultListener<FaceMeshResult>() {
            @Override
            public void run(FaceMeshResult result) {
                // Return 468 points
                Log.d("Ynsuper","FaceMeshResult size: "+result.getFacesMesh().size());
            }
        });
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        mTextures = new int[1];
        mTextures[0] = OpenGLUtils.createOESTexture();
        mSurfaceTexture = new SurfaceTexture(mTextures[0]);
        mSurfaceTexture.setOnFrameAvailableListener(this);

        //Enter the samplerExternalOES into the texture
        cameraFilter = new CameraFilter(this.context);

        //Set the face sticker path under assets directory
        String folderPath = "sticker1";
        stickerFilter = new FaceStickerFilter(this.context, folderPath);

        //Responsible for drawing images onto the screen
        screenFilter = new BaseFilter(this.context);

        facePointsFilter = new FacePointsFilter(this.context);
        mEGLCamera.openCamera();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d(TAG, "onSurfaceChanged. width: " + width + ", height: " + height);
        int previewWidth = mEGLCamera.getPreviewWidth();
        int previewHeight = mEGLCamera.getPreviewHeight();
        if (width > height) {
            setAspectRatio(previewWidth, previewHeight);
        } else {
            setAspectRatio(previewHeight, previewWidth);
        }
        // Set the screen size, create a FrameBuffer, and set the display size
        cameraFilter.onInputSizeChanged(previewWidth, previewHeight);
        cameraFilter.initFrameBuffer(previewWidth, previewHeight);
        cameraFilter.onDisplaySizeChanged(width, height);

        stickerFilter.onInputSizeChanged(previewHeight, previewWidth);
        stickerFilter.initFrameBuffer(previewHeight, previewWidth);
        stickerFilter.onDisplaySizeChanged(width, height);

        screenFilter.onInputSizeChanged(previewWidth, previewHeight);
        screenFilter.initFrameBuffer(previewWidth, previewHeight);
        screenFilter.onDisplaySizeChanged(width, height);

        facePointsFilter.onInputSizeChanged(previewHeight, previewWidth);
        facePointsFilter.onDisplaySizeChanged(width, height);
        mEGLCamera.startPreview(mSurfaceTexture);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Clear screen and deep cache
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
        /// Update to get an image
        mSurfaceTexture.updateTexImage();
        //Get transform matrix from SurfaceTexture
        mSurfaceTexture.getTransformMatrix(mMatrix);
        // Set the camera display conversion matrix
        cameraFilter.setTextureTransformMatrix(mMatrix);

        //Draw the texture of camera
        int textureId = cameraFilter.drawFrameBuffer(mTextures[0], mVertexBuffer, mTextureBuffer);
        //Draw the texture of stickers
        textureId = stickerFilter.drawFrameBuffer(textureId, mVertexBuffer, mTextureBuffer);
        // Draw to the screen
        screenFilter.drawFrame(textureId, mDisplayVertexBuffer, mDisplayTextureBuffer);
        if (drawFacePoints) {
            facePointsFilter.drawFrame(textureId, mDisplayVertexBuffer, mDisplayTextureBuffer);
        }
        // Enable get bitmap and detect by TFlite => return Face Mesh with 468 points
        Bitmap bitmap = createBitmapFromGLSurface();
        faceMeshDetection.detect(bitmap);
    }


    public Bitmap createBitmapFromGLSurface() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int h = displayMetrics.heightPixels;
        int w = displayMetrics.widthPixels;


        int bitmapBuffer[] = new int[w * h];
        int bitmapSource[] = new int[w * h];
        IntBuffer intBuffer = IntBuffer.wrap(bitmapBuffer);
        intBuffer.position(0);
        try {


            GLES20.glReadPixels(0, 0, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, intBuffer);
            int offset1, offset2;
            for (int i = 0; i < h; i++) {
                offset1 = i * w;
                offset2 = (h - i - 1) * w;
                for (int j = 0; j < w; j++) {
                    int texturePixel = bitmapBuffer[offset1 + j];
                    int blue = (texturePixel >> 16) & 0xff;
                    int red = (texturePixel << 16) & 0x00ff0000;
                    int pixel = (texturePixel & 0xff00ff00) | red | blue;
                    bitmapSource[offset2 + j] = pixel;
                }
            }
        } catch (GLException e) {
            e.printStackTrace();
            return null;
        }
        return Bitmap.createBitmap(bitmapSource, w, h, Bitmap.Config.ARGB_8888);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        requestRender();
    }

    private void setAspectRatio(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        mRatioWidth = width;
        mRatioHeight = height;
        post(new Runnable() {
            @Override
            public void run() {
                requestLayout(); // must run in UI thread
            }
        });
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height);
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
            } else {
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() == 1) {
            mEGLCamera.focusOnPoint((int) event.getX(), (int) event.getY(), getWidth(), getHeight());
            return true;
        }
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_POINTER_DOWN:
                mOldDistance = getFingerSpacing(event);
                break;
            case MotionEvent.ACTION_MOVE:
                float newDistance = getFingerSpacing(event);
                if (newDistance > mOldDistance) {
                    mEGLCamera.handleZoom(true);
                } else if (newDistance < mOldDistance) {
                    mEGLCamera.handleZoom(false);
                }
                mOldDistance = newDistance;
                break;
            default:
                break;
        }
        return super.onTouchEvent(event);
    }

    private static float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    public void showFacePoints(boolean show) {
        this.drawFacePoints = show;
    }

    public EGLCamera getEGLCamera() {
        return mEGLCamera;
    }
}
