package com.example.edgedetection;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

public class MainActivity extends Activity {
    
    private static final String TAG = "MainActivity";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        // Set content view
        setContentView(R.layout.activity_main);
        
        Toast.makeText(this, "Edge Detection App Started", Toast.LENGTH_SHORT).show();
        
        // Initialize native library
        System.loadLibrary("edgedetection");
    }
    
    // Native method declarations
    public native void initializeOpenGL();
    public native void processFrame(byte[] frameData, int width, int height);
    public native void shutdown();
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        shutdown();
    }
}