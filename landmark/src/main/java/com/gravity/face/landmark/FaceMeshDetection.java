package com.gravity.face.landmark;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.util.Size;

import com.gravity.face.core.ErrorListener;
import com.gravity.face.core.ResultListener;
import com.gravity.face.core.SolutionBase;
import com.gravity.face.detection.FaceDetection;
import com.gravity.face.detection.models.Face;
import com.gravity.face.detection.models.FaceDetectionOptions;
import com.gravity.face.detection.models.FaceDetectionResult;
import com.gravity.face.landmark.models.FaceMesh;
import com.gravity.face.landmark.models.FaceMeshOptions;
import com.gravity.face.landmark.models.FaceMeshResult;
import com.gravity.face.landmark.models.TensorToMeshOptions;
import com.gravity.face.landmark.utils.CropOp;
import com.gravity.face.landmark.utils.RectTransformation;
import com.gravity.face.landmark.utils.TensorToMesh;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class FaceMeshDetection extends SolutionBase<Bitmap, FaceMeshResult> implements ResultListener<FaceDetectionResult>, ErrorListener {

    //Model name in assets folder
    private static final String MODEL_PATH = "face_landmark.tflite";
    //Model input image characteristics
    private static final int IMAGE_WIDTH = 192;
    private static final int IMAGE_HEIGHT = 192;
    private static final float IMAGE_MEAN = 0.0f;
    private static final float IMAGE_STD = 255.0f;
    //FaceMesh Options
    private final FaceMeshOptions options;
    //Face Detection
    private final FaceDetection faceDetection;
    //Image Processor
    private final ImageProcessor resizeAndNormalizeProcessor;
    //Model output
    private final float[][][][] regressionOutput;
    private final float[][][][] classificationOutput;
    //Encoding objects
    private final TensorToMeshOptions tensorToMeshOptions;
    private final TensorToMesh tensorToMesh;

    public FaceMeshDetection(@NonNull Context context, @NonNull FaceMeshOptions options) {
        super(context);

        this.options = Objects.requireNonNull(options);

        FaceDetectionOptions faceDetectionOptions = new FaceDetectionOptions.Builder().
                setMaxNumberOfFaces(this.options.getMaxNumberOfFaces()).
                setMinConfidence(this.options.getMinConfidence()).build();
        this.faceDetection = new FaceDetection(context, faceDetectionOptions);
        this.faceDetection.setErrorListener(this);
        this.faceDetection.setResultListener(this);

        this.resizeAndNormalizeProcessor = new ImageProcessor.Builder().
                add(new ResizeOp(IMAGE_HEIGHT, IMAGE_WIDTH, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR)).
                add(new NormalizeOp(IMAGE_MEAN, IMAGE_STD)).
                build();

        int[] regressionOutputShape = super.getOutputTensorShape(0);
        this.regressionOutput = new float[regressionOutputShape[0]][regressionOutputShape[1]][regressionOutputShape[2]][regressionOutputShape[3]];

        int[] classificationOutputShape = getOutputTensorShape(1);
        this.classificationOutput = new float[classificationOutputShape[0]][classificationOutputShape[1]][classificationOutputShape[2]][classificationOutputShape[3]];

        this.tensorToMeshOptions = TensorToMeshOptions.withDefaultValues();
        this.tensorToMesh = new TensorToMesh();
    }

    public void detect(@NonNull Bitmap bitmap) {
        this.faceDetection.detect(bitmap);
    }

    @Override
    public void onError(Exception exception) {
        super.sendError(exception);
    }

    @Override
    public void run(FaceDetectionResult faceDetectionResult) {
        FaceMeshResult result = new FaceMeshResult();

        Bitmap inputBitmap = faceDetectionResult.getInputBitmap();
        Size bitmapSize = new Size(inputBitmap.getWidth(), inputBitmap.getHeight());

        List<FaceMesh> facesMesh = new ArrayList<>();

        result.setFacesMesh(facesMesh);
        result.setInputBitmap(inputBitmap);

        for (Face face : faceDetectionResult.getFaces()) {
            /* PointF leftEye = RectTransformation.unNormalizePointF(face.getRelativeKeyPoint(Face.Landmarks.LEFT_EYE),
                    bitmapSize);
            PointF rightEye = RectTransformation.unNormalizePointF(face.getRelativeKeyPoint(Face.Landmarks.RIGHT_EYE),
                    bitmapSize);

            float rotationDegree = RectTransformation.getRotationRadian(leftEye, rightEye); */
            RectF roi = RectTransformation.transform(RectTransformation.unNormalizeRectF(face.getRelativeCoordinate(), bitmapSize), 0);

            TensorImage croppedTensor = new TensorImage(DataType.FLOAT32);
            croppedTensor.load(inputBitmap);
            Bitmap croppedBitmap = new ImageProcessor.Builder().add(new CropOp(roi)).build().process(croppedTensor).getBitmap();

            super.interpret(croppedBitmap);
            FaceMesh faceMesh = this.tensorToMesh.process(
                    new Size(inputBitmap.getWidth(), inputBitmap.getHeight()),
                    this.tensorToMeshOptions,
                    this.classificationOutput,
                    this.regressionOutput, roi
            );
            if (faceMesh != null)
                facesMesh.add(faceMesh);

        }
        this.sendResult(result);
    }

    @Override
    public void close() {
        super.close();
        this.faceDetection.close();
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
        image = this.resizeAndNormalizeProcessor.process(image);
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
