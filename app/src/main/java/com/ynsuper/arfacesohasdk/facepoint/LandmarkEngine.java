package com.ynsuper.arfacesohasdk.facepoint;


import android.util.SparseArray;

import com.tenginekit.engine.face.Face;

/**
 * 人脸关键点引擎
 */
public final class LandmarkEngine {

    private static class EngineHolder {
        public static LandmarkEngine instance = new LandmarkEngine();
    }

    private LandmarkEngine() {
        mFaceArrays = new SparseArray<Face>();
    }

    public static LandmarkEngine getInstance() {
        return EngineHolder.instance;
    }

    private final Object mSyncFence = new Object();

    // list of face objects
    // Due to the limited number of face data, the number of faces in the image is less than a thousand, and the face index is continuous, using Sparse Array has better performance than Hashmap
    private final SparseArray<Face> mFaceArrays;

    // The current direction of the phone, 0 means the front screen, 3 means the reverse, 1 means the left screen, 2 means the right screen
    private float mOrientation;
    private boolean mNeedFlip;

    /**
     * 设置旋转角度
     * @param orientation
     */
    public void setOrientation(int orientation) {
        mOrientation = orientation;
    }

    /**
     * 设置是否需要翻转
     * @param flip
     */
    public void setNeedFlip(boolean flip) {
        mNeedFlip = flip;
    }

    /**
     * 设置人脸数
     * @param size
     */
    public void setFaceSize(int size) {
        synchronized (mSyncFence) {
            // 剔除脏数据，有可能在前一次检测的人脸多余当前人脸
            if (mFaceArrays.size() > size) {
                mFaceArrays.removeAtRange(size, mFaceArrays.size() - size);
            }
        }
    }

    /**
     * 是否存在人脸
     * @return
     */
    public boolean hasFace() {
        boolean result;
        synchronized (mSyncFence) {
            result = mFaceArrays.size() > 0;
        }
        return result;
    }

    /**
     * Get a face keypoint data object
     * @return
     */
    public Face getOneFace(int index) {
        Face oneFace = null;
        synchronized (mSyncFence) {
            oneFace = mFaceArrays.get(index);
            if (oneFace == null) {
                oneFace = new Face();
            }
        }
        return oneFace;
    }

    /**
     * 插入一个人脸关键点数据对象
     * @param index
     */
    public void putOneFace(int index, Face oneFace) {
        synchronized (mSyncFence) {
            mFaceArrays.put(index, oneFace);
        }
    }


    /**
     * 获取人脸个数
     * @return
     */
    public int getFaceSize() {
        return mFaceArrays.size();
    }

    /**
     * 获取人脸列表
     * @return
     */
    public SparseArray<Face> getFaceArrays() {
        return mFaceArrays;
    }

    /**
     * 清空所有人脸对象
     */
    public void clearAll() {
        synchronized (mSyncFence) {
            mFaceArrays.clear();
        }
    }

