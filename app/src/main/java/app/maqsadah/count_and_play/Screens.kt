package app.maqsadah.count_and_play

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.key
import kotlin.math.max

@Composable
fun CountPlayApp(c: GameViewModel, speaker: Speaker) {
    val cfg = LocalConfiguration.current
    val vmin = minOf(cfg.screenWidthDp, cfg.screenHeightDp).toFloat()

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Palette.SkyTop,
                    0.6f to Palette.SkyMid,
                    1f to Palette.Mint
                )
            )
    ) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Header(c, speaker, vmin)
            EquationLine(c.equation, vmin)
            if (c.pickerVisible) {
                Picker(c, vmin, Modifier.weight(1f))
            } else {
                Stage(c, vmin, Modifier.weight(1f))
                PromptLine(c.prompt, vmin)
                BottomBar(c, vmin)
            }
        }

        when (c.screen) {
            Screen.START -> StartOverlay(vmin) { c.onPlay() }
            Screen.MENU -> MenuOverlay(vmin, onLearn = { c.enterLearn() }, onQuiz = { c.enterQuiz() })
            else -> {}
        }

        ConfettiOverlay(c.confettiTick)
    }
}

/* ---------- header ---------- */

@Composable
private fun IconCircleButton(
    icon: String,
    label: String,
    vmin: Float,
    onClick: () -> Unit
) {
    // Size the touch target directly (56dp floor, up to 84dp on big screens)
    // rather than deriving it from font size, so it stays comfortably tappable
    // for a small child on every screen size. The icon scales with the button.
    val d = clampDp(56f, 13f, 84f, vmin)
    val iconFs = with(LocalDensity.current) { (d * 0.5f).toSp() }
    val haptic = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.88f else 1f, label = "iconBtnPress")

    Box(
        Modifier
            .size(d)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(Palette.ChromeBtnBg, CircleShape)
            .border(2.dp, Palette.ChromeBtnBorder, CircleShape)
            .clickable(interactionSource = interaction, indication = ripple()) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .semantics {
                contentDescription = label
                role = Role.Button
            },
        contentAlignment = Alignment.Center
    ) {
        Text(icon, fontSize = iconFs)
    }
}

@Composable
private fun Header(c: GameViewModel, speaker: Speaker, vmin: Float) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconCircleButton("🏠", "Home", vmin) { c.showMenu() }
        val stars = if (c.mode == Mode.QUIZ) "⭐".repeat(minOf(c.correctCount, 10)) else ""
        Text(
            stars,
            Modifier.weight(1f),
            textAlign = TextAlign.Center,
            fontSize = clampSp(16f, 3.5f, 26f, vmin),
            letterSpacing = 2.sp
        )
        IconCircleButton(
            icon = if (speaker.soundOn) "🔊" else "🔇",
            label = if (speaker.soundOn) "Mute sound" else "Turn sound on",
            vmin = vmin
        ) { speaker.toggle() }
    }
}

/* ---------- equation line ---------- */

@Composable
private fun EquationLine(parts: List<EqPart>, vmin: Float) {
    val fs = clampSp(28f, 7f, 60f, vmin)
    val minH = with(LocalDensity.current) { fs.toDp() * 1.3f }
    val txt = buildAnnotatedString {
        parts.forEach { p ->
            val color = when (p.kind) {
                EqPart.Q -> Palette.Q
                EqPart.GHOST -> Palette.Ghost
                else -> Palette.TextBlue
            }
            withStyle(SpanStyle(color = color)) { append(p.text) }
        }
    }
    Text(
        txt,
        Modifier.fillMaxWidth().heightIn(min = minH),
        textAlign = TextAlign.Center,
        fontSize = fs,
        fontWeight = FontWeight.Bold,
        letterSpacing = 3.sp
    )
}

/* ---------- stage ---------- */

