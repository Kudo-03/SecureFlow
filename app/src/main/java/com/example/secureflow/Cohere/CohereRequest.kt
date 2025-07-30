package com.example.secureflow.Cohere

data class CohereRequest(val inputs: List<String>,
                         val examples: List<CohereExample>)