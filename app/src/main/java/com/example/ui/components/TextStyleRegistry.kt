package com.example.ui.components

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.model.TextAnimationType
import com.example.model.TextConfig
import com.example.model.TextFontChoice
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

data class TextTemplatePreset(
    val id: String,
    val name: String,
    val category: String,
    val badge: String,
    val config: TextConfig
)

data class AnimatedTextFrame(
    val visibleText: String,
    val alphaMultiplier: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scaleMultiplier: Float = 1f,
    val letterSpacingDelta: Float = 0f,
    val glitchXShift: Float = 0f
)

object TextStyleRegistry {

    val templates: List<TextTemplatePreset> = listOf(
        TextTemplatePreset(
            id = "tpl_cine_editorial",
            name = "Cinema Editorial",
            category = "Title",
            badge = "FILM",
            config = TextConfig(
                text = "DIRECTOR CUT",
                fontSizeSp = 34f,
                textColor = 0xFFFDF8EE,
                isBold = false,
                strokeColor = 0xFF000000,
                strokeWidth = 0f,
                letterSpacing = 7f,
                fontChoice = TextFontChoice.CINZEL,
                animationType = TextAnimationType.TRACKING_REVEAL,
                animationSpeed = 1.0f,
                hasBackgroundPill = false,
                shadowEnabled = true,
                templateId = "tpl_cine_editorial"
            )
        ),
        TextTemplatePreset(
            id = "tpl_cyber_glitch",
            name = "Cyber Glitch HUD",
            category = "Tech",
            badge = "FX",
            config = TextConfig(
                text = "SYSTEM ONLINE",
                fontSizeSp = 30f,
                textColor = 0xFF00E5FF,
                isBold = true,
                strokeColor = 0xFFFF2E63,
                strokeWidth = 3f,
                letterSpacing = 3.5f,
                fontChoice = TextFontChoice.JETBRAINS_MONO,
                animationType = TextAnimationType.GLITCH_FLICKER,
                animationSpeed = 1.2f,
                hasBackgroundPill = true,
                backgroundColor = 0xCC060B14,
                backgroundCornerRadius = 6f,
                shadowEnabled = true,
                templateId = "tpl_cyber_glitch"
            )
        ),
        TextTemplatePreset(
            id = "tpl_breaking_news",
            name = "Lower Third Badge",
            category = "Broadcast",
            badge = "LIVE",
            config = TextConfig(
                text = "CREATOR SPOTLIGHT",
                fontSizeSp = 22f,
                textColor = 0xFFFFFFFF,
                isBold = true,
                strokeColor = 0xFF000000,
                strokeWidth = 0f,
                letterSpacing = 2.5f,
                fontChoice = TextFontChoice.OSWALD,
                animationType = TextAnimationType.SLIDE_UP,
                animationSpeed = 1.1f,
                hasBackgroundPill = true,
                backgroundColor = 0xE6D81B60,
                backgroundCornerRadius = 10f,
                shadowEnabled = true,
                templateId = "tpl_breaking_news"
            )
        ),
        TextTemplatePreset(
            id = "tpl_kinetic_pop",
            name = "Kinetic Shorts Pop",
            category = "Social",
            badge = "VIRAL",
            config = TextConfig(
                text = "WATCH THIS!",
                fontSizeSp = 40f,
                textColor = 0xFFFFD54F,
                isBold = true,
                strokeColor = 0xFF111111,
                strokeWidth = 6f,
                letterSpacing = 1.5f,
                fontChoice = TextFontChoice.BEBAS_NEUE,
                animationType = TextAnimationType.BOUNCE_IN,
                animationSpeed = 1.0f,
                hasBackgroundPill = false,
                shadowEnabled = true,
                templateId = "tpl_kinetic_pop"
            )
        ),
        TextTemplatePreset(
            id = "tpl_script_signature",
            name = "Studio Signature",
            category = "Minimal",
            badge = "SCRIPT",
            config = TextConfig(
                text = "Golden Hour",
                fontSizeSp = 36f,
                textColor = 0xFFFFF3E0,
                isBold = false,
                strokeColor = 0xFF3E2723,
                strokeWidth = 2f,
                letterSpacing = 1f,
                fontChoice = TextFontChoice.PACIFICO,
                animationType = TextAnimationType.FADE_POP,
                animationSpeed = 0.9f,
                hasBackgroundPill = false,
                shadowEnabled = true,
                templateId = "tpl_script_signature"
            )
        ),
        TextTemplatePreset(
            id = "tpl_typewriter_note",
            name = "Typewriter Terminal",
            category = "Story",
            badge = "TYPE",
            config = TextConfig(
                text = "Chapter 01 // Tokyo",
                fontSizeSp = 24f,
                textColor = 0xFF10E878,
                isBold = true,
                strokeColor = 0xFF000000,
                strokeWidth = 0f,
                letterSpacing = 1.5f,
                fontChoice = TextFontChoice.SPACE_GROTESK,
                animationType = TextAnimationType.TYPEWRITER,
                animationSpeed = 1.0f,
                hasBackgroundPill = true,
                backgroundColor = 0xD90A120E,
                backgroundCornerRadius = 16f,
                shadowEnabled = false,
                templateId = "tpl_typewriter_note"
            )
        ),
        TextTemplatePreset(
            id = "tpl_retro_chrome",
            name = "Retro 80s Display",
            category = "Retro",
            badge = "80S",
            config = TextConfig(
                text = "NIGHT DRIVE",
                fontSizeSp = 36f,
                textColor = 0xFFFF4081,
                isBold = true,
                strokeColor = 0xFF00E5FF,
                strokeWidth = 4f,
                letterSpacing = 3f,
                fontChoice = TextFontChoice.RIGHTEOUS,
                animationType = TextAnimationType.ZOOM_BURST,
                animationSpeed = 1.0f,
                hasBackgroundPill = false,
                shadowEnabled = true,
                templateId = "tpl_retro_chrome"
            )
        ),
        TextTemplatePreset(
            id = "tpl_marker_callout",
            name = "Street Brush Tag",
            category = "Social",
            badge = "BRUSH",
            config = TextConfig(
                text = "BEHIND THE SCENES",
                fontSizeSp = 28f,
                textColor = 0xFF111111,
                isBold = false,
                strokeColor = 0xFFFFFFFF,
                strokeWidth = 0f,
                letterSpacing = 1f,
                fontChoice = TextFontChoice.PERMANENT_MARKER,
                animationType = TextAnimationType.KARAOKE_WAVE,
                animationSpeed = 1.1f,
                hasBackgroundPill = true,
                backgroundColor = 0xFFFEE440,
                backgroundCornerRadius = 18f,
                shadowEnabled = true,
                templateId = "tpl_marker_callout"
            )
        )
    )

