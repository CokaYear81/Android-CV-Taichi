package com.lenovo.taichivision

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.lenovo.taichivision.pose.PoseResultBundle
import com.lenovo.taichivision.ui.OverlayView
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var overlayView: OverlayView
    private lateinit var statusTextView: TextView
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var poseLandmarker: PoseLandmarker? = null
    @Volatile
    private var lastFrameWidth = 0
    @Volatile
    private var lastFrameHeight = 0
    @Volatile
    private var lastRotationDegrees = 0

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                statusTextView.text = "Permission granted, starting camera..."
                startCameraPreview()
            } else {
                statusTextView.text = "Camera permission denied."
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        overlayView = findViewById(R.id.overlayView)
        statusTextView = findViewById(R.id.statusTextView)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupPoseLandmarker()
        checkCameraPermission()
    }

    private fun setupPoseLandmarker() {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("pose_landmarker_lite.task")
                .build()

            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumPoses(1)
                .setMinPoseDetectionConfidence(0.5f)
                .setMinPosePresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setResultListener { result: PoseLandmarkerResult, _: MPImage ->
                    val poseCount = result.landmarks().size
                    val landmarkCount = result.landmarks().firstOrNull()?.size ?: 0
                    val poseLandmarks = result.landmarks().firstOrNull().orEmpty()
                    runOnUiThread {
                        statusTextView.text = if (poseCount > 0) {
                            "Pose detected: $poseCount pose(s), $landmarkCount landmarks."
                        } else {
                            "No pose detected."
                        }
                        if (poseCount > 0) {
                            overlayView.setResults(
                                PoseResultBundle(
                                    landmarks = poseLandmarks,
                                    inputImageWidth = lastFrameWidth,
                                    inputImageHeight = lastFrameHeight,
                                    rotationDegrees = lastRotationDegrees,
                                    hasPose = true
                                )
                            )
                        } else {
                            overlayView.clear()
                        }
                    }
                }
                .setErrorListener { error ->
                    runOnUiThread {
                        statusTextView.text = "Pose error: ${error.message}"
                        overlayView.clear()
                    }
                }
                .build()

            poseLandmarker = PoseLandmarker.createFromOptions(this, options)
            statusTextView.text = "Pose landmarker ready."
        } catch (e: Exception) {
            statusTextView.text = "Pose setup failed: ${e.message}"
        }
    }

    private fun checkCameraPermission() {
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            statusTextView.text = "Permission granted, starting camera..."
            startCameraPreview()
        } else {
            statusTextView.text = "Requesting camera permission..."
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCameraPreview() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also { analysis ->
                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    analyzePoseFrame(imageProxy)
                }
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
                statusTextView.text = "Camera preview started."
            } catch (exception: Exception) {
                statusTextView.text = "Camera start failed: ${exception.message}"
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun analyzePoseFrame(imageProxy: ImageProxy) {
        val poseDetector = poseLandmarker
        if (poseDetector == null) {
            imageProxy.close()
            return
        }

        try {
            lastFrameWidth = imageProxy.width
            lastFrameHeight = imageProxy.height
            lastRotationDegrees = imageProxy.imageInfo.rotationDegrees
            val bitmap = imageProxy.toBitmap()
            val mpImage = BitmapImageBuilder(bitmap).build()
            val imageProcessingOptions = ImageProcessingOptions.builder()
                .setRotationDegrees(imageProxy.imageInfo.rotationDegrees)
                .build()
            val timestampMs = imageProxy.imageInfo.timestamp / 1_000_000L

            poseDetector.detectAsync(mpImage, imageProcessingOptions, timestampMs)
        } catch (e: Exception) {
            runOnUiThread {
                statusTextView.text = "Pose analyze failed: ${e.message}"
                overlayView.clear()
            }
        } finally {
            imageProxy.close()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        poseLandmarker?.close()
        poseLandmarker = null
        cameraExecutor.shutdown()
    }
}
