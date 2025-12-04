package com.example.geminichat2

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminichat2.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive

class ChatViewModel : ViewModel() {
    private val TAG = "ChatViewModel"

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Use API key from BuildConfig
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    fun sendMessage(userMessage: String) {
        Log.d(TAG, "sendMessage called with message length: ${userMessage.length}")
        if (userMessage.isBlank()) {
            Log.d(TAG, "Message is blank, returning")
            return
        }

        try {
            // Add user message to the list
            Log.d(TAG, "Adding user message to list. Current message count: ${_messages.value.size}")
            _messages.value += ChatMessage(userMessage, true)
            Log.d(TAG, "User message added. New message count: ${_messages.value.size}")

            // Recreate the history from the current message list for the API call
            Log.d(TAG, "Creating history from ${_messages.value.size} messages")
            val history = _messages.value.map {
                content(if (it.isFromUser) "user" else "model") {
                    text(it.text)
                }
            }
            Log.d(TAG, "History created with ${history.size} items")

            viewModelScope.launch {
                try {
                    Log.d(TAG, "Starting content stream generation")
                    // Create a placeholder for the bot's response
                    val botMessagePlaceholder = ChatMessage("", false)
                    _messages.value += botMessagePlaceholder
                    val botMessageIndex = _messages.value.lastIndex
                    Log.d(TAG, "Created bot message placeholder at index: $botMessageIndex")

                    var chunkCount = 0
                    var totalTextLength = 0L
                    val textBuffer = StringBuilder()
                    var pendingUpdateJob: Job? = null
                    var lastUpdateTime = System.currentTimeMillis()
                    val UPDATE_INTERVAL_MS = 150L // Update UI every 150ms
                    val MIN_CHUNKS_FOR_UPDATE = 3 // Or every 3 chunks, whichever comes first

                    // Function to update the message state
                    fun updateMessageState() {
                        try {
                            val currentMessages = _messages.value
                            if (botMessageIndex >= currentMessages.size) {
                                Log.e(TAG, "ERROR: botMessageIndex $botMessageIndex >= messages.size ${currentMessages.size}")
                                return
                            }

                            val currentText = textBuffer.toString()
                            if (currentText.isNotEmpty()) {
                                _messages.value = currentMessages.toMutableList().also {
                                    it[botMessageIndex] = it[botMessageIndex].copy(text = currentText)
                                }
                                if (chunkCount % 50 == 0) {
                                    Log.d(TAG, "Batched update: chunks=$chunkCount, length=${currentText.length}")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Exception while updating message state", e)
                        }
                    }

                    generativeModel.generateContentStream(*history.toTypedArray())
                        .onStart {
                            Log.d(TAG, "Stream started")
                            _isLoading.value = true
                        }
                        .onCompletion {
                            // Final update with remaining text
                            updateMessageState()
                            Log.d(TAG, "Stream completed. Total chunks: $chunkCount, Total text length: $totalTextLength")
                            _isLoading.value = false
                        }
                        .catch { cause ->
                            Log.e(TAG, "Error in stream", cause)
                            val errorMessage = "Error: ${cause.message}"
                            Log.d(TAG, "Setting error message: $errorMessage")
                            // Update the placeholder with the error message
                            _messages.value = _messages.value.toMutableList().also {
                                it[botMessageIndex] = it[botMessageIndex].copy(text = errorMessage)
                            }
                            Log.d(TAG, "Error message set at index: $botMessageIndex")
                            _isLoading.value = false
                        }
                        .collect { chunk ->
                            chunkCount++
                            val chunkText = chunk.text ?: ""
                            val chunkLength = chunkText.length
                            totalTextLength += chunkLength

                            // Append to buffer instead of updating state immediately
                            textBuffer.append(chunkText)

                            if (chunkCount % 50 == 0 || chunkLength > 500) {
                                Log.d(TAG, "Chunk #$chunkCount received, length: $chunkLength, buffer length: ${textBuffer.length}")
                            }

                            if (textBuffer.length > 100000 && chunkCount % 100 == 0) {
                                Log.w(TAG, "WARNING: Buffer length is very long: ${textBuffer.length}")
                            }

                            // Batch updates: update every N chunks or every UPDATE_INTERVAL_MS ms
                            val timeSinceLastUpdate = System.currentTimeMillis() - lastUpdateTime
                            val shouldUpdate = (chunkCount % MIN_CHUNKS_FOR_UPDATE == 0) ||
                                             (timeSinceLastUpdate >= UPDATE_INTERVAL_MS)

                            if (shouldUpdate) {
                                updateMessageState()
                                lastUpdateTime = System.currentTimeMillis()

                                // Cancel any pending update job
                                pendingUpdateJob?.cancel()
                                pendingUpdateJob = null
                            } else {
                                // Schedule a delayed update if one isn't already pending
                                if (pendingUpdateJob == null || !pendingUpdateJob!!.isActive) {
                                    pendingUpdateJob = launch {
                                        delay(UPDATE_INTERVAL_MS)
                                        if (isActive) {
                                            updateMessageState()
                                            pendingUpdateJob = null
                                        }
                                    }
                                }
                            }
                        }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception in viewModelScope.launch", e)
                    _isLoading.value = false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in sendMessage", e)
        }
    }

    fun clearChat() {
        Log.d(TAG, "clearChat called. Current message count: ${_messages.value.size}")
        _messages.value = emptyList()
        Log.d(TAG, "Chat cleared")
    }
}
