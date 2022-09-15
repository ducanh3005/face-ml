
package com.ynsuper.arfacesohasdk.camera;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.Surface;


import com.tenginekit.engine.common.TenginekitPoint;
import com.tenginekit.engine.core.ImageConfig;
import com.tenginekit.engine.core.TengineKitSdk;
import com.tenginekit.engine.face.Face;
import com.tenginekit.engine.face.FaceConfig;
import com.tenginekit.engine.insightface.InsightFaceConfig;
import com.ynsuper.arfacesohasdk.facepoint.LandmarkEngine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EGLCamera implements PreviewCallback, Camera.AutoFocusCallback {
    private static final String TAG = "EGLCamera";

    private Activity mActivity;
    private Camera mCamera;
    private Parameters mParameters;
    private CameraInfo mCameraInfo = new CameraInfo();
    private int mCameraId = CameraInfo.CAMERA_FACING_FRONT;
    private int mPreviewWidth = 1280;
    private int mPreviewHeight = 720;
    private float mPreviewScale = mPreviewHeight * 1f / mPreviewWidth;
    private int mOrientation;
    protected byte[] mNV21Bytes;



    public EGLCamera(Activity activity) {
        mActivity = activity;
    }

    public void openCamera() {
        Log.d(TAG, "openCamera cameraId: " + mCameraId);
        mCamera = Camera.open(mCameraId);
        Camera.getCameraInfo(mCameraId, mCameraInfo);
        initConfig();
        mCamera.setPreviewCallback(this);
        setDisplayOrientation();
    }

    public void releaseCamera() {
        if (mCamera != null) {
            Log.v(TAG, "releaseCamera");
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        LandmarkEngine.getInstance().clearAll();
    }

    public void startPreview(SurfaceTexture surface) {
        if (mCamera != null) {
            Log.v(TAG, "startPreview");
            try {
                mCamera.setPreviewTexture(surface);
            } catch (IOException e) {
                Log.e(TAG, "setPreviewTexture fail." + e.getMessage());
            }
            mCamera.startPreview();
        }
    }

    public void stopPreview() {
        if (mCamera != null) {
            Log.v(TAG, "stopPreview");
            mCamera.stopPreview();
        }
    }

    public boolean isFrontCamera() {
        return mCameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT;
    }

    public boolean isBackCamera() {
        return mCameraInfo.facing == CameraInfo.CAMERA_FACING_BACK;
    }

    private void initConfig() {
        Log.v(TAG, "initConfig");
        try {
            mParameters = mCamera.getParameters();
            /**
             * If the camera doesn't support these parameters,
             * they will all go wrong, so make sure to check if they are supported
             */
            List<String> supportedFlashModes = mParameters.getSupportedFlashModes();
            if (supportedFlashModes != null && supportedFlashModes.contains(Parameters.FLASH_MODE_OFF)) {
                mParameters.setFlashMode(Parameters.FLASH_MODE_OFF); // Set focus mode
            }
            List<String> supportedFocusModes = mParameters.getSupportedFocusModes();
            if (supportedFocusModes != null && supportedFocusModes.contains(Parameters.FOCUS_MODE_AUTO)) {
                mParameters.setFocusMode(Parameters.FOCUS_MODE_AUTO); // Set focus mode
            }
            mParameters.setPreviewFormat(ImageFormat.NV21); // Set preview image format
            mParameters.setPictureFormat(ImageFormat.JPEG); // Set the format of photos
            mParameters.setExposureCompensation(0); // Set the exposure intensity
            Size previewSize = getSuitableSize(mParameters.getSupportedPreviewSizes());
            mPreviewWidth = previewSize.width;
            mPreviewHeight = previewSize.height;
            mParameters.setPreviewSize(mPreviewWidth, mPreviewHeight); // Set preview image size
            Log.d(TAG, "previewWidth: " + mPreviewWidth + ", previewHeight: " + mPreviewHeight);
            Size pictureSize = getSuitableSize(mParameters.getSupportedPictureSizes());
            mParameters.setPictureSize(pictureSize.width, pictureSize.height);
            Log.d(TAG, "pictureWidth: " + pictureSize.width + ", pictureHeight: " + pictureSize.height);
            mCamera.setParameters(mParameters); // Add parameters to the camera
        } catch (Exception e) {
            Log.e(TAG, "initConfig fail." + e.getMessage());
        }
    }

    private Size getSuitableSize(List<Size> sizes) {
        /**
         * The minimum difference, the initial value
         * should be set to a large point to
         * ensure that it will be reset later in the calculation
         */
        int minDelta = Integer.MAX_VALUE;
        int index = 0; // The index coordinates corresponding to the minimum difference
        for (int i = 0; i < sizes.size(); i++) {
            Size size = sizes.get(i);
            Log.v(TAG, "SupportedSize, width: " + size.width + ", height: " + size.height);
            // First determine if the proportion is equal
            if (Math.abs(size.width * mPreviewScale - size.height) < 0.001) {
                int delta = Math.abs(mPreviewWidth - size.width);
                if (delta == 0) {
                    return size;
                }
                if (minDelta > delta) {
                    minDelta = delta;
                    index = i;
                }
            }
        }
        return sizes.get(index);
    }

    /**
     * setDisplayOrientation
     */
    private void setDisplayOrientation() {
        int rotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
            default:
                degrees = 0;
                break;
        }
        int result;
        if (mCameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
            result = (mCameraInfo.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {
            result = (mCameraInfo.orientation - degrees + 360) % 360;
        }
        mOrientation = result;
        mCamera.setDisplayOrientation(result);
    }

    @Override
    public void onPreviewFrame(final byte[] imgData, final Camera camera) {
        int width = mPreviewWidth;
        int height = mPreviewHeight;


        try {
            if (mNV21Bytes == null) {
                Camera.Size previewSize = camera.getParameters().getPreviewSize();
                mPreviewWidth = previewSize.height;
                mPreviewHeight = previewSize.width;
                mNV21Bytes = new byte[mPreviewHeight * mPreviewWidth];
//                onPreviewSizeChosen(new Size(previewSize.width, previewSize.height));
            }
        } catch (final Exception e) {

        }
        mNV21Bytes = imgData;
        long startTime = System.currentTimeMillis();
        //Set in the same direction
        if (isFrontCamera()) {
            mOrientation = 0;
        } else {
            mOrientation = 2;
        }

        // SetConfig

            // Set the number of faces
        FaceConfig config = new FaceConfig();
        config.detect = true;
        config.landmark2d = true;
        config.attribute = true;
        config.eyeIris = true;
        config.maxFaceNum = 1;

        ImageConfig imageConfig = new ImageConfig();
        imageConfig.data = mNV21Bytes;
        imageConfig.degree = 270;
        imageConfig.mirror = true;
        imageConfig.height = height;
        imageConfig.width = width;
        imageConfig.format = ImageConfig.FaceImageFormat.YUV;

        InsightFaceConfig configIs = new InsightFaceConfig();
        configIs.scrfd = true;
        configIs.recognition= true;
        configIs.video = false;
        configIs.registered = true;

        Face[] faces = TengineKitSdk.getInstance().detectFace(imageConfig, config);


        LandmarkEngine.getInstance().setOrientation(270);
        LandmarkEngine.getInstance().setNeedFlip(true);



        if (faces != null && faces.length > 0) {
            for (int index = 0; index < faces.length; index++) {
                // 获取姿态角信息
                Face face = faces[index];
                Log.d("DuyHop", "The luoi :" + face.mouthBigOpen );
                Log.d("DuyHop", "khong the:" + face.mouthClose );

                Face myFace = LandmarkEngine.getInstance().getOneFace(index);
                myFace.landmark = new float[424];
                myFace.mouthBigOpen = face.mouthBigOpen;

                // 预览状态下，宽高交换



                myFace.headX = face.headX;
                if (isBackCamera()) {
                    myFace.headY = -face.headY;
                } else {
                    myFace.headY = face.headY;
                }
                myFace.headZ = (float) (Math.PI / 2.0f - face.headZ);
//                    if (faceTrackParam.previewTrack) {
//
//                        if (faceTrackParam.isBackCamera) {
//                            oneFace.headZ = (float) (Math.PI / 2.0f + oneFace.headZ);
//                        } else {
//                            oneFace.headZ = (float) (Math.PI / 2.0f - face.headZ);
//                        }
//                    }

                ArrayList<TenginekitPoint> landmark = new ArrayList<>();

                // 获取一个人的关键点坐标
                  /*  if (oneFace.landmark == null || oneFace.landmark.length != face.landmark.length * 2) {
                        oneFace.landmark = new float[212];
                    }*/
                for (int i = 0; i < 212; i++) {
                    landmark.add(
                            new TenginekitPoint(
                                    face.landmark[2 * i],
                                    face.landmark[2 * i + 1]
                            ).rotateByOrientation(0, 720, 1280)
                    );

//                         orientation = 0、3 表示竖屏，1、2 表示横屏
                    float x = (landmark.get(i).X / height) * 2 - 1;
                    float y = (landmark.get(i).Y / width) * 2 - 1;
                    float[] point = new float[] {-x, -y};

                    // 顶点坐标
                    if (true) {
                        if (isBackCamera()) {
                            myFace.landmark[2 * i] = (float) (point[0]);
                        } else {
                            myFace.landmark[2 * i] = (float) (-point[0]);
                        }
                    } else { // 非预览状态下，左右不需要翻转
                        myFace.landmark[2 * i] = point[0];
                    }
                    myFace.landmark[2 * i + 1] = point[1];
                }
                // 插入人脸对象
                LandmarkEngine.getInstance().putOneFace(index, myFace);
            }
        }

        long endTime = System.currentTimeMillis();
        Log.d("TAG", "Face detect time: " + String.valueOf(endTime - startTime));
    }


    public void takePicture(Camera.PictureCallback pictureCallback) {
        mCamera.takePicture(null, null, pictureCallback);
    }

    public void switchCamera() {
        mCameraId ^= 1; // Change the camera first
        releaseCamera();
        openCamera();
    }

//    public void setDetector(MLFaceAnalyzer detector) {
//        this.detector = detector;
//    }

    public void focusOnPoint(int x, int y, int width, int height) {
        Log.v(TAG, "touch point (" + x + ", " + y + ")");
        if (mCamera == null) {
            return;
        }
        Parameters parameters = mCamera.getParameters();
        if (parameters.getMaxNumFocusAreas() > 0) {
            int length = Math.min(width, height) >> 3;
            int left = x - length;
            int top = y - length;
            int right = x + length;
            int bottom = y + length;
            left = left * 2000 / width - 1000;
            top = top * 2000 / height - 1000;
            right = right * 2000 / width - 1000;
            bottom = bottom * 2000 / height - 1000;
            left = left < -1000 ? -1000 : left;
            top = top < -1000 ? -1000 : top;
            right = right > 1000 ? 1000 : right;
            bottom = bottom > 1000 ? 1000 : bottom;
            Log.d(TAG, "focus area (" + left + ", " + top + ", " + right + ", " + bottom + ")");
            ArrayList<Camera.Area> areas = new ArrayList<>();
            areas.add(new Camera.Area(new Rect(left, top, right, bottom), 600));
            parameters.setFocusAreas(areas);
        }
        try {
            mCamera.cancelAutoFocus();
            mCamera.setParameters(parameters);
            mCamera.autoFocus(this);
        } catch (Exception e) {
            Log.e(TAG, "Fail to set mCamera." + e.getMessage());
        }
    }

    public void handleZoom(boolean isZoomIn) {
        if (mParameters.isZoomSupported()) {
            int maxZoom = mParameters.getMaxZoom();
            int zoom = mParameters.getZoom();
            if (isZoomIn && zoom < maxZoom) {
                zoom++;
            } else if (zoom > 0) {
                zoom--;
            }
            Log.d(TAG, "handleZoom: zoom: " + zoom);
            mParameters.setZoom(zoom);
            mCamera.setParameters(mParameters);
        } else {
            Log.i(TAG, "zoom not supported");
        }
    }

    public int getPreviewWidth() {
        return mPreviewWidth;
    }

    public int getPreviewHeight() {
        return mPreviewHeight;
    }

    @Override
    public void onAutoFocus(boolean success, Camera camera) {
        Log.d(TAG, "onAutoFocus: " + success);
        //OnAutoFocus code add
    }
}