    fun fontResourceId(choice: TextFontChoice): Int? = when (choice) {
        TextFontChoice.BEBAS_NEUE -> R.font.bebas_neue
        TextFontChoice.CINZEL -> R.font.cinzel
        TextFontChoice.PACIFICO -> R.font.pacifico
        TextFontChoice.PLAYFAIR_DISPLAY -> R.font.playfair_display
        TextFontChoice.SPACE_GROTESK -> R.font.space_grotesk
        TextFontChoice.JETBRAINS_MONO -> R.font.jetbrains_mono
        TextFontChoice.OSWALD -> R.font.oswald
        TextFontChoice.MONTSERRAT -> R.font.montserrat
        TextFontChoice.RIGHTEOUS -> R.font.righteous
        TextFontChoice.PERMANENT_MARKER -> R.font.permanent_marker
        TextFontChoice.SYSTEM_SANS, TextFontChoice.SYSTEM_SERIF, TextFontChoice.SYSTEM_MONO -> null
    }

    fun composeFontFamily(choice: TextFontChoice): FontFamily {
        val resId = fontResourceId(choice)
        if (resId != null) {
            return FontFamily(Font(resId = resId, weight = FontWeight.Normal))
        }
        return when (choice) {
            TextFontChoice.SYSTEM_SERIF -> FontFamily.Serif
            TextFontChoice.SYSTEM_MONO -> FontFamily.Monospace
            else -> FontFamily.SansSerif
        }
    }

