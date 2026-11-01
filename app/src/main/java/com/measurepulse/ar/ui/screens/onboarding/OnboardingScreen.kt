package com.measurepulse.ar.ui.screens.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.measurepulse.ar.BuildConfig
import com.measurepulse.ar.R
import com.measurepulse.ar.data.ArSupport
import com.measurepulse.ar.ui.components.AiWaveformBars
import com.measurepulse.ar.ui.components.ElectricLimePill
import com.measurepulse.ar.ui.components.IridescentAiOrb
import com.measurepulse.ar.ui.components.SmokedFrostedGlassCard
import com.measurepulse.ar.ui.components.StatusDot
import com.measurepulse.ar.ui.components.TwilightGradientBackground
import com.measurepulse.ar.ui.components.glassClickable
import com.measurepulse.ar.ui.components.rememberPressInteraction
import com.measurepulse.ar.ui.theme.MpColors
import com.measurepulse.ar.ui.theme.MpType
import kotlinx.coroutines.launch

private data class OnboardingPage(
    @DrawableRes val image: Int,
    val engine: String,
    val engineDetail: String,
    val titleLine1: String,
    val titleLine2: String,
    val cardHeadline: String,
    val cardAccent: String,
    val cardLabel: String,
    val valueProp: String,
)

private val pages = listOf(
    OnboardingPage(
        image = R.drawable.bg_villa_cliff,
        engine = "ARCore Depth Engine", engineDetail = "Spatial Mesh",
        titleLine1 = "The Future of", titleLine2 = "Spatial Reality.",
        cardHeadline = "Measure Higher.", cardAccent = "Spatial AI.", cardLabel = "AI Room & Renovation Advisor",
        valueProp = "Precise AR point-to-point measuring, automated CAD floorplans and instant 3D room blueprints in real time.",
    ),
    OnboardingPage(
        image = R.drawable.bg_villa_pool,
        engine = "3D AR Laser Tape", engineDetail = "Anti-Shake",
        titleLine1 = "Point. Mark.", titleLine2 = "Measured.",
        cardHeadline = "Say \"Mark\".", cardAccent = "Pinned.", cardLabel = "Voice & volume-key capture",
        valueProp = "Hold steady and say \"Mark\" or press a volume key. The pin locks without you shaking the phone.",
    ),
    OnboardingPage(
        image = R.drawable.bg_living_tv_wall,
        engine = "Room Blueprint", engineDetail = "PDF Export",
        titleLine1 = "Blueprints in", titleLine2 = "One Sweep.",
        cardHeadline = "Scan. Plan.", cardAccent = "Export.", cardLabel = "2D CAD floorplan + PDF",
        valueProp = "Walk the room once to get a dimensioned floorplan, wall areas and a scaled PDF blueprint you can share.",
    ),
)

/** Screen 1 — full-bleed twilight splash & AI onboarding (3 swipeable pages). */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val arSupport by viewModel.arSupport.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val current = pages[pagerState.currentPage]
    val finish = { viewModel.finish(onFinished) }

    TwilightGradientBackground(imageRes = current.image) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            BrandHeader(arSupport, Modifier.padding(horizontal = 24.dp, vertical = 12.dp))

            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { index ->
                PageContent(pages[index], arSupport)
            }

            Column(
                Modifier.padding(horizontal = 24.dp).padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                SmokedFrostedGlassCard(
                    Modifier.fillMaxWidth().heightIn(min = 92.dp),
                    shape = RoundedCornerShape(22.dp),
                ) {
                    AnimatedContent(
                        targetState = current.valueProp,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "valueProp",
                    ) { text ->
                        Text(
                            text, style = MpType.Body.copy(fontSize = 14.5.sp, lineHeight = 22.sp, letterSpacing = 0.01.em),
                            color = MpColors.IceWhite,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                        )
                    }
                }

                ElectricLimePill(
                    text = "Start Spatial Scan",
                    trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    textStyle = MpType.Button.copy(fontSize = 17.sp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 17.dp),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = finish,
                )

                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val skipInteraction = rememberPressInteraction()
                    Text(
                        "Skip",
                        style = MpType.Chip.copy(fontSize = 14.sp, textDecoration = TextDecoration.Underline),
                        color = MpColors.IceWhite.copy(alpha = 0.85f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .glassClickable(skipInteraction, "Skip onboarding", finish)
                            .padding(horizontal = 6.dp, vertical = 12.dp),
                    )
                    Spacer(Modifier.weight(1f))
                    PageDots(
                        count = pages.size, current = pagerState.currentPage,
                        onSelect = { scope.launch { pagerState.animateScrollToPage(it) } },
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "V${BuildConfig.VERSION_NAME.substringBeforeLast('.')} AR",
                        style = MpType.MetricSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.em),
                        color = MpColors.ElectricLime,
                    )
                }
            }
        }
    }
}

