package com.lenovo.taichivision.data

import org.json.JSONArray
import org.json.JSONObject

data class CaptureMetadata(
    val sampleId: String,
    val subjectId: String,
    val actionName: String,
    val captureStartedAt: String,
    val captureEndedAt: String? = null,
    val deviceId: String,
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val rotationDegrees: Int = 0,
    val videoFile: String? = null,
    val isStandard: Boolean? = null,
    val errorTags: List<String> = emptyList()
)

data class LandmarkRecord(
    val x: Float,
    val y: Float,
    val z: Float,
    val visibility: Float
) {
    fun toJson(): JSONObject = JSONObject()
        .put("x", x.toDouble())
        .put("y", y.toDouble())
        .put("z", z.toDouble())
        .put("visibility", visibility.toDouble())
}

data class PoseFrameRecord(
    val frameIndex: Int,
    val timestampMs: Long,
    val hasPose: Boolean,
    val poseLandmarks: List<LandmarkRecord>
) {
    fun toJson(): JSONObject = JSONObject()
        .put("frame_index", frameIndex)
        .put("timestamp_ms", timestampMs)
        .put("has_pose", hasPose)
        .put(
            "pose_landmarks",
            JSONArray().apply {
                poseLandmarks.forEach { put(it.toJson()) }
            }
        )
}

data class PoseSampleRecord(
    val sampleId: String,
    val landmarkSchemaVersion: String,
    val subjectId: String,
    val actionName: String,
    val captureStartedAt: String,
    val captureEndedAt: String,
    val deviceId: String,
    val imageWidth: Int,
    val imageHeight: Int,
    val rotationDegrees: Int,
    val videoFile: String? = null,
    val isStandard: Boolean?,
    val errorTags: List<String>,
    val frames: List<PoseFrameRecord>
) {
    fun toJson(): JSONObject = JSONObject()
        .put("sample_id", sampleId)
        .put("landmark_schema_version", landmarkSchemaVersion)
        .put("subject_id", subjectId)
        .put("action_name", actionName)
        .put("capture_started_at", captureStartedAt)
        .put("capture_ended_at", captureEndedAt)
        .put("device_id", deviceId)
        .put("image_width", imageWidth)
        .put("image_height", imageHeight)
        .put("rotation_degrees", rotationDegrees)
        .put("video_file", videoFile?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
        .put("is_standard", isStandard?.let { if (it) true else false } ?: JSONObject.NULL)
        .put(
            "error_tags",
            JSONArray().apply {
                errorTags.forEach { put(it) }
            }
        )
        .put(
            "frames",
            JSONArray().apply {
                frames.forEach { put(it.toJson()) }
            }
        )
}
