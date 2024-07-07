package com.racoonfox.facefriend

import android.annotation.SuppressLint
import android.content.Intent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

@Composable
fun CameraPreview() {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val faceOverlay = remember { FaceOverlay(context) }


    val cameraController = remember {
        LifecycleCameraController(context).apply {
            bindToLifecycle(lifecycleOwner)
        }
    }

    val faceAnalyzer = remember {
        object : ImageAnalysis.Analyzer {
            private val detector = FaceDetection.getClient(
                FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                    .build()
            )

            @SuppressLint("UnsafeOptInUsageError")
            override fun analyze(imageProxy: ImageProxy) {
                val mediaImage = imageProxy.image
                if (mediaImage != null) {
                    val image =
                        InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    detector.process(image)
                        .addOnSuccessListener { faces ->
                            for (face in faces) {

                                faceOverlay.setFaces(faces)

                                val smileProb = face.smilingProbability
                                val leftEyeOpenProb = face.leftEyeOpenProbability
                                val rightEyeOpenProb = face.rightEyeOpenProbability

                                if (smileProb != null) {
                                    if (smileProb >= 0.5) {
                                        println("it smiled!!!")
                                        val intent =
                                            Intent(context, SoundGeneratorService::class.java)
                                        val frequency = 400.0
                                        intent.putExtra(
                                            SoundGeneratorService.EXTRA_FREQUENCY,
                                            frequency
                                        )
                                        context.startService(intent)
                                    }
                                }

                            }
                        }
                        .addOnFailureListener { e ->

                        }
                        .addOnCompleteListener {
                            imageProxy.close()
                        }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                val previewView = PreviewView(context)
                val preview = Preview.Builder().build()
                val selector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build()
                preview.setSurfaceProvider(previewView.surfaceProvider)

                faceOverlay.setCameraSelector(selector)
                faceOverlay.setPreviewSize(previewView.width, previewView.height)

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(ContextCompat.getMainExecutor(context), faceAnalyzer)
                    }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        context as LifecycleOwner,
                        selector,
                        preview,
                        imageAnalysis
                    )
                }, ContextCompat.getMainExecutor(context))

                previewView
            },
            onRelease = {
                cameraController.unbind()
            }
        )
        AndroidView(
            factory = { faceOverlay },
            modifier = Modifier.fillMaxSize()
        )
    }

}