@Composable
private fun SlotsFlow(c: GameViewModel, gap: Dp, fixedFs: TextUnit) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        val gapPx = with(density) { gap.toPx() }
        val n = max(c.capGone, c.slots.size)
        // Same fixed object size as everywhere else; only shrink if this box can't
        // hold n at that size. Deterministic column count = no clipped rows.
        val ownFit = fitItemFontSp(wPx, hPx, n, gapPx, density)
        val fs = if (fixedFs.value <= ownFit.value) fixedFs else ownFit
        val cols = minOf(n, gridCols(fs, wPx, gapPx, density))
        val slotSize = with(density) { fs.toDp() * 1.2f }
        CenteredGrid(
            items = c.slots.toList(),
            cols = cols,
            cellW = slotSize,
            cellH = slotSize,
            gap = gap,
            keyOf = { i, _ -> i }
        ) { s ->
            Box(
                Modifier
                    .size(slotSize)
                    .background(Color(0x80FFFFFF), CircleShape)
                    .drawBehind {
                        drawCircle(
                            color = Palette.SlotBorder,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                            )
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                s?.let { ItemView(it, fs) }
            }
        }
    }
}

@Composable
private fun GoneZoneContent(c: GameViewModel, vmin: Float, gap: Dp, fixedFs: TextUnit) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            c.goneLabel,
            color = Palette.LabelPop,
            fontSize = clampSp(18f, 4.2f, 30f, vmin),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (c.slots.isNotEmpty()) {
                SlotsFlow(c, gap, fixedFs)
            } else {
                AutoItemsFlow(c.goneItems, c.capGone, gap, fixedFs)
            }
        }
    }
}

@Composable
private fun Stage(c: GameViewModel, vmin: Float, modifier: Modifier) {
    val cfg = LocalConfiguration.current
    val portrait = cfg.screenHeightDp > cfg.screenWidthDp
    val gap = (1.6f / 100f * vmin).dp
    val density = LocalDensity.current

    BoxWithConstraints(modifier) {
        val stageW = maxWidth
        val stageH = maxHeight
        // ONE fixed object size for the whole app: the largest emoji that fits the
        // worst case — MAX_N objects in a main plate. Two equal plates stacked
        // (learn addition) is the tightest a main plate is ever shown at, so we
        // size for that geometry and reuse the single value in every zone and every
        // problem. Mirrors the exact layout constants below so measured == rendered.
        val padH = 10.dp; val padV = 6.dp; val zonePad = 8.dp; val zoneGap = 8.dp
        val plateInnerW: Dp
        val plateInnerH: Dp
        if (portrait) {
            plateInnerW = stageW - padH * 2 - zonePad * 2
            plateInnerH = (stageH - padV * 2 - zoneGap) / 2f - zonePad * 2
        } else {
            plateInnerW = (stageW - padH * 2 - zoneGap) / 2f - zonePad * 2
            plateInnerH = stageH - padV * 2 - zonePad * 2
        }
        val fixedFs = fitItemFontSp(
            with(density) { plateInnerW.toPx() },
            with(density) { plateInnerH.toPx() },
            G.MAX_N,
            with(density) { gap.toPx() },
            density
        )

        if (portrait) {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ZoneBox(Modifier.weight(1f).fillMaxWidth(), Palette.PlateABorder) {
                    AutoItemsFlow(c.plateA, c.capA, gap, fixedFs, onTap = { c.onFruitTap(it) })
                }
                if (c.plateBVisible) {
                    ZoneBox(Modifier.weight(1f).fillMaxWidth(), Palette.PlateBBorder) {
                        AutoItemsFlow(c.plateB, c.capB, gap, fixedFs)
                    }
                }
                if (c.goneVisible) {
                    ZoneBox(Modifier.weight(0.43f).fillMaxWidth(), Palette.GoneBorder, Palette.GoneBg) {
                        GoneZoneContent(c, vmin, gap, fixedFs)
                    }
                }
            }
        } else {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ZoneBox(Modifier.weight(1f).fillMaxHeight(), Palette.PlateABorder) {
                    AutoItemsFlow(c.plateA, c.capA, gap, fixedFs, onTap = { c.onFruitTap(it) })
                }
                if (c.plateBVisible) {
                    ZoneBox(Modifier.weight(1f).fillMaxHeight(), Palette.PlateBBorder) {
                        AutoItemsFlow(c.plateB, c.capB, gap, fixedFs)
                    }
                }
                if (c.goneVisible) {
                    ZoneBox(Modifier.weight(0.43f).fillMaxHeight(), Palette.GoneBorder, Palette.GoneBg) {
                        GoneZoneContent(c, vmin, gap, fixedFs)
                    }
                }
            }
        }
    }
}

