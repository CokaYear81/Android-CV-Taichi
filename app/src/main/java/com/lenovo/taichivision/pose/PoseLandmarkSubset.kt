package com.lenovo.taichivision.pose

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

object PoseLandmarkSubset {
    const val SCHEMA_VERSION = "pose17_v1"
    const val OUTPUT_LANDMARK_COUNT = 17

    val DISPLAY_CONNECTIONS = listOf(
        0 to 1,
        0 to 2,
        1 to 2,
        1 to 3,
        3 to 5,
        2 to 4,
        4 to 6,
        1 to 7,
        2 to 8,
        7 to 8,
        7 to 9,
        9 to 11,
        8 to 10,
        10 to 12,
        11 to 13,
        13 to 15,
        12 to 14,
        14 to 16
    )

    private val sourceLandmarkIndices = listOf(
        0,
        11, 12,
        13, 14,
        15, 16,
        23, 24,
        25, 26,
        27, 28,
        29, 30,
        31, 32
    )

    fun selectLandmarks(landmarks: List<NormalizedLandmark>): List<NormalizedLandmark> {
        if (landmarks.size == OUTPUT_LANDMARK_COUNT) {
            return landmarks.take(OUTPUT_LANDMARK_COUNT)
        }

        return sourceLandmarkIndices.mapNotNull { index ->
            landmarks.getOrNull(index)
        }
    }
}
