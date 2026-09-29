package io.github.chalexey.cashpet.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.chalexey.cashpet.R
import io.github.chalexey.cashpet.app.vm.AdultGateViewModel
import io.github.chalexey.cashpet.app.vm.AdultNav
import io.github.chalexey.cashpet.app.vm.AdultUiState
import io.github.chalexey.cashpet.app.vm.AdultViewModel
import io.github.chalexey.cashpet.app.vm.BulletUi
import io.github.chalexey.cashpet.app.vm.ChoiceResultUi
import io.github.chalexey.cashpet.app.vm.CloseWeekWarning
import io.github.chalexey.cashpet.app.vm.DemoNav
import io.github.chalexey.cashpet.app.vm.DemoUiState
import io.github.chalexey.cashpet.app.vm.DemoViewModel
import io.github.chalexey.cashpet.app.vm.FeedbackUi
import io.github.chalexey.cashpet.app.vm.GoalUi
import io.github.chalexey.cashpet.app.vm.GrowthIconUi
import io.github.chalexey.cashpet.app.vm.HomeUiState
import io.github.chalexey.cashpet.app.vm.HomeViewModel
import io.github.chalexey.cashpet.app.vm.NeighborTipUi
import io.github.chalexey.cashpet.app.vm.OnboardingUiState
import io.github.chalexey.cashpet.app.vm.OnboardingViewModel
import io.github.chalexey.cashpet.app.vm.PlanUiState
import io.github.chalexey.cashpet.app.vm.PlanViewModel
import io.github.chalexey.cashpet.app.vm.ProgressUiState
import io.github.chalexey.cashpet.app.vm.ProgressViewModel
import io.github.chalexey.cashpet.app.vm.SavingsUiState
import io.github.chalexey.cashpet.app.vm.SavingsViewModel
import io.github.chalexey.cashpet.app.vm.SettingsUiState
import io.github.chalexey.cashpet.app.vm.SettingsViewModel
import io.github.chalexey.cashpet.app.vm.ShopItemUi
import io.github.chalexey.cashpet.app.vm.ShopUiState
import io.github.chalexey.cashpet.app.vm.ShopViewModel
import io.github.chalexey.cashpet.app.vm.StartUiState
import io.github.chalexey.cashpet.app.vm.StartViewModel
import io.github.chalexey.cashpet.app.vm.TaskCardUi
import io.github.chalexey.cashpet.app.vm.TaskPlayUiState
import io.github.chalexey.cashpet.app.vm.TaskPlayViewModel
import io.github.chalexey.cashpet.app.vm.TasksUiState
import io.github.chalexey.cashpet.app.vm.TasksViewModel
import io.github.chalexey.cashpet.app.vm.TopBarUi
import io.github.chalexey.cashpet.app.vm.WeekSummaryUiState
import io.github.chalexey.cashpet.app.vm.WeekSummaryViewModel
import io.github.chalexey.cashpet.app.vm.WithdrawPreviewUi
import io.github.chalexey.cashpet.content.ScreenKind
import io.github.chalexey.cashpet.core.engine.Rejection
import io.github.chalexey.cashpet.core.model.Difficulty
import io.github.chalexey.cashpet.core.model.EffectKey
import io.github.chalexey.cashpet.core.model.GrowthIcon
import io.github.chalexey.cashpet.core.model.Part
import io.github.chalexey.cashpet.core.model.PetLook
import io.github.chalexey.cashpet.core.model.PetMood
import io.github.chalexey.cashpet.core.model.Slot
import io.github.chalexey.cashpet.core.model.Stage
import io.github.chalexey.cashpet.core.model.Stat
import io.github.chalexey.cashpet.core.model.Topic

// ---------------------------------------------------------------------------------------------
// Палитра — по референсу «Пастельная презентация CashPet»: тёплый белый фон, глубокий синий текст,
// пастельные панели (голубая, персиковая, мятная, жёлтая), тонкие мягкие рамки.
// ---------------------------------------------------------------------------------------------
private val Ink = Color(0xFF173D6B)
private val InkSoft = Color(0xFF4F6682)
private val Muted = Color(0xFF6F8299)
private val Canvas = Color(0xFFFFFCF8)
private val Surf = Color(0xFFFFFFFF)
private val BluePanel = Color(0xFFE9F5FF)
private val BlueSoft = Color(0xFFDDEEFF)
private val Blue = Color(0xFF5B9DEF)
private val BlueDark = Color(0xFF2F72C8)
private val Peach = Color(0xFFFFEBDD)
private val Mint = Color(0xFFE5F6EE)
private val Yellow = Color(0xFFFFF2D8)
private val Rose = Color(0xFFFFE6EC)
private val Border = Color(0xFFE6EAEF)
private val SoftGray = Color(0xFFF3F5F7)
private val White = Color(0xFFFFFFFF)
private val Gold = Color(0xFFF2A81D)
private val Green = Color(0xFF4DB58A)
private val Pink = Color(0xFFF08FA5)
private val Orange = Color(0xFFF4B55D)

/** Настройка «Анимации» из экрана настроек: выключена — коты и искры стоят на месте. */
private val LocalAnimations = staticCompositionLocalOf { true }

private object Routes {
    const val START = "start"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val PLAN = "plan"
    const val TASKS = "tasks"
    const val TASK = "task"
    const val SHOP = "shop"
    const val SAVINGS = "savings"
    const val SUMMARY = "summary"
    const val PROGRESS = "progress"
    const val SETTINGS = "settings"
    const val ADULT_GATE = "adult_gate"
    const val ADULT = "adult"
    const val DEMO = "demo"
}

/**
 * Переход между разделами. Home — стабильный корень игровой части стека.
 * Сначала возвращаемся к уже существующему Home через popBackStack(),
 * и только потом открываем нужную вкладку. restoreState/saveState здесь
 * намеренно не используются: после закрытия недели они могли возвращать
 * старое состояние маршрута и визуально блокировать переход на Дом.
 */
private fun NavHostController.goTab(route: String) {
    if (currentDestination?.route == route) return

    if (route == Routes.HOME) {
        if (!popBackStack(Routes.HOME, inclusive = false)) {
            // Fallback для редкого случая, когда Home отсутствует в стеке.
            navigate(Routes.HOME) { launchSingleTop = true }
        }
        return
    }

    // Для любой вкладки сначала удаляем промежуточные экраны (Итоги, настройки и т.п.)
    // и возвращаемся к живому Home. В обычном игровом flow Home всегда существует.
    popBackStack(Routes.HOME, inclusive = false)
    if (currentDestination?.route != Routes.HOME) {
        navigate(Routes.HOME) { launchSingleTop = true }
    }
    if (currentDestination?.route != route) {
        navigate(route) { launchSingleTop = true }
    }
}

/** Root Compose entry point used by MainActivity. */
@Composable
fun CashPetApp() {
    CashPetTheme {
        val settingsVm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
        val settings by settingsVm.uiState.collectAsStateWithLifecycle()
        val nav = rememberNavController()
        CompositionLocalProvider(LocalAnimations provides settings.animations) {
            NavHost(navController = nav, startDestination = Routes.START) {
                composable(Routes.START) { StartRoute(nav) }
                composable(
                    route = "${Routes.ONBOARDING}/{slot}",
                    arguments = listOf(navArgument("slot") { type = NavType.StringType })
                ) { entry ->
                    val slot = runCatching {
                        Slot.valueOf(entry.arguments?.getString("slot") ?: Slot.CHILD.name)
                    }.getOrDefault(Slot.CHILD)
                    OnboardingRoute(nav, slot)
                }
                composable(Routes.HOME) { HomeRoute(nav) }
                composable(Routes.PLAN) { PlanRoute(nav) }
                composable(Routes.TASKS) { TasksRoute(nav) }
                composable(
                    route = "${Routes.TASK}/{taskId}",
                    arguments = listOf(navArgument("taskId") { type = NavType.StringType })
                ) { entry ->
                    TaskRoute(nav, entry.arguments?.getString("taskId").orEmpty())
                }
                composable(Routes.SHOP) { ShopRoute(nav) }
                composable(Routes.SAVINGS) { SavingsRoute(nav) }
                composable(Routes.SUMMARY) { SummaryRoute(nav) }
                composable(Routes.PROGRESS) { ProgressRoute(nav) }
                composable(Routes.SETTINGS) { SettingsRoute(nav) }
                composable(Routes.ADULT_GATE) { AdultGateRoute(nav) }
                composable(Routes.ADULT) { AdultRoute(nav) }
                composable(Routes.DEMO) { DemoRoute(nav) }
            }
        }
    }
}

@Composable
private fun CashPetTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.lightColorScheme(
            primary = Blue,
            onPrimary = White,
            primaryContainer = BluePanel,
            onPrimaryContainer = Ink,
            background = Canvas,
            surface = Surf,
            onSurface = Ink,
            onBackground = Ink,
            secondary = Green,
            secondaryContainer = Mint,
            onSecondaryContainer = Ink,
            outline = Border,
        ),
        typography = MaterialTheme.typography.copy(
            headlineLarge = MaterialTheme.typography.headlineLarge.copy(fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Ink),
            headlineMedium = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Ink),
            titleLarge = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink),
            titleMedium = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink),
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 23.sp, color = Ink),
            bodyMedium = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp, color = InkSoft),
        ),
        content = content,
    )
}

// ---------------------------------------------------------------------------------------------
// Маршруты: связывают ViewModel и экраны. Логики здесь нет.
// ---------------------------------------------------------------------------------------------

@Composable
private fun StartRoute(nav: NavHostController) {
    val vm: StartViewModel = viewModel(factory = StartViewModel.Factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    StartScreen(
        state = state,
        onPlay = { if (state.hasProfile) nav.goTab(Routes.HOME) else nav.navigate("${Routes.ONBOARDING}/${if (state.demo) Slot.DEMO.name else Slot.CHILD.name}") },
        onAdult = { nav.navigate(Routes.ADULT_GATE) },
    )
}

@Composable
private fun OnboardingRoute(nav: NavHostController, slot: Slot) {
    val vm: OnboardingViewModel = viewModel(factory = OnboardingViewModel.factory(slot))
    val state by vm.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.finished) {
        // Профиль создан: сначала заменяем весь стек на Дом (Старт и Онбординг исчезают из «назад»),
        // потом поверх — первое задание. «Готово» на задании (popBackStack) вернёт на Дом, а не на Старт —
        // раньше это было не так, и после первого задания игрока выкидывало на стартовый экран.
        if (state.finished) {
            nav.navigate(Routes.HOME) { popUpTo(Routes.START) { inclusive = true } }
            nav.navigate("${Routes.TASK}/${state.firstTaskId ?: "0"}")
        }
    }
    OnboardingScreen(state, vm::onNameChange, vm::onLookChange, vm::onDifficultyChoose, vm::next, vm::skip, vm::back)
}