@Composable
private fun BrandHeader(arSupport: ArSupport, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        StatusDot(size = 9.dp)
        Spacer(Modifier.width(10.dp))
        Text(
            buildAnnotatedString {
                append("MEASUREPULSE")
                withStyle(SpanStyle(color = MpColors.ElectricLime)) { append(".AI") }
            },
            style = MpType.Chip.copy(fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.06.em),
            color = MpColors.TextPrimary,
        )
        Spacer(Modifier.weight(1f).widthIn(min = 10.dp))
        val (label, dot) = when (arSupport) {
            ArSupport.CHECKING -> "Checking AR" to MpColors.TextMuted
            ArSupport.SUPPORTED, ArSupport.SUPPORTED_NEEDS_INSTALL -> "AR Ready" to MpColors.ElectricLime
            ArSupport.UNSUPPORTED -> "Sensor Mode" to MpColors.Cyan
        }
        Row(
            Modifier
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f))
                .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                .padding(horizontal = 12.dp, vertical = 7.dp)
                .semantics { contentDescription = "Device status: $label" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot(color = dot, size = 7.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                label.uppercase(),
                style = MpType.Chip.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.1.em),
                color = MpColors.TextPrimary,
            )
        }
    }
}

@Composable
private fun PageContent(page: OnboardingPage, arSupport: ArSupport) {
    // Be honest on phones without ARCore: the first page names the engine that will actually run.
    val engine = if (page.engine == "ARCore Depth Engine" && arSupport == ArSupport.UNSUPPORTED) "Gyro Sensor Engine" else page.engine
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(10.dp))
        // Engine pill: lime-rimmed dark glass
        Row(
            Modifier
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
                .border(1.dp, MpColors.ElectricLime.copy(alpha = 0.45f), CircleShape)
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                engine.uppercase(),
                style = MpType.Chip.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.1.em),
                color = MpColors.ElectricLime,
            )
            Text("  •  ", style = MpType.Chip, color = Color.White.copy(alpha = 0.45f))
            Text(page.engineDetail, style = MpType.Chip.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium), color = MpColors.IceWhite)
        }
        Spacer(Modifier.height(14.dp))
        Text(page.titleLine1, style = MpType.HeroItalic.copy(fontSize = 34.sp, lineHeight = 38.sp))
        Text(
            page.titleLine2,
            style = MpType.HeroItalic.copy(
                fontSize = 34.sp, lineHeight = 38.sp,
                brush = Brush.linearGradient(listOf(Color.White, Color.White.copy(alpha = 0.92f), MpColors.ElectricLime)),
            ),
        )
        Spacer(Modifier.weight(1f))
        TiltedOrbCard(page, Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.weight(1.2f))
    }
}

/** Floating tilted frosted-glass pill with the AI orb + live waveform (design: rotate(-7deg)). */
@Composable
private fun TiltedOrbCard(page: OnboardingPage, modifier: Modifier = Modifier) {
    SmokedFrostedGlassCard(
        modifier
            .widthIn(max = 360.dp)
            .fillMaxWidth()
            .rotate(-7f),
        tint = Color(28, 30, 54, (0.58f * 255).toInt()),
    ) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            IridescentAiOrb(size = 60.dp, showStatusDot = true)
            Spacer(Modifier.width(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    buildAnnotatedString {
                        append(page.cardHeadline)
                        append(" ")
                        withStyle(SpanStyle(color = MpColors.ElectricLime, fontWeight = FontWeight.SemiBold)) { append(page.cardAccent) }
                    },
                    // Two lines allowed so the lime accent ("Spatial AI.") is never cut on 360 dp phones.
                    style = MpType.Chip.copy(fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.01).em),
                    color = MpColors.TextPrimary, maxLines = 2,
                )
                Text(
                    page.cardLabel.uppercase(),
                    style = MpType.Chip.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.06.em),
                    color = MpColors.IceWhite.copy(alpha = 0.85f), maxLines = 1,
                )
                AiWaveformBars(height = 18.dp)
            }
        }
    }
}

@Composable
private fun PageDots(count: Int, current: Int, onSelect: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            val active = i == current
            val width by animateDpAsState(if (active) 26.dp else 7.dp, label = "dotW")
            val alpha by animateFloatAsState(if (active) 1f else 0.4f, label = "dotA")
            val interaction = rememberPressInteraction()
            Box(
                Modifier
                    .glassClickable(interaction, "Go to page ${i + 1}") { onSelect(i) }
                    .padding(horizontal = 8.dp, vertical = 20.dp)
            ) {
                Box(
                    Modifier
                        .height(7.dp)
                        .width(width)
                        .clip(CircleShape)
                        .background(if (active) MpColors.ElectricLime else Color.White.copy(alpha = alpha))
                )
            }
        }
    }
}
