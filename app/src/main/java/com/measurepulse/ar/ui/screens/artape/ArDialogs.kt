package com.measurepulse.ar.ui.screens.artape

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.measurepulse.ar.data.ar.BoxItem
import com.measurepulse.ar.domain.math.DimensionFormatter
import com.measurepulse.ar.domain.math.FractionPrecision
import com.measurepulse.ar.domain.math.UnitSystem
import com.measurepulse.ar.ui.components.ElectricLimePill
import com.measurepulse.ar.ui.components.GlassPill
import com.measurepulse.ar.ui.components.SmokedFrostedGlassCard
import com.measurepulse.ar.ui.theme.MpColors
import com.measurepulse.ar.ui.theme.MpType
import kotlin.math.roundToInt

@Composable
private fun limeSliderColors() = SliderDefaults.colors(
    thumbColor = MpColors.ElectricLime,
    activeTrackColor = MpColors.ElectricLime,
    inactiveTrackColor = MpColors.GlassPillRim,
    activeTickColor = MpColors.Ink,
    inactiveTickColor = MpColors.TextMuted,
)

@Composable
private fun DialogCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    SmokedFrostedGlassCard(Modifier.fillMaxWidth(), tint = MpColors.TwilightHorizon.copy(alpha = 0.97f)) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
    }
}

/** Gyro engine: how high the phone is held above the floor (drives every floor distance). */
@Composable
fun CameraHeightDialog(
    current: Double,
    unitSystem: UnitSystem,
    canUseArCore: Boolean,
    onUseArCore: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
) {
    var h by remember { mutableFloatStateOf(current.toFloat()) }
    Dialog(onDismissRequest = onDismiss) {
        DialogCard {
            Text("Camera height", style = MpType.HeadlineItalic.copy(fontSize = 24.sp))
            Text(
                "Gyro mode measures along the floor using how high you hold the phone. " +
                    "Hold it at this height and pivot in place — don't walk between points.",
                style = MpType.Body,
            )
            Text(
                DimensionFormatter.formatLength(h.toDouble(), unitSystem, FractionPrecision.EIGHTH),
                style = MpType.MetricDisplay, color = MpColors.ElectricLime,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Slider(
                value = h, onValueChange = { h = (it * 100).roundToInt() / 100f },
                valueRange = 0.6f..2.2f, colors = limeSliderColors(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Seated" to 1.10f, "Chest" to 1.35f, "Eye level" to 1.55f).forEach { (label, v) ->
                    GlassPill(label, textStyle = MpType.Chip.copy(fontSize = 12.sp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp), onClick = { h = v })
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (canUseArCore) GlassPill("Use ARCore", onClick = onUseArCore)
                Spacer(Modifier.weight(1f))
                GlassPill("Cancel", onClick = onDismiss)
                Spacer(Modifier.width(8.dp))
                ElectricLimePill("Save", glow = false, onClick = { onConfirm(h.toDouble()) })
            }
        }
    }
}

/** 3D Box Fit: custom furniture size (L × W × H). */
@Composable
fun CustomBoxDialog(initial: BoxItem, unitSystem: UnitSystem, onDismiss: () -> Unit, onConfirm: (BoxItem) -> Unit) {
    var w by remember { mutableFloatStateOf(initial.widthM.toFloat()) }
    var d by remember { mutableFloatStateOf(initial.depthM.toFloat()) }
    var h by remember { mutableFloatStateOf(initial.heightM.toFloat()) }
    Dialog(onDismissRequest = onDismiss) {
        DialogCard {
            Text("Custom size", style = MpType.HeadlineItalic.copy(fontSize = 24.sp))
            DimSlider("Length (along the wall)", w, 0.10f..4.00f, unitSystem) { w = it }
            DimSlider("Width (depth)", d, 0.03f..2.50f, unitSystem) { d = it }
            DimSlider("Height", h, 0.05f..2.60f, unitSystem) { h = it }
            Row(Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                GlassPill("Cancel", onClick = onDismiss)
                Spacer(Modifier.width(8.dp))
                ElectricLimePill("Place box", glow = false, onClick = {
                    onConfirm(BoxItem("Custom", w.toDouble(), d.toDouble(), h.toDouble(), custom = true))
                })
            }
        }
    }
}

@Composable
private fun DimSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, unitSystem: UnitSystem, onChange: (Float) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MpType.Chip.copy(fontSize = 13.sp), color = MpColors.IceWhite, modifier = Modifier.weight(1f))
            Text(
                DimensionFormatter.formatLength(value.toDouble(), unitSystem, FractionPrecision.EIGHTH),
                style = MpType.MetricValue.copy(fontWeight = FontWeight.Bold), color = MpColors.ElectricLime,
            )
        }
        Slider(value = value, onValueChange = { onChange((it * 100).roundToInt() / 100f) }, valueRange = range, colors = limeSliderColors())
    }
}
