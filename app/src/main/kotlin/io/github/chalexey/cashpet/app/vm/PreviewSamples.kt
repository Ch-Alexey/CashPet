package io.github.chalexey.cashpet.app.vm

import io.github.chalexey.cashpet.content.ScreenKind
import io.github.chalexey.cashpet.core.model.Difficulty
import io.github.chalexey.cashpet.core.model.EffectKey
import io.github.chalexey.cashpet.core.model.GrowthIcon
import io.github.chalexey.cashpet.core.model.Part
import io.github.chalexey.cashpet.core.model.PetLook
import io.github.chalexey.cashpet.core.model.PetMood
import io.github.chalexey.cashpet.core.model.PetStats
import io.github.chalexey.cashpet.core.model.Stage
import io.github.chalexey.cashpet.core.model.Stat
import io.github.chalexey.cashpet.core.model.Topic

/**
 * Примеры состояний экранов для @Preview — чтобы верстать без ViewModel и без выдуманных чисел.
 * Числа — из сценария «разумная игра», 2-я неделя (docs/02-экономика.md). Только для превью, не для игры.
 */
object PreviewSamples {

    val top = TopBarUi(balance = 60, savings = 55, goalName = "Домик", goalCost = 100, goalProgressPct = 55)

    val topNoGoal = TopBarUi(balance = 100, savings = 0, goalName = null, goalCost = null, goalProgressPct = 0)

    val taskCard = TaskCardUi(taskId = "4.2", title = "Скидка на нужную и ненужную вещь", topic = Topic.PURCHASES, reward = 20)

    val home = HomeUiState(
        top = top,
        petName = "Пончик",
        look = PetLook("fluffy", "ginger"),
        stage = Stage.BABY,
        mood = PetMood.HAPPY,
        stats = PetStats(satiety = 75, care = 80, mood = 67),
        petReasonText = "Всё хорошо. Пончик ждёт новых дел",
        activeTask = taskCard,
        weekNumber = 2,
        needWarning = true,
        closeWarning = CloseWeekWarning.NEED_NOT_BOUGHT,
    )

    val homeSad = home.copy(
        mood = PetMood.SAD,
        stats = PetStats(satiety = 30, care = 45, mood = 40),
        petReasonText = "Пончик хочет есть. Пора купить корм",
    )

    val plan = PlanUiState(
        top = top,
        available = 115, need = 50, want = 35, save = 20,
        unallocated = 10,
        needHint = 50,
        canIncrease = true,
        confirmed = false,
        fact = null,
        neighborTips = listOf(
            NeighborTipUi("toroplivy", "Торопливый", "Я бы всё сразу на одну вещь потратил!"),
            NeighborTipUi("ostorozhny", "Осторожный", "А я — по чуть-чуть на каждое"),
        ),
    )

    val planConfirmed = plan.copy(
        save = 30, unallocated = 0, canIncrease = false, confirmed = true,
        fact = PlanFactUi(spentNeed = 30, spentWant = 45, saved = 10, earned = 20),
    )

    private val food = ShopItemUi("food_basic", "Пакет корма", 30, Part.NEED, mapOf(Stat.SATIETY to 40), affordable = true)
    private val shampoo = ShopItemUi("care_shampoo", "Шампунь", 20, Part.NEED, mapOf(Stat.CARE to 40), affordable = true)
    private val ball = ShopItemUi("want_ball", "Мячик", 15, Part.WANT, mapOf(Stat.MOOD to 15), affordable = true)
    private val scooter = ShopItemUi("want_scooter", "Самокат", 80, Part.WANT, mapOf(Stat.MOOD to 40), affordable = false)

    val shop = ShopUiState(
        top = top,
        planConfirmed = true,
        need = listOf(food, shampoo),
        want = listOf(ball, scooter),
        wishlist = listOf(scooter),
        planLeftNeed = 50,
        planLeftWant = 35,
    )

    val shopBeforePlan = shop.copy(planConfirmed = false, planLeftNeed = null, planLeftWant = null)

    private val house = GoalUi("goal_house", "Домик", cost = 100, saved = 55, cells = 10, filledCells = 5)

    val savings = SavingsUiState(
        top = top,
        planConfirmed = true,
        total = 55,
        goal = house,
        goals = listOf(
            GoalUi("goal_scratcher", "Когтеточка", 60, 0, 6, 0, bought = true),
            house,
            GoalUi("goal_bike", "Велосипед", 150, 0, 15, 0),
        ),
        weeksLeft = 2,
        canBuyGoal = false,
        wallet = 60,
    )

    val tasks = TasksUiState(
        top = top,
        open = listOf(taskCard, TaskCardUi("2.3", "Внезапная стрижка", Topic.BUDGET, 20)),
        done = listOf(
            TaskCardUi("0", "Первый день", Topic.INTRO, 20),
            TaskCardUi("1.1", "Пустая миска и большой мяч", Topic.PURCHASES, 15),
        ),
        jobsLeft = 2,
        jobReward = 10,
    )

