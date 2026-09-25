package io.github.chalexey.cashpet.app.vm

import io.github.chalexey.cashpet.content.ScreenKind
import io.github.chalexey.cashpet.core.engine.Rejection
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

// Что показывает каждый экран — docs/08-контракты.md, раздел 8. Экраны только показывают эти данные
// и отправляют действия во ViewModel. Тексты из контента (задания, причины, соседи) здесь уже готовые
// строки с подстановками; тексты интерфейса («Купить», «Сначала план») — в strings.xml.
// Для @Preview — готовые примеры в PreviewSamples.

// --- Общее ---

/** Верхняя панель: баланс, копилка, текущая цель. */
data class TopBarUi(
    val balance: Int,
    val savings: Int,
    val goalName: String?,                   // null — «Выбери мечту»
    val goalCost: Int?,
    val goalProgressPct: Int,
)

/** Общая панель снизу после любого действия: «Баланс 115 → 90, Настроение +20, почему». */
data class FeedbackUi(
    val balanceBefore: Int,
    val balanceAfter: Int,
    val savingsBefore: Int,
    val savingsAfter: Int,
    val statChanges: Map<Stat, Int>,         // только ненулевые
    val reasonText: String,
)

data class TaskCardUi(val taskId: String, val title: String, val topic: Topic, val reward: Int)

data class NeighborTipUi(val characterId: String, val name: String, val text: String)

// --- Дом ---

data class HomeUiState(
    val top: TopBarUi,
    val petName: String,
    val look: PetLook,
    val stage: Stage,
    val mood: PetMood,
    val stats: PetStats,
    val petReasonText: String,               // карточка по тапу на кота: «почему сейчас так»
    val activeTask: TaskCardUi?,             // карточка над меню
    val weekNumber: Int,
    val needWarning: Boolean,                // нужное на этой неделе не куплено
    val closeWarning: CloseWeekWarning?,     // что спросить перед «Завершить неделю»; null — завершать сразу
    val weekClosed: Boolean = false,         // неделя завершена — открыть «Итог недели», потом onWeekSummaryOpened()
)

/** Мягкое предупреждение перед «Завершить неделю» (docs/01-функционал.md, раздел 4.5). Завершить можно всё равно. */
enum class CloseWeekWarning {
    NO_PLAN,                                 // «Плана на эту неделю нет. Составим?» — «К плану» / «Завершить так»
    NEED_NOT_BOUGHT,                         // «На этой неделе {petName} пока без еды. Всё равно завершить?»
}

// --- План ---

data class PlanUiState(
    val top: TopBarUi,
    val available: Int,
    val need: Int,
    val want: Int,
    val save: Int,
    val unallocated: Int,
    val needHint: Int?,                      // «коту нужно 50», null — не показывать
    val canIncrease: Boolean,                // false — «+» неактивен, рядом пояснение
    val confirmed: Boolean,
    val fact: PlanFactUi?,                   // после подтверждения — план и факт по ходу недели
    val neighborTips: List<NeighborTipUi>,
)

data class PlanFactUi(val spentNeed: Int, val spentWant: Int, val saved: Int, val earned: Int)

// --- Магазин ---

data class ShopUiState(
    val top: TopBarUi,
    val planConfirmed: Boolean,              // false — вместо «Купить» кнопка «Сначала план»
    val need: List<ShopItemUi>,
    val want: List<ShopItemUi>,
    val wishlist: List<ShopItemUi>,          // «Хочу потом»
    val planLeftNeed: Int?,                  // напоминание о плане в карточке товара
    val planLeftWant: Int?,
    val feedback: FeedbackUi? = null,        // панель «что изменилось» после покупки или «Хочу потом»
    val rejection: Rejection? = null,        // «Сначала план», «Нужно 30, а есть 20. Что можно сделать?»
)

data class ShopItemUi(
    val id: String,
    val name: String,
    val price: Int,
    val part: Part,
    val effects: Map<Stat, Int>,             // «Сытость +40»
    val affordable: Boolean,
)

// --- Копилка ---

