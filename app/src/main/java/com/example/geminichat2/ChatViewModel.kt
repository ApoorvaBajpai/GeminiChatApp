import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminichat2.ChatMessage
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.geminichat2.BuildConfig

class ChatViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Use API key from BuildConfig
    private val generativeModel = GenerativeModel(
        modelName = "gemini-pro",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    fun sendMessage(userMessage: String) {
        if (userMessage.isBlank()) return

        // Add user message
        _messages.value += ChatMessage(userMessage, true)
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val response = generativeModel.generateContent(userMessage)
                val botMessage = response.text ?: "No response"

                // Add bot response
                _messages.value += ChatMessage(botMessage, false)
            } catch (e: Exception) {
                _messages.value += ChatMessage(
                    "Error: ${e.message}",
                    false
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearChat() {
        _messages.value = emptyList()
    }
}
