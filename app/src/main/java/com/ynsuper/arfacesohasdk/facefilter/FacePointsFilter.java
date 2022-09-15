/**
 * Copyright 2018 cain.huang@outlook.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * 2020.8.13-Changed name from GLImageFacePointsFilter to FacePointsFilter, and adjusted the architecture
 * Huawei Technologies Co., Ltd<liqiang220@huawei.com></>.
 */
package com.ynsuper.arfacesohasdk.facefilter;

import android.content.Context;
import android.opengl.GLES30;
import android.text.TextUtils;
import android.util.SparseArray;

import com.tenginekit.engine.face.Face;
import com.ynsuper.arfacesohasdk.facepoint.EGLFace;
import com.ynsuper.arfacesohasdk.facepoint.LandmarkEngine;
import com.ynsuper.arfacesohasdk.util.OpenGLUtils;

import java.nio.FloatBuffer;


public class FacePointsFilter extends BaseFilter {
    private static final String VertexShader = "" +
            "attribute vec4 aPosition;\n" +
            "void main() {\n" +
            "    gl_Position = aPosition;\n" +
            "    gl_PointSize = 8.0;\n" +
            "}";

    private static final String FragmentShader = "" +
            "precision mediump float;\n" +
            "uniform vec4 color;\n" +
            "void main() {\n" +
            "    gl_FragColor = color;\n" +
            "}";

    // 关键点滤镜
    private final float color[] = {1.0f, 0.0f, 0.0f, 1.0f};

    private int mColorHandle;
    private int mPointCount = 212;
    private float[] mPoints;
    private FloatBuffer mPointVertexBuffer;

    public FacePointsFilter(Context context) {
        this(context, VertexShader, FragmentShader);
    }

    public FacePointsFilter(Context context, String vertexShader, String fragmentShader) {
        super(context, vertexShader, fragmentShader);
        mPoints = new float[mPointCount * 2];
        mPointVertexBuffer = OpenGLUtils.createFloatBuffer(mPoints);
    }

    @Override
    public void initProgramHandle() {
        // 只有在shader都不为空的情况下才初始化程序句柄
        if (!TextUtils.isEmpty(mVertexShader) && !TextUtils.isEmpty(mFragmentShader)) {
            mProgramHandle = OpenGLUtils.createProgram(mVertexShader, mFragmentShader);
            mPositionHandle = GLES30.glGetAttribLocation(mProgramHandle, "aPosition");
            mColorHandle = GLES30.glGetUniformLocation(mProgramHandle, "color");
            mIsInitialized = true;
        } else {
            mPositionHandle = OpenGLUtils.GL_NOT_INIT;

            mColorHandle = OpenGLUtils.GL_NOT_INIT;
            mIsInitialized = false;
        }
        mTextureCoordinateHandle = OpenGLUtils.GL_NOT_INIT;
        mInputTextureHandle = OpenGLUtils.GL_NOT_TEXTURE;
    }

    @Override
    public void onInputSizeChanged(int width, int height) {
        super.onInputSizeChanged(width, height);
    }

    @Override
    public boolean drawFrame(int textureId, FloatBuffer vertexBuffer, FloatBuffer textureBuffer) {
        // 没有初始化、滤镜不可用时直接返回
        if (!mIsInitialized || !mFilterEnable) {
            return false;
        }
        // 设置视口大小
        GLES30.glViewport(0, 0, mDisplayWidth, mDisplayHeight);
        // 使用当前的program
        GLES30.glUseProgram(mProgramHandle);
        // 运行延时任务
        runPendingOnDrawTasks();
        // 使能顶点句柄
        GLES30.glEnableVertexAttribArray(mPositionHandle);
        // 绑定颜色
        GLES30.glUniform4fv(mColorHandle, 1, color, 0);
        onDrawFrameBegin();
        // 逐个顶点绘制出来
        synchronized (this) {
            if (LandmarkEngine.getInstance().getFaceSize() > 0) {
                Face eglFace = LandmarkEngine.getInstance().getOneFace(0);
                if (eglFace.landmark != null) {
                    for (int i = 0; i < 212; i++) {
                        mPoints[2 * i] = eglFace.landmark[i * 2];
                        mPoints[2 * i + 1] = eglFace.landmark[i * 2 + 1];
                    }
                    mPointVertexBuffer.clear();
                    mPointVertexBuffer.put(mPoints, 0, mPoints.length);
                    mPointVertexBuffer.position(0);
                    GLES30.glVertexAttribPointer(mPositionHandle, 2,
                            GLES30.GL_FLOAT, false, 8, mPointVertexBuffer);
                    GLES30.glDrawArrays(GLES30.GL_POINTS, 0, mPointCount);
                }
            }
        }
        onDrawFrameAfter();
        GLES30.glDisableVertexAttribArray(mPositionHandle);
        return true;
    }

    /**
     * 备注：用于调试使用，禁用FBO，直接绘制
     *
     * @param textureId
     * @param vertexBuffer
     * @param textureBuffer
     * @return
     */
    @Override
    public int drawFrameBuffer(int textureId, FloatBuffer vertexBuffer, FloatBuffer textureBuffer) {
        // 没有FBO、没初始化、输入纹理不合法、滤镜不可用时，直接返回
        if (textureId == OpenGLUtils.GL_NOT_TEXTURE || mFrameBuffers == null
                || !mIsInitialized || !mFilterEnable) {
            return textureId;
        }
        drawFrame(textureId, vertexBuffer, textureBuffer);
        return textureId;
    }

    @Override
    public void initFrameBuffer(int width, int height) {
        // do nothing
    }

    @Override
    public void destroyFrameBuffer() {
        // do nothing
    }
}
