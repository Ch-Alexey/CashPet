package io.github.chalexey.cashpet.core.task

import io.github.chalexey.cashpet.core.model.Difficulty
import io.github.chalexey.cashpet.core.model.EffectKey
import io.github.chalexey.cashpet.core.model.Outcome

/**
 * Прохождение одного задания (docs/08-контракты.md, раздел 5). Без Android: экран через ViewModel
 * показывает [step], передаёт выбор в [choose], по кнопкам вызывает [retry] или [next].
 *
 * Учебный кошелёк и копилка живут только внутри сессии и настоящий баланс не трогают.
 * В движок уходит только CompleteTask(task.id, [finalOutcome]).
 *
 * [values] — подстановки кроме {petName}, например {playerName}; ключи без фигурных скобок.
 */
class TaskSession(
    val task: TaskDef,
    val difficulty: Difficulty,
    val petName: String,
    values: Map<String, String> = emptyMap(),
) {
    private val values = values + ("petName" to petName)

    private var index = 0
    private var wallet: Int? = null
    private var savings: Int? = null
    private var chosen: Choice.Consequence? = null
    private var finished = false

    init {
        require(task.steps.isNotEmpty()) { "Задание ${task.id} без шагов" }
        resetWallet()
    }

    /** Текущий шаг — с подстановками и заменами «посложнее». После выбора кошелёк уже с его последствием. */
    val step: StepView
        get() {
            val def = currentStep()
            return StepView(
                number = index + 1,
                count = task.steps.size,
                intro = if (index == 0) task.intro?.let(::fill) else null,
                situation = fill(def.situation),
                options = def.options.map { OptionView(it.id, fill(it.label)) },
                wallet = wallet,
                savings = savings,
                chosen = chosen,
            )
        }

    /** Задание пройдено: последний шаг выбран и вызван [next]. */
    val isFinished: Boolean get() = finished

    /** Исход последнего шага — для CompleteTask. Правильность на монеты и рост не влияет. */
    val finalOutcome: Outcome
        get() {
            check(finished) { "Задание ${task.id} ещё не пройдено" }
            return chosen!!.outcome
        }

    fun choose(optionId: String): Choice {
        check(!finished) { "Задание ${task.id} уже пройдено" }
        check(chosen == null) { "Выбор в шаге уже сделан — сначала retry() или next()" }
        val option = currentStep().options.firstOrNull { it.id == optionId }
            ?: throw IllegalArgumentException("В шаге ${currentStep().id} нет варианта $optionId")

        // Вариант-заглушка ничего не меняет: ребёнок читает объяснение и выбирает снова
        if (option.blocked) {
            return Choice.Blocked(
                feedback = fill(option.feedback),
                followup = option.followup?.let { Followup(fill(it.prompt), it.buttons.map(::fill)) },
            )
        }

        wallet = wallet?.plus(option.effects[EffectKey.WALLET] ?: 0)
        savings = savings?.plus(option.effects[EffectKey.SAVINGS] ?: 0)
        val consequence = Choice.Consequence(
            effects = option.effects,
            consequence = option.consequence?.let(::fill),
            feedback = fill(option.feedback),
            outcome = requireNotNull(option.outcome) { "У варианта ${option.id} нет outcome" },
            recovery = option.recovery?.let(::fill),
            wallet = wallet,
            savings = savings,
        )
        chosen = consequence
        return consequence
    }

    /** «Попробовать иначе»: тот же шаг заново, кошелёк и копилка шага — к исходным. */
    fun retry() {
        check(!finished) { "Задание ${task.id} уже пройдено" }
        check(chosen != null) { "Выбора ещё не было — нечего повторять" }
        chosen = null
        resetWallet()
    }

    /** К следующему шагу. false — шагов больше нет, задание пройдено. */
    fun next(): Boolean {
        check(!finished) { "Задание ${task.id} уже пройдено" }
        check(chosen != null) { "Сначала выбор в шаге" }
        if (index == task.steps.lastIndex) {
            finished = true
            return false
        }
        index++
        chosen = null
        resetWallet()
        return true
    }

    // У каждого шага свой учебный кошелёк: следующий шаг начинается с собственного wallet
    private fun resetWallet() {
        wallet = currentStep().wallet
        savings = currentStep().savings
    }

    private fun currentStep(): StepDef {
        val def = task.steps[index]
        if (difficulty != Difficulty.HARD) return def
        val override = task.hardVariant?.steps?.get(def.id) ?: return def
        return def.copy(
            situation = override.situation ?: def.situation,
            options = def.options.map { option ->
                val o = override.options[option.id] ?: return@map option
                option.copy(
                    label = o.label ?: option.label,
                    effects = o.effects ?: option.effects,
                    consequence = o.consequence ?: option.consequence,
                    feedback = o.feedback ?: option.feedback,
                )
            },
        )
    }

    private fun fill(text: String): String =
        values.entries.fold(text) { acc, (key, value) -> acc.replace("{$key}", value) }
}

/** Что показывает экран задания. */
data class StepView(
    val number: Int,                          // с 1
    val count: Int,
    val intro: String?,                       // только у первого шага
    val situation: String,
    val options: List<OptionView>,
    val wallet: Int?,                         // null — у шага нет учебного кошелька
    val savings: Int?,
    val chosen: Choice.Consequence?,          // null — выбора ещё не было
)

data class OptionView(val id: String, val label: String)

sealed interface Choice {
    /** Последствие выбора: числа, объяснение и путь восстановления. */
    data class Consequence(
        val effects: Map<EffectKey, Int>,
        val consequence: String?,
        val feedback: String,
        val outcome: Outcome,
        val recovery: String?,
        val wallet: Int?,                     // учебный кошелёк после выбора
        val savings: Int?,
    ) : Choice {
        /** «Попробовать иначе» — только у неудачного исхода (docs/07-формат-заданий.md). */
        val canRetry: Boolean get() = outcome == Outcome.RETRY
    }

    /** Вариант-заглушка: не засчитывается, ведёт в диалог. */
    data class Blocked(val feedback: String, val followup: Followup?) : Choice
}