    /**
     * Calculate additional face vertices, add 8 additional vertex coordinates
     * @param vertexPoints
     * @param index
     */
    public void calculateExtraFacePoints(float[] vertexPoints, int index) {
        if (vertexPoints == null
                || index >= mFaceArrays.size()
                || mFaceArrays.get(index) == null
                || mFaceArrays.get(index).landmark.length + 8 * 2 > vertexPoints.length) {
            return;
        }
        Face oneFace = mFaceArrays.get(index);
        // Copy key data
        System.arraycopy(oneFace.landmark, 0, vertexPoints, 0, oneFace.landmark.length);
        // 新增的人脸关键点
        float[] point = new float[2];
        // 嘴唇中心
        FacePointsUtils.getCenter(point,
                vertexPoints[FaceLandmark.mouthUpperLipBottom * 2],
                vertexPoints[FaceLandmark.mouthUpperLipBottom * 2 + 1],
                vertexPoints[FaceLandmark.mouthLowerLipTop * 2],
                vertexPoints[FaceLandmark.mouthLowerLipTop * 2 + 1]
        );
        vertexPoints[FaceLandmark.mouthCenter * 2] = point[0];
        vertexPoints[FaceLandmark.mouthCenter * 2 + 1] = point[1];

        // left eyebrow
        FacePointsUtils.getCenter(point,
                vertexPoints[FaceLandmark.leftEyebrowUpperMiddle * 2],
                vertexPoints[FaceLandmark.leftEyebrowUpperMiddle * 2 + 1],
                vertexPoints[FaceLandmark.leftEyebrowLowerMiddle * 2],
                vertexPoints[FaceLandmark.leftEyebrowLowerMiddle * 2 + 1]
        );
        vertexPoints[FaceLandmark.leftEyebrowCenter * 2] = point[0];
        vertexPoints[FaceLandmark.leftEyebrowCenter * 2 + 1] = point[1];

        // Right eyebrow
        FacePointsUtils.getCenter(point,
                vertexPoints[FaceLandmark.rightEyebrowUpperMiddle * 2],
                vertexPoints[FaceLandmark.rightEyebrowUpperMiddle * 2 + 1],
                vertexPoints[FaceLandmark.rightEyebrowLowerMiddle * 2],
                vertexPoints[FaceLandmark.rightEyebrowLowerMiddle * 2 + 1]
        );
        vertexPoints[FaceLandmark.rightEyebrowCenter * 2] = point[0];
        vertexPoints[FaceLandmark.rightEyebrowCenter * 2 + 1] = point[1];

        // center of forehead
        vertexPoints[FaceLandmark.headCenter * 2] = vertexPoints[FaceLandmark.eyeCenter * 2] * 2.0f - vertexPoints[FaceLandmark.noseLowerMiddle * 2];
        vertexPoints[FaceLandmark.headCenter * 2 + 1] = vertexPoints[FaceLandmark.eyeCenter * 2 + 1] * 2.0f - vertexPoints[FaceLandmark.noseLowerMiddle * 2 + 1];

        // The left side of the forehead, note: this point is not very accurate, follow-up optimization
        FacePointsUtils.getCenter(point,
                vertexPoints[FaceLandmark.leftEyebrowLeftTopCorner * 2],
                vertexPoints[FaceLandmark.leftEyebrowLeftTopCorner * 2 + 1],
                vertexPoints[FaceLandmark.headCenter * 2],
                vertexPoints[FaceLandmark.headCenter * 2 + 1]
        );
        vertexPoints[FaceLandmark.leftHead * 2] = point[0];
        vertexPoints[FaceLandmark.leftHead * 2 + 1] = point[1];

        // On the right side of the forehead, note: this point is not very accurate, follow-up optimization
        FacePointsUtils.getCenter(point,
                vertexPoints[FaceLandmark.rightEyebrowRightTopCorner * 2],
                vertexPoints[FaceLandmark.rightEyebrowRightTopCorner * 2 + 1],
                vertexPoints[FaceLandmark.headCenter * 2],
                vertexPoints[FaceLandmark.headCenter * 2 + 1]
        );
        vertexPoints[FaceLandmark.rightHead * 2] = point[0];
        vertexPoints[FaceLandmark.rightHead * 2 + 1] = point[1];

        // left cheek center
        FacePointsUtils.getCenter(point,
                vertexPoints[FaceLandmark.leftCheekEdgeCenter * 2],
                vertexPoints[FaceLandmark.leftCheekEdgeCenter * 2 + 1],
                vertexPoints[FaceLandmark.noseLeft * 2],
                vertexPoints[FaceLandmark.noseLeft * 2 + 1]
        );
        vertexPoints[FaceLandmark.leftCheekCenter * 2] = point[0];
        vertexPoints[FaceLandmark.leftCheekCenter * 2 + 1] = point[1];

        // Right cheek center
        FacePointsUtils.getCenter(point,
                vertexPoints[FaceLandmark.rightCheekEdgeCenter * 2],
                vertexPoints[FaceLandmark.rightCheekEdgeCenter * 2 + 1],
                vertexPoints[FaceLandmark.noseRight * 2],
                vertexPoints[FaceLandmark.noseRight * 2 + 1]
        );
        vertexPoints[FaceLandmark.rightCheekCenter * 2] = point[0];
        vertexPoints[FaceLandmark.rightCheekCenter * 2 + 1] = point[1];
    }

