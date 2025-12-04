package com.example.geminichat2

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.geminichat2.ChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel(),
    isDarkTheme: Boolean = false,
    onThemeToggle: () -> Unit = {}
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Auto-scroll to bottom when new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            EnhancedTopBar(
                isDarkTheme = isDarkTheme,
                onThemeToggle = onThemeToggle,
                onClearChat = { viewModel.clearChat() }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Enhanced background gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.08f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )

            // Enhanced empty state
            if (messages.isEmpty()) {
                EnhancedEmptyState()
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f)
            ) {
                // Messages list
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages) { message ->
                        AnimatedMessageBubble(message)
                    }
                    if (isLoading) {
                        item {
                            TypingIndicator()
                        }
                    }
                }

                // Enhanced input field
                EnhancedInputField(
                    inputText = inputText,
                    onInputChange = { inputText = it },
                    onSendClick = {
                        if (inputText.isNotBlank() && !isLoading) {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                            keyboardController?.hide()
                        }
                    },
                    isLoading = isLoading
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedTopBar(
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onClearChat: () -> Unit
) {
    // Animated gradient colors
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val animatedAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = animatedAlpha),
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = animatedAlpha)
                        )
                    )
                )
        ) {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Animated Gemini icon
                        val rotation by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(3000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "rotation"
                        )

                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier
                                .size(28.dp)
                                .rotate(rotation),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Gemini AI",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Powered by Google",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                ),
                actions = {
                    // Theme toggle with animation
                    IconButton(
                        onClick = onThemeToggle,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        val scale by animateFloatAsState(
                            targetValue = if (isDarkTheme) 1f else 1.2f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "scale"
                        )

                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDarkTheme) "Light Mode" else "Dark Mode",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.scale(scale)
                        )
                    }

                    // Clear chat button
                    IconButton(
                        onClick = onClearChat,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear Chat",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
fun EnhancedEmptyState() {
    val infiniteTransition = rememberInfiniteTransition(label = "empty_state")

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated sparkle decoration
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = alpha * 0.5f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Hello! How can I help you today?",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Ask me anything...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun AnimatedMessageBubble(message: ChatMessage) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)) +
                slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ),
        exit = fadeOut()
    ) {
        MessageBubble(message)
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeString = dateFormat.format(Date(message.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (message.isFromUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomStart = if (message.isFromUser) 20.dp else 4.dp,
                    bottomEnd = if (message.isFromUser) 4.dp else 20.dp
                ),
                color = if (message.isFromUser)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 3.dp,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .border(
                        width = 1.5.dp,
                        color = if (message.isFromUser)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        else
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = if (message.isFromUser) 20.dp else 4.dp,
                            bottomEnd = if (message.isFromUser) 4.dp else 20.dp
                        )
                    )
            ) {
                // Format Gemini response with better readability
                FormattedMessageText(
                    text = message.text,
                    isFromUser = message.isFromUser
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                if (!message.isFromUser) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = timeString,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun FormattedMessageText(text: String, isFromUser: Boolean) {
    val formattedText = buildAnnotatedString {
        // Split by code blocks first
        val codeBlockRegex = "```([\\s\\S]*?)```".toRegex()
        var lastIndex = 0

        codeBlockRegex.findAll(text).forEach { matchResult ->
            // Add text before code block
            if (matchResult.range.first > lastIndex) {
                processTextWithFormatting(text.substring(lastIndex, matchResult.range.first))
            }

            // Add code block with special styling
            withStyle(
                SpanStyle(
                    background = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            ) {
                append("\n${matchResult.groupValues[1].trim()}\n")
            }

            lastIndex = matchResult.range.last + 1
        }

        // Add remaining text
        if (lastIndex < text.length) {
            processTextWithFormatting(text.substring(lastIndex))
        }
    }

    Text(
        text = formattedText,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        color = if (isFromUser)
            MaterialTheme.colorScheme.onPrimaryContainer
        else
            MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.bodyLarge,
        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.3
    )
}

@Composable
private fun androidx.compose.ui.text.AnnotatedString.Builder.processTextWithFormatting(text: String) {
    // Handle inline code first
    val inlineCodeRegex = "`([^`]+)`".toRegex()
    var lastIndex = 0

    inlineCodeRegex.findAll(text).forEach { matchResult ->
        if (matchResult.range.first > lastIndex) {
            // Process text before inline code for bold formatting
            processTextWithBold(text.substring(lastIndex, matchResult.range.first))
        }

        withStyle(
            SpanStyle(
                background = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        ) {
            append(matchResult.groupValues[1])
        }

        lastIndex = matchResult.range.last + 1
    }

    if (lastIndex < text.length) {
        processTextWithBold(text.substring(lastIndex))
    }
}

@Composable
private fun androidx.compose.ui.text.AnnotatedString.Builder.processTextWithBold(text: String) {
    // Handle bold text with ** or *
    val boldRegex = "\\*\\*([^*]+)\\*\\*|\\*([^*]+)\\*".toRegex()
    var lastIndex = 0

    boldRegex.findAll(text).forEach { matchResult ->
        if (matchResult.range.first > lastIndex) {
            append(text.substring(lastIndex, matchResult.range.first))
        }

        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            // Use group 1 for ** bold or group 2 for * bold
            append(matchResult.groupValues[1].ifEmpty { matchResult.groupValues[2] })
        }

        lastIndex = matchResult.range.last + 1
    }

    if (lastIndex < text.length) {
        append(text.substring(lastIndex))
    }
}

@Composable
fun TypingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp,
            modifier = Modifier.padding(4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    val infiniteTransition = rememberInfiniteTransition(label = "dot_$index")
                    val offsetY by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = -10f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, easing = FastOutSlowInEasing, delayMillis = index * 150),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "offset"
                    )

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .offset(y = offsetY.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                CircleShape
                            )
                    )

                    if (index < 2) Spacer(modifier = Modifier.width(6.dp))
                }
            }
        }
    }
}

@Composable
fun EnhancedInputField(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isLoading: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
            ) {
                TextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "Message Gemini...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(28.dp),
                    singleLine = false,
                    maxLines = 4,
                    textStyle = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Enhanced send button with animation
            val scale by animateFloatAsState(
                targetValue = if (inputText.isNotBlank() && !isLoading) 1f else 0.9f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "send_button_scale"
            )

            FloatingActionButton(
                onClick = onSendClick,
                modifier = Modifier
                    .size(56.dp)
                    .scale(scale),
                containerColor = if (inputText.isNotBlank() && !isLoading)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (inputText.isNotBlank() && !isLoading)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 12.dp
                )
            ) {
                AnimatedContent(
                    targetState = inputText.isNotBlank() && !isLoading,
                    transitionSpec = {
                        fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut()
                    },
                    label = "send_icon"
                ) { enabled ->
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChatScreenPreview() {
    MaterialTheme {
        ChatScreen()
    }
}