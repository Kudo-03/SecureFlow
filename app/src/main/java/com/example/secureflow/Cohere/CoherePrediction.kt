package com.example.secureflow.Cohere

data class CoherePrediction( val input: String,
                             val prediction: String,
                             val confidence: Double)