    val taskPlay = TaskPlayUiState(
        title = "Пустая миска и большой мяч",
        step = 1, steps = 1,
        intro = null,
        situation = "Пончик ждёт ужина, а в магазине лежит большой мяч. У тебя 30 монет. Корм и мяч стоят по 30. Что купить?",
        wallet = 30,
        savings = null,
        options = listOf(OptionUi("food", "Купить корм"), OptionUi("ball", "Купить мяч"), OptionUi("half", "Взять по половинке")),
        result = null,
        neighborLine = NeighborTipUi("toroplivy", "Торопливый", "Я бы сразу взял мяч, тут и решать нечего!"),
    )

    val taskPlayRetry = taskPlay.copy(
        wallet = 0,
        result = ChoiceResultUi(
            consequence = "Пончик играет, но всё ещё хочет есть",
            effects = mapOf(EffectKey.WALLET to -30, EffectKey.MOOD to 15),
            feedback = "Мяч — это желаемое, он приятен. Обычно сначала покупают нужное",
            canRetry = true,
            recovery = "Мяч можно добавить в «Хочу потом» и купить позже",
            followupButtons = emptyList(),
        ),
    )

    val weekSummary = WeekSummaryUiState(
        weekNumber = 2,
        planConfirmed = true,
        rows = listOf(
            PlanFactRowUi(Part.NEED, planned = 50, actual = 50),
            PlanFactRowUi(Part.WANT, planned = 35, actual = 45),
            PlanFactRowUi(Part.SAVE, planned = 30, actual = 30),
        ),
        earned = 60,
        icons = listOf(GrowthIconUi(GrowthIcon.NEED, 100), GrowthIconUi(GrowthIcon.PLAN, 100), GrowthIconUi(GrowthIcon.SAVE, 100)),
        gpToNextStage = 150,
        stageUp = Stage.TEEN,
        pet = PetChangeUi(
            before = PetStats(100, 100, 95),
            after = PetStats(75, 80, 67),
            mood = PetMood.HAPPY,
            reasonText = "Пончик радуется: неделя удалась",
        ),
        goal = house,
        recoveryHint = null,
    )

    val feedback = FeedbackUi(
        balanceBefore = 115, balanceAfter = 100,
        savingsBefore = 55, savingsAfter = 55,
        statChanges = mapOf(Stat.MOOD to 15),
        reasonText = "Покупка: Мячик. Пончик радуется",
    )

    val onboardingLook = OnboardingUiState(
        kind = ScreenKind.LOOK,
        step = 3, steps = 8,
        text = "Выбери, каким будет твой питомец: форму и окрас",
        bullets = emptyList(),
        finePrint = null,
        input = null,
        look = PetLook("fluffy", "ginger"),
        breeds = listOf(PetOptionUi("fluffy", "Пушистый"), PetOptionUi("smooth", "Гладкошёрстный"), PetOptionUi("lop", "Вислоухий")),
        colors = listOf(PetOptionUi("ginger", "Рыжий в полоску"), PetOptionUi("grey", "Серый в пятнышко"), PetOptionUi("black", "Чёрный однотонный")),
        choices = emptyList(),
        button = "Готово",
        petReply = null,
        canSkip = false,
        canGoBack = true,
    )

    val onboardingPetNameError = onboardingLook.copy(
        kind = ScreenKind.PET_NAME,
        step = 4,
        text = "Как его зовут? Придумай имя!",
        input = NameInputUi(value = "", placeholder = "Например: Пончик", hint = "Имя может быть придуманным",
            maxLength = 12, error = "Напиши хотя бы одну букву"),
        button = "Так и назовём",
    )

    val onboardingDecisions = onboardingLook.copy(
        kind = ScreenKind.INFO,
        step = 5,
        text = "Каждую неделю тебе решать, на что потратить монеты.",
        bullets = listOf(
            BulletUi("Нужно", "без этого не обойтись, например еда и уход"),
            BulletUi("Хочу", "приятные мелочи, игрушки и лакомства"),
            BulletUi("Отложить", "монеты в копилку на большую мечту"),
        ),
        finePrint = "Монеты в игре игровые, настоящие деньги не нужны",
        button = "Понятно",
        canSkip = true,
    )

    val onboardingDifficulty = onboardingLook.copy(
        kind = ScreenKind.DIFFICULTY,
        step = 7,
        text = "Как будем играть?",
        finePrint = "Поменять потом может взрослый",
        choices = listOf(
            DifficultyChoiceUi(Difficulty.EASY, "Попроще", selected = false),
            DifficultyChoiceUi(Difficulty.HARD, "Посложнее", selected = false),
        ),
        button = null,
    )

    val progress = ProgressUiState(
        stage = Stage.TEEN,
        totalGp = 200,
        gpToNextStage = 150,
        tasksByTopic = mapOf(
            Topic.PURCHASES to TopicProgressUi(done = 1, total = 2),
            Topic.BUDGET to TopicProgressUi(done = 0, total = 2),
            Topic.SAVINGS to TopicProgressUi(done = 0, total = 2),
        ),
        goal = house,
        boughtGoals = emptyList(),
        lastWeek = weekSummary,
        weeks = listOf(WeekLineUi(1, 100, Stage.BABY), WeekLineUi(2, 100, Stage.TEEN)),
    )

    val adult = AdultUiState(
        weeksPlayed = 2,
        stage = Stage.TEEN,
        tasksDone = 2,
        tasksTotal = 7,
        topicsDone = listOf(Topic.PURCHASES),
        goalsBought = 0,
        difficulty = Difficulty.EASY,
        hasChildProfile = true,
    )
}
