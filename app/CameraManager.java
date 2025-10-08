package com.example.edgedetection;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class CameraManager {
    private static final String TAG = "CameraManager";

    private Context context;
    private TextureView textureView;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;

    // Camera2 API objects
    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder captureRequestBuilder;
    private ImageReader imageReader;

    // Camera state
    private String cameraId;
    private Size previewSize;

    // Frame processing callback
    private FrameProcessor frameProcessor;

    // Mock camera mode
    private boolean mockMode = true;
    private Handler mockHandler;

    public interface FrameProcessor {
        void onFrameProcessed(byte[] frameData, int width, int height);
    }

    public CameraManager(Context context, TextureView textureView) {
        this.context = context;
        this.textureView = textureView;
        this.cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }

    public void setFrameProcessor(FrameProcessor processor) {
        this.frameProcessor = processor;
    }

    public void enableMockMode() {
        mockMode = true;
        Log.d(TAG, "Mock camera mode enabled");

        if (backgroundHandler == null) {
            startBackgroundThread();
        }

        startMockFrameGeneration();
    }

    public void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    public void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                Log.e(TAG, "Error stopping background thread", e);
            }
        }
    }

    public void openCamera() {
        if (mockMode) {
            enableMockMode();
            return;
        }

        try {
            cameraId = getBackCameraId();
            if (cameraId == null) {
                Log.e(TAG, "No back camera found");
                return;
            }

            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            Size[] previewSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    .getOutputSizes(SurfaceTexture.class);

            previewSize = chooseOptimalSize(previewSizes);

            imageReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(),
                    android.graphics.ImageFormat.YUV_420_888, 2);

            imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = reader.acquireLatestImage();
                    if (image != null) {
                        processImage(image);
                        image.close();
                    }
                }
            }, backgroundHandler);

            cameraManager.openCamera(cameraId, cameraStateCallback, backgroundHandler);

        } catch (CameraAccessException e) {
            Log.e(TAG, "Camera access exception", e);
        } catch (SecurityException e) {
            Log.e(TAG, "Camera permission not granted", e);
        }
    }

    private void startMockFrameGeneration() {
        mockHandler = new Handler(backgroundHandler.getLooper());

        mockHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mockMode && frameProcessor != null) {
                    byte[] mockFrame = generateTestPattern(640, 480);
                    frameProcessor.onFrameProcessed(mockFrame, 640, 480);
                }

                if (mockMode) {
                    mockHandler.postDelayed(this, 100); // 10 FPS
                }
            }
        });
    }

    private byte[] generateTestPattern(int width, int height) {
        int ySize = width * height;
        int uvSize = width * height / 2;
        byte[] frame = new byte[ySize + uvSize];

        // Create alternating black and white vertical stripes
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int yIndex = y * width + x;
                if ((x / 20) % 2 == 0) {
                    frame[yIndex] = (byte) 255; // White
                } else {
                    frame[yIndex] = (byte) 0;   // Black
                }
            }
        }

        // Fill UV planes with neutral values
        for (int i = ySize; i < frame.length; i++) {
            frame[i] = (byte) 128;
        }

        return frame;
    }

    private void processImage(Image image) {
        if (frameProcessor != null) {
            byte[] frameData = yuv420ToNv21(image);
            frameProcessor.onFrameProcessed(frameData, image.getWidth(), image.getHeight());
        }
    }

    private byte[] yuv420ToNv21(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        yBuffer.get(nv21, 0, ySize);

        byte[] uArray = new byte[uSize];
        byte[] vArray = new byte[vSize];
        uBuffer.get(uArray);
        vBuffer.get(vArray);

        System.arraycopy(uArray, 0, nv21, ySize, uSize);
        System.arraycopy(vArray, 0, nv21, ySize + uSize, vSize);

        return nv21;
    }

    private String getBackCameraId() throws CameraAccessException {
        for (String id : cameraManager.getCameraIdList()) {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
            Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                return id;
            }
        }
        return null;
    }

    private Size chooseOptimalSize(Size[] choices) {
        for (Size size : choices) {
            if (size.getWidth() == 640 && size.getHeight() == 480) {
                return size;
            }
        }
        return choices[0];
    }

    private final CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();
            cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            if (texture == null) {
                return;
            }

            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface previewSurface = new Surface(texture);
            Surface imageReaderSurface = imageReader.getSurface();

            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(previewSurface);
            captureRequestBuilder.addTarget(imageReaderSurface);

            cameraDevice.createCaptureSession(Arrays.asList(previewSurface, imageReaderSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            if (cameraDevice == null) {
                                return;
                            }

                            cameraCaptureSession = session;
                            try {
                                captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

                                session.setRepeatingRequest(captureRequestBuilder.build(),
                                        null, backgroundHandler);

                            } catch (CameraAccessException e) {
                                Log.e(TAG, "Failed to start camera preview", e);
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Log.e(TAG, "Failed to configure camera session");
                        }
                    }, backgroundHandler);

        } catch (CameraAccessException e) {
            Log.e(TAG, "Camera access exception in preview session", e);
        }
    }

    public void closeCamera() {
        mockMode = false;

        if (mockHandler != null) {
            mockHandler.removeCallbacksAndMessages(null);
        }

        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }

        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }

        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
    }
}