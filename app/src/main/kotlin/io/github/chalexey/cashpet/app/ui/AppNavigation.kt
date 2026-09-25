package io.github.chalexey.cashpet.app.ui

import android.content.Context
import android.media.MediaPlayer
import android.media.SoundPool
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.chalexey.cashpet.R
import io.github.chalexey.cashpet.app.ui.components.FeedbackBar
import io.github.chalexey.cashpet.app.ui.components.SectionTitle
import io.github.chalexey.cashpet.app.ui.components.StatChip
import io.github.chalexey.cashpet.app.ui.theme.*
import io.github.chalexey.cashpet.content.ContentLoader
import io.github.chalexey.cashpet.content.GameContent
import io.github.chalexey.cashpet.core.model.PetMood
import io.github.chalexey.cashpet.core.model.Part
import io.github.chalexey.cashpet.core.model.Stat
import io.github.chalexey.cashpet.core.model.Stage
import io.github.chalexey.cashpet.core.task.TaskDef
import androidx.core.content.edit

private enum class Screen { START, ONBOARDING, HOME, PLAN, SHOP, SAVINGS, TASKS, TASK, PROGRESS, SETTINGS, ADULT, DEMO, WEEK }
private enum class ThemeMode { SYSTEM, LIGHT, DARK }

private data class AppState(
    val playerName: String = "",
    val petName: String = "",
    val balance: Int = 100,
    val savings: Int = 0,
    val week: Int = 1,
    val planNeed: Int = 50,
    val planWant: Int = 25,
    val planSave: Int = 25,
    val planConfirmed: Boolean = false,
    val satiety: Int = 70,
    val care: Int = 70,
    val mood: Int = 70,
    val gp: Int = 0,
    val stage: Int = 0,
    val petKind: Int = 0,
    val petCoat: Int = 0,
    val completedTasks: Set<String> = emptySet(),
)

private object SaveStore {
    private const val PREF = "cashpet_mvp"
    private fun prefs(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
    private fun prefix(demo: Boolean) = if (demo) "demo_" else "child_"

    fun load(c: Context, demo: Boolean): AppState {
        val p=prefs(c); val x=prefix(demo)
        val tasks=p.getStringSet(x+"tasks", emptySet()) ?: emptySet()
        return AppState(
            playerName=p.getString(x+"player", "") ?: "", petName=p.getString(x+"pet", "") ?: "",
            balance=p.getInt(x+"balance", if(demo) 500 else 100), savings=p.getInt(x+"savings", if(demo) 80 else 0),
            week=p.getInt(x+"week", 1), planNeed=p.getInt(x+"need",50), planWant=p.getInt(x+"want",25), planSave=p.getInt(x+"save",25),
            planConfirmed=p.getBoolean(x+"confirmed",false), satiety=p.getInt(x+"sat",70), care=p.getInt(x+"care",70), mood=p.getInt(x+"mood",70),
            gp=p.getInt(x+"gp", if(demo) 120 else 0), stage=p.getInt(x+"stage", if(demo) 0 else 0), petKind=p.getInt(x+"kind",0), petCoat=p.getInt(x+"coat",0), completedTasks=tasks,
        )
    }
    fun save(c: Context, demo: Boolean, s: AppState) {
        prefs(c).edit {
            val x = prefix(demo)
            putString(x + "player", s.playerName).putString(x + "pet", s.petName)
                .putInt(x + "balance", s.balance).putInt(x + "savings", s.savings)
                .putInt(x + "week", s.week).putInt(x + "need", s.planNeed)
                .putInt(x + "want", s.planWant).putInt(x + "save", s.planSave)
                .putBoolean(x + "confirmed", s.planConfirmed)
                .putInt(x + "sat", s.satiety).putInt(x + "care", s.care).putInt(x + "mood", s.mood)
                .putInt(x + "gp", s.gp).putInt(x + "stage", s.stage).putInt(x + "kind", s.petKind)
                .putInt(x + "coat", s.petCoat).putStringSet(x + "tasks", s.completedTasks)
        }
    }
    fun loadDemoActive(c: Context): Boolean = prefs(c).getBoolean("demo_active", false)

    fun setDemoActive(c: Context, active: Boolean) {
        prefs(c).edit { putBoolean("demo_active", active) }
    }

    fun loadTheme(c: Context): ThemeMode = when (prefs(c).getString("theme_mode", "system")) {
        "light" -> ThemeMode.LIGHT
        "dark" -> ThemeMode.DARK
        else -> ThemeMode.SYSTEM
    }

    fun saveTheme(c: Context, mode: ThemeMode) {
        prefs(c).edit { putString("theme_mode", mode.name.lowercase()) }
    }

    fun reset(c: Context, demo: Boolean) { val p=prefs(c); val x=prefix(demo); p.edit {
        remove(x + "player").remove(
            x + "pet"
        ).remove(x + "balance").remove(x + "savings").remove(x + "week").remove(x + "need")
            .remove(x + "want").remove(x + "save").remove(x + "confirmed").remove(x + "sat")
            .remove(x + "care").remove(x + "mood").remove(x + "gp").remove(x + "stage")
            .remove(x + "kind").remove(x + "coat").remove(x + "tasks")
    } }
}

private class SoundController(context: Context) {
    private val app = context.applicationContext
    private var music: MediaPlayer? = null
    private val pool = SoundPool.Builder().setMaxStreams(3).build()
    private val click = pool.load(app, R.raw.cashpet_click, 1)
    private val reward = pool.load(app, R.raw.cashpet_reward, 1)
    var enabled by mutableStateOf(true)
    fun startMusic() { if (!enabled) return; if (music==null) music=MediaPlayer.create(app,R.raw.cashpet_music)?.apply { isLooping=true; setVolume(.18f,.18f) }; if (music?.isPlaying!=true) music?.start() }
    fun stopMusic() { music?.pause() }
    fun click() { if(enabled) pool.play(click, .35f,.35f,1,0,1f) }
    fun reward() { if(enabled) pool.play(reward,.55f,.55f,1,0,1f) }
    fun release(){ music?.release(); pool.release() }
}

@Composable
fun CashPetApp() {
    val context = LocalContext.current
    val sound = remember { SoundController(context) }
    DisposableEffect(Unit) { onDispose { sound.release() } }
    val content = remember { runCatching { ContentLoader().load() }.getOrNull() }
    val savedDemo = remember { SaveStore.loadDemoActive(context) }
    var demoMode by rememberSaveable { mutableStateOf(savedDemo) }
    var state by remember { mutableStateOf(SaveStore.load(context, savedDemo)) }
    var screen by rememberSaveable { mutableStateOf(if (savedDemo) Screen.DEMO else Screen.START) }
    var onboardingStep by rememberSaveable { mutableIntStateOf(0) }
    var selectedTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var closeWeek by remember { mutableStateOf(false) }
    var adultGate by remember { mutableStateOf(false) }
    var themeMode by remember { mutableStateOf(SaveStore.loadTheme(context)) }
    var helpScreen by rememberSaveable { mutableStateOf<Screen?>(null) }
    var tutorialPage by rememberSaveable { mutableIntStateOf(0) }
    var showTutorial by rememberSaveable { mutableStateOf(false) }

    fun commit(next: AppState, rewardSound: Boolean = false) {
        state = next
        SaveStore.save(context, demoMode, next)
        if (rewardSound) sound.reward() else sound.click()
    }

    fun enterDemo() {
        SaveStore.setDemoActive(context, true)
        demoMode = true
        state = SaveStore.load(context, true)
        screen = Screen.DEMO
        sound.startMusic()
    }

    fun leaveDemo() {
        SaveStore.setDemoActive(context, false)
        demoMode = false
        state = SaveStore.load(context, false)
        screen = Screen.HOME
        sound.startMusic()
    }

    LaunchedEffect(Unit) { sound.startMusic() }

    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
    }