@Composable
private fun HomeRoute(nav: NavHostController) {
    val vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
    val state by vm.uiState.collectAsStateWithLifecycle()

    // Закрытие недели — одноразовое событие. Home остаётся в стеке,
    // поэтому после итогов можно безопасно вернуться на него.
    LaunchedEffect(vm) {
        vm.events.collect { event ->
            when (event) {
                HomeViewModel.HomeEvent.WeekClosed -> {
                    nav.navigate(Routes.SUMMARY) { launchSingleTop = true }
                }
            }
        }
    }

    // null означает только загрузку GameStore. Не отправляем пользователя на Start:
    // после сохранения новой недели Store может на очень короткое время ещё не успеть
    // отдать обновлённое состояние UI.
    state?.let { HomeScreen(it, nav, vm::closeWeek) } ?: LoadingScreen()
}

@Composable
private fun LoadingScreen() {
    Surface(Modifier.fillMaxSize(), color = Canvas) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            PetArt(DefaultLook, Stage.BABY, Modifier.size(96.dp))
        }
    }
}

@Composable
private fun PlanRoute(nav: NavHostController) {
    val vm: PlanViewModel = viewModel(factory = PlanViewModel.Factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    var confirm by remember { mutableStateOf(false) }
    state?.let { PlanScreen(it, nav, vm::increase, vm::decrease, confirm = { confirm = true }) }
    if (confirm) {
        AppDialog(
            title = "Подтвердить план?",
            onDismiss = { confirm = false },
            confirm = { PrimaryButton("Подтвердить", { confirm = false; vm.confirm() }, compact = true) },
            dismiss = { TextButton(onClick = { confirm = false }) { Text("Ещё подумаю", color = Muted) } },
        ) { Text("Потом план уже не изменить, но тратить монеты можно по-своему.") }
    }
}

@Composable
private fun ShopRoute(nav: NavHostController) {
    val vm: ShopViewModel = viewModel(factory = ShopViewModel.Factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    state?.let { ShopScreen(it, nav, vm::buy, vm::addToWishlist, vm::removeFromWishlist, vm::buyWithSavings, vm::onMessageShown) }
}

@Composable
private fun SavingsRoute(nav: NavHostController) {
    val vm: SavingsViewModel = viewModel(factory = SavingsViewModel.Factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    var withdraw by remember { mutableStateOf<WithdrawPreviewUi?>(null) }
    state?.let { SavingsScreen(it, nav, vm::chooseGoal, vm::deposit, { amount -> withdraw = vm.previewWithdraw(amount) }, vm::withdraw, vm::buyGoal, vm::onMessageShown, withdraw) { withdraw = null } }
}

@Composable
private fun TasksRoute(nav: NavHostController) {
    val vm: TasksViewModel = viewModel(factory = TasksViewModel.Factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    state?.let { TasksScreen(it, nav, vm::doJob, vm::onMessageShown) }
}

@Composable
private fun TaskRoute(nav: NavHostController, taskId: String) {
    val vm: TaskPlayViewModel = viewModel(factory = TaskPlayViewModel.factory(taskId))
    val state by vm.uiState.collectAsStateWithLifecycle()
    state?.let { TaskScreen(it, vm::choose, vm::onFollowup, vm::retry, vm::next, onDone = { nav.popBackStack() }, onBack = { nav.popBackStack() }) }
}

@Composable
private fun SummaryRoute(nav: NavHostController) {
    val vm: WeekSummaryViewModel = viewModel(factory = WeekSummaryViewModel.Factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    val home = currentPet()
    state?.let { SummaryScreen(it, nav, home?.look ?: DefaultLook, home?.stage ?: Stage.BABY) }
}

@Composable
private fun ProgressRoute(nav: NavHostController) {
    val vm: ProgressViewModel = viewModel(factory = ProgressViewModel.Factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    val home = currentPet()
    state?.let { ProgressScreen(it, nav, home?.look ?: DefaultLook) }
}

@Composable
private fun SettingsRoute(nav: NavHostController) {
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    SettingsScreen(state, nav, vm::setSound, vm::setAnimations)
}

@Composable
private fun AdultGateRoute(nav: NavHostController) {
    val vm: AdultGateViewModel = viewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.passed) { if (state.passed) nav.navigate(Routes.ADULT) { popUpTo(Routes.ADULT_GATE) { inclusive = true } } }
    // Раньше сюда не передавались state.a и state.b — сам пример нигде не рисовался, и барьер
    // выглядел как пустая форма без вопроса. Теперь пример «a × b» показан явно.
    AdultGateScreen(state.a, state.b, state.input, if (state.wrongAttempt) "Неверно. Попробуй новый пример." else null, vm::onInputChange, vm::submit, back = { nav.popBackStack() })
}

@Composable
private fun AdultRoute(nav: NavHostController) {
    val vm: AdultViewModel = viewModel(factory = AdultViewModel.Factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    val demoVm: DemoViewModel = viewModel(factory = DemoViewModel.Factory)
    val demoState by demoVm.uiState.collectAsStateWithLifecycle()
    val home = currentPet()
    LaunchedEffect(state.navigate) {
        when (state.navigate) {
            AdultNav.ONBOARDING -> { vm.onNavigated(); nav.navigate("${Routes.ONBOARDING}/${if (state.demo) Slot.DEMO.name else Slot.CHILD.name}") }
            AdultNav.START -> { vm.onNavigated(); nav.navigate(Routes.START) { popUpTo(Routes.ADULT) { inclusive = true } } }
            null -> Unit
        }
    }
    AdultScreen(state, nav, home?.look ?: DefaultLook, onReset = vm::resetProfile, onDelete = vm::deleteProfile, onDemo = { demoVm.start() })
    LaunchedEffect(demoState.navigate) {
        when (demoState.navigate) {
            DemoNav.HOME -> { demoVm.onNavigated(); nav.navigate(Routes.HOME) }
            DemoNav.ONBOARDING -> { demoVm.onNavigated(); nav.navigate("${Routes.ONBOARDING}/${Slot.DEMO.name}") }
            DemoNav.START -> { demoVm.onNavigated(); nav.navigate(Routes.START) }
            null -> Unit
        }
    }
}

@Composable
private fun DemoRoute(nav: NavHostController) {
    val vm: DemoViewModel = viewModel(factory = DemoViewModel.Factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.navigate) {
        when (state.navigate) {
            DemoNav.HOME -> { vm.onNavigated(); nav.navigate(Routes.HOME) }
            DemoNav.ONBOARDING -> { vm.onNavigated(); nav.navigate("${Routes.ONBOARDING}/${Slot.DEMO.name}") }
            DemoNav.START -> { vm.onNavigated(); nav.navigate(Routes.START) }
            null -> Unit
        }
    }
    DemoScreen(state, vm::start, vm::speedUp, vm::reset, vm::exit, vm::onWeeksShown)
}

private val DefaultLook = PetLook("fluffy", "grey")

/** Текущий кот игрока (для картинок на экранах, где в состоянии кота нет). null — профиля нет. */
@Composable
private fun currentPet(): HomeUiState? {
    val vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    return state
}

// ---------------------------------------------------------------------------------------------
// Старт и онбординг
// ---------------------------------------------------------------------------------------------

@Composable
private fun StartScreen(state: StartUiState, onPlay: () -> Unit, onAdult: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = Canvas) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(12.dp))
            Icon(painterResource(R.drawable.ic_goal), null, tint = Blue, modifier = Modifier.size(34.dp))
            Text("CashPet", style = MaterialTheme.typography.headlineLarge.copy(fontSize = 44.sp), color = Ink, modifier = Modifier.padding(top = 2.dp))
            Text("Твои цели — наши мечты!", color = InkSoft, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(30.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFFDCEEFF), Color(0xFFF5FAFF), Color(0xFFFFF7EC)))),
                contentAlignment = Alignment.Center,
            ) {
                Clouds()
                PetArt(PetLook("smooth", "grey"), Stage.ADULT, Modifier.fillMaxWidth(0.82f).heightIn(max = 340.dp))
                Sparkle(Modifier.align(Alignment.TopStart).padding(start = 36.dp, top = 44.dp), 26.sp)
                Sparkle(Modifier.align(Alignment.CenterEnd).padding(end = 30.dp), 20.sp, delayMs = 500)
                Sparkle(Modifier.align(Alignment.BottomStart).padding(start = 40.dp, bottom = 60.dp), 16.sp, delayMs = 900)
            }
            Spacer(Modifier.height(18.dp))
            PrimaryButton("Начать", onPlay, enabled = !state.loading)
            TextButton(onClick = onAdult, modifier = Modifier.heightIn(min = 48.dp)) { Text("Для взрослых", color = Muted) }
        }
    }
}

