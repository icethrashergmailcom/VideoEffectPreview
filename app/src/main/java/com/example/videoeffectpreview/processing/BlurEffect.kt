package com.example.videoeffectpreview.processing

import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

class BlurEffect: ImageProcessor.Effect {
    private var innerSize: Int = 3

    var size: Int
        get() = this.innerSize
        set(value) {
            innerSize = when {
                value < 3 -> 3
                else -> value
            }
        }

    override fun process(input: Mat): Mat {
        val innerSizeD: Double = innerSize.toDouble()

        Imgproc.blur(input, input, Size(innerSizeD, innerSizeD))

        return input
    }
}