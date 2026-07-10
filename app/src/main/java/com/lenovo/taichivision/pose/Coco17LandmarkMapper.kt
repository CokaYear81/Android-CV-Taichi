package com.lenovo.taichivision.pose

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

object Coco17LandmarkMapper {
    const val SCHEMA_VERSION = "coco17_from_mediapipe_v1"
    const val OUTPUT_LANDMARK_COUNT = 17

    private val sourceLandmarkIndices = listOf(
        0,  // nose
        2,  // left_eye
        5,  // right_eye
        7,  // left_ear
        8,  // right_ear
        11, // left_shoulder
        12, // right_shoulder
        13, // left_elbow
        14, // right_elbow
        15, // left_wrist
        16, // right_wrist
        23, // left_hip
        24, // right_hip
        25, // left_knee
        26, // right_knee
        27, // left_ankle
        28  // right_ankle
    )

    fun selectLandmarks(landmarks: List<NormalizedLandmark>): List<NormalizedLandmark> =
        sourceLandmarkIndices.mapNotNull { index ->
            landmarks.getOrNull(index)
        }
}