    /**
     * 计算
     * @param vertexPoints
     */
    private void calculateImageEdgePoints(float[] vertexPoints) {
        if (vertexPoints == null || vertexPoints.length < 228 * 2) {
            return;
        }

        if (mOrientation == 0) {
            vertexPoints[220 * 2] = 0;
            vertexPoints[220 * 2 + 1] = 1;
            vertexPoints[221 * 2] = 1;
            vertexPoints[221 * 2 + 1] = 1;
            vertexPoints[222 * 2] = 1;
            vertexPoints[222 * 2 + 1] = 0;
            vertexPoints[223 * 2] = 1;
            vertexPoints[223 * 2 + 1] = -1;
        } else if (mOrientation == 1) {
            vertexPoints[220 * 2] = 1;
            vertexPoints[220 * 2 + 1] = 0;
            vertexPoints[221 * 2] = 1;
            vertexPoints[221 * 2 + 1] = -1;
            vertexPoints[222 * 2] = 0;
            vertexPoints[222 * 2 + 1] = -1;
            vertexPoints[223 * 2] = -1;
            vertexPoints[223 * 2 + 1] = -1;
        } else if (mOrientation == 2) {
            vertexPoints[220 * 2] = -1;
            vertexPoints[220 * 2 + 1] = 0;
            vertexPoints[221 * 2] = -1;
            vertexPoints[221 * 2 + 1] = 1;
            vertexPoints[222 * 2] = 0;
            vertexPoints[222 * 2 + 1] = 1;
            vertexPoints[223 * 2] = 1;
            vertexPoints[223 * 2 + 1] = 1;
        } else if (mOrientation == 3) {
            vertexPoints[220 * 2] = 0;
            vertexPoints[220 * 2 + 1] = -1;
            vertexPoints[221 * 2] = -1;
            vertexPoints[221 * 2 + 1] = -1;
            vertexPoints[222 * 2] = -1;
            vertexPoints[222 * 2 + 1] = 0;
            vertexPoints[223 * 2] = -1;
            vertexPoints[223 * 2 + 1] = 1;
        }
        // 118 ~ 121 与 114 ~ 117 的顶点坐标恰好反过来
        vertexPoints[224 * 2] = -vertexPoints[220 * 2];
        vertexPoints[224 * 2 + 1] = -vertexPoints[220 * 2 + 1];
        vertexPoints[225 * 2] = -vertexPoints[221 * 2];
        vertexPoints[225 * 2 + 1] = -vertexPoints[221 * 2 + 1];
        vertexPoints[226 * 2] = -vertexPoints[222 * 2];
        vertexPoints[226 * 2 + 1] = -vertexPoints[222 * 2 + 1];
        vertexPoints[227 * 2] = -vertexPoints[223 * 2];
        vertexPoints[227 * 2 + 1] = -vertexPoints[223 * 2 + 1];

        // 是否需要做翻转处理，前置摄像头预览时，关键点是做了翻转处理的，因此图像边沿的关键点也要做翻转能处理
        if (mNeedFlip) {
            for (int i = 0; i < 8; i++) {
                vertexPoints[(220 + i) * 2] = -vertexPoints[(220 + i) * 2];
                vertexPoints[(220 + i) * 2 + 1] = -vertexPoints[(220 + i) * 2 + 1];
            }
        }

    }

    /**
     * 获取用于美型处理的坐标
     * @param vertexPoints  Vertex coordinates, a total of 122 vertices
     * @param texturePoints Vertex coordinates, a total of 122 vertices
     * @param faceIndex     人脸索引
     */
    public void updateFaceAdjustPoints(float[] vertexPoints, float[] texturePoints, int faceIndex) {
        if (vertexPoints == null || vertexPoints.length != 228 * 2
                || texturePoints == null || texturePoints.length != 228 * 2) {
            return;
        }
        // 计算额外的人脸顶点坐标
        calculateExtraFacePoints(vertexPoints, faceIndex);
        // 计算图像边沿顶点坐标
        calculateImageEdgePoints(vertexPoints);
        // 计算纹理坐标
        for (int i = 0; i < vertexPoints.length; i++) {
            texturePoints[i] = vertexPoints[i] * 0.5f + 0.5f;
        }
    }

