package com.ynsuper.arfacesohasdk.facefilter;

import java.util.Arrays;

public class FaceStickerJson {
    // CenterIndexList multipoint will get center point
    public int[] centerIndexList;
    // Offset pixels on the X axis relative to the sticker's central coordinates
    public float offsetX;
    // Offset pixels on the Y-axis relative to the sticker's central coordinates
    public float offsetY;
    // Sticker base zoom multiple
    public float baseScale;
    // Face start index, used to calculate the width of a face
    public int startIndex;
    // Face end index, used to calculate the width of the face
    public int endIndex;
    // Sticker width
    public int width;
    // Sticker height
    public int height;
    // Number of stickers frames
    public int frames;
    // action, 0 for default display, used here to handle sticker action
    public int action;
    // Name of the sticker, used to mark the folder where the sticker is located and PNG file
    public String stickerName;
    // Stickers frame display interval
    public int duration;
    // Whether the sticker is rendered in a loop
    public boolean stickerLooping;
    // Maximum render times for stickers
    public int maxCount;

    @Override
    public String toString() {
        return "FaceStickerJson{" +
                "centerIndexList=" + Arrays.toString(centerIndexList) +
                ", offsetX=" + offsetX +
                ", offsetY=" + offsetY +
                ", baseScale=" + baseScale +
                ", startIndex=" + startIndex +
                ", endIndex=" + endIndex +
                ", width=" + width +
                ", height=" + height +
                ", frames=" + frames +
                ", action=" + action +
                ", stickerName='" + stickerName + '\'' +
                ", duration=" + duration +
                ", stickerLooping=" + stickerLooping +
                ", maxCount=" + maxCount +
                '}';
    }
}