data class SavingsUiState(
    val top: TopBarUi,
    val planConfirmed: Boolean,              // false — «Пополнить» ведёт на план
    val total: Int,
    val goal: GoalUi?,                       // null — «Выбери мечту»
    val goals: List<GoalUi>,
    val weeksLeft: Int?,                     // «примерно N недель», null — не показывать
    val canBuyGoal: Boolean,
    val wallet: Int,
    val feedback: FeedbackUi? = null,        // панель «что изменилось» после пополнения, снятия, мечты
    val rejection: Rejection? = null,        // почему не получилось: нет плана, не хватает, «уже есть»
)

/** Предпросмотр снятия «было → станет» — до подтверждения (п. 2.5.7 ТЗ). */
data class WithdrawPreviewUi(
    val amount: Int,
    val savedBefore: Int,
    val savedAfter: Int,
    val weeksBefore: Int?,                   // null — срок не показываем
    val weeksAfter: Int?,
)

data class GoalUi(
    val id: String,
    val name: String,
    val cost: Int,
    val saved: Int,
    val cells: Int,                          // полоска из клеточек по 10
    val filledCells: Int,
    val bought: Boolean = false,             // «Уже есть»: купленную мечту выбрать снова нельзя
)

// --- Задания ---

data class TasksUiState(
    val top: TopBarUi,
    val open: List<TaskCardUi>,
    val done: List<TaskCardUi>,
    val jobsLeft: Int,                       // подработок осталось на этой неделе
    val jobReward: Int,
    val feedback: FeedbackUi? = null,        // панель после подработки
    val rejection: Rejection? = null,        // лимит подработок на неделю
)

data class TaskPlayUiState(
    val title: String,
    val step: Int,                           // «Шаг 1 из 2»
    val steps: Int,
    val intro: String?,                      // только в первом шаге
    val situation: String,
    val wallet: Int?,                        // учебный кошелёк, null — не показывать
    val savings: Int?,                       // учебная копилка, null — не показывать
    val options: List<OptionUi>,
    val result: ChoiceResultUi?,             // null — выбора ещё не было
    val neighborLine: NeighborTipUi?,
    val finished: Boolean = false,           // задание пройдено — панель с наградой, потом назад
    val feedback: FeedbackUi? = null,        // награда на настоящий баланс или «ещё раз, без монет»
)

data class OptionUi(val id: String, val label: String)

data class ChoiceResultUi(
    val consequence: String?,
    val effects: Map<EffectKey, Int>,        // «Кошелёк −30», «Настроение +15»
    val feedback: String,
    val canRetry: Boolean,                   // кнопка «Попробовать иначе»
    val recovery: String?,
    val followupButtons: List<String>,       // диалог варианта-заглушки
    val followupPrompt: String? = null,      // вопрос диалога: «Что делаем?»
)

// --- Итог недели ---

data class WeekSummaryUiState(
    val weekNumber: Int,
    val planConfirmed: Boolean,              // false — «плана не было», иконка «План удался» не горит
    val rows: List<PlanFactRowUi>,
    val earned: Int,
    val icons: List<GrowthIconUi>,
    val gpToNextStage: Int?,                 // null — уже Взрослый
    val stageUp: Stage?,                     // не null — праздник «Пончик теперь Подросток!»
    val pet: PetChangeUi,
    val goal: GoalUi?,
    val recoveryHint: String?,
)

data class PlanFactRowUi(val part: Part, val planned: Int, val actual: Int)

data class GrowthIconUi(val icon: GrowthIcon, val pct: Int)   // 100 — горит, 50 — наполовину, 0 — нет

/** Что стало с котом за неделю и почему — строка по pet_reasons.week из texts.json. */
data class PetChangeUi(val before: PetStats, val after: PetStats, val mood: PetMood, val reasonText: String)

// --- Простые экраны: договариваем с Саней (раздел 8 контрактов) ---

/** Старт: «Играть» и ссылка «Для взрослых» (docs/01-функционал.md, раздел 1). */
data class StartUiState(
    val loading: Boolean = true,             // сохранение ещё читается — «Играть» неактивна
    val hasProfile: Boolean = false,         // «Играть» → Дом; нет профиля — онбординг
    val demo: Boolean = false,               // открыт демо-профиль (демо не закрыли до перезапуска): онбординг — в демо
)

