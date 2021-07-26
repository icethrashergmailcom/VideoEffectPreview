package com.example.videoeffectpreview.camera

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.telecom.Call
import android.util.Log
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.security.auth.callback.Callback

private const val TAG = "Camera"
const val FRONT_CAMERA = 0
const val BACK_CAMERA = 1

class Camera(context: Context): ImageAnalysis.Analyzer {
    private val context: Context = context
    private var callbacks: Callbacks? = null
    private var cameraExecutor: ExecutorService? = null

    init {
        callbacks = context as Callbacks
        if (!OpenCVLoader.initDebug()) {
            error("Unable to load OpenCV");
        } else {
            Log.d(TAG, "OpenCV loaded");
        }
    }

    interface Callbacks {
        // TODO: need an error id for localization
        fun onError(message: String)
        fun onFrame(frame: ImageProxy)
    }

    // TODO: need an error id for localization
    private fun error(message: String) {
        Log.e(TAG, "Unable to load OpenCV");
        callbacks?.onError(message)
    }

    fun start(cameraIdx: Int) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.display?.rotation ?: Surface.ROTATION_0
            } else {
                Surface.ROTATION_0
            }

            cameraExecutor = Executors.newSingleThreadExecutor()
            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetRotation(r)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor!!, this)
                }

            // Select back camera as a default
            val cameraSelector = if (cameraIdx == FRONT_CAMERA) CameraSelector.DEFAULT_FRONT_CAMERA
                else CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(context as LifecycleOwner, cameraSelector, imageAnalyzer)

            } catch(exc: Exception) {
                error("Use case binding failed: ${exc.toString()}")
            }

        }, ContextCompat.getMainExecutor(context))
    }

    fun stop() {
        cameraExecutor?.shutdown()
    }

    override fun analyze(image: ImageProxy) {
        try {
            callbacks?.onFrame(image)
        } catch (exc: java.lang.Exception) {
            callbacks?.onError("Analyze crashed: ${exc.toString()}")
        }
        image.close()
    }
}