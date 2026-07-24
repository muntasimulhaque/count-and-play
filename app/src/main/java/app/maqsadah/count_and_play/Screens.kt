package app.maqsadah.count_and_play

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

        if (!c.langSet) {
            // First launch: a grown-up picks the language before the child plays.
            LanguagePickerOverlay(vmin) { c.setLanguage(it) }
        } else {
            when (c.screen) {
                Screen.START -> StartOverlay(c, vmin) { c.onPlay() }
                Screen.MENU -> MenuOverlay(
                    c, vmin,
                    onGuided = { c.enterGuided() },
                    onFree = { c.enterFree() },
                    onQuiz = { c.enterQuiz() }
                )
                else -> {}
            }

            if (c.settingsVisible) SettingsOverlay(c, speaker, vmin)
            if (c.levelUpVisible) LevelUpOverlay(c, vmin)
        }

        ConfettiOverlay(c.confettiTick)
    }
}

/* ---------- header ---------- */

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IconCircleButton(
    icon: String,
    label: String,
    vmin: Float,
    onLongClick: (() -> Unit)? = null,
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
            .combinedClickable(
                interactionSource = interaction,
                indication = ripple(),
                onLongClick = onLongClick?.let { lc ->
                    {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        lc()
                    }
                },
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            )
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
        val center = when (c.mode) {
            // Guided: level badge + star dots showing progress to the next level.
            Mode.GUIDED -> {
                val dots = if (c.starsInLevel <= G.STARS_PER_LEVEL) {
                    "⭐".repeat(c.starsInLevel)
                } else {
                    "⭐×${c.L.digits(c.starsInLevel)}"   // at the top level stars just accumulate
                }
                "🎓 ${c.L.digits(c.level)}  $dots"
            }
            // Quiz: session stars, as before.
            Mode.QUIZ -> "⭐".repeat(minOf(c.correctCount, 10))
            else -> ""
        }
        Text(
            center,
            Modifier.weight(1f),
            textAlign = TextAlign.Center,
            fontSize = clampSp(16f, 3.5f, 26f, vmin),
            fontWeight = FontWeight.Bold,
            color = Palette.TextBlue,
            letterSpacing = 2.sp
        )
        // Grown-ups settings (tap). Kept next to mute, away from the big play area.
        IconCircleButton("⚙️", c.L.settingsLabel(), vmin) { c.openSettings() }
        // Speaker button: mute toggle only.
        IconCircleButton(
            icon = if (speaker.soundOn) "🔊" else "🔇",
            label = if (speaker.soundOn) c.L.muteLabel() else c.L.unmuteLabel(),
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
            AutoItemsFlow(c.goneItems, c.capGone, gap, fixedFs, digits = { c.L.digits(it) })
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
        // worst case — MAX_N objects in a main plate, laid out in rows of five.
        // Two equal plates stacked (addition) is the tightest a main plate is ever
        // shown at, so we size for that geometry and reuse the single value in
        // every zone and every problem. Mirrors the exact layout constants below
        // so measured == rendered.
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
        val fixedFs = fitGrid(
            with(density) { plateInnerW.toPx() },
            with(density) { plateInnerH.toPx() },
            G.MAX_N,
            with(density) { gap.toPx() },
            density
        ).first

        if (portrait) {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ZoneBox(Modifier.weight(1f).fillMaxWidth(), Palette.PlateABorder) {
                    AutoItemsFlow(c.plateA, c.capA, gap, fixedFs, onTap = { c.onItemTap(it) }, digits = { c.L.digits(it) })
                }
                if (c.plateBVisible) {
                    ZoneBox(Modifier.weight(1f).fillMaxWidth(), Palette.PlateBBorder) {
                        AutoItemsFlow(c.plateB, c.capB, gap, fixedFs, onTap = { c.onItemTap(it) }, digits = { c.L.digits(it) })
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
                    AutoItemsFlow(c.plateA, c.capA, gap, fixedFs, onTap = { c.onItemTap(it) }, digits = { c.L.digits(it) })
                }
                if (c.plateBVisible) {
                    ZoneBox(Modifier.weight(1f).fillMaxHeight(), Palette.PlateBBorder) {
                        AutoItemsFlow(c.plateB, c.capB, gap, fixedFs, onTap = { c.onItemTap(it) }, digits = { c.L.digits(it) })
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

/* ---------- picker (free play) ---------- */

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
                                    text = c.L.digits(num),
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
                    c.L.surprise(), Palette.Purple, Palette.PurpleShadow,
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
            text = c.answers.getOrNull(idx)?.let { c.L.digits(it) } ?: "",
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
            ChunkyButton(c.L.putTogetherBtn(), Palette.Green, Palette.GreenShadow, actionFs) { c.onMerge() }
        }
        if (c.showAgain) {
            ChunkyButton(c.L.againBtn(), Palette.Blue, Palette.BlueShadow, actionFs) { c.onAgain() }
        }
        if (c.showNew) {
            ChunkyButton(c.L.newNumbersBtn(), Palette.Purple, Palette.PurpleShadow, actionFs) { c.onNew() }
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
private fun OverlayTitle(c: GameViewModel, vmin: Float) {
    Text("🍎 🍊 🥭", fontSize = clampSp(38f, 10f, 76f, vmin), textAlign = TextAlign.Center)
    Text(
        c.L.appTitle(),
        fontSize = clampSp(32f, 9f, 66f, vmin),
        fontWeight = FontWeight.Bold,
        color = Color.White,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun StartOverlay(c: GameViewModel, vmin: Float, onPlay: () -> Unit) {
    val bigFs = clampSp(22f, 6f, 42f, vmin)
    OverlayScaffold {
        OverlayTitle(c, vmin)
        ChunkyButton(
            c.L.playBtn(), Palette.Yellow, Palette.YellowShadow, bigFs,
            shape = RoundedCornerShape(60.dp),
            modifier = Modifier.widthIn(min = 200.dp)
        ) { onPlay() }
    }
}

/** One huge icon-dominant menu button with a small caption (3-year-olds can't read). */
@Composable
private fun MenuButton(
    icon: String,
    caption: String,
    bg: Color,
    shadow: Color,
    vmin: Float,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ChunkyButton(
            icon, bg, shadow,
            clampSp(38f, 11f, 72f, vmin),
            shape = RoundedCornerShape(60.dp),
            modifier = Modifier.widthIn(min = 180.dp)
        ) { onClick() }
        Text(
            caption,
            fontSize = clampSp(13f, 3f, 19f, vmin),
            fontWeight = FontWeight.Bold,
            color = Color(0xE6FFFFFF)
        )
    }
}

/** A secondary circular icon button with a small caption (the settings gear). */
@Composable
private fun MenuIconButton(
    icon: String,
    caption: String,
    vmin: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconCircleButton(icon, caption, vmin, onClick = onClick)
        Text(
            caption,
            fontSize = clampSp(13f, 3f, 19f, vmin),
            fontWeight = FontWeight.Bold,
            color = Color(0xE6FFFFFF)
        )
    }
}

@Composable
private fun MenuOverlay(
    c: GameViewModel,
    vmin: Float,
    onGuided: () -> Unit,
    onFree: () -> Unit,
    onQuiz: () -> Unit
) {
    OverlayScaffold {
        Box(Modifier.fillMaxSize()) {
            // The three big play buttons (and the title) are the stars of the menu —
            // keep them centered as a group in the main space.
            Column(
                Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                OverlayTitle(c, vmin)
                MenuButton("▶", c.L.menuPlay(), Palette.Yellow, Palette.YellowShadow, vmin, onGuided)
                MenuButton("🧺", c.L.menuFree(), Palette.Green, Palette.GreenShadow, vmin, onFree)
                MenuButton("⭐", c.L.menuQuiz(), Palette.Blue, Palette.BlueShadow, vmin, onQuiz)
            }
            // Grown-ups settings — a labeled gear pinned to the bottom so it doesn't
            // compete with the play buttons (language, voice, rate, reset).
            MenuIconButton(
                "⚙️", c.L.settingsLabel(), vmin,
                Modifier.align(Alignment.BottomCenter)
            ) { c.openSettings() }
        }
    }
}

/* ---------- first-launch language picker (a grown-up chooses once) ---------- */

@Composable
private fun LanguagePickerOverlay(vmin: Float, onPick: (Lang) -> Unit) {
    val titleFs = clampSp(22f, 6f, 40f, vmin)
    val btnFs = clampSp(20f, 5.5f, 36f, vmin)
    OverlayScaffold {
        Text("🌐", fontSize = clampSp(48f, 13f, 96f, vmin), textAlign = TextAlign.Center)
        // Bilingual title — no language is active yet, so this can't be localized.
        Text(
            "Choose language\nভাষা বেছে নিন",
            fontSize = titleFs,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Lang.values().forEach { lang ->
                ChunkyButton(
                    lang.nativeName, Color.White, Palette.BtnShadow, btnFs,
                    textColor = Palette.TextBlue,
                    shape = RoundedCornerShape(60.dp),
                    modifier = Modifier.widthIn(min = 220.dp)
                ) { onPick(lang) }
            }
        }
    }
}

/* ---------- grown-ups settings (tap the ⚙ gear) ---------- */

@Composable
private fun SettingsOverlay(c: GameViewModel, speaker: Speaker, vmin: Float) {
    val titleFs = clampSp(22f, 6f, 38f, vmin)
    val rowFs = clampSp(14f, 3.5f, 22f, vmin)
    // Reset is destructive, so it needs an explicit confirm — a toddler poking at
    // the (tap-to-open) settings can't wipe progress with a single stray tap.
    var confirmReset by remember { mutableStateOf(false) }

    OverlayScaffold {
        Text(
            c.L.settingsTitle(),
            fontSize = titleFs,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        // Language — shown in each language's own script so a grown-up finds theirs.
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Lang.values().forEach { lang ->
                val selected = c.lang == lang
                ChunkyButton(
                    lang.nativeName,
                    if (selected) Palette.Green else Color.White,
                    if (selected) Palette.GreenShadow else Palette.BtnShadow,
                    rowFs,
                    textColor = if (selected) Color.White else Palette.TextBlue
                ) { c.setLanguage(lang) }
            }
        }
        // Voice picker: voices for the current language (the list can fill in a beat
        // late while the engine initialises, so it lives in a scrollable box).
        Column(
            Modifier
                .heightIn(max = 200.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChunkyButton(
                c.L.defaultVoice(),
                if (speaker.voiceName == null) Palette.Green else Color.White,
                if (speaker.voiceName == null) Palette.GreenShadow else Palette.BtnShadow,
                rowFs,
                textColor = if (speaker.voiceName == null) Color.White else Palette.TextBlue
            ) { c.previewVoice(null) }
            speaker.voices.forEach { v ->
                val selected = speaker.voiceName == v.name
                ChunkyButton(
                    "🗣 ${v.label}",
                    if (selected) Palette.Green else Color.White,
                    if (selected) Palette.GreenShadow else Palette.BtnShadow,
                    rowFs,
                    textColor = if (selected) Color.White else Palette.TextBlue
                ) { c.previewVoice(v.name) }
            }
        }
        if (!speaker.voiceAvailable) {
            Text(
                c.L.noVoiceNote(),
                fontSize = rowFs,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
        // Speech rate
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ChunkyButton(
                c.L.slow(),
                if (speaker.slowRate) Palette.Green else Color.White,
                if (speaker.slowRate) Palette.GreenShadow else Palette.BtnShadow,
                rowFs,
                textColor = if (speaker.slowRate) Color.White else Palette.TextBlue
            ) { c.setSlowRate(true) }
            ChunkyButton(
                c.L.normal(),
                if (!speaker.slowRate) Palette.Green else Color.White,
                if (!speaker.slowRate) Palette.GreenShadow else Palette.BtnShadow,
                rowFs,
                textColor = if (!speaker.slowRate) Color.White else Palette.TextBlue
            ) { c.setSlowRate(false) }
        }
        ChunkyButton(
            c.L.resetProgress(), Palette.OrangeBtn, Palette.OrangeBtnShadow, rowFs
        ) { confirmReset = true }
        ChunkyButton(
            c.L.done(), Palette.Blue, Palette.BlueShadow, titleFs,
            shape = RoundedCornerShape(60.dp)
        ) { c.closeSettings() }
    }

    if (confirmReset) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xCC000000))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* swallow touches behind the confirm dialog */ }
                .systemBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier
                    .background(Palette.PromptChip, RoundedCornerShape(24.dp))
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    c.L.areYouSure(),
                    fontSize = titleFs,
                    fontWeight = FontWeight.Bold,
                    color = Palette.PromptPop,
                    textAlign = TextAlign.Center
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ChunkyButton(
                        c.L.yes(), Palette.OrangeBtn, Palette.OrangeBtnShadow, rowFs
                    ) {
                        c.resetProgress()
                        confirmReset = false
                    }
                    ChunkyButton(
                        c.L.no(), Palette.Blue, Palette.BlueShadow, rowFs
                    ) { confirmReset = false }
                }
            }
        }
    }
}

/* ---------- level-up celebration ---------- */

@Composable
private fun LevelUpOverlay(c: GameViewModel, vmin: Float) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x55FFFFFF))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* swallow touches while celebrating (auto-hides) */ }
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .background(Palette.PromptChip, RoundedCornerShape(28.dp))
                .padding(horizontal = 28.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🎉 🎓 🎉", fontSize = clampSp(40f, 11f, 80f, vmin), textAlign = TextAlign.Center)
            Text(
                c.L.levelTitle(c.level),
                fontSize = clampSp(30f, 8f, 56f, vmin),
                fontWeight = FontWeight.Bold,
                color = Palette.PromptPop,
                textAlign = TextAlign.Center
            )
            Text(
                c.L.nowCountingTo(c.levelCap()),
                fontSize = clampSp(18f, 5f, 32f, vmin),
                fontWeight = FontWeight.Bold,
                color = Palette.TextBlue,
                textAlign = TextAlign.Center
            )
        }
    }
}