    CashPetTheme(darkTheme = darkTheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (screen) {
                Screen.START -> {
                    StartScreen(
                        onPlay = { onboardingStep = 0; screen = Screen.ONBOARDING },
                        onAdult = { adultGate = true }
                    )
                }
                Screen.ONBOARDING -> {
                    OnboardingScreen(
                        step = onboardingStep,
                        state = state,
                        onNext = {
                            if (onboardingStep < 6) {
                                onboardingStep++
                            } else if (state.playerName.isBlank() || state.petName.isBlank()) {
                                feedback = "Заполни имя игрока и имя котёнка"
                            } else {
                                commit(state)
                                screen = Screen.HOME
                                tutorialPage = 0
                                showTutorial = true
                            }
                        },
                        onSkip = { onboardingStep = (onboardingStep + 1).coerceAtMost(6) },
                        onState = { next -> commit(next) },
                        sound = sound
                    )
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) {
                            when (screen) {
                                Screen.HOME -> HomeScreen(
                                    state, content,
                                    onPet = { screen = Screen.PROGRESS },
                                    onTask = { screen = Screen.TASKS },
                                    onPlan = { screen = Screen.PLAN },
                                    onSettings = { screen = Screen.SETTINGS },
                                    onShop = { screen = Screen.SHOP },
                                    onSavings = { screen = Screen.SAVINGS },
                                    onClose = { closeWeek = true },
                                    onHelp = { tutorialPage = 0; helpScreen = Screen.HOME }
                                )

                                Screen.PLAN -> PlanScreen(
                                    state,
                                    onBack = { screen = Screen.HOME },
                                    onChange = { next -> commit(next) },
                                    onConfirm = {
                                        commit(state.copy(planConfirmed = true))
                                        feedback = "План сохранён. Теперь можно тратить по-своему."
                                    }
                                )

                                Screen.SHOP -> ShopScreen(
                                    state, content,
                                    onBack = { screen = Screen.HOME },
                                    onPlan = { screen = Screen.PLAN },
                                    onBuy = { item ->
                                        when {
                                            !state.planConfirmed -> screen = Screen.PLAN
                                            state.balance < item.price -> {
                                                feedback =
                                                    "Не хватает ${item.price - state.balance} монет. Можно пройти задание или подработку."
                                            }

                                            else -> {
                                                val satiety = (state.satiety + (item.effects[Stat.SATIETY]
                                                    ?: 0)).coerceIn(25, 100)
                                                val care =
                                                    (state.care + (item.effects[Stat.CARE] ?: 0)).coerceIn(
                                                        25,
                                                        100
                                                    )
                                                val mood =
                                                    (state.mood + (item.effects[Stat.MOOD] ?: 0)).coerceIn(
                                                        25,
                                                        100
                                                    )
                                                commit(
                                                    state.copy(
                                                        balance = state.balance - item.price,
                                                        satiety = satiety,
                                                        care = care,
                                                        mood = mood
                                                    )
                                                )
                                                feedback = "Баланс изменён. ${item.name} помог питомцу."
                                            }
                                        }
                                    }
                                )

                                Screen.SAVINGS -> SavingsScreen(
                                    state, content,
                                    onBack = { screen = Screen.HOME },
                                    onPlan = { screen = Screen.PLAN },
                                    onDeposit = { amount ->
                                        when {
                                            !state.planConfirmed -> screen = Screen.PLAN
                                            state.balance >= amount -> {
                                                commit(
                                                    state.copy(
                                                        balance = state.balance - amount,
                                                        savings = state.savings + amount
                                                    )
                                                )
                                                feedback = "В копилке стало ${state.savings + amount} монет."
                                            }

                                            else -> feedback = "На балансе пока недостаточно монет."
                                        }
                                    },
                                    onWithdraw = { amount ->
                                        if (state.savings >= amount) {
                                            commit(
                                                state.copy(
                                                    balance = state.balance + amount,
                                                    savings = state.savings - amount
                                                )
                                            )
                                            feedback = "Ты снял $amount монет."
                                        }
                                    }
                                )

                                Screen.TASKS -> TasksScreen(
                                    state, content,
                                    onBack = { screen = Screen.HOME },
                                    onOpen = { id ->
                                        selectedTaskId = id
                                        screen = Screen.TASK
                                    },
                                    onJob = {
                                        commit(state.copy(balance = state.balance + 10))
                                        feedback = "Подработка: +10 монет"
                                    }
                                )

                                Screen.TASK -> TaskScreen(
                                    task = content?.tasks?.firstOrNull { task -> task.id == selectedTaskId },
                                    completed = selectedTaskId in state.completedTasks,
                                    onBack = { screen = Screen.TASKS },
                                    onFinish = { id, reward ->
                                        if (id !in state.completedTasks) {
                                            commit(
                                                state.copy(
                                                    balance = state.balance + reward,
                                                    gp = state.gp + 40,
                                                    completedTasks = state.completedTasks + id
                                                ),
                                                true
                                            )
                                            feedback = "Задание пройдено: +$reward монет и +40 GP."
                                        }
                                        screen = Screen.TASKS
                                    }
                                )

                                Screen.PROGRESS -> ProgressScreen(
                                    state,
                                    onBack = { screen = Screen.HOME },
                                    onStage = {
                                        val nextStage = (state.stage + 1).coerceAtMost(2)
                                        val requiredGp = if (state.stage == 0) 150 else 350
                                        commit(state.copy(stage = nextStage, gp = maxOf(state.gp, requiredGp)))
                                    }
                                )

                                Screen.SETTINGS -> SettingsScreen(
                                    state = state,
                                    sound = sound,
                                    themeMode = themeMode,
                                    onTheme = { mode -> themeMode = mode; SaveStore.saveTheme(context, mode) },
                                    onHelp = { tutorialPage = 0; helpScreen = Screen.SETTINGS },
                                    onBack = { screen = Screen.HOME },
                                    onAdult = { adultGate = true },
                                    onLook = { onboardingStep = 2; screen = Screen.ONBOARDING }
                                )

                                Screen.ADULT -> AdultScreen(
                                    state,
                                    onBack = { screen = Screen.HOME },
                                    onReset = {
                                        SaveStore.reset(context, demoMode)
                                        state = SaveStore.load(context, demoMode)
                                        screen = Screen.START
                                    },
                                    onDemo = { enterDemo() }
                                )

                                Screen.DEMO -> DemoScreen(
                                    state,
                                    onBack = { leaveDemo() },
                                    onAdd = { amount ->
                                        commit(
                                            state.copy(balance = state.balance + amount),
                                            true
                                        )
                                    },
                                    onGp = { amount -> commit(state.copy(gp = state.gp + amount), true) },
                                    onStage = {
                                        val nextStage = (state.stage + 1).coerceAtMost(2)
                                        val nextGp = when (state.stage) {
                                            0 -> 150
                                            1 -> 350
                                            else -> state.gp
                                        }
                                        commit(
                                            state.copy(
                                                stage = nextStage,
                                                gp = nextGp,
                                                satiety = 100,
                                                care = 100,
                                                mood = 100
                                            ),
                                            true
                                        )
                                    },
                                    onReset = {
                                        SaveStore.reset(context, true)
                                        SaveStore.setDemoActive(context, true)
                                        state = SaveStore.load(context, true)
                                    }
                                )

                                Screen.WEEK -> WeekScreen(
                                    state,
                                    onNext = {
                                        commit(
                                            state.copy(
                                                week = state.week + 1,
                                                planConfirmed = false,
                                                planNeed = 50,
                                                planWant = 25,
                                                planSave = 25
                                            )
                                        )
                                        screen = Screen.PLAN
                                    }
                                )

                                Screen.START, Screen.ONBOARDING -> Unit
                            }
                        }

                        if (screen != Screen.START && screen != Screen.ONBOARDING) {
                            Box(
                                Modifier.fillMaxWidth().padding(top = 8.dp, end = 16.dp),
                                contentAlignment = Alignment.TopEnd
                            ) {
                                CircleButton("?", onClick = { tutorialPage = 0; helpScreen = screen })
                            }
                        }

                        feedback?.let { message ->
                            FeedbackBar(
                                text = message,
                                onDismiss = { feedback = null },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        if (screen == Screen.HOME || screen == Screen.PLAN || screen == Screen.SHOP ||
                            screen == Screen.SAVINGS || screen == Screen.TASKS
                        ) {
                            BottomBar(screen = screen, onNav = { target -> screen = target; sound.click() })
                        }
                    }
                }
            }
    }

