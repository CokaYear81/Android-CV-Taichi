package com.lenovo.taichivision.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.lenovo.taichivision.pose.PoseResultBundle
import kotlin.math.min

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val pointPaint = Paint().apply {
        color = Color.CYAN
        style = Paint.Style.FILL
        strokeWidth = 8f
        isAntiAlias = true
    }

    private val linePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    private var poseResult: PoseResultBundle? = null

    private val primaryConnections = com.lenovo.taichivision.pose.PoseLandmarkSubset.DISPLAY_CONNECTIONS

    fun setResults(result: PoseResultBundle?) {
        poseResult = result
        invalidate()
    }

    fun clear() {
        poseResult = null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val result = poseResult ?: return
        if (!result.hasPose || result.landmarks.isEmpty()) {
            return
        }

        val displayLandmarks = com.lenovo.taichivision.pose.PoseLandmarkSubset.selectLandmarks(result.landmarks)
        if (displayLandmarks.isEmpty()) {
            return
        }

        drawConnections(canvas, displayLandmarks)
        drawLandmarks(canvas, displayLandmarks)
    }

    private fun drawConnections(canvas: Canvas, landmarks: List<NormalizedLandmark>) {
        val result = poseResult ?: return
        primaryConnections.forEach { (startIndex, endIndex) ->
            if (startIndex >= landmarks.size || endIndex >= landmarks.size) {
                return@forEach
            }

            val start = landmarks[startIndex]
            val end = landmarks[endIndex]
            val (startX, startY) = mapLandmarkToOverlay(start, result)
            val (endX, endY) = mapLandmarkToOverlay(end, result)
            canvas.drawLine(
                startX,
                startY,
                endX,
                endY,
                linePaint
            )
        }
    }

    private fun drawLandmarks(canvas: Canvas, landmarks: List<NormalizedLandmark>) {
        val result = poseResult ?: return
        landmarks.forEach { landmark ->
            val (mappedX, mappedY) = mapLandmarkToOverlay(landmark, result)
            canvas.drawCircle(
                mappedX,
                mappedY,
                10f,
                pointPaint
            )
        }
    }

    private fun mapLandmarkToOverlay(
        landmark: NormalizedLandmark,
        result: PoseResultBundle
    ): Pair<Float, Float> {
        if (result.inputImageWidth <= 0 || result.inputImageHeight <= 0 || width <= 0 || height <= 0) {
            return 0f to 0f
        }

        val normalizedX = landmark.x()
        val normalizedY = landmark.y()

        val normalizedRotation = ((result.rotationDegrees % 360) + 360) % 360
        val (rotatedX, rotatedY) = when (normalizedRotation) {
            90 -> (1f - normalizedY) to normalizedX
            180 -> (1f - normalizedX) to (1f - normalizedY)
            270 -> normalizedY to (1f - normalizedX)
            else -> normalizedX to normalizedY
        }

        val (rotatedSourceWidth, rotatedSourceHeight) = when (normalizedRotation) {
            90, 270 -> result.inputImageHeight.toFloat() to result.inputImageWidth.toFloat()
            else -> result.inputImageWidth.toFloat() to result.inputImageHeight.toFloat()
        }

        val scale = min(width / rotatedSourceWidth, height / rotatedSourceHeight)
        val displayWidth = rotatedSourceWidth * scale
        val displayHeight = rotatedSourceHeight * scale
        val offsetX = (width - displayWidth) / 2f
        val offsetY = (height - displayHeight) / 2f

        val pixelX = offsetX + rotatedX * displayWidth
        val pixelY = offsetY + rotatedY * displayHeight

        return pixelX to pixelY
    }
}
