package com.example.videoeffectpreview.recorder

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.media.MediaRecorder
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


// TODO: This implementation is quick and dirty

class Recorder {
    private var mediaRecorder: MediaRecorder? = null
    private val mediaRecorderCs = ReentrantLock()
    var filePath: String? = null

    val isRecording: Boolean
        get() {
            // weak, but keep for now
            return mediaRecorder != null
        }

    fun start(fileName: String, width: Int, height: Int) {
        mediaRecorderCs.withLock {
            if (mediaRecorder != null) innerStop()
            mediaRecorder = MediaRecorder()

            val m = mediaRecorder!!

            // Hardcode is OK for now
            m.setAudioSource(MediaRecorder.AudioSource.MIC)
            m.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            m.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            m.setVideoEncodingBitRate(100000000)
            m.setVideoFrameRate(30)
            m.setVideoSize(width, height)
            m.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            m.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            m.setAudioEncodingBitRate(128000)
            m.setAudioSamplingRate(44100)
            m.setOutputFile(fileName)
            m.prepare()
            m.start()

            filePath = fileName
        }
    }

    private fun innerStop() {
        mediaRecorder ?: return

        with (mediaRecorder!!) {
            stop()
            reset()
            release()
        }
        mediaRecorder = null
    }

    fun stop() {
        mediaRecorderCs.withLock {
            innerStop()
        }
    }

    fun putFrame(bitmap: Bitmap) {
        // TODO: need synchronization, but catch will smooth races for now
        try {
            val surface = mediaRecorder!!.surface
            val canvas: Canvas = surface.lockCanvas(null)
            canvas.drawBitmap(bitmap, 0F, 0F, Paint())
            surface.unlockCanvasAndPost(canvas)
        } catch(exc: Exception) { }
    }
}