    if (closeWeek) {
        AlertDialog(
            onDismissRequest = { closeWeek = false },
            title = { Text("Завершить неделю?") },
            text = { Text("Итог покажет план, покупки и накопления. Можно завершить даже с неидеальным результатом.") },
            confirmButton = {
                TextButton(onClick = { closeWeek = false; screen = Screen.WEEK }) { Text("Завершить") }
            },
            dismissButton = {
                TextButton(onClick = { closeWeek = false }) { Text("Ещё подумаю") }
            }
        )
    }

    if (adultGate) {
        AdultGate(
            onPassed = { adultGate = false; screen = Screen.ADULT },
            onDismiss = { adultGate = false }
        )
    }

    if (showTutorial || helpScreen != null) {
        val target = helpScreen ?: screen
        ChildHelpDialog(
            screen = target,
            page = tutorialPage,
            onNext = {
                if (tutorialPage < 2) tutorialPage++ else {
                    showTutorial = false
                    helpScreen = null
                    tutorialPage = 0
                }
            },
            onDismiss = { showTutorial = false; helpScreen = null; tutorialPage = 0 }
        )
    }
    }
}

@Composable
private fun AdultGate(
    onPassed: () -> Unit,
    onDismiss: () -> Unit
) {
    var answer by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Раздел для взрослых") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Реши пример, чтобы открыть настройки взрослого.")
                Text("7 × 8 = ?")
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it.filter(Char::isDigit) },
                    singleLine = true,
                    isError = error,
                    label = { Text("Ответ") }
                )
                if (error) {
                    Text(
                        "Попробуй ещё раз.",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (answer == "56") {
                        onPassed()
                    } else {
                        error = true
                    }
                }
            ) {
                Text("Открыть")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
private fun StartScreen(
    onPlay: () -> Unit,
    onAdult: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceCream)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PetImage(0, 0, 0, modifier = Modifier.size(190.dp))
            Text(
                "CashPet",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Котёнок, план, мечта и понятные решения",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
            Button(
                onClick = onPlay,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Играть")
            }
            TextButton(onClick = onAdult) {
                Text("Для взрослых")
            }
        }
    }
}

