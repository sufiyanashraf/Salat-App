package com.example.salattracker.ml

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.imageclassifier.ImageClassifier

/**
 * Wraps MediaPipe's ImageClassifier to determine whether a captured image
 * contains a prayer-mat-like object (rug, doormat, fabric, etc.).
 *
 * Uses a generic EfficientNet-Lite0 model with lenient (~50%) confidence
 * thresholds so it works even in low-light conditions like Fajr or Isha.
 */
class ImageClassifierHelper(private val context: Context) {

    private var imageClassifier: ImageClassifier? = null

    fun setupImageClassifier() {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("efficientnet_lite0.tflite")
            .build()

        val options = ImageClassifier.ImageClassifierOptions.builder()
            .setBaseOptions(baseOptions)
            .setMaxResults(5)
            .build()

        imageClassifier = ImageClassifier.createFromOptions(context, options)
    }

    /**
     * Classify the given bitmap and return `true` if it looks like a prayer mat.
     *
     * @param bitmap          The captured image.
     * @param rotationDegrees Rotation from [ImageProxy.imageInfo.rotationDegrees].
     * @return `true` when any top result matches a mat-like label with >50% confidence.
     */
    fun classify(bitmap: Bitmap, rotationDegrees: Int): Boolean {
        val mpImage = BitmapImageBuilder(bitmap).build()

        val imageProcessingOptions = ImageProcessingOptions.builder()
            .setRotationDegrees(rotationDegrees)
            .build()

        val results = imageClassifier?.classify(mpImage, imageProcessingOptions)

        val validLabels = listOf("rug", "doormat", "fabric", "velvet", "window shade")
        return results?.classificationResult()?.classifications()?.firstOrNull()?.categories()?.any { category ->
            validLabels.contains(category.categoryName()) && category.score() > 0.5f
        } == true
    }
}