@Composable
private fun OnboardingScreen(
    state: OnboardingUiState,
    onName: (String) -> Unit,
    onLook: (PetLook) -> Unit,
    onDifficulty: (Difficulty) -> Unit,
    next: () -> Unit,
    skip: () -> Unit,
    back: () -> Unit,
) {
    Surface(Modifier.fillMaxSize(), color = Canvas) {
        Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.canGoBack) BackButton(back) else Spacer(Modifier.size(48.dp))
                Spacer(Modifier.weight(1f))
                Text("${state.step} / ${state.steps}", color = Muted, fontWeight = FontWeight.Bold)
            }
            SoftProgress(state.step.toFloat() / state.steps, Blue, BlueSoft, 8.dp)
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier.fillMaxWidth().height(if (state.kind == ScreenKind.LOOK) 190.dp else 210.dp).clip(RoundedCornerShape(28.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFFDCEEFF), Color(0xFFF2F8FF)))),
                contentAlignment = Alignment.Center,
            ) {
                Clouds()
                PetArt(state.look, Stage.BABY, Modifier.size(if (state.kind == ScreenKind.LOOK) 170.dp else 180.dp))
                Sparkle(Modifier.align(Alignment.TopEnd).padding(24.dp), 20.sp)
            }
            Text(state.text, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 14.dp))
            if (state.bullets.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(9.dp)) { state.bullets.forEach { BulletCard(it) } }
            if (state.kind == ScreenKind.LOOK) LookPicker(state, onLook)
            state.input?.let { input ->
                OutlinedTextField(
                    value = input.value,
                    onValueChange = onName,
                    label = { Text("Имя") },
                    placeholder = { Text(input.placeholder) },
                    supportingText = { Text(input.error ?: input.hint.orEmpty()) },
                    isError = input.error != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    shape = RoundedCornerShape(18.dp),
                )
            }
            if (state.kind == ScreenKind.DIFFICULTY) {
                Spacer(Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.choices.forEach { choice -> ChoiceCard(choice.label, choice.selected) { onDifficulty(choice.difficulty) } }
                }
            }
            state.petReply?.let { Callout(it, BluePanel) }
            state.finePrint?.let { Text(it, color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 10.dp)) }
            Spacer(Modifier.height(16.dp))
            state.button?.let { PrimaryButton(it, next, enabled = !state.creating) }
            if (state.canSkip) TextButton(onClick = skip, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Пропустить", color = Muted) }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun LookPicker(state: OnboardingUiState, onLook: (PetLook) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Форма", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            state.breeds.forEach { breed ->
                val selected = state.look.breedId == breed.id
                Column(
                    Modifier.clip(RoundedCornerShape(18.dp)).clickable { onLook(state.look.copy(breedId = breed.id)) }.padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.size(84.dp).clip(CircleShape).background(if (selected) BlueSoft else SoftGray)
                            .border(if (selected) 3.dp else 1.dp, if (selected) Blue else Border, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { PetArt(PetLook(breed.id, state.look.colorId), Stage.BABY, Modifier.size(70.dp), animated = false) }
                    Text(breed.name, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = if (selected) BlueDark else Muted, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        Text("Окрас", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            state.colors.forEach { color ->
                val selected = state.look.colorId == color.id
                Column(
                    Modifier.clip(RoundedCornerShape(18.dp)).clickable { onLook(state.look.copy(colorId = color.id)) }.padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.size(52.dp).clip(CircleShape).background(colorDot(color.id))
                            .border(if (selected) 3.dp else 1.dp, if (selected) Blue else Border, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { if (selected) Text("✓", color = White, fontWeight = FontWeight.Bold, fontSize = 20.sp) }
                    Text(color.name, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = if (selected) BlueDark else Muted, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

private fun colorDot(id: String): Color = when (id) {
    "ginger" -> Color(0xFFF4A259)
    "black" -> Color(0xFF3A3F52)
    else -> Color(0xFF9AA4B5)
}

// ---------------------------------------------------------------------------------------------
// Дом
// ---------------------------------------------------------------------------------------------

@Composable
private fun HomeScreen(state: HomeUiState, nav: NavHostController, closeWeek: () -> Unit) {
    var petOpen by remember { mutableStateOf(false) }
    var closeDialog by remember { mutableStateOf(false) }
    AppScaffold(nav, Routes.HOME, "Дом", state.top) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.needWarning) Callout("Нужные покупки для котика ещё не закрыты", Peach)
            RoomScene(Modifier.fillMaxWidth().height(250.dp).clickable { petOpen = true }) {
                Text("Неделя ${state.weekNumber}", color = BlueDark, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.TopEnd).padding(14.dp).clip(RoundedCornerShape(50.dp)).background(White.copy(alpha = 0.85f)).padding(horizontal = 12.dp, vertical = 5.dp))
                PetArt(state.look, state.stage, Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp).size(190.dp))
                Sparkle(Modifier.align(Alignment.TopStart).padding(start = 130.dp, top = 30.dp), 20.sp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text(displayName(state.petName), style = MaterialTheme.typography.headlineMedium)
                Text("  ·  ${moodLabel(state.mood)}", color = Muted, fontWeight = FontWeight.SemiBold)
            }
            SoftCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatRow("❤", "Настроение", state.stats.mood, Pink)
                    StatRow("🍗", "Сытость", state.stats.satiety, Orange)
                    StatRow("🧼", "Уход", state.stats.care, Blue)
                }
            }
            GoalStrip(state.top) { nav.goTab(Routes.SAVINGS) }
            state.activeTask?.let { TaskMiniCard(it) { nav.navigate("${Routes.TASK}/${it.taskId}") } }
            SoftCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    SectionTitle("Сегодня")
                    Text("Составь план, реши задание и позаботься о своём котике", color = Muted, modifier = Modifier.padding(top = 3.dp))
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallAction("План", Routes.PLAN, nav)
                        SmallAction("Задания", Routes.TASKS, nav)
                        SmallAction("Мечта", Routes.SAVINGS, nav)
                    }
                }
            }
            PrimaryButton("Завершить неделю", { closeDialog = true })
            Spacer(Modifier.height(4.dp))
        }
    }
    if (petOpen) {
        AppDialog(
            title = displayName(state.petName),
            onDismiss = { petOpen = false },
            confirm = { PrimaryButton("Понятно", { petOpen = false }, compact = true) },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(state.petReasonText)
                Text("🍗 Сытость ${state.stats.satiety}", fontWeight = FontWeight.SemiBold)
                Text("🧼 Уход ${state.stats.care}", fontWeight = FontWeight.SemiBold)
                Text("😊 Настроение ${state.stats.mood}", fontWeight = FontWeight.SemiBold)
            }
        }
    }
    if (closeDialog) {
        val warning = state.closeWarning
        AppDialog(
            title = "Закончить неделю?",
            onDismiss = { closeDialog = false },
            confirm = { PrimaryButton(if (warning == CloseWeekWarning.NO_PLAN) "Завершить так" else "Завершить", { closeDialog = false; closeWeek() }, compact = true) },
            dismiss = {
                TextButton(onClick = { closeDialog = false; if (warning == CloseWeekWarning.NO_PLAN) nav.goTab(Routes.PLAN) }) {
                    Text(if (warning == CloseWeekWarning.NO_PLAN) "К плану" else "Отмена", color = BlueDark)
                }
            },
        ) {
            Text(
                when (warning) {
                    CloseWeekWarning.NO_PLAN -> "Плана на эту неделю нет. Можно сначала составить его или завершить так."
                    CloseWeekWarning.NEED_NOT_BOUGHT -> "У ${displayName(state.petName)} пока нет нужной покупки. Всё равно завершить?"
                    null -> "Все действия за неделю будут подведены в итог."
                }
            )
        }
    }
}

/** Комната кота: стена, окно, растение, коврик и миска — рисуется кодом в пастельных цветах. */
@Composable
private fun RoomScene(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.clip(RoundedCornerShape(28.dp)).border(1.dp, Border, RoundedCornerShape(28.dp))) {
        Canvas(Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            drawRect(Brush.verticalGradient(listOf(Color(0xFFE6F3FF), Color(0xFFFFF5E8))))
            drawRect(Color(0xFFF6E6D0), topLeft = Offset(0f, h * 0.74f), size = Size(w, h * 0.26f))
            drawRect(Color(0xFFEBD5B8), topLeft = Offset(0f, h * 0.74f), size = Size(w, 3.dp.toPx()))
            // окно
            drawRoundRect(White, Offset(w * 0.07f, h * 0.12f), Size(w * 0.22f, h * 0.36f), CornerRadius(14.dp.toPx()))
            drawRoundRect(Color(0xFFCFE8FF), Offset(w * 0.09f, h * 0.15f), Size(w * 0.18f, h * 0.30f), CornerRadius(10.dp.toPx()))
            drawRect(White, Offset(w * 0.18f - 1.5.dp.toPx(), h * 0.15f), Size(3.dp.toPx(), h * 0.30f))
            // растение слева
            val potX = w * 0.10f
            drawRoundRect(Color(0xFFE9A97C), Offset(potX, h * 0.64f), Size(w * 0.10f, h * 0.14f), CornerRadius(8.dp.toPx()))
            drawOval(Color(0xFF8FCB9F), Offset(potX - w * 0.02f, h * 0.44f), Size(w * 0.07f, h * 0.22f))
            drawOval(Color(0xFF6DB884), Offset(potX + w * 0.03f, h * 0.38f), Size(w * 0.07f, h * 0.28f))
            drawOval(Color(0xFF8FCB9F), Offset(potX + w * 0.08f, h * 0.46f), Size(w * 0.07f, h * 0.20f))
            // коврик и миска
            drawOval(Color(0xFFD9E9FA), Offset(w * 0.22f, h * 0.80f), Size(w * 0.56f, h * 0.13f))
            drawRoundRect(Color(0xFF7FB2F0), Offset(w * 0.76f, h * 0.73f), Size(w * 0.14f, h * 0.07f), CornerRadius(10.dp.toPx()))
            drawRoundRect(Color(0xFFFFC9A8), Offset(w * 0.78f, h * 0.715f), Size(w * 0.10f, h * 0.02f), CornerRadius(6.dp.toPx()))
            // полка справа
            drawRoundRect(Color(0xFFEBD5B8), Offset(w * 0.70f, h * 0.30f), Size(w * 0.24f, 5.dp.toPx()), CornerRadius(3.dp.toPx()))
            drawRoundRect(Color(0xFFFFD9A0), Offset(w * 0.74f, h * 0.22f), Size(w * 0.06f, h * 0.08f), CornerRadius(6.dp.toPx()))
            drawRoundRect(Color(0xFFBFDDF9), Offset(w * 0.83f, h * 0.19f), Size(w * 0.05f, h * 0.11f), CornerRadius(6.dp.toPx()))
        }
        content()
    }
}

@Composable
private fun GoalStrip(top: TopBarUi, onClick: () -> Unit) {
    SoftCard(Modifier.fillMaxWidth(), color = Peach, onClick = onClick) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Текущая цель", color = InkSoft, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                if (top.goalCost != null) Text("${top.goalProgressPct}%", fontWeight = FontWeight.Bold, color = BlueDark)
            }
            Text(top.goalName?.let { displayName(it) } ?: "Выбери мечту", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 2.dp))
            if (top.goalCost != null) SoftProgress(top.goalProgressPct / 100f, Green, White, 9.dp, Modifier.padding(top = 8.dp))
        }
    }
}

// ---------------------------------------------------------------------------------------------
// План
// ---------------------------------------------------------------------------------------------