@Composable
private fun OnboardingScreen(
    step: Int,
    state: AppState,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onState: (AppState) -> Unit,
    sound: SoundController
) {
    var player by rememberSaveable { mutableStateOf(state.playerName) }
    var pet by rememberSaveable { mutableStateOf(state.petName) }
    var selectedKind by rememberSaveable { mutableIntStateOf(state.petKind) }
    var selectedCoat by rememberSaveable { mutableIntStateOf(state.petCoat) }

    val titles = listOf(
        "Привет",
        "Добро пожаловать в Котляндию",
        "Выбери котёнка",
        "Придумай имя котёнку",
        "Три вида решений",
        "Как растёт питомец",
        "Как будем играть"
    )
    val bodies = listOf(
        "Придумай имя для игры. Настоящее имя не нужно.",
        "У питомца будут еда, уход, настроение и большая мечта.",
        "Выбери силуэт и окрас. Внешность можно изменить позже.",
        "До 12 букв. Например, Пончик.",
        "Нужно — базовые расходы. Хочу — приятные вещи. Отложить — мечта.",
        "Малыш → Подросток → Взрослый. Рост не уменьшается.",
        "Попроще — больше подсказок. Посложнее — меньше подсказок."
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                "${step + 1} / 7",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                titles[step],
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                bodies[step],
                style = MaterialTheme.typography.bodyLarge
            )

            if (step == 0) {
                OutlinedTextField(
                    value = player,
                    onValueChange = {
                        if (it.length <= 12) {
                            player = it
                            onState(state.copy(playerName = it))
                        }
                    },
                    placeholder = { Text("Например: Саша") },
                    label = { Text("Имя для игры") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )
            }

            if (step == 2) {
                Text("3 силуэта × 3 окраса", fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (k in 0..2) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (c in 0..2) {
                                val selected = selectedKind == k && selectedCoat == c
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clickable {
                                            selectedKind = k
                                            selectedCoat = c
                                            onState(
                                                state.copy(
                                                    petKind = k,
                                                    petCoat = c
                                                )
                                            )
                                            sound.click()
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        PetImage(
                                            k,
                                            c,
                                            0,
                                            modifier = Modifier.size(74.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (step == 3) {
                OutlinedTextField(
                    value = pet,
                    onValueChange = {
                        if (it.length <= 12) {
                            pet = it
                            onState(state.copy(petName = it))
                        }
                    },
                    placeholder = { Text("Например: Пончик") },
                    label = { Text("Имя питомца") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (step == 4) {
                InfoCard("Нужно", "Еда и уход", NeedGreen)
                InfoCard("Хочу", "Игрушки и приятные мелочи", WantBlue)
                InfoCard("Отложить", "Копилка на мечту", SaveViolet)
            }

            if (step == 5) {
                InfoCard("Малыш", "Стартовая стадия", WantBlue)
                InfoCard("Подросток", "После накопления GP", NeedGreen)
                InfoCard("Взрослый", "После дальнейшего прогресса", SaveViolet)
            }

            if (step == 6) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = true,
                        onClick = {},
                        label = { Text("Попроще") }
                    )
                    FilterChip(
                        selected = false,
                        onClick = {},
                        label = { Text("Посложнее") }
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    sound.click()
                    onNext()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    when (step) {
                        6 -> "Начать"
                        1 -> "Помогу"
                        else -> "Дальше"
                    }
                )
            }

            if (step in 4..5) {
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Пропустить")
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    s: AppState,
    content: GameContent?,
    onPet: () -> Unit,
    onTask: () -> Unit,
    onPlan: () -> Unit,
    onSettings: () -> Unit,
    onShop: () -> Unit,
    onSavings: () -> Unit,
    onClose: () -> Unit,
    onHelp: () -> Unit,
) {
    val mood = petMood(s)
    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Котляндия", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text("Привет, ${s.playerName.ifBlank { "друг" }}!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Неделя ${s.week} · ${stageName(s.stage)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircleButton("?", onHelp)
                    CircleButton("⚙", onSettings)
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = MaterialTheme.shapes.large,
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(moodText(mood), fontWeight = FontWeight.Bold)
                        Text("GP ${s.gp}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(4.dp))
                    PetImage(s.petKind, s.petCoat, s.stage, Modifier.size(190.dp).clickable(onClick = onPet))
                    Text(s.petName.ifBlank { "Твой котёнок" }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Нажми на котёнка, чтобы посмотреть состояние", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("🪙", "Баланс", "${s.balance}", Modifier.weight(1f))
                MetricCard("🐷", "Копилка", "${s.savings}", Modifier.weight(1f))
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Состояние питомца", fontWeight = FontWeight.Bold)
                        Text("${((s.mood + s.satiety + s.care) / 3)} / 100")
                    }
                    StatChip("🍗", "Сытость", s.satiety)
                    StatChip("🧼", "Уход", s.care)
                    StatChip("😊", "Настроение", s.mood)
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth().clickable(onClick = onTask), shape = MaterialTheme.shapes.large) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("✨", fontSize = 30.sp)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text("Учимся через задания", fontWeight = FontWeight.Bold)
                        Text("${s.completedTasks.size} из ${content?.tasks?.size ?: 7} пройдено", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("›", fontSize = 28.sp)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionCard("☷", "План", onPlan, Modifier.weight(1f))
                ActionCard("🛍", "Магазин", onShop, Modifier.weight(1f))
                ActionCard("🎯", "Мечта", onSavings, Modifier.weight(1f))
            }
        }
        item {
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth().height(54.dp), shape = MaterialTheme.shapes.medium) {
                Text("Завершить неделю")
            }
        }
    }
}

@Composable
private fun ActionCard(icon: String, title: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier.clickable(onClick = onClick), shape = MaterialTheme.shapes.medium) {
        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 24.sp)
            Text(title, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable private fun PlanScreen(s:AppState,onBack:()->Unit,onChange:(AppState)->Unit,onConfirm:()->Unit){val total=s.planNeed+s.planWant+s.planSave;val over=total>s.balance;LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){item{Header("План на неделю","Три части, один общий кошелёк",onBack)};item{MetricCard("🪙","Доступно","${s.balance}", modifier = Modifier.fillMaxWidth())};item{PlanPart("🍗","Нужно",s.planNeed,NeedGreen,"Корм 30 + шампунь 20",!s.planConfirmed,{d->onChange(s.copy(planNeed=(s.planNeed+d).coerceIn(0,100)))})};item{PlanPart("🎈","Хочу",s.planWant,WantBlue,"Игрушки и приятные вещи",!s.planConfirmed,{d->onChange(s.copy(planWant=(s.planWant+d).coerceIn(0,100)))})};item{PlanPart("🐷","Отложить",s.planSave,SaveViolet,"На большую мечту",!s.planConfirmed,{d->onChange(s.copy(planSave=(s.planSave+d).coerceIn(0,100)))})};item{Text("Не разложено: ${s.balance-total}",color=if(over)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)};item{if(s.planConfirmed){Card(colors=CardDefaults.cardColors(containerColor=NeedGreen.copy(alpha=.10f))){Text("План подтверждён. Тратить можно по-своему.", modifier = Modifier.padding(16.dp),fontWeight=FontWeight.Bold)}}else Button(enabled = !over, onClick = onConfirm, modifier = Modifier.fillMaxWidth().height(54.dp)){Text("Подтвердить план")};if(over)Text("Сумма плана больше доступных монет.",color=MaterialTheme.colorScheme.error)}}}

@Composable private fun PlanPart(icon:String,title:String,amount:Int,accent:Color,hint:String,enabled:Boolean,onDelta:(Int)->Unit){Card(colors=CardDefaults.cardColors(containerColor=accent.copy(alpha=.09f))){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("$icon  $title",fontWeight=FontWeight.Bold);Text("$amount 🪙",fontWeight=FontWeight.Bold)};Text(hint,style=MaterialTheme.typography.bodyMedium);if(enabled)Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick = { onDelta(-5) }, modifier = Modifier.weight(1f).height(48.dp), enabled = amount > 0){Text("−5")};OutlinedButton(onClick = { onDelta(5) }, modifier = Modifier.weight(1f).height(48.dp)){Text("+5")}}}}}

@Composable private fun ShopScreen(s:AppState,content:GameContent?,onBack:()->Unit,onPlan:()->Unit,onBuy:(io.github.chalexey.cashpet.core.model.ShopItem)->Unit){val items=content?.catalog?.items.orEmpty();LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{Header("Магазин","Сначала план, потом покупки",onBack);Text("Баланс: ${s.balance} 🪙",fontWeight=FontWeight.Bold)};items(items){item->Card{Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(item.name,fontWeight=FontWeight.Bold);Text("${item.price} 🪙")};Text(effectText(item.effects));if(!s.planConfirmed){OutlinedButton(onClick = onPlan, modifier = Modifier.fillMaxWidth().height(48.dp)){Text("Сначала план")}}else Button(onClick = { onBuy(item) }, modifier = Modifier.fillMaxWidth().height(48.dp), enabled = s.balance >= item.price){Text(if(s.balance>=item.price)"Купить" else "Не хватает монет")}}}}}}

