package com.example.secureflow.ThreatDetection

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class ThreatDetector(context: Context) {

    private var interpreter: Interpreter? = null

    init {
        try {
            interpreter = Interpreter(loadModelFile(context, "network_model.tflite"))
            Log.d("ThreatDetector", "✅ Model loaded successfully")
        } catch (e: IOException) {
            Log.e("ThreatDetector", "❌ Failed to load model: ${e.message}")
        }
    }

    private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )
    }

    /**
     * Predicts the threat score for given input features.
     * @param features FloatArray of 6 values: [port, protocol, size, entropy, reputation, time_delta]
     * @return threat score between 0.0 (benign) and 1.0 (malicious)
     */
    fun predict(features: FloatArray): Float {
        require(features.size == 6) {
            "Expected 6 features but got ${features.size}"
        }

        if (interpreter == null) {
            Log.e("ThreatDetector", "❌ Interpreter not initialized")
            return -1f
        }

        val input = arrayOf(features)
        val output = FloatArray(1)

        try {
            interpreter?.run(input, output)
        } catch (e: Exception) {
            Log.e("ThreatDetector", "❌ Inference failed: ${e.message}")
            return -1f
        }

        Log.d("ThreatDetector", "✅ Prediction complete: ${output[0]}")
        return output[0]
    }

    fun close() {
        interpreter?.close()
    }
}