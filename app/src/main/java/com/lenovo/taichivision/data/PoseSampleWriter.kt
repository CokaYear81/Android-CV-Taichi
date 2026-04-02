package com.lenovo.taichivision.data

import android.content.Context
import java.io.File

object PoseSampleWriter {
    private const val CAPTURES_DIR = "captures"
    private const val LANDMARKS_DIR = "landmarks"
    private const val RAW_VIDEOS_DIR = "raw_videos"

    fun buildVideoFile(context: Context, sampleId: String): File {
        val rawVideosDir = File(File(context.filesDir, CAPTURES_DIR), RAW_VIDEOS_DIR).apply {
            mkdirs()
        }
        return File(rawVideosDir, "$sampleId.mp4")
    }

    fun getRelativeVideoPath(sampleId: String): String = "$RAW_VIDEOS_DIR/$sampleId.mp4"

    fun writeSample(context: Context, sampleRecord: PoseSampleRecord): File {
        val landmarksDir = File(File(context.filesDir, CAPTURES_DIR), LANDMARKS_DIR).apply {
            mkdirs()
        }
        val outputFile = File(landmarksDir, "${sampleRecord.sampleId}.json")
        outputFile.writeText(sampleRecord.toJson().toString(2), Charsets.UTF_8)
        return outputFile
    }
}