@Composable private fun SavingsScreen(s:AppState,content:GameContent?,onBack:()->Unit,onPlan:()->Unit,onDeposit:(Int)->Unit,onWithdraw:(Int)->Unit){var withdraw by remember{mutableIntStateOf(0)};val goals=content?.catalog?.goals.orEmpty();val goal=goals.firstOrNull();if(withdraw>0)AlertDialog(onDismissRequest={withdraw=0},title={Text("Снять $withdraw монет?")},text={Text("Копилка: ${s.savings} → ${s.savings-withdraw}. Баланс: ${s.balance} → ${s.balance+withdraw}.")},confirmButton={TextButton(onClick = { onWithdraw(withdraw); withdraw = 0 }){Text("Снять")}},dismissButton={TextButton(onClick = { withdraw = 0 }){Text("Передумать")}});LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){item{Header("Копилка","Мечта растёт маленькими шагами",onBack)};item{Card(colors=CardDefaults.cardColors(containerColor=SaveViolet.copy(alpha=.10f))){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text("🎯 ${goal?.name?:"Домик"}",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text("${s.savings} из ${goal?.cost?:100}",style=MaterialTheme.typography.headlineMedium);LinearProgressIndicator(progress = { (s.savings / (goal?.cost ?: 100).toFloat()).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(12.dp))}}};item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()){for(a in listOf(5,10,20))Button(onClick = { if (!s.planConfirmed) onPlan() else onDeposit(a) }, modifier = Modifier.weight(1f).height(52.dp), enabled = s.planConfirmed && s.balance >= a){Text("+$a")}}};item{OutlinedButton(onClick = { if (s.savings >= 10) withdraw = 10 }, modifier = Modifier.fillMaxWidth().height(52.dp), enabled = s.savings >= 10){Text("Снять 10")}};if(!s.planConfirmed)item{Text("Пополнять копилку можно после подтверждения плана.")}}}

