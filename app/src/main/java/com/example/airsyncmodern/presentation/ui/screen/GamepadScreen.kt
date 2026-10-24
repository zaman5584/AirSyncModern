package com.example.airsyncmodern.presentation.ui.screen

import android.content.pm.ActivityInfo
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.airsyncmodern.presentation.ui.theme.md_theme_dark_background
import com.example.airsyncmodern.presentation.ui.theme.md_theme_dark_onPrimary
import com.example.airsyncmodern.presentation.ui.theme.md_theme_dark_onSurface
import com.example.airsyncmodern.presentation.ui.theme.md_theme_dark_onSurfaceVariant
import com.example.airsyncmodern.presentation.ui.theme.md_theme_dark_primary
import com.example.airsyncmodern.presentation.ui.theme.md_theme_dark_surface
import com.example.airsyncmodern.presentation.ui.theme.md_theme_dark_surfaceContainerHigh
import com.example.airsyncmodern.presentation.viewmodel.RemoteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamepadScreen(
    viewModel: RemoteViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    
    // Force landscape mode for Gamepad
    DisposableEffect(Unit) {
        val activity = context as? ComponentActivity
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
        
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }

    Scaffold(
        containerColor = md_theme_dark_background,
        topBar = {
            TopAppBar(
                title = { Text("Gamepad") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = md_theme_dark_surface,
                    titleContentColor = md_theme_dark_onSurface,
                    navigationIconContentColor = md_theme_dark_onSurface
                )
            )
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Side: D-Pad
            DPadController(
                onUp = { viewModel.navigateUp() },
                onDown = { viewModel.navigateDown() },
                onLeft = { viewModel.navigateLeft() },
                onRight = { viewModel.navigateRight() }
            )
            
            // Right Side: Action Buttons A, B, X, Y
            ActionButtonsController(
                onA = { viewModel.pressGamepadA() },
                onB = { viewModel.pressGamepadB() },
                onX = { viewModel.pressGamepadX() },
                onY = { viewModel.pressGamepadY() }
            )
        }
    }
}

@Composable
private fun DPadController(
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit
) {
    Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        // Cross background
        Box(
            modifier = Modifier
                .size(60.dp, 200.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(md_theme_dark_surfaceContainerHigh)
        )
        Box(
            modifier = Modifier
                .size(200.dp, 60.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(md_theme_dark_surfaceContainerHigh)
        )
        
        // Buttons
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            DPadButton(Icons.Default.KeyboardArrowUp, onClick = onUp)
            Spacer(modifier = Modifier.height(80.dp))
            DPadButton(Icons.Default.KeyboardArrowDown, onClick = onDown)
        }
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DPadButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, onClick = onLeft)
            Spacer(modifier = Modifier.width(80.dp))
            DPadButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, onClick = onRight)
        }
    }
}

@Composable
private fun DPadButton(
    icon: ImageVector,
    onClick: () -> Unit
) {
    val view = LocalView.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dpad_scale"
    )
    
    Box(
        modifier = Modifier
            .size(60.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(if (isPressed) md_theme_dark_surface else Color.Transparent)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        onClick()
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = md_theme_dark_onSurface,
            modifier = Modifier.size(36.dp)
        )
    }
}

@Composable
private fun ActionButtonsController(
    onA: () -> Unit,
    onB: () -> Unit,
    onX: () -> Unit,
    onY: () -> Unit
) {
    Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        // Y Button (Top)
        ActionButton(
            text = "Y",
            color = Color(0xFFFDD835), // Yellow
            onClick = onY,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        // A Button (Bottom)
        ActionButton(
            text = "A",
            color = Color(0xFF43A047), // Green
            onClick = onA,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        // X Button (Left)
        ActionButton(
            text = "X",
            color = Color(0xFF1E88E5), // Blue
            onClick = onX,
            modifier = Modifier.align(Alignment.CenterStart)
        )
        // B Button (Right)
        ActionButton(
            text = "B",
            color = Color(0xFFE53935), // Red
            onClick = onB,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

@Composable
private fun ActionButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "action_btn_scale"
    )
    
    Box(
        modifier = modifier
            .size(64.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(md_theme_dark_surfaceContainerHigh)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        onClick()
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (isPressed) color else color.copy(alpha = 0.8f)
        )
    }
}
