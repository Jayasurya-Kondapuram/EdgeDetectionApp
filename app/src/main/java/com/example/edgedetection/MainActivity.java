package com.example.edgedetection;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

public class MainActivity extends Activity implements TextureView.SurfaceTextureListener,
        CameraManager.FrameProcessor {

    private static final String TAG = "MainActivity";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    private TextureView textureView;
    private TextView fpsTextView;
    private CameraManager cameraManager;

    private long lastFrameTime = 0;
    private int frameCount = 0;
    private float currentFPS = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        initializeViews();

        // Initialize native library
        System.loadLibrary("edgedetection");
        initializeOpenGL();

        // Start with mock camera mode for demonstration
        initializeCamera();

        Toast.makeText(this, "Edge Detection Demo Started - Mock Camera Active", Toast.LENGTH_LONG).show();
    }

    private void initializeViews() {
        textureView = findViewById(R.id.texture_view);
        fpsTextView = findViewById(R.id.fps_counter);
        textureView.setSurfaceTextureListener(this);
    }

    private void initializeCamera() {
        cameraManager = new CameraManager(this, textureView);
        cameraManager.setFrameProcessor(this);
        cameraManager.startBackgroundThread();

        // Enable mock mode for demonstration
        cameraManager.enableMockMode();

        Log.d(TAG, "Camera system initialized in mock mode");
    }

    // TextureView.SurfaceTextureListener methods
    @Override
    public void onSurfaceTextureAvailable(android.graphics.SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "Surface texture available");
    }

    @Override
    public void onSurfaceTextureSizeChanged(android.graphics.SurfaceTexture surface, int width, int height) {
        // Handle surface size change if needed
    }

    @Override
    public boolean onSurfaceTextureDestroyed(android.graphics.SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(android.graphics.SurfaceTexture surface) {
        // Surface texture updated
    }

    // FrameProcessor method - called when new frame is available
    @Override
    public void onFrameProcessed(byte[] frameData, int width, int height) {
        // Calculate FPS
        calculateFPS();

        // Send frame to native code for processing
        processFrame(frameData, width, height);

        // Update FPS display on UI thread
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fpsTextView.setText(String.format("FPS: %.1f | Demo Mode", currentFPS));
            }
        });
    }

    private void calculateFPS() {
        long currentTime = System.currentTimeMillis();
        frameCount++;

        if (lastFrameTime == 0) {
            lastFrameTime = currentTime;
        }

        long elapsedTime = currentTime - lastFrameTime;
        if (elapsedTime >= 1000) {
            currentFPS = frameCount * 1000.0f / elapsedTime;
            frameCount = 0;
            lastFrameTime = currentTime;

            Log.d(TAG, "Current FPS: " + currentFPS);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraManager != null) {
            cameraManager.closeCamera();
            cameraManager.stopBackgroundThread();
        }
        shutdown();
    }

    // Native method declarations
    public native void initializeOpenGL();
    public native void processFrame(byte[] frameData, int width, int height);
    public native void shutdown();
}