@Composable private fun TasksScreen(s:AppState,content:GameContent?,onBack:()->Unit,onOpen:(String)->Unit,onJob:()->Unit){val tasks=content?.tasks.orEmpty();LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{Header("Задания","За первое прохождение — фиксированная награда",onBack)};item{Card(colors=CardDefaults.cardColors(containerColor=WantBlue.copy(alpha=.09f))){Text("Выбор не меняет награду. После ответа покажем последствия и разбор.", modifier = Modifier.padding(14.dp))}};items(tasks){task->val done=task.id in s.completedTasks;Card(Modifier.fillMaxWidth().clickable(enabled=!done){onOpen(task.id)},colors=CardDefaults.cardColors(containerColor=if(done)NeedGreen.copy(alpha=.10f)else MaterialTheme.colorScheme.surface)){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(task.title,fontWeight=FontWeight.Bold);Text("${topicName(task)} · ${task.reward.coins} 🪙")};Text(if(done)"✓ Пройдено" else "Открыть")}}};item{OutlinedButton(onClick = onJob, modifier = Modifier.fillMaxWidth().height(52.dp)){Text("Подработка: помочь по дому · +10")}}}}

@Composable private fun TaskScreen(task:TaskDef?,completed:Boolean,onBack:()->Unit,onFinish:(String,Int)->Unit){var selected by rememberSaveable(task?.id){mutableStateOf<String?>(null)};var revealed by rememberSaveable(task?.id){mutableStateOf(false)};if(task==null){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("Задание не найдено")};return};val step=task.steps.firstOrNull();LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){item{Header(task.title,"Ситуация → выбор → последствия → разбор",onBack)};item{Card{Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(step?.situation?:task.intro.orEmpty(),fontWeight=FontWeight.SemiBold);Text("Ни один вариант не помечен как правильный.")}}};items(step?.options.orEmpty()){o->Card(Modifier.fillMaxWidth().clickable{selected=o.id;revealed=true},colors=CardDefaults.cardColors(containerColor=if(selected==o.id)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){Text(o.label,fontWeight=FontWeight.Bold);if(selected==o.id){Text(o.consequence.orEmpty());Text(o.feedback)}}}};item{Button(enabled = selected != null && !completed, onClick = { onFinish(task.id, task.reward.coins) }, modifier = Modifier.fillMaxWidth().height(54.dp)){Text(if(completed)"Уже пройдено" else "Завершить и получить ${task.reward.coins} монет")};if(completed)Text("Награда за это задание уже получена. Повторно монеты не начисляются.",color=MaterialTheme.colorScheme.onSurfaceVariant)}}}

@Composable private fun ProgressScreen(s:AppState,onBack:()->Unit,onStage:()->Unit){val next=when(s.stage){0->150;1->350;else->350};LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){item{Header("Прогресс","Рост котёнка не уменьшается",onBack)};item{Card{Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(stageName(s.stage),style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text("${s.gp} / $next GP");LinearProgressIndicator(progress = { (s.gp / next.toFloat()).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(12.dp));if(s.stage<2)Button(onClick = onStage, enabled = s.gp >= next, modifier = Modifier.fillMaxWidth().height(52.dp)){Text("Перейти на следующую стадию")}}}};item{SectionTitle("Что уже сделано")};item{Text("Заданий пройдено: ${s.completedTasks.size}")};item{Text("Баланс: ${s.balance} монет")};item{Text("Копилка: ${s.savings} монет")};item{SectionTitle("Словарь")};item{InfoCard("План","Намерение, как распределить монеты на неделю.",WantBlue)};item{InfoCard("Нужно","То, без чего питомцу не обойтись.",NeedGreen)};item{InfoCard("Отложить","Монеты на большую мечту.",SaveViolet)}}}

