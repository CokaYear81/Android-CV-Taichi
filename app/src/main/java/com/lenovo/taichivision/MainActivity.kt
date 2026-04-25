package com.lenovo.taichivision

import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
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
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.lenovo.taichivision.data.CaptureMetadata
import com.lenovo.taichivision.data.LandmarkRecord
import com.lenovo.taichivision.data.PoseFrameRecord
import com.lenovo.taichivision.data.PoseSampleRecord
import com.lenovo.taichivision.data.PoseSampleWriter
import com.lenovo.taichivision.pose.PoseResultBundle
import com.lenovo.taichivision.ui.OverlayView
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.ArrayDeque
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private enum class CaptureState {
        IDLE,
        RECORDING,
        STOPPING
    }

    private data class PendingFrameInfo(
        val frameIndex: Int,
        val timestampMs: Long,
        val imageWidth: Int,
        val imageHeight: Int,
        val rotationDegrees: Int
    )

    private lateinit var previewView: PreviewView
    private lateinit var overlayView: OverlayView
    private lateinit var statusTextView: TextView
    private lateinit var subjectIdEditText: EditText
    private lateinit var actionNameEditText: EditText
    private lateinit var captureButton: Button
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var poseLandmarker: PoseLandmarker? = null
    @Volatile
    private var lastFrameWidth = 0
    @Volatile
    private var lastFrameHeight = 0
    @Volatile
    private var lastRotationDegrees = 0
    private val captureLock = Any()
    private var captureState = CaptureState.IDLE
    private var currentCaptureMetadata: CaptureMetadata? = null
    private val currentFrameRecords = mutableListOf<PoseFrameRecord>()
    private val pendingFrameInfos = ArrayDeque<PendingFrameInfo>()
    private var captureStartFrameTimestampMs: Long? = null
    private var nextFrameIndex = 0

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
        subjectIdEditText = findViewById(R.id.subjectIdEditText)
        actionNameEditText = findViewById(R.id.actionNameEditText)
        captureButton = findViewById(R.id.captureButton)

        setupCaptureControls()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupPoseLandmarker()
        checkCameraPermission()
    }

    private fun setupCaptureControls() {
        captureButton.setOnClickListener {
            when (captureState) {
                CaptureState.IDLE -> startCapture()
                CaptureState.RECORDING -> stopCapture()
                CaptureState.STOPPING -> statusTextView.text = "Capture is stopping..."
            }
        }
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
                    appendCapturedFrameIfNeeded(poseLandmarks, poseCount > 0)
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
            enqueueCaptureFrameIfNeeded(
                timestampMs = timestampMs,
                imageWidth = imageProxy.width,
                imageHeight = imageProxy.height,
                rotationDegrees = imageProxy.imageInfo.rotationDegrees
            )
        } catch (e: Exception) {
            runOnUiThread {
                statusTextView.text = "Pose analyze failed: ${e.message}"
                overlayView.clear()
            }
        } finally {
            imageProxy.close()
        }
    }

    private fun startCapture() {
        if (captureState != CaptureState.IDLE) {
            return
        }

        val subjectId = subjectIdEditText.text.toString().trim()
        val actionName = actionNameEditText.text.toString().trim()

        if (subjectId.isEmpty() || actionName.isEmpty()) {
            statusTextView.text = "subject_id and action_name are required."
            return
        }

        val sampleId = buildSampleId(subjectId, actionName)
        val captureStartedAt = currentIsoTimestamp()

        synchronized(captureLock) {
            captureState = CaptureState.RECORDING
            currentCaptureMetadata = CaptureMetadata(
                sampleId = sampleId,
                subjectId = subjectId,
                actionName = actionName,
                captureStartedAt = captureStartedAt,
                deviceId = buildDeviceId(),
                videoFile = null
            )
            currentFrameRecords.clear()
            pendingFrameInfos.clear()
            captureStartFrameTimestampMs = null
            nextFrameIndex = 0
        }

        setCaptureInputsEnabled(false)
        captureButton.text = "Stop Capture"
        statusTextView.text = "Capture started: $sampleId"
    }

    private fun stopCapture() {
        if (captureState != CaptureState.RECORDING) {
            return
        }

        captureState = CaptureState.STOPPING
        captureButton.isEnabled = false
        statusTextView.text = "Stopping capture..."
        cameraExecutor.execute {
            repeat(10) {
                val queueEmpty = synchronized(captureLock) { pendingFrameInfos.isEmpty() }
                if (queueEmpty) {
                    return@repeat
                }
                Thread.sleep(50)
            }
            finalizeCaptureAndWriteJson()
        }
    }

    private fun finalizeCaptureAndWriteJson() {
        val sampleRecord = synchronized(captureLock) {
            val metadata = currentCaptureMetadata ?: return@synchronized null
            PoseSampleRecord(
                sampleId = metadata.sampleId,
                landmarkSchemaVersion = com.lenovo.taichivision.pose.PoseLandmarkSubset.SCHEMA_VERSION,
                subjectId = metadata.subjectId,
                actionName = metadata.actionName,
                captureStartedAt = metadata.captureStartedAt,
                captureEndedAt = currentIsoTimestamp(),
                deviceId = metadata.deviceId,
                imageWidth = if (metadata.imageWidth > 0) metadata.imageWidth else lastFrameWidth,
                imageHeight = if (metadata.imageHeight > 0) metadata.imageHeight else lastFrameHeight,
                rotationDegrees = metadata.rotationDegrees,
                videoFile = metadata.videoFile,
                isStandard = metadata.isStandard,
                errorTags = metadata.errorTags,
                frames = currentFrameRecords.toList()
            )
        }

        if (sampleRecord == null) {
            runOnUiThread {
                setCaptureInputsEnabled(true)
                captureButton.isEnabled = true
                captureButton.text = "Start Capture"
                statusTextView.text = "Capture state is missing."
            }
            return
        }

        try {
            PoseSampleWriter.writeSample(this, sampleRecord)
            runOnUiThread {
                statusTextView.text = "Capture saved: ${sampleRecord.sampleId}"
            }
        } catch (e: Exception) {
            runOnUiThread {
                statusTextView.text = "JSON write failed: ${e.message}"
            }
        } finally {
            synchronized(captureLock) {
                captureState = CaptureState.IDLE
                currentCaptureMetadata = null
                currentFrameRecords.clear()
                pendingFrameInfos.clear()
                captureStartFrameTimestampMs = null
                nextFrameIndex = 0
            }
            runOnUiThread {
                setCaptureInputsEnabled(true)
                captureButton.isEnabled = true
                captureButton.text = "Start Capture"
            }
        }
    }

    private fun enqueueCaptureFrameIfNeeded(
        timestampMs: Long,
        imageWidth: Int,
        imageHeight: Int,
        rotationDegrees: Int
    ) {
        synchronized(captureLock) {
            if (captureState != CaptureState.RECORDING) {
                return
            }

            val baseTimestamp = captureStartFrameTimestampMs ?: timestampMs.also {
                captureStartFrameTimestampMs = it
            }
            val relativeTimestampMs = timestampMs - baseTimestamp
            pendingFrameInfos.addLast(
                PendingFrameInfo(
                    frameIndex = nextFrameIndex,
                    timestampMs = relativeTimestampMs,
                    imageWidth = imageWidth,
                    imageHeight = imageHeight,
                    rotationDegrees = rotationDegrees
                )
            )
            nextFrameIndex += 1

            val metadata = currentCaptureMetadata
            if (metadata != null && metadata.imageWidth == 0 && metadata.imageHeight == 0) {
                currentCaptureMetadata = metadata.copy(
                    imageWidth = imageWidth,
                    imageHeight = imageHeight,
                    rotationDegrees = rotationDegrees
                )
            }
        }
    }

    private fun appendCapturedFrameIfNeeded(
        landmarks: List<NormalizedLandmark>,
        hasPose: Boolean
    ) {
        val frameInfo = synchronized(captureLock) {
            if (pendingFrameInfos.isEmpty()) {
                null
            } else {
                pendingFrameInfos.removeFirst()
            }
        } ?: return

        val frameRecord = PoseFrameRecord(
            frameIndex = frameInfo.frameIndex,
            timestampMs = frameInfo.timestampMs,
            hasPose = hasPose,
            poseLandmarks = toFixedLandmarkRecords(landmarks, hasPose)
        )

        synchronized(captureLock) {
            currentFrameRecords.add(frameRecord)
        }
    }

    private fun toFixedLandmarkRecords(
        landmarks: List<NormalizedLandmark>,
        hasPose: Boolean
    ): List<LandmarkRecord> {
        if (!hasPose) {
            return zeroLandmarkRecords()
        }

        val selectedLandmarks =
            com.lenovo.taichivision.pose.PoseLandmarkSubset.selectLandmarks(landmarks)

        val mappedLandmarks = selectedLandmarks.map { landmark ->
            LandmarkRecord(
                x = landmark.x(),
                y = landmark.y(),
                z = landmark.z(),
                visibility = landmark.visibility().orElse(0f)
            )
        }.toMutableList()

        while (mappedLandmarks.size < com.lenovo.taichivision.pose.PoseLandmarkSubset.OUTPUT_LANDMARK_COUNT) {
            mappedLandmarks.add(
                LandmarkRecord(
                    x = 0f,
                    y = 0f,
                    z = 0f,
                    visibility = 0f
                )
            )
        }

        return mappedLandmarks
    }

    private fun zeroLandmarkRecords(): List<LandmarkRecord> =
        List(com.lenovo.taichivision.pose.PoseLandmarkSubset.OUTPUT_LANDMARK_COUNT) {
        LandmarkRecord(
            x = 0f,
            y = 0f,
            z = 0f,
            visibility = 0f
        )
    }

    private fun setCaptureInputsEnabled(enabled: Boolean) {
        subjectIdEditText.isEnabled = enabled
        actionNameEditText.isEnabled = enabled
    }

    private fun buildSampleId(subjectId: String, actionName: String): String {
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.US)
        val timestamp = OffsetDateTime.now(ZoneId.systemDefault()).format(formatter)
        return "${sanitizeForId(subjectId)}_${sanitizeForId(actionName)}_$timestamp"
    }

    private fun sanitizeForId(value: String): String {
        return value.trim()
            .replace(Regex("\\s+"), "_")
            .replace(Regex("[^\\p{L}\\p{N}_-]"), "_")
    }

    private fun currentIsoTimestamp(): String {
        return OffsetDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    private fun buildDeviceId(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL}".trim()
    }

    override fun onDestroy() {
        super.onDestroy()
        poseLandmarker?.close()
        poseLandmarker = null
        cameraExecutor.shutdown()
    }
}