@Composable
private fun PlanScreen(state: PlanUiState, nav: NavHostController, inc: (Part) -> Unit, dec: (Part) -> Unit, confirm: () -> Unit) {
    AppScaffold(nav, Routes.PLAN, "План", state.top) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GoalStrip(state.top) { nav.goTab(Routes.SAVINGS) }
            BalanceHero(state.available, state.unallocated)
            if (state.needHint != null) Callout("Коту на неделю нужно: корм 30 + шампунь 20 = ${state.needHint}", Mint)
            PlanPartCard("🍗 Нужно", "То, без чего не обойтись", state.need, Part.NEED, state.canIncrease, state.confirmed, inc, dec)
            PlanPartCard("🎈 Хочу", "Приятные вещи и развлечения", state.want, Part.WANT, state.canIncrease, state.confirmed, inc, dec)
            PlanPartCard("🐷 Отложить", "На большую мечту", state.save, Part.SAVE, state.canIncrease, state.confirmed, inc, dec)
            if (!state.confirmed && !state.canIncrease) Text("Все доступные монеты уже разложены — «+» отдыхает.", color = Muted)
            Text("Не разложено: ${state.unallocated}", color = if (state.unallocated == 0) BlueDark else InkSoft, fontWeight = FontWeight.Bold)
            if (!state.confirmed) {
                if (state.neighborTips.isNotEmpty()) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { state.neighborTips.forEach { NeighborChip(it) } }
                }
                PrimaryButton("Подтвердить план", confirm)
            } else {
                state.fact?.let { fact ->
                    SoftCard(Modifier.fillMaxWidth(), color = BluePanel) {
                        Column(Modifier.padding(16.dp)) {
                            SectionTitle("План и факт")
                            FactLine("🍗 Нужно", state.need, fact.spentNeed)
                            FactLine("🎈 Хочу", state.want, fact.spentWant)
                            FactLine("🐷 Отложить", state.save, fact.saved)
                            Text("Заработано на неделе: ${fact.earned} — в план не входит", color = Muted, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Магазин
// ---------------------------------------------------------------------------------------------

@Composable
private fun ShopScreen(
    state: ShopUiState,
    nav: NavHostController,
    buy: (String) -> Unit,
    add: (String) -> Unit,
    remove: (String) -> Unit,
    buySavings: (String) -> Unit,
    shown: () -> Unit,
) {
    var pending by remember { mutableStateOf<ShopItemUi?>(null) }
    var lastTried by remember { mutableStateOf<ShopItemUi?>(null) }
    val onBuy: (ShopItemUi) -> Unit = { item ->
        if (state.planConfirmed) pending = item else nav.goTab(Routes.PLAN)
    }
    AppScaffold(nav, Routes.SHOP, "Магазин", state.top) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PillTabs(listOf("Товары", "Мечты"), 0) { if (it == 1) nav.goTab(Routes.SAVINGS) }
            ShopSection("Нужное", state.need, state.planLeftNeed, state.planConfirmed, onBuy, add)
            ShopSection("Желаемое", state.want, state.planLeftWant, state.planConfirmed, onBuy, add)
            if (state.wishlist.isNotEmpty()) {
                SectionTitle("Хочу потом")
                state.wishlist.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { item ->
                            Box(Modifier.weight(1f)) {
                                ItemCard(item, null, state.planConfirmed, onBuy = onBuy, onWish = null, onRemove = { remove(item.id) })
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
    pending?.let { item ->
        AppDialog(
            title = "Купить ${item.name}?",
            onDismiss = { pending = null },
            confirm = { PrimaryButton("Купить за ${item.price}", { lastTried = item; pending = null; buy(item.id) }, compact = true) },
            dismiss = { TextButton(onClick = { pending = null }) { Text("Отмена", color = Muted) } },
        ) { Text("Отмены и возврата нет. Если передумаешь — есть «Хочу потом».") }
    }
    state.feedback?.let { FeedbackDialog(it, shown) }
    state.rejection?.let { r ->
        val item = lastTried
        val needMissing = (r as? Rejection.NotEnoughMoney)?.missing
        AppDialog(
            title = "Не получилось",
            onDismiss = shown,
            confirm = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    if (r is Rejection.PlanNotConfirmed) PrimaryButton("К плану", { shown(); nav.goTab(Routes.PLAN) }, compact = true)
                    if (needMissing != null && item != null && state.top.savings >= needMissing) {
                        PrimaryButton("Взять $needMissing из копилки", { shown(); buySavings(item.id) }, compact = true)
                    }
                    if (needMissing != null && item != null) {
                        SecondaryButton("Хочу потом", { shown(); add(item.id) })
                    }
                    if (r !is Rejection.PlanNotConfirmed) SecondaryButton("Понятно", shown)
                }
            },
        ) { Text(rejectionText(r)) }
    }
}

@Composable
private fun ShopSection(
    title: String,
    items: List<ShopItemUi>,
    left: Int?,
    confirmed: Boolean,
    onBuy: (ShopItemUi) -> Unit,
    onWish: (String) -> Unit,
) {
    SectionTitle(title)
    items.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { item -> Box(Modifier.weight(1f)) { ItemCard(item, left, confirmed, onBuy, onWish = { onWish(item.id) }, onRemove = null) } }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun ItemCard(
    item: ShopItemUi,
    planLeft: Int?,
    confirmed: Boolean,
    onBuy: (ShopItemUi) -> Unit,
    onWish: (() -> Unit)?,
    onRemove: (() -> Unit)?,
) {
    SoftCard(Modifier.fillMaxWidth(), radius = 22.dp) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth().height(86.dp), contentAlignment = Alignment.Center) { ProductPlaceholder(item) }
            Text(item.name, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
            CoinAmount(item.price, Modifier.padding(top = 2.dp))
            if (item.effects.isNotEmpty()) {
                StatEffectRow(item.effects, Modifier.padding(top = 4.dp))
            }
            if (planLeft != null) Text("По плану: $planLeft", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            Spacer(Modifier.height(8.dp))
            // Кнопка всегда активна: если монет мало, движок объяснит и предложит варианты (docs/01, 4.3)
            SmallButton(if (confirmed) "Купить" else "Сначала план", { onBuy(item) })
            if (onWish != null) TextButton(onClick = onWish, modifier = Modifier.heightIn(min = 48.dp)) { Text("Хочу потом", fontSize = 13.sp, color = BlueDark) }
            if (onRemove != null) TextButton(onClick = onRemove, modifier = Modifier.heightIn(min = 48.dp)) { Text("Убрать", fontSize = 13.sp, color = Muted) }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Копилка
// ---------------------------------------------------------------------------------------------

@Composable
private fun SavingsScreen(
    state: SavingsUiState,
    nav: NavHostController,
    choose: (String) -> Unit,
    deposit: (Int) -> Unit,
    preview: (Int) -> Unit,
    withdraw: (Int) -> Unit,
    buyGoal: () -> Unit,
    shown: () -> Unit,
    withdrawal: WithdrawPreviewUi?,
    clearWithdrawal: () -> Unit,
) {
    AppScaffold(nav, Routes.SAVINGS, "Копилка", state.top) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SoftCard(Modifier.fillMaxWidth(), color = Peach, radius = 26.dp) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(72.dp).clip(CircleShape).background(White), contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.ic_savings), "Копилка", tint = Color.Unspecified, modifier = Modifier.size(46.dp))
                    }
                    Column(Modifier.padding(start = 14.dp).weight(1f)) {
                        CoinAmount(state.total, big = true)
                        Text(if (state.goal == null) "Выбери мечту или копи про запас" else "Цель: ${state.goal.name}", color = InkSoft, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
            if (state.goal != null) GoalCard(state.goal, state.weeksLeft, state.canBuyGoal, buyGoal)
            SectionTitle("Мечты")
            state.goals.forEach { goal -> GoalOption(goal, state.goal?.id == goal.id, choose) }
            SectionTitle("Пополнить")
            if (!state.planConfirmed) Text("Сначала подтверди план — кнопки ведут на экран плана.", color = Muted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10, 20).forEach { amount ->
                    PrimaryButton("+$amount", { if (state.planConfirmed) deposit(amount) else nav.goTab(Routes.PLAN) }, compact = true, modifier = Modifier.weight(1f))
                }
            }
            SectionTitle("Снять")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10, 20).forEach { amount ->
                    SecondaryButton("−$amount", { preview(amount) }, enabled = amount <= state.total, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
    state.feedback?.let { FeedbackDialog(it, shown) }
    state.rejection?.let { RejectionDialog(it, shown, onPlan = { shown(); nav.goTab(Routes.PLAN) }) }
    withdrawal?.let { p ->
        AppDialog(
            title = "Проверить снятие",
            onDismiss = clearWithdrawal,
            confirm = { PrimaryButton("Снять ${p.amount}", { clearWithdrawal(); withdraw(p.amount) }, compact = true) },
            dismiss = { TextButton(onClick = clearWithdrawal) { Text("Передумать", color = Muted) } },
        ) {
            Text("Копилка: ${p.savedBefore} → ${p.savedAfter}\nСрок: ${p.weeksBefore?.let { "примерно $it нед." } ?: "—"} → ${p.weeksAfter?.let { "примерно $it нед." } ?: "—"}")
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Задания
// ---------------------------------------------------------------------------------------------

@Composable
private fun TasksScreen(state: TasksUiState, nav: NavHostController, doJob: () -> Unit, shown: () -> Unit) {
    var tab by remember { mutableStateOf(0) }
    AppScaffold(nav, Routes.TASKS, "Задания", state.top) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PillTabs(listOf("Доступные", "Мои"), tab) { tab = it }
            if (tab == 0) {
                SoftCard(Modifier.fillMaxWidth(), color = Mint) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Подработка", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.weight(1f))
                            Chip("осталось ${state.jobsLeft}", White, Ink)
                        }
                        Text("Помочь по дому · +${state.jobReward} монет", color = InkSoft, modifier = Modifier.padding(top = 3.dp))
                        PrimaryButton("Помочь", doJob, enabled = state.jobsLeft > 0, modifier = Modifier.padding(top = 10.dp))
                    }
                }
                SectionTitle("Открыто")
                if (state.open.isEmpty()) Callout("Все открытые задания пройдены. Новые появятся на следующей неделе.", BluePanel)
                state.open.forEach { TaskCard(it) { nav.navigate("${Routes.TASK}/${it.taskId}") } }
            } else {
                SectionTitle("Пройдено")
                if (state.done.isEmpty()) Callout("Пока ничего не пройдено. Начни с любого задания слева.", BluePanel)
                state.done.forEach { TaskCard(it, done = true) { nav.navigate("${Routes.TASK}/${it.taskId}") } }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
    state.feedback?.let { FeedbackDialog(it, shown) }
    state.rejection?.let { RejectionDialog(it, shown, null) }
}

@Composable
private fun TaskScreen(
    state: TaskPlayUiState,
    choose: (String) -> Unit,
    followup: (String) -> Unit,
    retry: () -> Unit,
    next: () -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    Surface(Modifier.fillMaxSize(), color = Canvas) {
        Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BackButton(onBack)
                Spacer(Modifier.weight(1f))
                Chip("Шаг ${state.step} из ${state.steps}", BluePanel, BlueDark)
            }
            SoftProgress(state.step.toFloat() / state.steps, Blue, BlueSoft, 8.dp)
            Text(state.title, style = MaterialTheme.typography.headlineMedium)
            state.intro?.let { Callout(it, BluePanel) }
            SoftCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text(state.situation, style = MaterialTheme.typography.bodyLarge)
                    if (state.wallet != null || state.savings != null) {
                        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.wallet?.let { Chip("Кошелёк 🪙 $it", Yellow, Ink) }
                            state.savings?.let { Chip("Копилка 🐷 $it", Peach, Ink) }
                        }
                    }
                }
            }
            state.neighborLine?.let { NeighborChip(it, Modifier.fillMaxWidth()) }
            if (state.result == null) {
                state.options.forEach { option -> ChoiceCard(option.label, false) { choose(option.id) } }
            }
            state.result?.let { result -> TaskResult(result, state, followup, retry, next) }
            Spacer(Modifier.height(8.dp))
        }
    }
    if (state.finished && state.feedback != null) FeedbackDialog(state.feedback, onDone)
}

@Composable
private fun TaskResult(result: ChoiceResultUi, state: TaskPlayUiState, followup: (String) -> Unit, retry: () -> Unit, next: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        result.consequence?.let { Callout(it, Peach) }
        EffectChips(result.effects)
        SoftCard(Modifier.fillMaxWidth(), color = Mint) {
            Column(Modifier.padding(18.dp)) {
                Text("Разбор", style = MaterialTheme.typography.titleLarge)
                Text(result.feedback, modifier = Modifier.padding(top = 6.dp))
                result.recovery?.let { Text("Что можно сделать: $it", color = InkSoft, modifier = Modifier.padding(top = 8.dp)) }
            }
        }
        result.followupPrompt?.let { Text(it, fontWeight = FontWeight.Bold) }
        result.followupButtons.forEach { label -> PrimaryButton(label, { followup(label) }) }
        if (result.canRetry) SecondaryButton("Попробовать иначе", retry, modifier = Modifier.fillMaxWidth())
        else PrimaryButton(if (state.step < state.steps) "Дальше" else "Завершить", next)
    }
}

// ---------------------------------------------------------------------------------------------
// Итоги недели, прогресс, настройки
// ---------------------------------------------------------------------------------------------

@Composable
private fun SummaryScreen(state: WeekSummaryUiState, nav: NavHostController, look: PetLook, stage: Stage) {
    Surface(Modifier.fillMaxSize(), color = Canvas) {
        Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Text("Итоги недели", style = MaterialTheme.typography.headlineLarge)
            Text("Неделя ${state.weekNumber}", color = Muted)
            Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(28.dp)).background(Brush.verticalGradient(listOf(Color(0xFFFFF0DF), Color(0xFFFFFAF3)))), contentAlignment = Alignment.Center) {
                PetArt(look, stage, Modifier.size(170.dp))
                Sparkle(Modifier.align(Alignment.TopStart).padding(28.dp), 24.sp)
                Sparkle(Modifier.align(Alignment.BottomEnd).padding(28.dp), 20.sp, delayMs = 600)
            }
            state.stageUp?.let { Callout("🎉 Кот теперь ${stageLabel(it)}!", Yellow) }
            SoftCard(Modifier.fillMaxWidth(), color = BluePanel, radius = 24.dp) {
                Column(Modifier.padding(18.dp)) {
                    Text("План и факт", style = MaterialTheme.typography.titleLarge)
                    state.rows.forEach { FactLine(partLabel(it.part), it.planned, it.actual) }
                    Text("Заработано за неделю: ${state.earned}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                }
            }
            SectionTitle("Как прошла неделя")
            state.icons.forEach { GrowthIconRow(it) }
            SoftProgress(state.stageProgressPct / 100f, Blue, BlueSoft, 10.dp)
            Text(state.gpToNextStage?.let { "До следующей стадии: $it GP" } ?: "Стадия взрослого достигнута", color = Muted)
            SoftCard(Modifier.fillMaxWidth(), color = Peach, radius = 24.dp) {
                Column(Modifier.padding(18.dp)) {
                    Text("${moodEmoji(state.pet.mood)} Кот", style = MaterialTheme.typography.titleLarge)
                    Text(state.pet.reasonText, modifier = Modifier.padding(top = 6.dp))
                    Text("Сытость ${state.pet.after.satiety} · Уход ${state.pet.after.care} · Настроение ${state.pet.after.mood}", color = InkSoft, modifier = Modifier.padding(top = 8.dp))
                }
            }
            state.recoveryHint?.let { Callout(it, Peach) }
            PrimaryButton("Следующая неделя", { nav.goTab(Routes.PLAN) })
            SecondaryButton("На главную", { nav.goTab(Routes.HOME) }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ProgressScreen(state: ProgressUiState, nav: NavHostController, look: PetLook) {
    val doneTotal = state.tasksByTopic.values.sumOf { it.done }
    val allTotal = state.tasksByTopic.values.sumOf { it.total }
    AppScaffold(nav, Routes.PROGRESS, "Прогресс", null) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            SoftCard(Modifier.fillMaxWidth(), color = BluePanel, radius = 26.dp) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(132.dp), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.matchParentSize().padding(6.dp)) {
                            val stroke = 12.dp.toPx()
                            drawArc(White, -90f, 360f, false, style = Stroke(stroke, cap = StrokeCap.Round))
                            drawArc(Blue, -90f, 360f * state.stageProgressPct.coerceIn(0, 100) / 100f, false, style = Stroke(stroke, cap = StrokeCap.Round))
                        }
                        PetArt(look, state.stage, Modifier.size(78.dp), animated = false)
                    }
                    Column(Modifier.padding(start = 16.dp)) {
                        Text(stageLabel(state.stage), style = MaterialTheme.typography.headlineMedium)
                        Text("${state.totalGp} GP · ${state.stageProgressPct}%", color = InkSoft)
                        Text(state.gpToNextStage?.let { "До следующей стадии: $it GP" } ?: "Стадия взрослого достигнута", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Задания", "$doneTotal / $allTotal", Yellow, Modifier.weight(1f))
                StatTile("Мечты", "${state.boughtGoals.size}", Rose, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Недели", "${state.weeks.size}", Mint, Modifier.weight(1f))
                StatTile("Очки роста", "${state.totalGp}", Peach, Modifier.weight(1f))
            }
            SectionTitle("Задания по темам")
            state.tasksByTopic.forEach { (topic, progress) -> ProgressLine(topicLabel(topic), progress.done, progress.total) }
            state.goal?.let { SectionTitle("Мечта"); GoalOption(it, true, onClick = {}) }
            if (state.weeks.isNotEmpty()) {
                SectionTitle("История недель")
                state.weeks.forEach { Text("Неделя ${it.weekNumber} · +${it.gp} GP · ${stageLabel(it.stageAfter)}", color = InkSoft) }
            }
            if (state.glossary.isNotEmpty()) {
                SectionTitle("Словарь")
                state.glossary.forEach { Callout("${it.term}: ${it.definition}", Surf) }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun SettingsScreen(state: SettingsUiState, nav: NavHostController, sound: (Boolean) -> Unit, animations: (Boolean) -> Unit) {
    var about by remember { mutableStateOf(false) }
    AppScaffold(nav, Routes.SETTINGS, "Настройки", null) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ToggleRow("Звуки", "Музыка и звуковые подсказки", state.sound, sound)
            ToggleRow("Анимации", "Движения кота и переходы", state.animations, animations)
            Spacer(Modifier.height(6.dp))
            SectionTitle("Раздел")
            // Прогресс (статистика) раньше жил в скрытом меню «Ещё» — теперь он здесь, рядом с остальными настройками.
            SettingsRow(R.drawable.ic_chart, "Прогресс", "Стадия, задания, история недель") { nav.goTab(Routes.PROGRESS) }
            SettingsRow(R.drawable.ic_tasks, "Для взрослых", "Полный прогресс и управление профилем") { nav.navigate(Routes.ADULT_GATE) }
            SettingsRow(R.drawable.ic_goal, "О приложении", "Версия, авторы и правила игры") { about = true }
        }
    }
    if (about) {
        AppDialog(
            title = "О приложении",
            onDismiss = { about = false },
            confirm = { PrimaryButton("Понятно", { about = false }, compact = true) },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("CashPet — версия 1.0.0", fontWeight = FontWeight.Bold)
                Text("Игра о деньгах и заботе о питомце: ставь цели, выполняй задания, планируй бюджет и корми своего кота.")
                Text("Приложение полностью офлайн: все данные хранятся только на этом устройстве и никуда не отправляются.", color = InkSoft)
                Text("© CashPet", color = Muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SettingsRow(icon: Int, title: String, subtitle: String, onClick: () -> Unit) {
    SoftCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon, BluePanel, 40.dp)
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Muted, fontSize = 12.5.sp, lineHeight = 16.sp)
            }
            Text("›", fontSize = 26.sp, color = Muted)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Взрослые и демо
// ---------------------------------------------------------------------------------------------

@Composable
private fun AdultGateScreen(a: Int, b: Int, input: String, error: String?, change: (String) -> Unit, submit: () -> Unit, back: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = Canvas) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(22.dp), verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) { BackButton(back); Text("Назад", color = Ink) }
            Spacer(Modifier.height(8.dp))
            Text("Раздел для взрослых", style = MaterialTheme.typography.headlineLarge)
            Text("Реши короткий пример, чтобы открыть настройки прогресса.", color = InkSoft, modifier = Modifier.padding(vertical = 8.dp))
            // Сам пример — самое важное на этом экране, раньше он никак не выводился
            SoftCard(Modifier.fillMaxWidth(), color = BluePanel, radius = 24.dp) {
                Text(
                    "$a × $b = ?",
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 40.sp),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 22.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = input, onValueChange = change, isError = error != null, label = { Text("Ответ") },
                supportingText = { Text(error ?: "Только цифры") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
            )
            PrimaryButton("Открыть", submit, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun AdultScreen(state: AdultUiState, nav: NavHostController, look: PetLook, onReset: () -> Unit, onDelete: () -> Unit, onDemo: () -> Unit) {
    var reset by remember { mutableStateOf(false) }
    var delete by remember { mutableStateOf(false) }
    AppScaffold(nav, Routes.ADULT, "Взрослые", null) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SoftCard(Modifier.fillMaxWidth(), color = Peach, radius = 26.dp) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    PetArt(look, if (state.hasProfile) state.stage else Stage.ADULT, Modifier.size(96.dp))
                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                        Text(if (state.demo) "Демо-профиль" else if (state.hasProfile) displayName(state.playerName) else "Профиля пока нет", style = MaterialTheme.typography.headlineMedium)
                        Text(if (state.hasProfile) "Питомец: ${displayName(state.petName)}" else "Для тех, кто хочет больше", color = InkSoft)
                        if (state.hasProfile) {
                            Text("${stageLabel(state.stage)} · недель: ${state.weeksPlayed}", color = InkSoft, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }

            // Раздел заданий — всегда на виду: список, пустое состояние или подсказка про профиль
            SoftCard(Modifier.fillMaxWidth(), color = BluePanel, radius = 24.dp) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconTile(R.drawable.ic_tasks, White, 44.dp)
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text("Сложные задания", style = MaterialTheme.typography.titleLarge)
                            Text(if (state.hasProfile) "Пройдено ${state.tasksDone} из ${state.tasksTotal}" else "Появятся после создания профиля", color = InkSoft, fontSize = 13.sp)
                        }
                        if (state.hasProfile) Chip("${state.openTasks.size}", White, BlueDark)
                    }
                    if (state.hasProfile) {
                        SoftProgress(if (state.tasksTotal == 0) 0f else state.tasksDone.toFloat() / state.tasksTotal, Blue, White, 9.dp)
                        if (state.openTasks.isEmpty()) {
                            Text("Новых заданий сейчас нет — в разделе «Задания» можно пройти сценарии ещё раз.", color = InkSoft)
                        } else {
                            state.openTasks.forEach { task -> TaskCard(task) { nav.navigate("${Routes.TASK}/${task.taskId}") } }
                        }
                        SecondaryButton("Все задания", { nav.goTab(Routes.TASKS) }, modifier = Modifier.fillMaxWidth())
                    } else {
                        Text("Профиля пока нет. Нажми «Запустить демо» ниже или начни игру — задания сразу появятся здесь.", color = InkSoft)
                    }
                }
            }

            AdultFeatureCard(R.drawable.ic_goal, "Расширенные мечты", "Посмотреть цели и накопления") { nav.goTab(Routes.SAVINGS) }
            AdultFeatureCard(R.drawable.ic_chart, "Статистика", "Подробный прогресс, темы и недели") { nav.goTab(Routes.PROGRESS) }

            if (state.hasProfile && state.tasksByTopic.isNotEmpty()) {
                SectionTitle("Прогресс по темам")
                state.tasksByTopic.forEach { (topic, p) -> ProgressLine(topicLabel(topic), p.done, p.total) }
            }

            PrimaryButton("Запустить демо", onDemo)
            SecondaryButton("Сбросить профиль", { reset = true }, enabled = state.hasProfile, modifier = Modifier.fillMaxWidth())
            SecondaryButton("Удалить профиль", { delete = true }, enabled = state.hasProfile, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
        }
    }
    if (reset) {
        AppDialog(
            title = "Сбросить профиль?", onDismiss = { reset = false },
            confirm = { PrimaryButton("Сбросить", { reset = false; onReset() }, compact = true) },
            dismiss = { TextButton(onClick = { reset = false }) { Text("Отмена", color = Muted) } },
        ) { Text("Прогресс будет удалён, а онбординг начнётся заново.") }
    }
    if (delete) {
        AppDialog(
            title = "Удалить профиль?", onDismiss = { delete = false },
            confirm = { PrimaryButton("Удалить", { delete = false; onDelete() }, compact = true) },
            dismiss = { TextButton(onClick = { delete = false }) { Text("Отмена", color = Muted) } },
        ) { Text("Все сохранённые данные этого профиля будут удалены.") }
    }
}

@Composable
private fun AdultFeatureCard(icon: Int, title: String, subtitle: String, onClick: () -> Unit) {
    SoftCard(Modifier.fillMaxWidth(), radius = 22.dp, onClick = onClick) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon, BluePanel, 48.dp)
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Muted, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 2.dp))
            }
            Text("›", fontSize = 28.sp, color = Muted)
        }
    }
}