/* ---------- picker (learn mode) ---------- */

@Composable
private fun Picker(c: GameViewModel, vmin: Float, modifier: Modifier) {
    BoxWithConstraints(
        modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        val gap = 8.dp
        val byW = (minOf(maxWidth, 560.dp) - gap * 4) / 5
        val byH = (maxHeight - 140.dp - gap * 3) / 4
        val btn = maxOf(34.dp, minOf(byW, byH))
        val numFs = clampSp(20f, 5f, 40f, vmin)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy((2.5f / 100f * vmin).dp)
        ) {
            Text(
                c.pickerTitle,
                fontSize = clampSp(20f, 4.5f, 34f, vmin),
                color = Palette.PromptGrey,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            if (c.gridVisible) {
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    for (row in 0 until 4) {
                        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                            for (col in 0 until 5) {
                                val num = row * 5 + col + 1
                                ChunkyButton(
                                    text = "$num",
                                    bg = Color.White,
                                    shadow = Palette.BtnShadow,
                                    fontSize = numFs,
                                    textColor = Palette.TextBlue,
                                    shape = RoundedCornerShape(20.dp),
                                    enabled = num <= c.gridEnabledMax,
                                    fillSize = true,
                                    modifier = Modifier.size(btn)
                                ) { c.onPickNumber(num) }
                            }
                        }
                    }
                }
            }
            if (c.opRowVisible) {
                val opSize = clampDp(90f, 22f, 150f, vmin)
                val opFs = clampSp(40f, 11f, 80f, vmin)
                Row(horizontalArrangement = Arrangement.spacedBy((6f / 100f * vmin).dp)) {
                    ChunkyButton(
                        "+", Palette.Green, Palette.GreenShadow, opFs,
                        shape = RoundedCornerShape(30.dp),
                        enabled = c.plusEnabled,
                        fillSize = true,
                        modifier = Modifier.size(opSize)
                    ) { c.onPickOp("+") }
                    ChunkyButton(
                        "−", Palette.OrangeBtn, Palette.OrangeBtnShadow, opFs,
                        shape = RoundedCornerShape(30.dp),
                        enabled = c.minusEnabled,
                        fillSize = true,
                        modifier = Modifier.size(opSize)
                    ) { c.onPickOp("−") }
                }
            }
            if (c.diceVisible) {
                ChunkyButton(
                    "🎲 Surprise me!", Palette.Purple, Palette.PurpleShadow,
                    clampSp(18f, 4f, 30f, vmin),
                    shape = RoundedCornerShape(40.dp)
                ) { c.diceRoll() }
            }
        }
    }
}

/* ---------- prompt + bottom bar ---------- */

@Composable
private fun PromptLine(prompt: String, vmin: Float) {
    val fs = clampSp(22f, 5.5f, 40f, vmin)
    val minH = with(LocalDensity.current) { fs.toDp() * 1.9f }
    Box(
        Modifier.fillMaxWidth().heightIn(min = minH).padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        if (prompt.isNotBlank()) {
            Text(
                prompt,
                Modifier
                    .background(Palette.PromptChip, RoundedCornerShape(22.dp))
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                textAlign = TextAlign.Center,
                fontSize = fs,
                fontWeight = FontWeight.Bold,
                color = Palette.PromptPop
            )
        }
    }
}

