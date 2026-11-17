package com.mfouad.muslimtv.core.designsystem.component

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mfouad.muslimtv.core.designsystem.R
import kotlinx.coroutines.delay

/** Bundled CC0 nature scenes (Wikimedia Commons; see Credits). 1280×720 WebP. */
object Scenes {
    val all = listOf(
        R.drawable.scene_1, R.drawable.scene_2, R.drawable.scene_3, R.drawable.scene_4,
        R.drawable.scene_5, R.drawable.scene_6, R.drawable.scene_7, R.drawable.scene_8,
    )
    const val ROTATE = -1
}

/**
 * Full-bleed nature scene behind content, on a soft dark overlay for readability. With
 * [scene] = [Scenes.ROTATE] the scenes change slowly with long crossfades. It deliberately
 * ignores the safe-area padding of its parent so the picture fills the whole screen.
 */
@Composable
fun NatureBackground(scene: Int, modifier: Modifier = Modifier, rotateEveryMs: Long = 90_000L, dim: Float = 0.55f) {
    val config = LocalConfiguration.current
    var index by remember(scene) { mutableIntStateOf(if (scene in Scenes.all.indices) scene else 0) }
    if (scene == Scenes.ROTATE) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(rotateEveryMs)
                index = (index + 1) % Scenes.all.size
            }
        }
    }
    Box(modifier.requiredSize(config.screenWidthDp.dp, config.screenHeightDp.dp)) {
        Crossfade(targetState = index, animationSpec = tween(CROSSFADE_MS), label = "scene") { i ->
            Image(painterResource(Scenes.all[i]), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Black.copy(alpha = dim - 0.1f), Color.Black.copy(alpha = dim), Color.Black.copy(alpha = dim + 0.15f))),
            ),
        )
    }
}

private const val CROSSFADE_MS = 4_000