@Composable
private fun DemoScreen(state: DemoUiState, start: () -> Unit, speed: () -> Unit, reset: () -> Unit, exit: () -> Unit, shown: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = Canvas) {
        Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Text("Демо", style = MaterialTheme.typography.headlineLarge)
            Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(28.dp)).background(Brush.verticalGradient(listOf(Color(0xFFFFF0DF), Color(0xFFFFFAF3)))), contentAlignment = Alignment.Center) {
                PetArt(PetLook("fluffy", "ginger"), state.stage ?: Stage.ADULT, Modifier.size(170.dp))
                Sparkle(Modifier.align(Alignment.TopEnd).padding(28.dp), 22.sp)
            }
            if (!state.active) {
                Text("Попробуй всё бесплатно!", style = MaterialTheme.typography.headlineMedium)
                Text("Здесь можно протестировать все функции приложения.", color = InkSoft)
                PrimaryButton("Начать демо", start)
            } else {
                Callout("Профиль работает отдельно от основной игры.", BluePanel)
                Text("Стадия: ${state.stage?.let(::stageLabel) ?: "—"}", style = MaterialTheme.typography.titleLarge)
                PrimaryButton(if (state.running) "Считаю…" else "Ускорить до взрослого", speed, enabled = state.canSpeedUp)
                SecondaryButton("Сбросить демо", reset, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = exit, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Выйти из демо", color = Muted) }
            }
        }
    }
    if (state.weeks.isNotEmpty()) {
        val week = state.weeks.first()
        FeedbackDialog(FeedbackUi(0, 0, 0, 0, emptyMap(), "Демо: неделя ${week.weekNumber} завершена, +${week.earned} заработано"), shown)
    }
}

