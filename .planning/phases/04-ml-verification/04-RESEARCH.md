# Phase 4 Research: ML & Verification

## Validation Architecture
The core logic relies on a lenient verification (~50% confidence) of a pre-trained Image Classification model executing on-device. The validation flow goes:
1. User clicks "Verify with Camera".
2. CameraX captures a high-resolution frame (`ImageCapture`).
3. The frame is converted to a `Bitmap` via `ImageProxy.toBitmap()`.
4. MediaPipe Tasks Vision (`ImageClassifier`) processes the `Bitmap` asynchronously.
5. If target classes (e.g., "rug", "fabric", "doormat") meet the confidence threshold, the lock overlay is dismissed. Otherwise, instant retry is permitted without closing the camera.

## Standard Stack
- **CameraX (1.6.1)**: Essential for providing a stable camera stream and managing the camera lifecycle. Specifically use `camera-core`, `camera-camera2`, `camera-lifecycle`, and `camera-view`.
- **MediaPipe Tasks Vision (0.10.35)**: The required library for on-device ML image classification (`com.google.mediapipe:tasks-vision:0.10.35`).
- **Generic Pre-trained Model**: Download a generic `.task` or `.tflite` file (e.g., EfficientNet-Lite0 or MobileNetV2) trained on ImageNet/COCO. These models include built-in labels such as "rug", "doormat", "velvet", or "window shade" which can act as a proxy for "prayer mat".

## Architecture Patterns
- **Jetpack Compose + CameraX `AndroidView`**: Wrap CameraX's `PreviewView` in a Compose `AndroidView` for the modal overlay UI.
- **ImageClassifierHelper**: Abstract ML initialization, inference, and callback handling into a dedicated `ImageClassifierHelper` class. This isolates MediaPipe logic from the UI layer.
- **ImageCapture Usecase**: To support the "take a picture" requirement explicitly, use the `ImageCapture` usecase rather than live `ImageAnalysis`. Wait for the user to manually trigger the capture.
- **Lifecycle Integration**: Bind `ProcessCameraProvider` with `LocalLifecycleOwner.current` to tie the camera lifecycle cleanly to the Compose overlay.

## Don't Hand-Roll
- **YUV to Bitmap Conversion**: Do not write custom YUV-to-RGB byte array converters. In CameraX 1.6.1, use the natively provided `ImageProxy.toBitmap()` extension function.
- **Custom ML Model Training**: Do not train a custom TensorFlow model or build one in Model Maker just yet. Standard image classification models are sufficient for the ~50% confidence leniency constraint.
- **Camera Device Management**: Do not manually open/close camera devices using Camera2 APIs. Rely exclusively on `ProcessCameraProvider`.

## Common Pitfalls
- **Forgetting to close ImageProxy**: You MUST call `imageProxy.close()` after converting the image to a Bitmap. If left unclosed, CameraX will freeze and stop delivering subsequent frames, completely breaking the instant retry loop.
- **Blocking the UI Thread**: Image classification must not run on the main thread. Ensure `classifier.classify()` runs on a background dispatcher (`Dispatchers.IO` or `Dispatchers.Default`).
- **Orientation Mismatches**: MediaPipe requires the image to be properly rotated. You must pass `imageProxy.imageInfo.rotationDegrees` to the `ImageProcessingOptions` (or manually rotate the Bitmap) before inference to ensure accurate detection.
- **Permission Lifecycle in Overlay**: The overlay might be drawn over a locked device. Ensure runtime camera permissions are correctly requested and granted before attempting to bind the camera provider, and configure the window flags properly (e.g., `FLAG_SHOW_WHEN_LOCKED`).

## Code Examples

### 1. Image Classification Helper
```kotlin
import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.imageclassifier.ImageClassifier

class ImageClassifierHelper(private val context: Context) {
    private var imageClassifier: ImageClassifier? = null

    fun setupImageClassifier() {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("efficientnet_lite0.tflite") // or relevant generic model
            .build()
        
        val options = ImageClassifier.ImageClassifierOptions.builder()
            .setBaseOptions(baseOptions)
            .setMaxResults(5)
            .build()

        imageClassifier = ImageClassifier.createFromOptions(context, options)
    }

    fun classify(bitmap: Bitmap, rotationDegrees: Int): Boolean {
        val mpImage = BitmapImageBuilder(bitmap).build()
        
        val imageProcessingOptions = ImageProcessingOptions.builder()
            .setRotationDegrees(rotationDegrees)
            .build()

        val results = imageClassifier?.classify(mpImage, imageProcessingOptions)
        
        // Lenient verification: check if ANY top result is a mat-like object
        val validLabels = listOf("rug", "doormat", "fabric", "velvet")
        return results?.classificationResult()?.classifications()?.firstOrNull()?.categories()?.any { category ->
            validLabels.contains(category.categoryName()) && category.score() > 0.5f
        } == true
    }
}
```

### 2. CameraX ImageCapture processing with Coroutines
```kotlin
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun captureAndVerify(
    imageCapture: ImageCapture, 
    executor: Executor,
    classifierHelper: ImageClassifierHelper
): Boolean = withContext(Dispatchers.IO) {
    suspendCancellableCoroutine { continuation ->
        imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                try {
                    val bitmap = imageProxy.toBitmap()
                    val rotation = imageProxy.imageInfo.rotationDegrees
                    val success = classifierHelper.classify(bitmap, rotation)
                    continuation.resume(success)
                } finally {
                    imageProxy.close() // CRITICAL: Avoid freezing camera
                }
            }

            override fun onError(exception: ImageCaptureException) {
                continuation.resumeWithException(exception)
            }
        })
    }
}
```
