package sszj.s.geminiapi.data.repository

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import sszj.s.geminiapi.data.model.Chat

class ChatRepository {

    private val apiKey = "AIzaSyDdQWIl6318o_x3jMu4XNxA804StM372GA" // Paste your API key here

    private val generativeModel by lazy {
        GenerativeModel(modelName = "gemini-pro", apiKey = apiKey)
    }

    private val generativeModelWithVision by lazy {
        GenerativeModel(modelName = "gemini-1.5-flash", apiKey = apiKey)
    }

    suspend fun getResponse(prompt: String): Chat {
        return try {
            val response = withContext(Dispatchers.IO) {
                generativeModel.generateContent(prompt)
            }

            Chat(
                prompt = response.text ?: "error",
                imageBitmap = null,
                isUser = false
            )
        } catch (e: Exception) {
            Chat(
                prompt = e.message ?: "error",
                imageBitmap = null,
                isUser = false
            )
        }
    }

    suspend fun getResponseWithImage(prompt: String, bitmap: Bitmap): Chat {
        return try {
            val inputContent = content {
                image(bitmap)
                text(prompt)
            }

            val response = withContext(Dispatchers.IO) {
                generativeModelWithVision.generateContent(inputContent)
            }

            Chat(
                prompt = response.text ?: "error",
                imageBitmap = null,
                isUser = false
            )
        } catch (e: Exception) {
            Chat(
                prompt = e.message ?: "error",
                imageBitmap = null,
                isUser = false
            )
        }
    }
}