// ---------------------------------------------------------------------------------------------
// Каркас: шапка и нижнее меню.
// «Ещё» убрали — пять разделов помещаются в один ряд: Дом · План · Задания · Магазин · Копилка.
// Прогресс (статистика) и «Для взрослых» переехали в Настройки — их не нужно было прятать за лишним тапом.
// ---------------------------------------------------------------------------------------------

private val TabRoutes = setOf(Routes.HOME, Routes.PLAN, Routes.TASKS, Routes.SHOP, Routes.SAVINGS)

@Composable
private fun AppScaffold(nav: NavHostController, current: String, title: String, top: TopBarUi?, content: @Composable () -> Unit) {
    Scaffold(
        containerColor = Canvas,
        topBar = { ScreenHeader(title, top, nav, showBack = current !in TabRoutes, showSettings = current != Routes.SETTINGS) },
        bottomBar = { BottomBar(current, nav) },
    ) { padding -> Box(Modifier.fillMaxSize().padding(padding)) { content() } }
}

@Composable
private fun ScreenHeader(title: String, top: TopBarUi?, nav: NavHostController, showBack: Boolean, showSettings: Boolean) {
    Row(
        Modifier.fillMaxWidth().background(Canvas).statusBarsPadding().padding(start = if (showBack) 4.dp else 20.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBack) BackButton { nav.popBackStack() }
        Text(title, style = MaterialTheme.typography.headlineLarge.copy(fontSize = 28.sp), modifier = Modifier.weight(1f))
        if (top != null) BalancePill(top.balance, top.savings)
        if (showSettings) {
            IconButton(onClick = { nav.goTab(Routes.SETTINGS) }, modifier = Modifier.size(48.dp)) {
                Icon(painterResource(R.drawable.ic_settings), "Настройки", tint = Muted, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun BottomBar(current: String, nav: NavHostController) {
    val items = listOf(
        Triple(Routes.HOME, "Дом", R.drawable.ic_home),
        Triple(Routes.PLAN, "План", R.drawable.ic_plan),
        Triple(Routes.TASKS, "Задания", R.drawable.ic_tasks),
        Triple(Routes.SHOP, "Магазин", R.drawable.ic_shop),
        Triple(Routes.SAVINGS, "Копилка", R.drawable.ic_savings),
    )
    Surface(color = White, shadowElevation = 10.dp, shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp), border = BorderStroke(1.dp, Border)) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 4.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            items.forEach { (route, label, icon) ->
                NavItem(label, icon, current == route, Modifier.weight(1f)) { if (current != route) nav.goTab(route) }
            }
        }
    }
}

@Composable
private fun NavItem(label: String, icon: Int, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.heightIn(min = 52.dp).clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(width = 46.dp, height = 28.dp).clip(RoundedCornerShape(14.dp)).background(if (selected) BlueSoft else Color.Transparent), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), label, tint = Color.Unspecified, modifier = Modifier.size(21.dp))
        }
        Text(label, fontSize = 11.sp, maxLines = 1, color = if (selected) BlueDark else Muted, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun BalancePill(balance: Int, savings: Int) {
    Row(
        Modifier.padding(end = 4.dp).clip(RoundedCornerShape(50.dp)).background(White).border(1.dp, Border, RoundedCornerShape(50.dp)).padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_coin), "Монеты", tint = Color.Unspecified, modifier = Modifier.size(18.dp))
        Text(" $balance", fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.width(8.dp))
        Icon(painterResource(R.drawable.ic_savings), "Копилка", tint = Color.Unspecified, modifier = Modifier.size(18.dp))
        Text(" $savings", fontWeight = FontWeight.Bold, color = Ink)
    }
}

// ---------------------------------------------------------------------------------------------
// Иллюстрации
// ---------------------------------------------------------------------------------------------

@Composable
private fun ProductPlaceholder(item: ShopItemUi) {
    val (icon, bg) = when (item.id) {
        "food_vitamins" -> R.drawable.ic_vitamins to Mint
        "food_basic" -> R.drawable.ic_food to Mint
        "food_fish" -> R.drawable.ic_fish to BluePanel
        "food_premium" -> R.drawable.ic_can to BluePanel
        "care_comb" -> R.drawable.ic_comb to BluePanel
        "care_shampoo" -> R.drawable.ic_shampoo to Peach
        "care_towel" -> R.drawable.ic_towel to Mint
        "want_rattle" -> R.drawable.ic_rattle to Yellow
        "want_ball" -> R.drawable.ic_ball to Yellow
        "want_plush_mouse" -> R.drawable.ic_mouse to Peach
        "want_bow" -> R.drawable.ic_bow to Rose
        "want_cap" -> R.drawable.ic_cap to Rose
        "want_bed" -> R.drawable.ic_bed to Peach
        "want_jacket" -> R.drawable.ic_jacket to Mint
        "want_scooter" -> R.drawable.ic_scooter to Yellow
        else -> R.drawable.ic_ball to Peach
    }
    Box(Modifier.size(78.dp).clip(RoundedCornerShape(22.dp)).background(bg), contentAlignment = Alignment.Center) {
        Icon(painterResource(icon), item.name, tint = Color.Unspecified, modifier = Modifier.size(50.dp))
    }
}

@Composable
private fun GoalPlaceholder(goal: GoalUi) {
    val icon = when (goal.id) {
        "goal_scratcher" -> R.drawable.ic_goal
        "goal_house" -> R.drawable.ic_house
        "goal_bike" -> R.drawable.ic_bike
        else -> R.drawable.ic_goal
    }
    Box(Modifier.size(60.dp).clip(RoundedCornerShape(18.dp)).background(BlueSoft), contentAlignment = Alignment.Center) {
        Icon(painterResource(icon), goal.name, tint = Color.Unspecified, modifier = Modifier.size(40.dp))
    }
}

