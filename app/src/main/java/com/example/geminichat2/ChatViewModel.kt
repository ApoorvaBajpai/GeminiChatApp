package com.example.geminichat2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

val generativeModel = GenerativeModel(
    modelName = "gemini-1.5-flash",
    apiKey = com.example.geminichat2.BuildConfig.GEMINI_API_KEY
)

data class ChatMessage(
    val message: String,
    val isUser: Boolean
)

class ChatViewModel : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY

    )

    fun sendMessage(userMessage: String) {
        if (userMessage.isBlank()) return

        _messages.value = _messages.value + ChatMessage(userMessage, true)
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val response = generativeModel.generateContent(userMessage)
                val botReply = response.text ?: "No response"
                _messages.value = _messages.value + ChatMessage(botReply, false)
            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage("Error: ${e.message}", false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendMessageStream(userMessage: String) {
        if (userMessage.isBlank()) return

        _messages.value = _messages.value + ChatMessage(userMessage, true)
        _isLoading.value = true

        viewModelScope.launch {
            try {
                var botResponse = ""
                generativeModel.generateContentStream(userMessage).collect { chunk ->
                    botResponse += (chunk.text ?: "")
                    // update last bot message progressively
                    val withoutUser = _messages.value.filter { it.isUser } + ChatMessage(botResponse, false)
                    val userMsgs = _messages.value.filter { it.isUser }
                    _messages.value = userMsgs + ChatMessage(botResponse, false)
                }
            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage("Error: ${e.message}", false)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
