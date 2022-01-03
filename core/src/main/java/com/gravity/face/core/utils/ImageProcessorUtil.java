package com.gravity.face.core.utils;

import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;

public final class ImageProcessorUtil {

    private final float mean;
    private final float std;
    private int currentWidth;
    private int currentHeight;
    private int targetWidth;
    private int targetHeight;
    private ImageProcessor imageProcessor;

    public ImageProcessorUtil(float mean, float std) {
        this.mean = mean;
        this.std = std;
    }

    public synchronized ImageProcessor getImageProcessor(int currentWidth, int currentHeight, int targetWidth, int targetHeight) {
        if (this.imageProcessor == null) {
            this.currentWidth = currentWidth;
            this.currentHeight = currentHeight;
            this.targetWidth = targetWidth;
            this.targetHeight = targetHeight;

            int maxDimension = Math.max(currentWidth, currentHeight);
            this.imageProcessor = new ImageProcessor.Builder().
                    add(new ResizeWithCropOrPadOp(maxDimension, maxDimension)).
                    add(new ResizeOp(targetHeight, targetWidth, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR)).
                    add(new NormalizeOp(this.mean, this.std)).
                    build();
        } else {
            if (this.currentWidth != currentWidth || this.currentHeight != currentHeight || this.targetWidth != targetWidth || this.targetHeight != targetHeight) {
                this.imageProcessor = null;
                this.imageProcessor = this.getImageProcessor(currentWidth, currentHeight, targetWidth, targetHeight);
            }
        }
        return this.imageProcessor;
    }
}