/** Кот: картинка по породе, окрасу и стадии. Лёгкая анимация «дыхания» отключается в настройках. */
@Composable
private fun PetArt(look: PetLook, stage: Stage, modifier: Modifier = Modifier, animated: Boolean = true) {
    val breed = when (look.breedId) { "smooth" -> "smooth"; "lop" -> "lop"; else -> "fluffy" }
    val color = when (look.colorId) { "ginger" -> "ginger"; "black" -> "black"; else -> "grey" }
    val s = when (stage) { Stage.BABY -> "s1"; Stage.TEEN -> "s2"; Stage.ADULT -> "s3" }
    val res = when ("${breed}_${color}_$s") {
        "fluffy_ginger_s1" -> R.drawable.cat_fluffy_ginger_s1; "fluffy_ginger_s2" -> R.drawable.cat_fluffy_ginger_s2; "fluffy_ginger_s3" -> R.drawable.cat_fluffy_ginger_s3
        "fluffy_black_s1" -> R.drawable.cat_fluffy_black_s1; "fluffy_black_s2" -> R.drawable.cat_fluffy_black_s2; "fluffy_black_s3" -> R.drawable.cat_fluffy_black_s3
        "smooth_ginger_s1" -> R.drawable.cat_smooth_ginger_s1; "smooth_ginger_s2" -> R.drawable.cat_smooth_ginger_s2; "smooth_ginger_s3" -> R.drawable.cat_smooth_ginger_s3
        "smooth_black_s1" -> R.drawable.cat_smooth_black_s1; "smooth_black_s2" -> R.drawable.cat_smooth_black_s2; "smooth_black_s3" -> R.drawable.cat_smooth_black_s3
        "lop_ginger_s1" -> R.drawable.cat_lop_ginger_s1; "lop_ginger_s2" -> R.drawable.cat_lop_ginger_s2; "lop_ginger_s3" -> R.drawable.cat_lop_ginger_s3
        "lop_black_s1" -> R.drawable.cat_lop_black_s1; "lop_black_s2" -> R.drawable.cat_lop_black_s2; "lop_black_s3" -> R.drawable.cat_lop_black_s3
        "fluffy_grey_s1" -> R.drawable.cat_fluffy_grey_s1; "fluffy_grey_s2" -> R.drawable.cat_fluffy_grey_s2; "fluffy_grey_s3" -> R.drawable.cat_fluffy_grey_s3
        "smooth_grey_s1" -> R.drawable.cat_smooth_grey_s1; "smooth_grey_s2" -> R.drawable.cat_smooth_grey_s2; "smooth_grey_s3" -> R.drawable.cat_smooth_grey_s3
        "lop_grey_s1" -> R.drawable.cat_lop_grey_s1; "lop_grey_s2" -> R.drawable.cat_lop_grey_s2; else -> R.drawable.cat_lop_grey_s3
    }
    val allowed = animated && LocalAnimations.current
    val transition = rememberInfiniteTransition(label = "pet")
    val breath by transition.animateFloat(
        initialValue = 1f, targetValue = 1.035f,
        animationSpec = infiniteRepeatable(tween(1700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breath",
    )
    val sway by transition.animateFloat(
        initialValue = -1.2f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(2300, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "sway",
    )
    val anim = if (allowed) {
        Modifier.graphicsLayer {
            transformOrigin = TransformOrigin(0.5f, 1f)
            scaleY = breath
            scaleX = 2f - breath
            rotationZ = sway
        }
    } else Modifier
    Image(painterResource(res), contentDescription = "Кот", contentScale = ContentScale.Fit, modifier = modifier.then(anim))
}

@Composable
private fun Sparkle(modifier: Modifier = Modifier, size: androidx.compose.ui.unit.TextUnit = 20.sp, delayMs: Int = 0) {
    val allowed = LocalAnimations.current
    val transition = rememberInfiniteTransition(label = "sparkle")
    val a by transition.animateFloat(
        initialValue = 0.35f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100 + delayMs / 2, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "alpha",
    )
    Text("✦", color = Color(0xFFFFC66D), fontSize = size, modifier = modifier.graphicsLayer { alpha = if (allowed) a else 0.9f })
}

@Composable
private fun Clouds() {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val c = Color(0xCCFFFFFF)
        drawOval(c, Offset(-w * 0.05f, h * 0.68f), Size(w * 0.45f, h * 0.22f))
        drawOval(c, Offset(w * 0.55f, h * 0.74f), Size(w * 0.55f, h * 0.24f))
        drawOval(c, Offset(w * 0.62f, h * 0.10f), Size(w * 0.30f, h * 0.10f))
    }
}

// ---------------------------------------------------------------------------------------------
// Общие компоненты
// ---------------------------------------------------------------------------------------------

@Composable
private fun SoftCard(
    modifier: Modifier = Modifier,
    color: Color = Surf,
    radius: Dp = 22.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    val colors = CardDefaults.cardColors(containerColor = color)
    val border = BorderStroke(1.dp, Border)
    if (onClick != null) Card(onClick = onClick, modifier = modifier, shape = shape, colors = colors, border = border, content = content)
    else Card(modifier = modifier, shape = shape, colors = colors, border = border, content = content)
}

@Composable
private fun AppDialog(
    title: String,
    onDismiss: () -> Unit,
    confirm: @Composable () -> Unit,
    dismiss: (@Composable () -> Unit)? = null,
    text: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surf,
        shape = RoundedCornerShape(28.dp),
        title = { Text(title, fontWeight = FontWeight.ExtraBold, color = Ink) },
        text = text,
        confirmButton = confirm,
        dismissButton = dismiss,
    )
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit, enabled: Boolean = true, compact: Boolean = false, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(if (compact) 48.dp else 54.dp),
        shape = RoundedCornerShape(50.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Blue, contentColor = White, disabledContainerColor = Color(0xFFD7E2EF), disabledContentColor = White),
    ) { Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
}

@Composable
private fun SecondaryButton(label: String, onClick: () -> Unit, enabled: Boolean = true, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(50.dp),
        border = BorderStroke(1.5.dp, if (enabled) Blue else Border),
    ) { Text(label, fontWeight = FontWeight.Bold, color = if (enabled) BlueDark else Muted) }
}

@Composable
private fun SmallButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(50.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Blue, contentColor = White),
        contentPadding = PaddingValues(horizontal = 8.dp),
    ) { Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1) }
}

@Composable
private fun BackButton(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) { Text("‹", fontSize = 34.sp, color = Ink) }
}

@Composable
private fun PillTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(50.dp)).background(SoftGray).padding(3.dp)) {
        labels.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier.weight(1f).heightIn(min = 44.dp).clip(RoundedCornerShape(50.dp))
                    .background(if (on) White else Color.Transparent)
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) { Text(label, color = if (on) BlueDark else Muted, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium) }
        }
    }
}

@Composable
private fun Chip(text: String, bg: Color, fg: Color) {
    Text(text, color = fg, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.clip(RoundedCornerShape(50.dp)).background(bg).padding(horizontal = 12.dp, vertical = 6.dp))
}

@Composable
private fun IconTile(icon: Int, bg: Color, size: Dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(size / 3)).background(bg), contentAlignment = Alignment.Center) {
        Icon(painterResource(icon), null, tint = BlueDark, modifier = Modifier.size(size * 0.55f))
    }
}

@Composable
private fun CoinAmount(value: Int, modifier: Modifier = Modifier, big: Boolean = false) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(R.drawable.ic_coin), "Монеты", tint = Color(0xFFFFB72B), modifier = Modifier.size(if (big) 26.dp else 17.dp))
        Text(" $value", fontWeight = FontWeight.ExtraBold, color = if (big) Ink else Gold, fontSize = if (big) 28.sp else 15.sp)
    }
}

@Composable
private fun SoftProgress(progress: Float, color: Color, track: Color, height: Dp, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(50.dp)),
        color = color,
        trackColor = track,
    )
}

@Composable
private fun StatTile(label: String, value: String, bg: Color, modifier: Modifier = Modifier) {
    SoftCard(modifier, color = bg, radius = 20.dp) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(label, color = InkSoft, fontSize = 13.sp)
            Text(value, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
private fun BulletCard(b: BulletUi) {
    SoftCard(Modifier.fillMaxWidth(), radius = 20.dp) {
        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(BluePanel), contentAlignment = Alignment.Center) { Text("✓", color = Blue, fontWeight = FontWeight.Bold) }
            Column(Modifier.padding(start = 12.dp)) {
                Text(b.term, fontWeight = FontWeight.Bold)
                Text(b.text, color = InkSoft)
            }
        }
    }
}

@Composable
private fun ChoiceCard(label: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = if (selected) BluePanel else Surf),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Blue else Border),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(24.dp).clip(CircleShape).background(if (selected) Blue else SoftGray), contentAlignment = Alignment.Center) {
                if (selected) Text("✓", fontSize = 13.sp, color = White)
            }
            Text(label, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
        }
    }
}

@Composable
private fun Callout(text: String, color: Color) {
    SoftCard(Modifier.fillMaxWidth(), color = color, radius = 20.dp) { Text(text, Modifier.padding(14.dp), color = Ink) }
}

@Composable
private fun BalanceHero(available: Int, left: Int) {
    SoftCard(Modifier.fillMaxWidth(), color = BluePanel, radius = 24.dp) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Доступно на неделю", color = InkSoft)
                CoinAmount(available, big = true)
            }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text("Не разложено", color = InkSoft)
                Text("$left", style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}

@Composable
private fun PlanPartCard(title: String, subtitle: String, value: Int, part: Part, canIncrease: Boolean, confirmed: Boolean, inc: (Part) -> Unit, dec: (Part) -> Unit) {
    SoftCard(Modifier.fillMaxWidth(), radius = 22.dp) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(subtitle, color = Muted, fontSize = 13.sp)
            }
            RoundButton("−", { dec(part) }, !confirmed && value > 0)
            Text("$value", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.width(64.dp), textAlign = TextAlign.Center)
            RoundButton("+", { inc(part) }, !confirmed && canIncrease)
        }
    }
}

@Composable
private fun RoundButton(text: String, onClick: () -> Unit, enabled: Boolean) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(48.dp),
        contentPadding = PaddingValues(0.dp),
        shape = CircleShape,
        border = BorderStroke(1.5.dp, if (enabled) Blue else Border),
    ) { Text(text, fontSize = 22.sp, color = if (enabled) BlueDark else Muted) }
}

