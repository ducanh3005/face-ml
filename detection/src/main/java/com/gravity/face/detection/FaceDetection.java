package com.gravity.face.detection;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Size;

import com.gravity.face.core.SolutionBase;
import com.gravity.face.core.utils.ImageProcessorUtil;
import com.gravity.face.detection.models.Anchor;
import com.gravity.face.detection.models.AnchorOptions;
import com.gravity.face.detection.models.Face;
import com.gravity.face.detection.models.FaceDetectionOptions;
import com.gravity.face.detection.models.FaceDetectionResult;
import com.gravity.face.detection.models.TensorToFacesOptions;
import com.gravity.face.detection.utils.AnchorGenerator;
import com.gravity.face.core.utils.LimitedSizeQueue;
import com.gravity.face.detection.utils.TensorToFaces;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class FaceDetection extends SolutionBase<Bitmap, FaceDetectionResult> implements Runnable {

    //Model name in assets folder
    private static final String MODEL_PATH = "face_detection_short_range.tflite";
    //Queue Size
    private static final int QUEUE_SIZE = 10;
    //Model input image characteristics
    private static final int IMAGE_WIDTH = 128;
    private static final int IMAGE_HEIGHT = 128;
    private static final float IMAGE_MEAN = 127.5f;
    private static final float IMAGE_STD = 127.5f;
    //FaceDetection Options
    private final FaceDetectionOptions options;
    //Image Processor
    private final ImageProcessorUtil imageProcessorUtil;
    //Model output
    private final float[][][] regressionOutput;
    private final float[][][] classificationOutput;
    //Encoding objects
    private final List<Anchor> anchors;
    private final TensorToFacesOptions detectionsOption;
    private final TensorToFaces tensorToFaces;
    private final LimitedSizeQueue<Bitmap> queue;

    public FaceDetection(Context context, FaceDetectionOptions options) {
        super(context);

        this.options = options;
        this.imageProcessorUtil = new ImageProcessorUtil(IMAGE_MEAN, IMAGE_STD);

        this.anchors = AnchorGenerator.generate(AnchorOptions.withDefaultValues());
        this.detectionsOption = TensorToFacesOptions.withDefaultValues(this.options.getMinConfidence(), this.options.getMaxNumberOfFaces());
        this.tensorToFaces = new TensorToFaces();

        int[] regressionOutputShape = super.getOutputTensorShape(0);
        this.regressionOutput = new float[regressionOutputShape[0]][regressionOutputShape[1]][regressionOutputShape[2]];

        int[] classificationOutputShape = getOutputTensorShape(1);
        this.classificationOutput = new float[classificationOutputShape[0]][classificationOutputShape[1]][classificationOutputShape[2]];

        this.queue = new LimitedSizeQueue<>(QUEUE_SIZE);
        new Thread(this).start();
    }

    public void detect(@NonNull Bitmap bitmap) {
        try {
            this.queue.add(Objects.requireNonNull(bitmap));
        } catch (NullPointerException e) {
            super.sendError(e);
        }
    }

    @Override
    public void run() {
        while (!super.isClosed()) {
            Bitmap bitmap = this.queue.poll();
            if (bitmap != null) {
                super.interpret(bitmap);
                List<Face> faces = this.tensorToFaces.process(
                        new Size(bitmap.getWidth(),
                                bitmap.getHeight()),
                        this.detectionsOption,
                        this.classificationOutput,
                        this.regressionOutput,
                        this.anchors);
                this.sendResult(new FaceDetectionResult(faces, bitmap));
            } else {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    super.close();
                }
            }
        }
    }

    @Override
    protected String getModelPath() {
        return MODEL_PATH;
    }

    @Override
    protected Interpreter.Options getInterpreterOptions() {
        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(-1);
        options.setUseXNNPACK(true);
        options.setCancellable(true);
        return options;
    }

    @Override
    protected Object[] getInputs(Bitmap input) throws Exception {
        TensorImage image = new TensorImage(DataType.FLOAT32);
        image.load(input);

        ImageProcessor imageProcessor = this.imageProcessorUtil.getImageProcessor(input.getWidth(),
                input.getHeight(), IMAGE_WIDTH, IMAGE_HEIGHT);

        image = imageProcessor.process(image);
        return new Object[]{image.getBuffer()};
    }

    @Override
    protected Map<Integer, Object> getOutputs() throws Exception {
        Map<Integer, Object> outputMap = new HashMap<>();
        outputMap.put(0, this.regressionOutput);
        outputMap.put(1, this.classificationOutput);
        return outputMap;
    }

}
