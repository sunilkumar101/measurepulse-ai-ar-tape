package com.measurepulse.ar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.measurepulse.ar.data.monetization.OfferChoice
import com.measurepulse.ar.data.monetization.RewardOffer
import com.measurepulse.ar.data.monetization.RewardedExportGate
import com.measurepulse.ar.ui.theme.MpColors
import com.measurepulse.ar.ui.theme.MpType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class MonetizationViewModel @Inject constructor(val gate: RewardedExportGate) : ViewModel()

/**
 * App-wide overlay for the rewarded-export offer and its confirmation toasts.
 * Lives above the NavHost so any screen's export can use it.
 */
@Composable
fun BoxScope.MonetizationHost(onOpenPro: () -> Unit, vm: MonetizationViewModel = hiltViewModel()) {
    val offer by vm.gate.offer.collectAsStateWithLifecycle()
    var notice by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { vm.gate.notices.collect { notice = it } }
    LaunchedEffect(Unit) { vm.gate.openPro.collect { onOpenPro() } }
    LaunchedEffect(notice) { if (notice != null) { delay(3200); notice = null } }

    offer?.let { RewardOfferDialog(it, onChoice = vm.gate::choose) }

    notice?.let {
        Text(
            it, style = MpType.Chip.copy(fontSize = 13.sp), color = MpColors.Ink, textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 70.dp, start = 24.dp, end = 24.dp)
                .clip(RoundedCornerShape(20.dp)).background(MpColors.ElectricLime).padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun RewardOfferDialog(offer: RewardOffer, onChoice: (OfferChoice) -> Unit) {
    Dialog(
        onDismissRequest = { if (!offer.loadingVideo) onChoice(OfferChoice.NOT_NOW) },
        properties = DialogProperties(dismissOnClickOutside = !offer.loadingVideo, dismissOnBackPress = !offer.loadingVideo),
    ) {
        // Solid twilight base under the glass so the screen behind never shows through the text.
        SmokedFrostedGlassCard(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Brush.verticalGradient(listOf(MpColors.TwilightZenith, MpColors.TwilightNadir))),
            shape = RoundedCornerShape(28.dp), tint = Color(0x661C1E36),
        ) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(56.dp).limeGlow(radius = 14.dp, alpha = 0.4f).clip(RoundedCornerShape(18.dp)).background(MpColors.ElectricLime), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.PlayCircle, null, tint = MpColors.Ink, modifier = Modifier.size(32.dp))
                }
                Text(offer.kind.title, style = MpType.CardTitle.copy(fontSize = 21.sp), color = MpColors.TextPrimary, textAlign = TextAlign.Center)
                Text(offer.kind.body, style = MpType.Body, color = MpColors.IceWhite, textAlign = TextAlign.Center)
                Spacer(Modifier.height(2.dp))
                if (offer.loadingVideo) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 10.dp)) {
                        CircularProgressIndicator(color = MpColors.ElectricLime, strokeWidth = 3.dp, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Loading video…", style = MpType.Chip, color = MpColors.TextPrimary)
                    }
                } else {
                    ElectricLimePill(
                        "Watch short video", icon = Icons.Rounded.PlayCircle, glow = false,
                        textStyle = MpType.Button.copy(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold),
                        contentPadding = PaddingValues(vertical = 14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onChoice(OfferChoice.WATCH_VIDEO) },
                    )
                    GlassPill(
                        "Go Pro · no ads, unlimited PDFs", icon = Icons.Rounded.WorkspacePremium, iconTint = MpColors.ElectricLime,
                        textStyle = MpType.Chip.copy(fontSize = 14.sp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 13.dp),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onChoice(OfferChoice.GO_PRO) },
                    )
                    Text(
                        "Not now", style = MpType.Chip, color = MpColors.IceWhite.copy(alpha = 0.8f),
                        modifier = Modifier.clip(RoundedCornerShape(12.dp))
                            .glassClickable(rememberPressInteraction(), "Not now") { onChoice(OfferChoice.NOT_NOW) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Verified, null, tint = MpColors.IceWhite.copy(alpha = 0.6f), modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("No video available? Your export is unlocked anyway.", style = MpType.Body.copy(fontSize = 11.sp, lineHeight = 15.sp), color = MpColors.IceWhite.copy(alpha = 0.65f))
                }
            }
        }
    }
}