@Composable
private fun SettingsScreen(
    state: AppState,
    sound: SoundController,
    themeMode: ThemeMode,
    onTheme: (ThemeMode) -> Unit,
    onHelp: () -> Unit,
    onBack: () -> Unit,
    onAdult: () -> Unit,
    onLook: () -> Unit,
) {
    var enabled by remember { mutableStateOf(sound.enabled) }
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Настройки", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Всё можно изменить без потери прогресса", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                CircleButton("?", onHelp)
            }
            TextButton(onClick = onBack) { Text("← Назад") }
        }
        item {
            Text("Внешний вид", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeChoice("Система", themeMode == ThemeMode.SYSTEM, { onTheme(ThemeMode.SYSTEM) }, Modifier.weight(1f))
                ThemeChoice("Светлая", themeMode == ThemeMode.LIGHT, { onTheme(ThemeMode.LIGHT) }, Modifier.weight(1f))
                ThemeChoice("Тёмная", themeMode == ThemeMode.DARK, { onTheme(ThemeMode.DARK) }, Modifier.weight(1f))
            }
        }
        item { SettingRow("🔊", "Музыка и звуки", if (enabled) "Включены" else "Выключены") { enabled = !enabled; sound.enabled = enabled; if (enabled) sound.startMusic() else sound.stopMusic() } }
        item { SettingRow("🐱", "Изменить внешность", "Имя и прогресс сохранятся", onLook) }
        item { SettingRow("📖", "Обучение и подсказки", "Повторить объяснение механик", onHelp) }
        item { SettingRow("🛡", "Для взрослых", "Профиль, демо и сброс", onAdult) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Профиль", fontWeight = FontWeight.Bold)
                    Text(state.playerName.ifBlank { "Ребёнок" })
                    Text("Неделя ${state.week} · ${stageName(state.stage)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ThemeChoice(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) }, modifier = modifier)
}

@Composable private fun AdultScreen(s:AppState,onBack:()->Unit,onReset:()->Unit,onDemo:()->Unit){var reset by remember{mutableStateOf(false)};if(reset)AlertDialog(onDismissRequest={reset=false},title={Text("Сбросить профиль?")},text={Text("Игровой прогресс ребёнка будет удалён. Настройки приложения сохранятся.")},confirmButton={TextButton(onClick = { reset = false; onReset() }){Text("Сбросить")}},dismissButton={TextButton(onClick = { reset = false }){Text("Отмена")}});LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){item{Header("Для взрослых","Без оценок и сравнений",onBack)};item{MetricCard("📚","Задания","${s.completedTasks.size}", modifier = Modifier.fillMaxWidth())};item{MetricCard("🐾","Стадия",stageName(s.stage), modifier = Modifier.fillMaxWidth())};item{MetricCard("🎯","Копилка","${s.savings} монет", modifier = Modifier.fillMaxWidth())};item{Button(onClick = onDemo, modifier = Modifier.fillMaxWidth().height(54.dp)){Text("Запустить демо")};Text("Демо использует отдельный профиль и не меняет игру ребёнка.")};item{OutlinedButton(onClick = { reset = true }, modifier = Modifier.fillMaxWidth().height(52.dp)){Text("Сбросить профиль ребёнка")}}}}

@Composable private fun DemoScreen(s:AppState,onBack:()->Unit,onAdd:(Int)->Unit,onGp:(Int)->Unit,onStage:()->Unit,onReset:()->Unit){LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){item{Header("Демо-режим","Можно быстро показать разные состояния игры",onBack)};item{Card(colors=CardDefaults.cardColors(containerColor=WantBlue.copy(alpha=.10f))){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text("Тестовый баланс: ${s.balance} 🪙",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text("GP: ${s.gp} · стадия: ${stageName(s.stage)}")}}};item{Text("Монеты",fontWeight=FontWeight.Bold)};item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick = { onAdd(100) }, modifier = Modifier.weight(1f).height(52.dp)){Text("+100")};Button(onClick = { onAdd(500) }, modifier = Modifier.weight(1f).height(52.dp)){Text("+500")}}};item{Text("Прогресс кота",fontWeight=FontWeight.Bold)};item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick = { onGp(50) }, modifier = Modifier.weight(1f).height(52.dp)){Text("+50 GP")};Button(onClick = { onGp(150) }, modifier = Modifier.weight(1f).height(52.dp)){Text("+150 GP")}}};item{Button(onClick = onStage, modifier = Modifier.fillMaxWidth().height(52.dp)){Text("Следующая стадия")};OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth().height(52.dp)){Text("Сбросить демо")}}}}

@Composable private fun WeekScreen(s:AppState,onNext:()->Unit){LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){item{Text("Итог недели",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)};item{MetricCard("🪙","Заработано","За задания и подработки", modifier = Modifier.fillMaxWidth())};item{MetricCard("🍗","Нужно","План ${s.planNeed} · доступно ${s.balance}", modifier = Modifier.fillMaxWidth())};item{MetricCard("🎈","Хочу","План ${s.planWant}", modifier = Modifier.fillMaxWidth())};item{MetricCard("🐷","Копилка","${s.savings}", modifier = Modifier.fillMaxWidth())};item{Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(56.dp)){Text("Следующая неделя")}}}}