@Composable
private fun AnswerButton(c: GameViewModel, idx: Int, vmin: Float) {
    val size = clampDp(78f, 19f, 132f, vmin)
    val fs = clampSp(32f, 9f, 64f, vmin)
    val isCorrect = c.ansCorrectIdx == idx
    val isWrong = c.ansWrongIdx == idx
    val dim = c.dimOthers && !isCorrect

    val shake = remember { Animatable(0f) }
    LaunchedEffect(c.ansWrongTick) {
        if (c.ansWrongTick > 0 && c.ansWrongIdx == idx) {
            repeat(2) {
                shake.animateTo(-10f, tween(90))
                shake.animateTo(10f, tween(90))
            }
            shake.animateTo(0f, tween(90))
        }
    }

    val bg = when {
        isCorrect -> Palette.Green
        isWrong -> Palette.WrongPink
        else -> Color.White
    }
    Box(
        Modifier
            .graphicsLayer { translationX = shake.value.dp.toPx() }
            .alpha(if (dim) 0.4f else 1f)
    ) {
        ChunkyButton(
            text = "${c.answers.getOrNull(idx) ?: ""}",
            bg = bg,
            shadow = if (isCorrect) Palette.GreenShadow else Palette.BtnShadow,
            fontSize = fs,
            textColor = if (isCorrect) Color.White else Palette.TextBlue,
            shape = RoundedCornerShape(26.dp),
            enabled = !dim,
            fillSize = true,
            modifier = Modifier.size(size)
        ) { c.onAnswer(idx) }
    }
}

@Composable
private fun BottomBar(c: GameViewModel, vmin: Float) {
    val minH = clampDp(96f, 22f, 160f, vmin)
    val actionFs = clampSp(22f, 5.5f, 40f, vmin)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = minH)
            .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy((4f / 100f * vmin).dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (c.answers.isNotEmpty()) {
            for (i in 0..2) AnswerButton(c, i, vmin)
        }
        if (c.showMerge) {
            ChunkyButton("🧺 Put together!", Palette.Green, Palette.GreenShadow, actionFs) { c.onMerge() }
        }
        if (c.showAgain) {
            ChunkyButton("🔁 Again", Palette.Blue, Palette.BlueShadow, actionFs) { c.onAgain() }
        }
        if (c.showNew) {
            ChunkyButton("🎲 New numbers", Palette.Purple, Palette.PurpleShadow, actionFs) { c.onNew() }
        }
    }
}

/* ---------- overlays ---------- */

@Composable
private fun OverlayScaffold(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Palette.OverlayTop, Palette.OverlayBottom)))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* swallow touches behind the overlay */ }
            .systemBarsPadding()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)
    ) {
        content()
    }
}

@Composable
private fun OverlayTitle(vmin: Float) {
    Text("🍎 🍊 🥭", fontSize = clampSp(38f, 10f, 76f, vmin), textAlign = TextAlign.Center)
    Text(
        "Count & Play",
        fontSize = clampSp(32f, 9f, 66f, vmin),
        fontWeight = FontWeight.Bold,
        color = Color.White,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun StartOverlay(vmin: Float, onPlay: () -> Unit) {
    val bigFs = clampSp(22f, 6f, 42f, vmin)
    OverlayScaffold {
        OverlayTitle(vmin)
        ChunkyButton(
            "▶ Play", Palette.Yellow, Palette.YellowShadow, bigFs,
            shape = RoundedCornerShape(60.dp),
            modifier = Modifier.widthIn(min = 200.dp)
        ) { onPlay() }
    }
}

@Composable
private fun MenuOverlay(vmin: Float, onLearn: () -> Unit, onQuiz: () -> Unit) {
    val bigFs = clampSp(22f, 6f, 42f, vmin)
    val subFs = clampSp(13f, 3f, 19f, vmin)
    OverlayScaffold {
        OverlayTitle(vmin)
        ChunkyButton(
            "🧺 Learn", Palette.Green, Palette.GreenShadow, bigFs,
            shape = RoundedCornerShape(60.dp),
            modifier = Modifier.widthIn(min = 220.dp)
        ) { onLearn() }
        Text("pick numbers, watch and do", fontSize = subFs, color = Color(0xE6FFFFFF))
        ChunkyButton(
            "⭐ Quiz", Palette.Blue, Palette.BlueShadow, bigFs,
            shape = RoundedCornerShape(60.dp),
            modifier = Modifier.widthIn(min = 220.dp)
        ) { onQuiz() }
        Text("watch, count, answer", fontSize = subFs, color = Color(0xE6FFFFFF))
    }
}
