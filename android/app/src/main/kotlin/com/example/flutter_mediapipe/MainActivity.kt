package com.example.flutter_mediapipe

import android.graphics.BitmapFactory
import android.os.Bundle
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import kotlin.math.min

class MainActivity: FlutterActivity() {

    private val channelName = "flutter_mediapipe"
    private var handLandmarker: HandLandmarker? = null

    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupHandLandMarker()
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        val channel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, channelName)
        channel.setMethodCallHandler { call, result ->

            if (call.method == "handMarker") {
                try {
                    val landMarkers = detectHandMarks(call.arguments as Map<*, *>)
                    val resultPoints = mapPointsAndLines(landMarkers)

                    result.success(mapOf(
                        "points" to resultPoints?.points,
                        "lines" to resultPoints?.lines
                    ))

                } catch (e: Exception) {
                    result.error("Kotlin => ", e.message, e.stackTraceToString())
                }
            } else {
                result.notImplemented()
            }
        }
    }

    fun setupHandLandMarker() {
        val baseOptionBuilder = BaseOptions.builder()
        baseOptionBuilder.setDelegate(Delegate.GPU)
        baseOptionBuilder.setModelAssetPath(MP_HAND_LANDMARKER_TASK)

        val baseOptions = baseOptionBuilder.build()
        val optionsBuilder =
            HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setMinHandDetectionConfidence(DEFAULT_HAND_DETECTION_CONFIDENCE)
                .setMinTrackingConfidence(DEFAULT_HAND_TRACKING_CONFIDENCE)
                .setMinHandPresenceConfidence(DEFAULT_HAND_PRESENCE_CONFIDENCE)
                .setNumHands(DEFAULT_NUM_HANDS)
                .setRunningMode(RunningMode.IMAGE)

        val options = optionsBuilder.build()
        handLandmarker = HandLandmarker.createFromOptions(context, options)
    }

    fun detectHandMarks(arguments: Map<*, *>): List<HandLandmarkerResult>? {

        val bytes = arguments["bytes"] as ByteArray
        val width = arguments["width"] as Int
        val height = arguments["height"] as Int

        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val mpImage = BitmapImageBuilder(bitmap).build()

        imageHeight = bitmap.width
        imageWidth = bitmap.height
        scaleFactor = min(width * 1f / imageWidth, height * 1f / imageHeight)

        handLandmarker?.detect(mpImage)?.also { landmarkResult ->
            return listOf(landmarkResult)
        }

        return null
    }

    fun mapPointsAndLines(handMarks: List<HandLandmarkerResult>?): ResultPoints? {
        if (handMarks == null) return null

        val points = mutableListOf<List<Float>>()
        val lines = mutableListOf<List<List<Float>>>()

        for (handMark in handMarks) {
            for (landmark in handMark.landmarks()) {
                for (normalizedLandmark in landmark) {
                   points.add(listOf(
                       normalizedLandmark.x() * imageWidth * scaleFactor,
                       normalizedLandmark.y() * imageHeight * scaleFactor,
                   ))
                }

                HandLandmarker.HAND_CONNECTIONS.forEach {
                    val start = listOf(
                        landmark.get(it!!.start()).x() * imageWidth * scaleFactor,
                        landmark.get(it.start()).y() * imageHeight * scaleFactor,
                    )
                    val end = listOf(
                        landmark.get(it.end()).x() * imageWidth * scaleFactor,
                        landmark.get(it.end()).y() * imageHeight * scaleFactor,
                    )
                    lines.add(listOf(start, end))
                }
            }
        }

        return ResultPoints(points, lines)
    }

    companion object {
        private const val MP_HAND_LANDMARKER_TASK = "hand_landmarker.task"

        const val DEFAULT_HAND_DETECTION_CONFIDENCE = 0.5F
        const val DEFAULT_HAND_TRACKING_CONFIDENCE = 0.5F
        const val DEFAULT_HAND_PRESENCE_CONFIDENCE = 0.5F
        const val DEFAULT_NUM_HANDS = 1
    }

    data class ResultPoints(
        val points: List<List<Float>>,
        val lines: List<List<List<Float>>>
    )

}