    /**
     * 阴影(修容)顶点坐标，修容用的是整个人脸的顶点坐标
     * @param vetexPoints
     * @param faceIndex
     */
    public void getShadowVertices(float[] vetexPoints, int faceIndex) {

    }

    /**
     * 取得脸颊(腮红)顶点坐标
     * @param vertexPoints
     * @param faceIndex
     */
    public void getBlushVertices(float[] vertexPoints, int faceIndex) {

    }

    /**
     * 取得眉毛顶点坐标
     * @param vertexPoints
     * @param faceIndex
     */
    public void getEyeBrowVertices(float[] vertexPoints, int faceIndex) {

    }

    /**
     * To get the vertex coordinates of the eyes (eye shadow, eyeliner, etc.), please refer to the eye mask label in the assets directory.jpg
     * @param vertexPoints
     * @param faceIndex
     */
    public synchronized void getEyeVertices(float[] vertexPoints, int faceIndex) {
        if (vertexPoints == null || vertexPoints.length < 80
                || faceIndex >= mFaceArrays.size() || mFaceArrays.get(faceIndex) == null) {
            return;
        }

        // Keypoint 0 ~ 3, index = 0 ~ 3 4
        for (int i = 0; i < 4; i++) {
            vertexPoints[i * 2] = mFaceArrays.get(faceIndex).landmark[i * 2];
            vertexPoints[i * 2 + 1] = mFaceArrays.get(faceIndex).landmark[i * 2 + 1];
        }

        // Key points 29 ~ 33, index = 4 ~ 8 5
        for (int i = 29; i < 34; i++) {
            vertexPoints[(i - 29 + 4) * 2] = mFaceArrays.get(faceIndex).landmark[i * 2];
            vertexPoints[(i - 29 + 4) * 2 + 1] = mFaceArrays.get(faceIndex).landmark[i * 2 + 1];
        }

        // Key points 42 ~ 44, index = 9 ~ 11 3
        for (int i = 42; i < 45; i++) {
            vertexPoints[(i - 42 + 9) * 2] = mFaceArrays.get(faceIndex).landmark[i * 2];
            vertexPoints[(i - 42 + 9) * 2 + 1] = mFaceArrays.get(faceIndex).landmark[i * 2 +  1];
        }

        // Key points 52 ~ 73, index = 12 ~ 33 22
        for (int i = 52; i < 74; i++) {
            vertexPoints[(i - 52 + 12) * 2] = mFaceArrays.get(faceIndex).landmark[i * 2];
            vertexPoints[(i - 52 + 12) * 2 + 1] = mFaceArrays.get(faceIndex).landmark[i * 2 + 1];
        }

        // upper center of right eye
        vertexPoints[34 * 2] = mFaceArrays.get(faceIndex).landmark[75 * 2];
        vertexPoints[34 * 2 + 1] = mFaceArrays.get(faceIndex).landmark[75 * 2 + 1];

        // Right eye lower center
        vertexPoints[35 * 2] = mFaceArrays.get(faceIndex).landmark[76 * 2];
        vertexPoints[35 * 2 + 1] = mFaceArrays.get(faceIndex).landmark[76 * 2 + 1];

        // key point 78
        vertexPoints[36 * 2] = mFaceArrays.get(faceIndex).landmark[78 * 2];
        vertexPoints[36 * 2 + 1] = mFaceArrays.get(faceIndex).landmark[78 * 2 + 1];

        // key point 79
        vertexPoints[37 * 2] = mFaceArrays.get(faceIndex).landmark[79 * 2];
        vertexPoints[37 * 2 + 1] = mFaceArrays.get(faceIndex).landmark[79 * 2 + 1];

        // Center point below left eyebrow
        vertexPoints[38 * 2] = (mFaceArrays.get(faceIndex).landmark[3 * 2] + mFaceArrays.get(faceIndex).landmark[44 * 2]) * 0.5f;
        vertexPoints[38 * 2 + 1] = (mFaceArrays.get(faceIndex).landmark[3 * 2 + 1] + mFaceArrays.get(faceIndex).landmark[44 * 2 + 1]) * 0.5f;

        // Center point below right eyebrow
        vertexPoints[39 * 2] = (mFaceArrays.get(faceIndex).landmark[29 * 2] + mFaceArrays.get(faceIndex).landmark[44 * 2]) * 0.5f;
        vertexPoints[39 * 2 + 1] = (mFaceArrays.get(faceIndex).landmark[29 * 2 + 1] + mFaceArrays.get(faceIndex).landmark[44 * 2 + 1]) * 0.5f;
    }

