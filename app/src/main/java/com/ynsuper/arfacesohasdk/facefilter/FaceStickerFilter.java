package com.ynsuper.arfacesohasdk.facefilter;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.util.Log;


import com.tenginekit.engine.face.Face;
import com.ynsuper.arfacesohasdk.facepoint.LandmarkEngine;
import com.ynsuper.arfacesohasdk.util.OpenGLUtils;

import org.json.JSONException;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public class FaceStickerFilter extends BaseFilter {
    private static final float PROJECTION_SCALE = 2.0f;

    protected float[] cubeVertices = {
            -1.0f, -1.0f,  // 0 bottom left
            1.0f, -1.0f,  // 1 bottom right
            -1.0f, 1.0f,  // 2 top left
            1.0f, 1.0f,  // 3 top rtight
    };

    protected float[] textureVerticesFlipx = {
            1.0f, 0.0f,     // 0 right bottom
            0.0f, 0.0f,     // 1 left  bottom
            1.0f, 1.0f,     // 2 right top
            0.0f, 1.0f      // 3 left  top
    };

    protected List<FaceStickerJson> mStickerList;
    protected List<FaceStickerLoader> mStickerLoaderList;

    private int mMVPMatrixHandle;

    private float[] mProjectionMatrix = new float[16];
    private float[] mViewMatrix = new float[16];
    private float[] mModelMatrix = new float[16];
    private float[] mMVPMatrix = new float[16];

    private float mRatio;

    private FloatBuffer mVertexBuffer;
    private FloatBuffer mTextureBuffer;

    private float[] mStickerVertices = new float[8];

    private Context context;

    public FaceStickerFilter(Context context, String folderPath) {
        super(context, OpenGLUtils.getShaderFromAssets(context, "shader/vertex_sticker_normal.glsl"),
                OpenGLUtils.getShaderFromAssets(context, "shader/fragment_sticker_normal.glsl"));

        this.context = context;
        try {
            mStickerList = ResourceDecode.decodeStickerData(context, folderPath + "/" + "json");
        } catch (IOException | JSONException e) {
            Log.d(TAG, "IOException or JSONException: ");
        }

        mStickerLoaderList = new ArrayList<>();

        if (mStickerList != null) {
            for (int i = 0; i < mStickerList.size(); i++) {
                if (mStickerList.get(i) instanceof FaceStickerJson) {
                    String path = folderPath + "/" + mStickerList.get(i).stickerName;
                    mStickerLoaderList.add(new FaceStickerLoader(this, mStickerList.get(i), path));
                }
            }
        }
        initMatrix();
        initBuffer();
    }

    /**
     * initMatrix
     */
    private void initMatrix() {
        Matrix.setIdentityM(mProjectionMatrix, 0);
        Matrix.setIdentityM(mViewMatrix, 0);
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.setIdentityM(mMVPMatrix, 0);
    }

    /**
     * initBuffer
     */
    private void initBuffer() {
        releaseBuffer();
        mVertexBuffer = OpenGLUtils.createFloatBuffer(cubeVertices);
        mTextureBuffer = OpenGLUtils.createFloatBuffer(textureVerticesFlipx);
    }

    /**
     * releaseBuffer
     */
    private void releaseBuffer() {
        if (mVertexBuffer != null) {
            mVertexBuffer.clear();
            mVertexBuffer = null;
        }
        if (mTextureBuffer != null) {
            mTextureBuffer.clear();
            mTextureBuffer = null;
        }
    }

    @Override
    public void initProgramHandle() {
        super.initProgramHandle();
        if (mProgramHandle != OpenGLUtils.GL_NOT_INIT) {
            mMVPMatrixHandle = GLES30.glGetUniformLocation(mProgramHandle, "uMVPMatrix");
        } else {
            mMVPMatrixHandle = OpenGLUtils.GL_NOT_INIT;
        }
    }

    @Override
    public void onInputSizeChanged(int width, int height) {
        super.onInputSizeChanged(width, height);
        mRatio = (float) width / height;
        Matrix.frustumM(mProjectionMatrix, 0, -mRatio, mRatio, -1.0f, 1.0f, 3.0f, 9.0f);
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, 6.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
    }

    @Override
    public void release() {
        super.release();
        releaseBuffer();
    }

    @Override
    public boolean drawFrame(int textureId, FloatBuffer vertexBuffer, FloatBuffer textureBuffer) {
        int stickerTexture = drawFrameBuffer(textureId, vertexBuffer, textureBuffer);
        return super.drawFrame(stickerTexture, vertexBuffer, textureBuffer);
    }

    @Override
    public int drawFrameBuffer(int textureId, FloatBuffer vertexBuffer, FloatBuffer textureBuffer) {
        Matrix.setIdentityM(mMVPMatrix, 0);
        super.drawFrameBuffer(textureId, vertexBuffer, textureBuffer);

        if (mStickerLoaderList.size() > 0 && LandmarkEngine.getInstance().hasFace()) {
            int faceCount = Math.min(LandmarkEngine.getInstance().getFaceSize(),
                    mStickerLoaderList.get(0).getMaxCount());
            for (int faceIndex = 0; faceIndex < faceCount; faceIndex++) {
                Face eglFace = LandmarkEngine.getInstance().getOneFace(faceIndex);
                for (int stickerIndex = 0; stickerIndex < mStickerLoaderList.size(); stickerIndex++) {
                    synchronized (this) {
                        mStickerLoaderList.get(stickerIndex).updateStickerTexture(this.context);
                        calculateStickerVertices(mStickerLoaderList.get(stickerIndex).getStickerData(),
                                eglFace);
                        super.drawFrameBuffer(mStickerLoaderList.get(stickerIndex).getStickerTexture(),
                                mVertexBuffer, mTextureBuffer);
                    }
                }
            }
            GLES30.glFlush();
        }
        return mFrameBufferTextures[0];
    }

    @Override
    public void onDrawFrameBegin() {
        super.onDrawFrameBegin();
        if (mMVPMatrixHandle != OpenGLUtils.GL_NOT_INIT) {
            GLES30.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        }

        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendEquation(GLES30.GL_FUNC_ADD);
        GLES30.glBlendFuncSeparate(GLES30.GL_ONE, GLES30.GL_ONE_MINUS_SRC_ALPHA, GLES30.GL_ONE, GLES30.GL_ONE);
    }

    @Override
    public void onDrawFrameAfter() {
        super.onDrawFrameAfter();
        GLES30.glDisable(GLES30.GL_BLEND);
    }

    private void calculateStickerVertices(FaceStickerJson stickerData, Face oneFace) {
        if (oneFace == null || oneFace.landmark == null) {
            return;
        }
        // 1. Calculate the center point and vertex coordinates of the sticker
        // 备注：Since the bottom and top set by frustumM are -1.0 and 1.0, for the convenience of calculation, the height is directly used as the reference value to calculate
        // 1.1、Calculate the width and height of the sticker relative to the face
        float stickerWidth = (float) OpenGLUtils.getDistance(
                (oneFace.landmark[stickerData.startIndex * 2] * 0.5f + 0.5f) * mImageWidth,
                (oneFace.landmark[stickerData.startIndex * 2 + 1] * 0.5f + 0.5f) * mImageHeight,
                (oneFace.landmark[stickerData.endIndex * 2] * 0.5f + 0.5f) * mImageWidth,
                (oneFace.landmark[stickerData.endIndex * 2 + 1] * 0.5f + 0.5f) * mImageHeight) * stickerData.baseScale;
        float stickerHeight = stickerWidth * (float) stickerData.height / (float) stickerData.width;

        // 1.2、Calculate the coordinates of the center point according to the parameters of the sticker
        float centerX = 0.0f;
        float centerY = 0.0f;
        for (int i = 0; i < stickerData.centerIndexList.length; i++) {
            centerX += (oneFace.landmark[stickerData.centerIndexList[i] * 2] * 0.5f + 0.5f) * mImageWidth;
            centerY += (oneFace.landmark[stickerData.centerIndexList[i] * 2 + 1] * 0.5f + 0.5f) * mImageHeight;
        }
        centerX /= (float) stickerData.centerIndexList.length;
        centerY /= (float) stickerData.centerIndexList.length;
        centerX = centerX / mImageHeight * PROJECTION_SCALE;
        centerY = centerY / mImageHeight * PROJECTION_SCALE;
        // Find the real center point vertex coordinates. Since frustum M sets the aspect ratio, the ndc coordinates need to become m Ratio: 1 when calculating, and it needs to be converted here.
        float ndcCenterX = (centerX - mRatio) * PROJECTION_SCALE;
        float ndcCenterY = (centerY - 1.0f) * PROJECTION_SCALE;

        // 1.4、The length of the width and height of the sticker in the ndc coordinate system
        float ndcStickerWidth = stickerWidth / mImageHeight * PROJECTION_SCALE;
        float ndcStickerHeight = ndcStickerWidth * (float) stickerData.height / (float) stickerData.width;

        // 1.5、Find the offset ndc coordinates according to the sticker parameters
        float offsetX = (stickerWidth * stickerData.offsetX) / mImageHeight * PROJECTION_SCALE;
        float offsetY = (stickerHeight * stickerData.offsetY) / mImageHeight * PROJECTION_SCALE;

        // 1.6、The ndc coordinate of the sticker's anchor point with offset, that is, the position of the actual sticker's center point in the vertex coordinate system of Open GL
        float anchorX = ndcCenterX + offsetX * PROJECTION_SCALE;
        float anchorY = ndcCenterY + offsetY * PROJECTION_SCALE;

        // 1.7, According to the previous anchor point, calculate the actual vertex coordinates of the sticker
        mStickerVertices[0] = anchorX - ndcStickerWidth;
        mStickerVertices[1] = anchorY - ndcStickerHeight;
        mStickerVertices[2] = anchorX + ndcStickerWidth;
        mStickerVertices[3] = anchorY - ndcStickerHeight;
        mStickerVertices[4] = anchorX - ndcStickerWidth;
        mStickerVertices[5] = anchorY + ndcStickerHeight;
        mStickerVertices[6] = anchorX + ndcStickerWidth;
        mStickerVertices[7] = anchorY + ndcStickerHeight;
        mVertexBuffer.clear();
        mVertexBuffer.position(0);
        mVertexBuffer.put(mStickerVertices);

        // Step 2. Calculate the total transformation matrix of perspective transformation according to the face attitude angle
        // 2.1、Translate the Z axis to the center point of the sticker, because the sticker model matrix needs to do attitude angle transformation
        // The translation is mainly to prevent the sticker from being deformed
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, ndcCenterX, ndcCenterY, 0);

        // 2.2、Sticker attitude angle rotation
        // The pitch angle returned by the key points of the TODO face doesn't seem right? ? The pitch angle value given by the SDK is too small. For example, the actual angle of the pitch when looking up and down is 30 degrees, and the result returned by the SDK is only a dozen degrees. We will see how to optimize it later.
        float pitchAngle = -oneFace.headX * 1.5f;
        float yawAngle = oneFace.headY * 1.5f;
        float rollAngle = (oneFace.headZ + 180f);
        // The left and right head turning is limited to no more than 50°, and the deviation caused by the SDK of the key points of the face is destroyed.
        if (Math.abs(yawAngle) > 50) {
            yawAngle = (yawAngle / Math.abs(yawAngle)) * 50;
        }
        //y the SDK of the key points of the face
        if (Math.abs(pitchAngle) > 30) {
            pitchAngle = (pitchAngle / Math.abs(pitchAngle)) * 30;
        }
        // Sticker attitude angle transformation, giving priority to z-axis transformation to eliminate the influence of the rotation angle of the mobile phone, otherwise the sticker will be deformed when turning, looking up, and bowing.
        Matrix.rotateM(mModelMatrix, 0, rollAngle, 0, 0, 1);
        Matrix.rotateM(mModelMatrix, 0, yawAngle, 0, 1, 0);
        Matrix.rotateM(mModelMatrix, 0, pitchAngle, 1, 0, 0);

        // 2.4. Translate the Z-axis back to the position of the originally constructed viewing cone, that is, you need to translate the coordinate Z-axis back to the center of the screen. At this time, it is the actual model matrix of the sticker.
        Matrix.translateM(mModelMatrix, 0, -ndcCenterX, -ndcCenterY, 0);

        // 2.5. Calculate the total transformation matrix. The matrix calculation of MVPMatrix is MVPMatrix = ProjectionMatrix ViewMatrix ModelMatrix
        // Remarks: The results obtained by multiplying matrices in different orders are different. Different orders will lead to inconsistent calculation processes. I hope everyone should pay attention to this.
        Matrix.setIdentityM(mMVPMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mMVPMatrix, 0, mModelMatrix, 0);
    }
}