/**
 * Онбординг с созданием питомца (docs/01-функционал.md, раздел 1). Экраны идут списком из onboarding.json,
 * что показывать — по [kind]; все тексты, подсказки и ошибки — оттуда же, уже с подстановками.
 */
data class OnboardingUiState(
    val kind: ScreenKind,                    // INFO, PLAYER_NAME, LOOK, PET_NAME, DIFFICULTY
    val step: Int,                           // «2 из 8»
    val steps: Int,
    val text: String,
    val bullets: List<BulletUi>,             // «Нужно · Хочу · Отложить»
    val finePrint: String?,
    val input: NameInputUi?,                 // только PLAYER_NAME и PET_NAME
    val look: PetLook,                       // выбранный вариант в сетке 3 × 3 — на LOOK и в превью кота
    val breeds: List<PetOptionUi>,
    val colors: List<PetOptionUi>,
    val choices: List<DifficultyChoiceUi>,   // только DIFFICULTY: выбор сразу ведёт дальше
    val button: String?,                     // null — кнопки нет (на DIFFICULTY выбирают вариант)
    val petReply: String?,                   // реплика кота, когда имя подходит
    val canSkip: Boolean,                    // «Пропустить» — экраны «три решения» и «как растёт»
    val canGoBack: Boolean,
    val creating: Boolean = false,           // профиль сохраняется — кнопки неактивны
    val finished: Boolean = false,           // профиль создан — открыть задание [firstTaskId]
    val firstTaskId: String? = null,
)

data class BulletUi(val term: String, val text: String)

/** Поле имени: текст, подсказки и ошибка — из onboarding.json. [error] появляется после нажатия «Дальше». */
data class NameInputUi(
    val value: String,
    val placeholder: String,
    val hint: String?,
    val maxLength: Int,
    val error: String?,
)

data class DifficultyChoiceUi(val difficulty: Difficulty, val label: String, val selected: Boolean)

data class PetOptionUi(val id: String, val name: String)

/** Прогресс (📈): стадия, задания по темам, цели, недели, словарь — docs/01-функционал.md, раздел 6. */
data class ProgressUiState(
    val stage: Stage,
    val totalGp: Int,
    val gpToNextStage: Int?,
    val tasksByTopic: Map<Topic, TopicProgressUi>,
    val goal: GoalUi?,
    val boughtGoals: List<GoalUi>,
    val lastWeek: WeekSummaryUiState?,
    val weeks: List<WeekLineUi>,
)

data class TopicProgressUi(val done: Int, val total: Int)

data class WeekLineUi(val weekNumber: Int, val gp: Int, val stageAfter: Stage)

/**
 * Демо-режим (docs/01-функционал.md, раздел 9): отдельный демо-профиль, «Ускорить», «Сбросить демо», «Выйти из демо».
 * Плашку «Демо» с кнопками экран показывает поверх Дома, пока [active].
 */
data class DemoUiState(
    val active: Boolean = false,             // открыт демо-профиль
    val stage: Stage? = null,                // null — демо-профиля ещё нет
    val canSpeedUp: Boolean = false,         // кот ещё не Взрослый и прогон не идёт
    val running: Boolean = false,            // «Ускорить» считает — кнопки неактивны
    val weeks: List<WeekSummaryUiState> = emptyList(),   // итоги недель прогона: показать по очереди, потом onWeeksShown()
    val navigate: DemoNav? = null,           // куда перейти после кнопки; перешли — onNavigated()
)

enum class DemoNav {
    HOME,                                    // демо-профиль есть — продолжаем
    ONBOARDING,                              // демо-профиля нет или сбросили — онбординг в демо
    START,                                   // вышли из демо — на старт, там профиль ребёнка
}

/** Раздел взрослого после барьера — docs/01-функционал.md, раздел 8. Без оценок ребёнка. */
data class AdultUiState(
    val weeksPlayed: Int,
    val stage: Stage,
    val tasksDone: Int,
    val tasksTotal: Int,
    val topicsDone: List<Topic>,             // пройденные темы
    val goalsBought: Int,
    val difficulty: Difficulty,
    val hasChildProfile: Boolean,            // есть что сбрасывать
)
