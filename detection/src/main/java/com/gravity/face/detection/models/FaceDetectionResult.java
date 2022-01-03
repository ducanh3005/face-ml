package com.gravity.face.detection.models;

import android.graphics.Bitmap;

import java.util.List;

public final class FaceDetectionResult {
    private final List<Face> faces;
    private final Bitmap inputBitmap;

    public FaceDetectionResult(List<Face> faces, Bitmap inputBitmap) {
        this.faces = faces;
        this.inputBitmap = inputBitmap;
    }

    public List<Face> getFaces() {
        return faces;
    }

    public Bitmap getInputBitmap() {
        return inputBitmap;
    }
}
