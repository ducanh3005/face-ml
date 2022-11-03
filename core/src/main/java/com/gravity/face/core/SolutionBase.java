package com.gravity.face.core;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.Objects;

public abstract class SolutionBase<T, U> {

    //Tensorflow lite interpreter;
    private Interpreter interpreter;
    //Listeners
    private ResultListener<U> resultListener;
    private ErrorListener errorListener;

    protected SolutionBase(@NonNull Context context) {
        try {
            MappedByteBuffer model = this.loadModel(context.getAssets(), getModelPath());
            Interpreter.Options options = this.getInterpreterOptions();
            this.interpreter = new Interpreter(model, options);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setResultListener(@NonNull ResultListener<U> listener) {
        this.resultListener = Objects.requireNonNull(listener);
    }

    public void setErrorListener(@Nullable ErrorListener listener) {
        this.errorListener = listener;
    }

    protected final synchronized void interpret(@NonNull T input) {
        try {
            this.checkNotClose();
            Object[] inputs = this.getInputs(Objects.requireNonNull(input));
            Map<Integer, Object> outputs = this.getOutputs();
            this.interpreter.runForMultipleInputsOutputs(inputs, outputs);
        } catch (IllegalStateException e) {
            this.close();
            this.sendError(e);
        } catch (Exception e) {
            this.sendError(e);
        }
    }

    @Nullable
    protected final int[] getOutputTensorShape(int index) {
        try {
            this.checkNotClose();
            return this.interpreter.getOutputTensor(index).shape();
        } catch (IllegalStateException e) {
            this.close();
            this.sendError(e);
            return null;
        }
    }

    protected boolean isClosed() {
        return this.interpreter == null;
    }

    public void close() {
        if (this.interpreter != null) {
            this.interpreter = null;
        }
    }

    protected void sendResult(U result) {
        if (this.resultListener != null)
            this.resultListener.run(result);
    }

    protected void sendError(Exception e) {
        if (this.errorListener != null)
            this.errorListener.onError(e);
    }

    private void checkNotClose() {
        if (this.interpreter == null) {
            throw new IllegalStateException("Internal error: The Interpreter has already been closed.");
        }
    }

    private MappedByteBuffer loadModel(AssetManager assetManager, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    protected abstract Interpreter.Options getInterpreterOptions();

    protected abstract String getModelPath();

    protected abstract Object[] getInputs(T input) throws Exception;

    protected abstract Map<Integer, Object> getOutputs() throws Exception;
}
