
package com.ynsuper.arfacesohasdk.facepoint;

import android.graphics.PointF;

public class EGLFace {
    public float pitch;

    public float yaw;

    public float roll;

    // Contour point coordinates, refer to 212 contour points index in FacePointIndex.java
    public PointF[] vertexPoints;

    @Override
    protected EGLFace clone() {
        EGLFace copy = new EGLFace();
        copy.pitch = this.pitch;
        copy.yaw = this.yaw;
        copy.roll = this.roll;
        if (vertexPoints != null)
            copy.vertexPoints = this.vertexPoints.clone();
        return copy;
    }
}
