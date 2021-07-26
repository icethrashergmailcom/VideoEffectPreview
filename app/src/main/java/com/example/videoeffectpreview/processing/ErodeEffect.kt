package com.example.videoeffectpreview.processing

import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

class ErodeEffect: ImageProcessor.Effect {
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

        Imgproc.blur(input, input, Size(13.0, 13.0))

        val element: Mat = Imgproc.getStructuringElement(
            Imgproc.MORPH_ELLIPSE,
            Size(2 * innerSizeD + 1, 2 * innerSizeD + 1),
            Point(innerSizeD, innerSizeD)
        )
        Imgproc.erode(input, input, element)

        return input
    }
}