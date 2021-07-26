package com.example.videoeffectpreview

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.camera.core.ImageProxy
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.videoeffectpreview.camera.Camera
import com.example.videoeffectpreview.fragments.effectControl.EffectControlFragment
import com.example.videoeffectpreview.fragments.preview.PreviewFragment
import com.example.videoeffectpreview.processing.BlurEffect
import com.example.videoeffectpreview.processing.ErodeEffect
import com.example.videoeffectpreview.processing.ImageProcessor
import com.example.videoeffectpreview.recorder.Recorder
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class MainActivity :
    AppCompatActivity(),
    EffectControlFragment.Callbacks,
    Camera.Callbacks,
    PreviewFragment.Callbacks {
    // We control camera and processing from main activity
    private val camera = Camera(this)
    private val imageProcess = ImageProcessor(this)
    private val recorder = Recorder()

    // Two predefined effects
    private val blurEffect = BlurEffect()
    private val erodeEffect = ErodeEffect()

    private var cameraIdx: Int = com.example.videoeffectpreview.camera.BACK_CAMERA
    private var screenOrientationLocked: Boolean = false

    private lateinit var previewFragment: PreviewFragment
    private lateinit var effectControlFragment: EffectControlFragment
    private lateinit var effectConfigFragmentView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewFragment = supportFragmentManager.findFragmentById(R.id.previewFragment) as PreviewFragment
        effectControlFragment = supportFragmentManager.findFragmentById(R.id.effectControlFragment) as EffectControlFragment
        effectConfigFragmentView = findViewById(R.id.effectControlFragment)

        previewFragment.restoreState(savedInstanceState)
        effectControlFragment.restoreState(savedInstanceState)
        if (savedInstanceState != null) {
            cameraIdx = savedInstanceState.getInt(CAMERA_IDX_KEY)
        }

        if (allPermissionsGranted()) {
            camera.start(cameraIdx)
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)

        previewFragment.saveState(savedInstanceState)
        effectControlFragment.saveState(savedInstanceState)
        savedInstanceState.putInt(CAMERA_IDX_KEY, cameraIdx)
    }

    override fun onPause() {
        super.onPause()
        // TODO: misleading
        onPreviewStopRecording()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                camera.start(cameraIdx)
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    // processing pipeline stub
    private fun addEffects() {
        imageProcess.addEffect("blur", blurEffect)
        imageProcess.addEffect("erode", erodeEffect)
    }

    private fun removeEffects() {
        imageProcess.removeEffect("blur")
        imageProcess.removeEffect("erode")
    }

    // effect configuration fragment handler
    override fun onPadPositionChanged(x: Double, y: Double) {
        // effects size values are [3;25]
        blurEffect.size = 3 + (25 * x).toInt()
        erodeEffect.size = 3 + (25 * y).toInt()
    }

    // preview fragment handlers
    override fun onPreviewIsExpandedChanged(isExpanded: Boolean) {
        val orientation = resources.configuration.orientation
        val params =  when (isExpanded) {
            true -> when (orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> LinearLayoutCompat.LayoutParams(0, LinearLayoutCompat.LayoutParams.MATCH_PARENT, 0F)
                else -> LinearLayoutCompat.LayoutParams(LinearLayoutCompat.LayoutParams.MATCH_PARENT, 0, 0F)
            }
            else -> when (orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> LinearLayoutCompat.LayoutParams(0, LinearLayoutCompat.LayoutParams.MATCH_PARENT, 1F)
                else -> LinearLayoutCompat.LayoutParams(LinearLayoutCompat.LayoutParams.MATCH_PARENT, 0, 1F)
            }
        }

        effectConfigFragmentView.layoutParams = params
    }

    override fun onPreviewEffectSwitched(isOn: Boolean) {
        if (isOn) addEffects()
        else removeEffects()
    }

    override fun onPreviewCameraToggled() {
        camera.stop()
        cameraIdx = (cameraIdx + 1) % 2
        camera.start(cameraIdx)
    }

    override fun onPreviewStartRecording(width: Int, height: Int): Boolean {
        // file extension is always mp4 for now
        try {
            val file = File(
                getOutputDirectory(),
                SimpleDateFormat(
                    FILENAME_FORMAT, Locale.US
                ).format(System.currentTimeMillis()) + ".mp4"
            )
            recorder.start(file.path,width, height)

            lockScreenOrientation()
            Toast.makeText(
                this,
                "Orientation change is locked during recording",
                Toast.LENGTH_SHORT
            ).show()

            previewFragment.onRecordingStarted()
            return true
        } catch (exc: Exception){
            onError("Recorder error: $exc")
            return false
        }
    }

    override fun onPreviewStopRecording(): Boolean {
        if (recorder.isRecording) {
            recorder.stop()
            Toast.makeText(
                this,
                "File ${recorder.filePath} saved",
                Toast.LENGTH_SHORT
            ).show()
            MediaScannerConnection.scanFile(this, arrayOf(recorder.filePath), null
            ) { path, uri ->
                Log.i("ExternalStorage", "Scanned $path:")
                Log.i("ExternalStorage", "-> uri=$uri")
            }
            unlockScreenOrientation()
        }

        previewFragment.onRecordingStopped()
        return true
    }

    // from https://stackoverflow.com/questions/6599770/screen-orientation-lock
    private fun lockScreenOrientation() {
        if (!screenOrientationLocked) {
            val orientation = resources.configuration.orientation
            val rotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display?.rotation
            } else {
                Surface.ROTATION_0
            }
            if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_90) {
                if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
            } else if (rotation == Surface.ROTATION_180 || rotation == Surface.ROTATION_270) {
                if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                }
            }
            screenOrientationLocked = true
        }
    }

    private fun unlockScreenOrientation() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        screenOrientationLocked = false
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() } }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    // camera handlers
    override fun onError(message: String) {
        runOnUiThread {
            Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onFrame(frame: ImageProxy) {
        val bitmap = imageProcess.process(frame)

        if (bitmap != null) {
            previewFragment.onFrame(bitmap)
            if (recorder.isRecording) recorder.putFrame(bitmap)
        }
    }

    companion object {
        // private const val TAG = "MainActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        private const val CAMERA_IDX_KEY = "camera_cameraIdx"

        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }
}