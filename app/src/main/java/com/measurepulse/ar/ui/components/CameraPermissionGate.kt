package com.measurepulse.ar.ui.components

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.measurepulse.ar.ui.theme.MpColors
import com.measurepulse.ar.ui.theme.MpType

/** Shows [content] once camera permission is granted; otherwise a frosted explainer card. */
@Composable
fun CameraPermissionGate(
    reason: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasCameraPermission(context)) }
    var asked by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it; asked = true }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }
    if (granted) { content(); return }

    TwilightGradientBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(20.dp)) {
            GlassCircleButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
            Spacer(Modifier.weight(1f))
            SmokedFrostedGlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.limeGlow().size(64.dp).clip(CircleShape).background(MpColors.ElectricLime), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.CameraAlt, null, tint = MpColors.Ink, modifier = Modifier.size(32.dp))
                    }
                    Text("Camera access needed", style = MpType.HeadlineItalic.copy(fontSize = 24.sp))
                    Text("$reason Images stay on your phone — nothing is uploaded.", style = MpType.Body, color = MpColors.IceWhite)
                    ElectricLimePill(
                        if (asked) "Open Settings" else "Allow Camera",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (asked) context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
                            else launcher.launch(Manifest.permission.CAMERA)
                        },
                    )
                }
            }
            Spacer(Modifier.weight(1.3f))
        }
    }
}
