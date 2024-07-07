package com.racoonfox.facefriend

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.view.View
import androidx.camera.core.CameraSelector
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceLandmark

class FaceOverlay(context: Context) : View(context) {

    private var cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
    private var previewWidth = 0
    private var previewHeight = 0
    private var isFrontCamera = true

    private val contourPaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    fun setCameraSelector(selector: CameraSelector) {
        cameraSelector = selector
        isFrontCamera = (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA)
    }

    fun setPreviewSize(width: Int, height: Int) {
        previewWidth = width
        previewHeight = height
    }

    private val facePaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val landmarkPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
        strokeWidth = 2f
    }

    private var faces: List<Face> = emptyList()

    fun setFaces(detectedFaces: List<Face>) {
        faces = detectedFaces
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (face in faces) {
            val bounds = face.boundingBox
            val left = if (isFrontCamera) previewWidth - bounds.right else bounds.left
            val top = bounds.top
            val right = if (isFrontCamera) previewWidth - bounds.left else bounds.right
            val bottom = bounds.bottom

            val adjustedBounds = Rect(left, top, right, bottom)
            canvas.drawRect(adjustedBounds, facePaint)

            drawLandmark(canvas, face, FaceLandmark.LEFT_EAR)
            drawLandmark(canvas, face, FaceLandmark.RIGHT_EAR)
            drawLandmark(canvas, face, FaceLandmark.LEFT_EYE)
            drawLandmark(canvas, face, FaceLandmark.RIGHT_EYE)
            drawLandmark(canvas, face, FaceLandmark.NOSE_BASE)
            drawLandmark(canvas, face, FaceLandmark.MOUTH_LEFT)
            drawLandmark(canvas, face, FaceLandmark.MOUTH_RIGHT)


            drawContour(canvas, face, FaceContour.FACE)
            drawContour(canvas, face, FaceContour.LEFT_EYEBROW_TOP)
            drawContour(canvas, face, FaceContour.RIGHT_EYEBROW_TOP)
            drawContour(canvas, face, FaceContour.LEFT_EYE)
            drawContour(canvas, face, FaceContour.RIGHT_EYE)
            drawContour(canvas, face, FaceContour.UPPER_LIP_TOP)
            drawContour(canvas, face, FaceContour.LOWER_LIP_BOTTOM)


        }
    }

    private fun drawContour(canvas: Canvas, face: Face, contourType: Int) {
        val contour = face.getContour(contourType)
        contour?.let {
            val path = Path()
            var firstPoint: PointF? = null

            for (point in contour.points) {
                val x = if (isFrontCamera) previewWidth - point.x else point.x
                val y = point.y

                if (firstPoint == null) {
                    firstPoint = PointF(x, y)
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            firstPoint?.let {
                path.lineTo(it.x, it.y)
            }

            canvas.drawPath(path, contourPaint)
        }
    }

    private fun drawLandmark(canvas: Canvas, face: Face, landmarkType: Int) {
        val landmark = face.getLandmark(landmarkType)
        landmark?.let {
            val x = if (isFrontCamera) previewWidth - it.position.x else it.position.x
            val y = it.position.y
            canvas.drawCircle(x, y, 8f, landmarkPaint)
        }
    }
}