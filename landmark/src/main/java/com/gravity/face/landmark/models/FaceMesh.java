package com.gravity.face.landmark.models;

import java.util.List;

public final class FaceMesh {

    private List<Landmark> relativeLandmarks;
    private float faceScorePresence;

    public FaceMesh() {
    }

    public void setRelativeLandmarks(List<Landmark> relativeLandmarks) {
        this.relativeLandmarks = relativeLandmarks;
    }

    public void setFaceScorePresence(float faceScorePresence) {
        this.faceScorePresence = faceScorePresence;
    }

    public List<Landmark> getRelativeLandmarks() {
        return relativeLandmarks;
    }

    public float getFaceScorePresence() {
        return faceScorePresence;
    }
}