@Composable
private fun ChildHelpDialog(
    screen: Screen,
    page: Int,
    onNext: () -> Unit,
    onDismiss: () -> Unit,
) {
    val pages = when (screen) {
        Screen.HOME -> listOf(
            "Дом", "Здесь сразу видны котёнок, баланс, копилка и его состояние.",
            "План", "Сначала реши, сколько монет пойдёт на Нужно, Хочу и Отложить.",
            "Задания", "В заданиях нет ловушки: сначала выбираешь, потом видишь последствия и разбор."
        )
        Screen.SETTINGS -> listOf(
            "Настройки", "Здесь можно сменить тему и звук, не теряя игровой прогресс.",
            "Внешность", "Котёнка можно выбрать заново в любой момент.",
            "Подсказки", "Кнопка ? снова покажет объяснение основных механик."
        )
        Screen.PLAN -> listOf(
            "План", "Это твой план, а не строгий запрет.",
            "Нужно", "Сначала подумай о базовых расходах питомца.",
            "Отложить", "Часть монет можно сохранить для большой мечты."
        )
        Screen.SHOP -> listOf(
            "Магазин", "Покупки начинаются после подтверждения плана.",
            "Карточка товара", "Цена и влияние на питомца показаны прямо здесь.",
            "Не хватает", "Можно пройти задание, заработать или выбрать покупку позже."
        )
        Screen.SAVINGS -> listOf(
            "Копилка", "Это монеты, которые ждут большой мечты.",
            "Пополнение", "Клади монеты небольшими шагами +5, +10 или +20.",
            "Снятие", "Перед снятием всегда увидишь, как изменятся суммы."
        )
        Screen.TASKS -> listOf(
            "Задания", "Главная часть игры — учиться принимать финансовые решения.",
            "Выбор", "Ни один вариант заранее не отмечен как правильный.",
            "Разбор", "После выбора игра показывает последствия и объясняет их."
        )
        else -> listOf("Подсказка", "Разберись с экраном шаг за шагом. Можно попробовать снова.", "Совет", "Если что-то непонятно, нажми ? в настройках.", "Готово", "Теперь можно продолжать игру.")
    }
    val title = pages[page * 2]
    val text = pages[page * 2 + 1]
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("💡 $title") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(text); Text("${page + 1} / 3", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) } },
        confirmButton = { TextButton(onClick = onNext) { Text(if (page == 2) "Понятно" else "Дальше") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } },
    )
}

@Composable private fun BottomBar(screen:Screen,onNav:(Screen)->Unit){Row(Modifier.fillMaxWidth().navigationBarsPadding().background(MaterialTheme.colorScheme.surface),horizontalArrangement=Arrangement.SpaceEvenly){listOf(Screen.HOME to "⌂\nДом",Screen.PLAN to "☷\nПлан",Screen.SHOP to "▣\nМагазин",Screen.SAVINGS to "🐷\nКопилка",Screen.TASKS to "✦\nЗадания").forEach{(target,label)->TextButton(onClick={onNav(target)},modifier=Modifier.weight(1f).height(60.dp)){Text(label,textAlign=TextAlign.Center,color=if(screen==target)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)}}}}

@Composable private fun Header(title:String,subtitle:String,onBack:()->Unit){Column(verticalArrangement=Arrangement.spacedBy(4.dp)){TextButton(onClick = onBack) { Text("← Назад") };Text(title,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text(subtitle,style=MaterialTheme.typography.bodyMedium)}}
@Composable private fun MetricCard(icon:String,title:String,value:String,modifier:Modifier=Modifier){Card(modifier){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Text(icon,fontSize=25.sp);Spacer(Modifier.width(10.dp));Column{Text(title);Text(value,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}}}}
@Composable private fun InfoCard(title:String,text:String,accent:Color){Card(colors=CardDefaults.cardColors(containerColor=accent.copy(alpha=.10f))){Column(Modifier.padding(14.dp)){Text(title,fontWeight=FontWeight.Bold);Text(text)}}}
@Composable private fun CircleButton(text:String,onClick:()->Unit){Box(Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).clickable(onClick=onClick),contentAlignment=Alignment.Center){Text(text,fontSize=20.sp)}}
@Composable private fun SettingRow(icon:String,title:String,value:String,onClick:()->Unit){Card(Modifier.fillMaxWidth().clickable(onClick=onClick)){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Text(icon,fontSize=24.sp);Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(title,fontWeight=FontWeight.Bold);Text(value,style=MaterialTheme.typography.bodyMedium)};Text("›",fontSize=28.sp)}}}
@Composable private fun PetImage(kind:Int,coat:Int,stage:Int,modifier:Modifier){val k=listOf("fluffy","smooth","lop").getOrElse(kind){"fluffy"};val c=listOf("ginger","grey","black").getOrElse(coat){"ginger"};val res=when("${k}_${c}"){"fluffy_ginger"->R.drawable.kitten_fluffy_ginger;"fluffy_grey"->R.drawable.kitten_fluffy_grey;"fluffy_black"->R.drawable.kitten_fluffy_black;"smooth_ginger"->R.drawable.kitten_smooth_ginger;"smooth_grey"->R.drawable.kitten_smooth_grey;"smooth_black"->R.drawable.kitten_smooth_black;"lop_ginger"->R.drawable.kitten_lop_ginger;"lop_grey"->R.drawable.kitten_lop_grey;else->R.drawable.kitten_lop_black};Image(painterResource(res),contentDescription="Котёнок",modifier=modifier)}
private fun stageName(s:Int)=when(s){0->"Малыш";1->"Подросток";else->"Взрослый"}
private fun moodText(m:PetMood)=when(m){PetMood.HAPPY->"Радуется";PetMood.CALM->"Спокоен";PetMood.SAD->"Грустит"}
private fun petMood(s:AppState):PetMood{val v=.4f*s.mood+.3f*s.satiety+.3f*s.care;return when{v>=75->PetMood.HAPPY;v>=50->PetMood.CALM;else->PetMood.SAD}}
private fun effectText(e:Map<Stat,Int>)=buildString{val a=e[Stat.SATIETY]?:0;val b=e[Stat.CARE]?:0;val c=e[Stat.MOOD]?:0;if(a!=0)append("🍗 Сытость ${if(a>0)"+" else ""}$a  ");if(b!=0)append("🧼 Уход ${if(b>0)"+" else ""}$b  ");if(c!=0)append("😊 Настроение ${if(c>0)"+" else ""}$c")}.ifBlank{"Влияет на игровой выбор"}
private fun topicName(t:TaskDef)=when(t.topic.name.lowercase()){"purchases"->"Покупки";"budget"->"Бюджет";"savings"->"Сбережения";"intro"->"Первый день";else->"Навык"}