@Composable
private fun NeighborChip(t: NeighborTipUi, modifier: Modifier = Modifier.width(210.dp)) {
    SoftCard(modifier, color = Yellow, radius = 20.dp) {
        Column(Modifier.padding(13.dp)) {
            Text(t.name, fontWeight = FontWeight.Bold)
            Text("“${t.text}”", color = InkSoft, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun FactLine(label: String, planned: Int, actual: Int) {
    val diff = actual - planned
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
        Text("$actual / $planned", color = InkSoft)
        // Превышение — значком и словом, не только цветом
        if (diff > 0) Text("  ▲ +$diff", color = BlueDark, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 2.dp))
}

@Composable
private fun RowScope.SmallAction(label: String, route: String, nav: NavHostController) {
    OutlinedButton(
        onClick = { nav.goTab(route) },
        modifier = Modifier.weight(1f).height(48.dp),
        shape = RoundedCornerShape(50.dp),
        border = BorderStroke(1.5.dp, Blue),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) { Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BlueDark, maxLines = 1) }
}

@Composable
private fun StatRow(emoji: String, label: String, value: Int, color: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(color.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) { Text(emoji, fontSize = 15.sp) }
        Text(label, Modifier.padding(start = 10.dp).width(96.dp), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        SoftProgress(value / 100f, color, SoftGray, 10.dp, Modifier.weight(1f))
        Text("$value%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = InkSoft, modifier = Modifier.padding(start = 10.dp).width(42.dp), textAlign = TextAlign.End)
    }
}

@Composable
private fun TaskMiniCard(task: TaskCardUi, onClick: () -> Unit) {
    SoftCard(Modifier.fillMaxWidth(), color = Yellow, radius = 22.dp, onClick = onClick) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(White), contentAlignment = Alignment.Center) { Text("📚", fontSize = 24.sp) }
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text("Активное задание", color = InkSoft, fontSize = 12.sp)
                Text(task.title, fontWeight = FontWeight.Bold)
            }
            Chip("+${task.reward}", White, Gold)
        }
    }
}

@Composable
private fun GoalCard(goal: GoalUi, weeks: Int?, canBuy: Boolean, buy: () -> Unit) {
    SoftCard(Modifier.fillMaxWidth(), color = BluePanel, radius = 24.dp) {
        Column(Modifier.padding(18.dp)) {
            Text("🎯 ${goal.name}", style = MaterialTheme.typography.headlineMedium)
            Text("Накоплено ${goal.saved} из ${goal.cost}", color = InkSoft, modifier = Modifier.padding(top = 2.dp))
            SoftProgress(goal.saved.toFloat() / goal.cost.coerceAtLeast(1), Blue, White, 10.dp, Modifier.padding(vertical = 12.dp))
            Text(if (weeks != null) "Примерно $weeks ${weeksWord(weeks)}" else "Срок появится после первой недели с копилкой", color = InkSoft)
            if (canBuy) PrimaryButton("Купить мечту", buy, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun GoalOption(goal: GoalUi, selected: Boolean, onClick: (String) -> Unit) {
    Card(
        onClick = { if (!goal.bought) onClick(goal.id) },
        colors = CardDefaults.cardColors(containerColor = if (selected) BluePanel else Surf),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Blue else Border),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GoalPlaceholder(goal)
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(goal.name, fontWeight = FontWeight.Bold)
                    Text("${goal.cost} монет", color = Muted, fontSize = 13.sp)
                }
                if (goal.bought) Chip("Уже есть ✓", Mint, Ink) else Text("${goal.saved}/${goal.cost}", color = InkSoft, fontWeight = FontWeight.SemiBold)
            }
            if (!goal.bought) SoftProgress(goal.saved.toFloat() / goal.cost.coerceAtLeast(1), Green, SoftGray, 8.dp, Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun TaskCard(task: TaskCardUi, done: Boolean = false, onClick: () -> Unit = {}) {
    SoftCard(Modifier.fillMaxWidth(), color = if (done) SoftGray else Surf, radius = 22.dp, onClick = onClick) {
        Row(Modifier.fillMaxWidth().heightIn(min = 68.dp).padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(if (done) White else Peach), contentAlignment = Alignment.Center) {
                Text(if (done) "✓" else "🐾", fontSize = 20.sp, color = BlueDark)
            }
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(task.title, fontWeight = FontWeight.Bold)
                Text(topicLabel(task.topic), color = Muted, fontSize = 13.sp)
            }
            Chip("+${task.reward}", if (done) White else Yellow, Gold)
            Text("›", fontSize = 24.sp, color = Muted, modifier = Modifier.padding(start = 6.dp))
        }
    }
}

/** Эффекты товара (сытость/уход/настроение) — маленькая иконка вместо слова, чтобы карточка читалась с одного взгляда. */
@Composable
private fun StatEffectRow(effects: Map<Stat, Int>, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        effects.forEach { (stat, value) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(statIcon(stat)), statLabel(stat), tint = Color.Unspecified, modifier = Modifier.size(15.dp))
                Text(" ${if (value > 0) "+" else ""}$value", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (value >= 0) Green else Pink)
            }
        }
    }
}

@Composable
private fun EffectChips(effects: Map<EffectKey, Int>) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        effects.forEach { (k, v) ->
            Chip("${effectLabel(k)} ${if (v > 0) "+" else ""}$v", if (v >= 0) Mint else Rose, Ink)
        }
    }
}

@Composable
private fun FeedbackDialog(feedback: FeedbackUi, onDismiss: () -> Unit) {
    AppDialog(
        title = "Что изменилось",
        onDismiss = onDismiss,
        confirm = { PrimaryButton("Понятно", onDismiss, compact = true) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Баланс: ${feedback.balanceBefore} → ${feedback.balanceAfter}", fontWeight = FontWeight.SemiBold)
            Text("Копилка: ${feedback.savingsBefore} → ${feedback.savingsAfter}", fontWeight = FontWeight.SemiBold)
            feedback.statChanges.forEach { (s, v) -> Text("${statLabel(s)} ${if (v > 0) "+" else ""}$v") }
            Text(feedback.reasonText, color = InkSoft, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun RejectionDialog(rejection: Rejection, onDismiss: () -> Unit, onPlan: (() -> Unit)?) {
    val toPlan = rejection is Rejection.PlanNotConfirmed
    AppDialog(
        title = "Не получилось",
        onDismiss = onDismiss,
        confirm = { PrimaryButton(if (toPlan) "К плану" else "Понятно", { onDismiss(); if (toPlan) onPlan?.invoke() }, compact = true) },
    ) { Text(rejectionText(rejection)) }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    SoftCard(Modifier.fillMaxWidth(), radius = 22.dp) {
        Row(Modifier.fillMaxWidth().heightIn(min = 68.dp).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Muted, fontSize = 13.sp)
            }
            Switch(
                checked, onChange,
                colors = SwitchDefaults.colors(checkedTrackColor = Blue, checkedThumbColor = White, uncheckedTrackColor = SoftGray, uncheckedThumbColor = White, uncheckedBorderColor = Border),
            )
        }
    }
}

@Composable
private fun GrowthIconRow(item: GrowthIconUi) {
    Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(if (item.pct >= 100) "●" else if (item.pct >= 50) "◐" else "○", fontSize = 24.sp, color = Blue)
        Text(
            when (item.icon) { GrowthIcon.NEED -> "Нужное в порядке"; GrowthIcon.PLAN -> "План удался"; GrowthIcon.SAVE -> "Копилка растёт" },
            Modifier.padding(start = 10.dp).weight(1f),
        )
        Text("${item.pct}%", color = InkSoft, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ProgressLine(label: String, done: Int, total: Int) {
    Column {
        Row {
            Text(label, Modifier.weight(1f))
            Text("$done / $total", color = InkSoft)
        }
        SoftProgress(if (total == 0) 0f else done.toFloat() / total, Blue, BlueSoft, 9.dp, Modifier.padding(top = 5.dp))
    }
}

// ---------------------------------------------------------------------------------------------
// Подписи
// ---------------------------------------------------------------------------------------------

private fun displayName(value: String): String = value.trimStart().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

private fun stageLabel(stage: Stage) = when (stage) { Stage.BABY -> "Малыш"; Stage.TEEN -> "Подросток"; Stage.ADULT -> "Взрослый" }
private fun moodLabel(mood: PetMood) = when (mood) { PetMood.HAPPY -> "Радуется"; PetMood.CALM -> "Спокоен"; PetMood.SAD -> "Грустит" }
private fun moodEmoji(mood: PetMood) = when (mood) { PetMood.HAPPY -> "😊"; PetMood.CALM -> "🙂"; PetMood.SAD -> "😿" }
private fun partLabel(part: Part) = when (part) { Part.NEED -> "🍗 Нужно"; Part.WANT -> "🎈 Хочу"; Part.SAVE -> "🐷 Отложить" }
private fun topicLabel(topic: Topic) = when (topic) {
    Topic.INTRO -> "Первый день"
    Topic.BUDGET -> "Бюджет"
    Topic.SAVINGS -> "Сбережения"
    Topic.PURCHASES -> "Покупки"
    Topic.SAFETY -> "Безопасность"
    Topic.HOLIDAYS -> "Праздники"
}
private fun statLabel(stat: Stat) = when (stat) { Stat.SATIETY -> "Сытость"; Stat.CARE -> "Уход"; Stat.MOOD -> "Настроение" }
private fun effectLabel(key: EffectKey) = when (key) {
    EffectKey.WALLET -> "🪙"
    EffectKey.SAVINGS -> "🐷"
    EffectKey.SATIETY -> "🍗"
    EffectKey.CARE -> "🧼"
    EffectKey.MOOD -> "😊"
}
private fun statIcon(stat: Stat) = when (stat) { Stat.SATIETY -> R.drawable.ic_food; Stat.CARE -> R.drawable.ic_comb; Stat.MOOD -> R.drawable.ic_mood }
private fun rejectionText(r: Rejection) = when (r) {
    is Rejection.NotEnoughMoney -> "Не хватает ${r.missing} монет. Можно выполнить задание или подработку, выбрать что-то дешевле, добавить покупку в «Хочу потом» или взять из копилки."
    is Rejection.PlanExceedsBudget -> "План превышает доступный бюджет на ${r.excess} монет."
    Rejection.PlanAlreadyConfirmed -> "План уже подтверждён."
    Rejection.PlanNotConfirmed -> "Сначала подтверди план недели."
    is Rejection.NotEnoughSavings -> "В копилке не хватает ${r.missing} монет."
    Rejection.NoActiveGoal -> "Сначала выбери мечту."
    is Rejection.GoalNotReached -> "До мечты не хватает ${r.missing} монет."
    Rejection.GoalAlreadyBought -> "Эта мечта уже куплена."
    Rejection.JobLimitReached -> "На этой неделе подработки закончились."
    Rejection.TaskClosed -> "Это задание пока недоступно."
    Rejection.InvalidAmount -> "Сумма должна быть положительной и кратной 5."
    is Rejection.UnknownId -> "Элемент не найден."
}

// «1 неделя», «3 недели», «5 недель» — срок до мечты в Копилке
private fun weeksWord(n: Int): String = when {
    n % 100 in 11..14 -> "недель"
    n % 10 == 1 -> "неделя"
    n % 10 in 2..4 -> "недели"
    else -> "недель"
}
