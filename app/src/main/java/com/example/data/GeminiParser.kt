package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiParser {
    private const val TAG = "GeminiParser"
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

    suspend fun parseTransaction(input: String): Result<Transaction> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(Exception("Gemini API key is not configured. Please enter a valid key in the Secrets panel."))
        }

        val systemInstruction = """
            You are a professional financial transaction parser. Extract details from natural language and return a structured JSON response.
            Fields to extract:
            - 'category': A short capitalized category string (e.g. Food, Transport, Utilities, Entertainment, Shopping, Salary, Investment, Housing, Others). Match the closest common category.
            - 'amount': A positive number representing the financial value.
            - 'type': Must be exactly either 'EXPENSE' or 'INCOME'. If they spent money, it is 'EXPENSE'. If they received or earned money (like salary, transfer, refund), it is 'INCOME'.
            - 'description': A short, friendly label for the item (e.g. "Coffee at Starbucks", "Salary payment").
            
            Return ONLY a valid raw JSON object matching this structure:
            {
              "category": "Food",
              "amount": 15.00,
              "type": "EXPENSE",
              "description": "Coffee"
            }
            Do not wrap the response in markdown blocks (like ```json), do not include any explanatory text, and do not add any additional fields.
        """.trimIndent()

        // Construct raw JSON body for Gemini REST API
        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "Parse the following input: '$input'")
                        })
                    })
                })
            })
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", systemInstruction)
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.1) // Low temperature for factual extraction
            })
        }

        val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
        val url = "$BASE_URL?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()
                if (!response.isSuccessful || bodyString == null) {
                    Log.e(TAG, "Request failed: ${response.code} - $bodyString")
                    return@withContext Result.failure(Exception("Gemini API request failed: HTTP ${response.code}"))
                }

                val responseJson = JSONObject(bodyString)
                val candidates = responseJson.optJSONArray("candidates")
                val textResponse = candidates?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text")

                if (textResponse.isNullOrEmpty()) {
                    return@withContext Result.failure(Exception("Empty or invalid response from Gemini API."))
                }

                // Parse the clean JSON object returned by Gemini
                val cleanJsonText = textResponse.trim()
                Log.d(TAG, "Gemini Parsed Response: $cleanJsonText")
                
                val parsedObj = JSONObject(cleanJsonText)
                val category = parsedObj.optString("category", "Others").trim()
                val amount = parsedObj.optDouble("amount", 0.0)
                val typeStr = parsedObj.optString("type", "EXPENSE").uppercase().trim()
                val description = parsedObj.optString("description", "").trim()

                val finalType = if (typeStr == "INCOME" || typeStr == "EXPENSE") typeStr else "EXPENSE"

                val parsedTransaction = Transaction(
                    id = "",
                    amount = amount,
                    category = category.ifEmpty { "Others" },
                    type = finalType,
                    description = description.ifEmpty { input },
                    date = System.currentTimeMillis()
                )

                Result.success(parsedTransaction)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Gemini API", e)
            Result.failure(e)
        }
    }
}