    /**
     * Get lips (lip gloss) vertex coordinates
     * @param vertexPoints  存放嘴唇顶点坐标
     * @param faceIndex     人脸索引
     */
    public synchronized void getLipsVertices(float[] vertexPoints, int faceIndex) {
        // 嘴唇一共20个顶点，大小必须为40
        if (vertexPoints == null || vertexPoints.length < 40
                || faceIndex >= mFaceArrays.size() || mFaceArrays.get(faceIndex) == null) {
            return;
        }
        // 复制84 ~ 103共20个顶点坐标
        for (int i = 0; i < 20; i++) {
            // 顶点坐标
            vertexPoints[i * 2] = mFaceArrays.get(faceIndex).landmark[(84 + i) * 2];
            vertexPoints[i * 2 + 1] = mFaceArrays.get(faceIndex).landmark[(84 + i) * 2 + 1];
        }
    }

    /**
     * 取得亮眼需要的顶点坐标
     * @param vertexPoints
     * @param faceIndex
     */
    public synchronized void getBrightEyeVertices(float[] vertexPoints, int faceIndex) {
        if (vertexPoints == null || vertexPoints.length < 32
                || faceIndex >= mFaceArrays.size() || mFaceArrays.get(faceIndex) == null) {
            return;
        }
        // 眼睛边沿部分 index = 0 ~ 11
        for (int i = 52; i < 64; i++) {
            vertexPoints[(i - 52) * 2] = mFaceArrays.get(faceIndex).landmark[i * 2];
            vertexPoints[(i - 52) * 2 + 1] = mFaceArrays.get(faceIndex).landmark[i * 2 + 1];
        }

        vertexPoints[12 * 2] = mFaceArrays.get(faceIndex).landmark[72 * 2];
        vertexPoints[12 * 2 + 1] = mFaceArrays.get(faceIndex).landmark[72 * 2 + 1];

        vertexPoints[13 * 2] = mFaceArrays.get(faceIndex).landmark[73 * 2];
        vertexPoints[13 * 2 + 1] = mFaceArrays.get(faceIndex).landmark[73 * 2 + 1];

        vertexPoints[14 * 2] = mFaceArrays.get(faceIndex).landmark[75 * 2];
        vertexPoints[14 * 2 + 1] = mFaceArrays.get(faceIndex).landmark[75 * 2 + 1];

        vertexPoints[15 * 2] = mFaceArrays.get(faceIndex).landmark[76 * 2];
        vertexPoints[15 * 2 + 1] = mFaceArrays.get(faceIndex).landmark[76 * 2 + 1];

    }

    /**
     * 取得美牙需要的顶点坐标，嘴巴周围12个顶点
     * @param vertexPoints
     * @param faceIndex
     */
    public synchronized void getBeautyTeethVertices(float[] vertexPoints, int faceIndex) {
        if (vertexPoints == null || vertexPoints.length < 24
                || faceIndex >= mFaceArrays.size() || mFaceArrays.get(faceIndex) == null) {
            return;
        }
        for (int i = 84; i < 96; i++) {
            vertexPoints[(i - 84) * 2] = mFaceArrays.get(faceIndex).landmark[i * 2];
            vertexPoints[(i - 84) * 2 + 1] = mFaceArrays.get(faceIndex).landmark[i * 2 + 1];
        }
    }
}