    fun androidTypeface(context: Context, choice: TextFontChoice, isBold: Boolean): Typeface {
        val resId = fontResourceId(choice)
        val loaded = if (resId != null) {
            runCatching { ResourcesCompat.getFont(context, resId) }.getOrNull()
        } else null
        val base = loaded ?: when (choice) {
            TextFontChoice.SYSTEM_SERIF -> Typeface.SERIF
            TextFontChoice.SYSTEM_MONO -> Typeface.MONOSPACE
            else -> Typeface.SANS_SERIF
        }
        return if (isBold) Typeface.create(base, Typeface.BOLD) else base
    }

    fun evaluateAnimatedText(
        config: TextConfig,
        relTimeMs: Long,
        durationMs: Long
    ): AnimatedTextFrame {
        val fullText = config.text
        if (fullText.isEmpty() || config.animationType == TextAnimationType.NONE) {
            return AnimatedTextFrame(visibleText = fullText)
        }
        val speed = config.animationSpeed.coerceIn(0.3f, 3.0f)
        val safeDur = durationMs.coerceAtLeast(600L).toFloat()
        val elapsedSec = (relTimeMs.coerceAtLeast(0L) / 1000f) * speed
        val introSpan = minOf(850f / speed, safeDur * 0.45f)
        val p = (relTimeMs.toFloat() / introSpan).coerceIn(0f, 1f)

        return when (config.animationType) {
            TextAnimationType.NONE -> AnimatedTextFrame(visibleText = fullText)
            TextAnimationType.TYPEWRITER -> {
                val typeSpan = (safeDur * 0.65f / speed).coerceAtLeast(350f)
                val typeFrac = (relTimeMs.toFloat() / typeSpan).coerceIn(0.05f, 1f)
                val count = (fullText.length * typeFrac).roundToInt().coerceIn(1, fullText.length)
                AnimatedTextFrame(visibleText = fullText.take(count))
            }
            TextAnimationType.FADE_POP -> {
                val eased = 1f - (1f - p) * (1f - p)
                AnimatedTextFrame(
                    visibleText = fullText,
                    alphaMultiplier = eased,
                    scaleMultiplier = 0.72f + 0.28f * eased
                )
            }
            TextAnimationType.SLIDE_UP -> {
                val eased = 1f - (1f - p) * (1f - p) * (1f - p)
                AnimatedTextFrame(
                    visibleText = fullText,
                    alphaMultiplier = (p * 1.4f).coerceIn(0f, 1f),
                    offsetY = (1f - eased) * 44f
                )
            }
            TextAnimationType.BOUNCE_IN -> {
                val s = if (p < 0.7f) {
                    (p / 0.7f) * 1.18f
                } else {
                    1.18f - ((p - 0.7f) / 0.3f) * 0.18f
                }
                AnimatedTextFrame(
                    visibleText = fullText,
                    alphaMultiplier = (p * 2f).coerceIn(0f, 1f),
                    scaleMultiplier = s.coerceAtLeast(0.15f)
                )
            }
            TextAnimationType.GLITCH_FLICKER -> {
                val phase = elapsedSec * 18f
                val flicker = if (sin(phase) > 0.78f) 0.55f else 1f
                val shift = sin(phase * 1.7f) * 5.5f
                AnimatedTextFrame(
                    visibleText = fullText,
                    alphaMultiplier = flicker,
                    glitchXShift = shift
                )
            }
            TextAnimationType.KARAOKE_WAVE -> {
                val wave = sin(elapsedSec * 4.5f)
                AnimatedTextFrame(
                    visibleText = fullText,
                    offsetY = wave * 7f,
                    scaleMultiplier = 1f + 0.04f * abs(sin(elapsedSec * 2.5f))
                )
            }
            TextAnimationType.ZOOM_BURST -> {
                val scale = if (p < 1f) 1.85f - 0.85f * (1f - (1f - p) * (1f - p)) else 1f
                AnimatedTextFrame(
                    visibleText = fullText,
                    alphaMultiplier = (p * 1.6f).coerceIn(0f, 1f),
                    scaleMultiplier = scale
                )
            }
            TextAnimationType.TRACKING_REVEAL -> {
                val eased = 1f - (1f - p) * (1f - p)
                AnimatedTextFrame(
                    visibleText = fullText,
                    alphaMultiplier = eased,
                    letterSpacingDelta = (1f - eased) * 14f
                )
            }
        }
    }
}
