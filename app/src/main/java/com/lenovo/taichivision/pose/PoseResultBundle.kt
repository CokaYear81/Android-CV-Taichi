package com.lenovo.taichivision.pose

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

data class PoseResultBundle(
    val landmarks: List<NormalizedLandmark>,
    val inputImageWidth: Int,
    val inputImageHeight: Int,
    val rotationDegrees: Int,
    val hasPose: Boolean
)
