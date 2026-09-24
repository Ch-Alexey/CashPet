package io.github.chalexey.cashpet.content

import io.github.chalexey.cashpet.core.engine.GameEngine
import io.github.chalexey.cashpet.core.model.Difficulty
import io.github.chalexey.cashpet.core.model.FeedbackReason
import io.github.chalexey.cashpet.core.model.PetReason
import io.github.chalexey.cashpet.core.model.EffectKey
import io.github.chalexey.cashpet.core.model.PetLook
import io.github.chalexey.cashpet.core.model.Profile
import io.github.chalexey.cashpet.core.model.Stage
import io.github.chalexey.cashpet.core.sim.Autoplay
import io.github.chalexey.cashpet.core.sim.ReasonableStrategy
import io.github.chalexey.cashpet.core.sim.SpenderStrategy
import io.github.chalexey.cashpet.core.model.Outcome
import io.github.chalexey.cashpet.core.model.Part
import io.github.chalexey.cashpet.core.model.Topic
import io.github.chalexey.cashpet.core.task.TaskDef
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Тесты-сторожа настоящего контента (src/main/resources/content) — docs/07-формат-заданий.md, раздел «Тесты-сторожа».
 * Красный тест — в сообщении правило и id задания. Чинится правкой JSON, а не теста.
 */
class ContentGuardTest {
    private val content = ContentLoader().load()
    private val tasks = content.tasks

    private val allowedSubstitutions = setOf(
        "playerName", "petName", "goal", "cost", "saved", "left", "weeks", "weeksAfter", "amount", "needMin", "item",
    )

    /** Все тексты задания, которые увидит ребёнок, — с пометкой, откуда. */
    private fun TaskDef.texts(): List<Pair<String, String>> = buildList {
        add("$id.title" to title)
        intro?.let { add("$id.intro" to it) }
        neighborLines.forEach { add("$id.neighbor" to it.text) }
        for (step in steps) {
            add("$id.${step.id}.situation" to step.situation)
            for (o in step.options) {
                add("$id.${step.id}.${o.id}.label" to o.label)
                o.consequence?.let { add("$id.${step.id}.${o.id}.consequence" to it) }
                add("$id.${step.id}.${o.id}.feedback" to o.feedback)
                o.recovery?.let { add("$id.${step.id}.${o.id}.recovery" to it) }
                o.followup?.let { f -> add("$id.${step.id}.${o.id}.followup" to f.prompt); f.buttons.forEach { add("$id.${step.id}.${o.id}.button" to it) } }
            }
        }
        hardVariant?.steps?.forEach { (stepId, step) ->
            step.situation?.let { add("$id.hard.$stepId.situation" to it) }
            step.options.forEach { (optId, o) ->
                listOfNotNull(o.label, o.consequence, o.feedback).forEach { add("$id.hard.$stepId.$optId" to it) }
            }
        }
    }

    private val allTexts: List<Pair<String, String>> by lazy {
        tasks.flatMap { it.texts() } +
            content.texts.feedback.map { (k, v) -> "texts.feedback.$k" to v } +
            content.texts.petWeek.map { (k, v) -> "texts.week.$k" to v } +
            content.texts.petNow.map { (k, v) -> "texts.now.$k" to v } +
            content.onboarding.flatMap { sc ->
                listOfNotNull(sc.text, sc.finePrint, sc.button, sc.petReply).map { "onboarding.${sc.id}" to it } +
                    sc.bullets.map { "onboarding.${sc.id}.bullet" to it.text } +
                    sc.choices.values.map { "onboarding.${sc.id}.choice" to it } +
                    listOfNotNull(sc.input?.placeholder, sc.input?.hint, sc.input?.emptyError, sc.input?.tooLongError)
                        .map { "onboarding.${sc.id}.input" to it }
            } +
            content.glossary.map { "glossary.${it.id}" to it.definition } +
            content.neighbors.flatMap { n -> n.planTips.map { "neighbors.${n.id}.plan_tips" to it } }
    }

    private fun violations(check: (String, String) -> String?): List<String> =
        allTexts.mapNotNull { (where, text) -> check(where, text)?.let { "$where: $it — «$text»" } }

    private fun assertNone(rule: String, found: List<String>) =
        assertTrue(found.isEmpty(), "$rule:\n" + found.joinToString("\n"))

    @Test
    fun `заданий не меньше 6, по каждой обязательной теме — не меньше 2`() {
        val counted = tasks.filter { it.topic != Topic.INTRO }
        assertTrue(counted.size >= 6, "заданий ${counted.size}")
        for (topic in listOf(Topic.BUDGET, Topic.SAVINGS, Topic.PURCHASES)) {
            assertTrue(counted.count { it.topic == topic } >= 2, "тема $topic")
        }
    }

    @Test
    fun `id заданий, шагов и вариантов уникальны`() {
        assertEquals(tasks.size, tasks.map { it.id }.toSet().size, "повторяются id заданий")
        for (t in tasks) {
            assertEquals(t.steps.size, t.steps.map { it.id }.toSet().size, "${t.id}: повторяются id шагов")
            for (s in t.steps) assertEquals(s.options.size, s.options.map { it.id }.toSet().size, "${t.id}.${s.id}: повторяются id вариантов")
        }
    }

    @Test
    fun `в шаге 2–3 варианта, хотя бы один удачный, у заглушки есть followup`() {
        for (t in tasks) for (s in t.steps) {
            val real = s.options.filterNot { it.blocked }
            assertTrue(real.size in 2..3, "${t.id}.${s.id}: вариантов ${real.size}")
            assertTrue(real.any { it.outcome == Outcome.GOOD }, "${t.id}.${s.id}: нет варианта good")
            s.options.filter { it.blocked }.forEach { assertTrue(it.followup != null, "${t.id}.${s.id}.${it.id}: заглушка без followup") }
        }
    }

    @Test
    fun `у каждого варианта разбор, у частичных и неудачных — путь восстановления`() {
        for (t in tasks) for (s in t.steps) for (o in s.options) {
            assertTrue(o.feedback.isNotBlank(), "${t.id}.${s.id}.${o.id}: нет feedback")
            if (!o.blocked) assertTrue(o.outcome != null, "${t.id}.${s.id}.${o.id}: нет outcome")
            if (o.outcome == Outcome.OK || o.outcome == Outcome.RETRY) {
                assertTrue(!o.recovery.isNullOrBlank(), "${t.id}.${s.id}.${o.id}: нет recovery")
            }
        }
    }

    @Test
    fun `награда больше нуля, суммы кратны 5 и не больше 999`() {
        for (t in tasks) {
            assertTrue(t.reward.coins > 0 && t.reward.coins % 5 == 0, "${t.id}: награда ${t.reward.coins}")
            for (s in t.steps) {
                listOfNotNull(s.wallet, s.savings).forEach { assertTrue(it % 5 == 0 && it in 0..999, "${t.id}.${s.id}: $it") }
                for (o in s.options) for ((key, v) in o.effects) {
                    if (key == EffectKey.WALLET || key == EffectKey.SAVINGS) {
                        assertTrue(v % 5 == 0 && kotlin.math.abs(v) <= 999, "${t.id}.${s.id}.${o.id}: $key $v")
                    }
                }
            }
        }
        for (item in content.catalog.items) assertTrue(item.price > 0 && item.price % 5 == 0, "${item.id}: цена ${item.price}")
        for (goal in content.catalog.goals) assertTrue(goal.cost % 5 == 0, "${goal.id}: ${goal.cost}")
    }

    @Test
    fun `учебный кошелёк и копилка шага не уходят в минус`() {
        for (t in tasks) for (s in t.steps) for (o in s.options) {
            val wallet = (s.wallet ?: 0) + (o.effects[EffectKey.WALLET] ?: 0)
            val savings = (s.savings ?: 0) + (o.effects[EffectKey.SAVINGS] ?: 0)
            assertTrue(wallet >= 0, "${t.id}.${s.id}.${o.id}: кошелёк $wallet")
            assertTrue(savings >= 0, "${t.id}.${s.id}.${o.id}: копилка $savings")
        }
    }

    @Test
    fun `компетенция 1–6, соседи есть в neighbors_json`() {
        for (t in tasks) {
            if (t.topic != Topic.INTRO) assertTrue(t.competence in 1..6, "${t.id}: компетенция ${t.competence}")
            t.neighborLines.forEach { assertTrue(content.neighbor(it.character) != null, "${t.id}: нет соседа ${it.character}") }
        }
    }

    @Test
    fun `соседи — постоянный состав, советов на Плане у Торопливого и Осторожного поровну`() {
        // Состав фиксирован решением команды (раздел 22 рамок): новых персонажей без решения не добавляем
        assertEquals(listOf("Торопливый", "Осторожный", "Рыжехвостик", "Длинноус"), content.neighbors.map { it.name })
        // Совет не подсвечивается как верный: если у одного советов больше, он выглядит «главным»
        val hasty = content.neighbor("toroplivy")!!.planTips
        val careful = content.neighbor("ostorozhny")!!.planTips
        assertEquals(hasty.size, careful.size, "советы на Плане: Торопливый ${hasty.size}, Осторожный ${careful.size}")
    }

    @Test
    fun `hard_variant ссылается только на существующие шаги и варианты`() {
        for (t in tasks) t.hardVariant?.steps?.forEach { (stepId, override) ->
            val step = t.steps.firstOrNull { it.id == stepId }
            assertTrue(step != null, "${t.id}: нет шага $stepId")
            override.options.keys.forEach { optId -> assertTrue(step!!.options.any { it.id == optId }, "${t.id}.$stepId: нет варианта $optId") }
        }
    }

    @Test
    fun `подстановки — только из закрытого списка, без выражений`() = assertNone("Недопустимые подстановки", violations { _, text ->
        Regex("""\{([^}]*)}""").findAll(text).map { it.groupValues[1] }.filter { it !in allowedSubstitutions }
            .toList().takeIf { it.isNotEmpty() }?.joinToString(prefix = "{", postfix = "}")
    })

    @Test
    fun `предложение не длиннее 12 слов`() = assertNone("Слишком длинные предложения", violations { _, text ->
        text.split(Regex("""(?<=[.!?])\s+""")).firstOrNull { s -> Regex("""[\p{L}\d{}]+""").findAll(s).count() > 12 }
            ?.let { "в предложении больше 12 слов" }
    })

    @Test
    fun `числа перед словом монет кратны 5`() = assertNone("Суммы не кратны 5", violations { _, text ->
        Regex("""(\d+)\s+монет""").findAll(text).map { it.groupValues[1].toInt() }.firstOrNull { it % 5 != 0 }?.let { "сумма $it" }
    })

    @Test
    fun `в разборе неудачного варианта нет восклицаний`() {
        for (t in tasks) for (s in t.steps) for (o in s.options) if (o.outcome == Outcome.RETRY) {
            assertTrue('!' !in o.feedback, "${t.id}.${s.id}.${o.id}: «!» в разборе неудачного выбора")
        }
    }

    @Test
    fun `имя кота — только в именительном`() = assertNone("Имя кота не в именительном падеже", violations { _, text ->
        when {
            Regex("""(?iU)\b(у|о|с|к|про|для|без)\s+\{petName}""").containsMatchIn(text) -> "предлог перед {petName}"
            // (?U) — чтобы \b понимал кириллицу. Прошедшее время и краткие формы сразу после имени выдают род: «Пончик доволен», «Мурка поела»
            Regex("""(?U)\{petName}\s+\p{L}+(л|ла|ли|лся|лась|ен|на)\b""").containsMatchIn(text) -> "род или прошедшее время после {petName}"
            else -> null
        }
    })

    @Test
    fun `в магазине не меньше 8 позиций, из них не меньше 4 нужных и 4 желаемых`() {
        // ТЗ, таблица 2.6: «не менее 8 позиций двух типов»; рамки, раздел 19: минимум 4 и 4.
        // Пластыря на витрине нет до непредвиденного расхода (после 29.09) — сейчас 7 нужных и 8 желаемых
        val items = content.catalog.items
        assertTrue(items.size >= 8, "позиций ${items.size}")
        assertTrue(items.count { it.part == Part.NEED } >= 4, "нужных ${items.count { it.part == Part.NEED }}")
        assertTrue(items.count { it.part == Part.WANT } >= 4, "желаемых ${items.count { it.part == Part.WANT }}")
    }

    @Test
    fun `на настоящем контенте разумная игра — Взрослый к 4-й неделе, транжира — не выше Подростка`() {
        // Те же выводы, что в таблицах docs/02-экономика.md: если цены или доходы в JSON поменялись — видно здесь
        val engine = GameEngine(content.economy, content.catalog)
        val start = engine.newGame(Profile("Тест", Difficulty.HARD), "Пончик", PetLook("fluffy", "ginger"))
        val goals = listOf("goal_house", "goal_bike")
        val reasonable = Autoplay.weeks(start, engine, ReasonableStrategy(content.economy, content.catalog, goals), weeks = 5)
        assertEquals(Stage.ADULT, reasonable[3].pet.stage, "разумная игра к 4-й неделе")
        val spender = Autoplay.weeks(start, engine, SpenderStrategy(content.economy, content.catalog, goals), weeks = 5)
        assertTrue(spender.last().pet.stage <= Stage.TEEN, "транжира за 5 недель")
        assertTrue((reasonable + spender).all { it.balance >= 0 && it.savings.total >= 0 })
    }

    @Test
    fun `фраза есть для каждой причины — иначе экран останется без текста`() {
        // Новая причина в движке (как WISHLIST_REMOVED) без фразы в texts.json — красный тест, а не пустая панель
        assertEquals(FeedbackReason.entries.toSet(), content.texts.feedback.keys, "texts.json → feedback_reasons")
        assertEquals(PetReason.entries.toSet(), content.texts.petWeek.keys, "texts.json → pet_reasons.week")
        assertEquals(PetReason.entries.toSet(), content.texts.petNow.keys, "texts.json → pet_reasons.now")
    }

    @Test
    fun `неравенства баланса из раздела 19 рамок — на настоящей экономике`() {
        // Те же проверки, что в tools/econ_sim.py и в таблице docs/02-экономика.md, но по настоящим JSON
        val e = content.economy
        val wants = content.catalog.items.filter { it.part == Part.WANT }.map { it.price }
        val need = e.needMin                                                         // O — обязательное за неделю
        val earned = content.catalog.tasks.filter { it.unlockWeek == 1 }.sumOf { it.rewardCoins } +
            e.jobsPerWeek * e.jobReward                                              // заработок первой недели
        val income = e.pocketMoney + earned                                          // I
        val free = income - need                                                     // S
        val goalAvg = content.catalog.goals.map { it.cost }.average()

        // 8б: свободные монеты самой «богатой» недели разумной игры — с переносом остатка
        val engine = GameEngine(e, content.catalog)
        val start = engine.newGame(Profile("Тест", Difficulty.HARD), "Пончик", PetLook("fluffy", "ginger"))
        val weeks = Autoplay.weeks(start, engine, ReasonableStrategy(e, content.catalog), weeks = 5)
        val freeMax = weeks.mapIndexed { i, s ->
            val r = s.history.last()
            (if (i == 0) start else weeks[i - 1]).week.available + r.earned - r.factNeed
        }.max()

        val broken = buildList {
            if (need * 10 !in income * 4..income * 6) add("1. O ≈ 0,5 · I: $need из $income")
            if (wants.min() >= free) add("2. P_min < S: ${wants.min()} и $free")
            if (wants.sum() < 3 * free) add("3. ΣP ≥ 3 · S: ${wants.sum()} и ${3 * free}")
            if (2 * wants.max() <= free) add("4. P_max > 0,5 · S: ${wants.max()} и $free")
            if (kotlin.math.abs(goalAvg - 1.75 * free) > 25) add("5. C_средняя ≈ 3,5 · 0,5 · S: $goalAvg и ${1.75 * free}")
            // 6. U ≤ 0,8 · S — непредвиденный расход после 29.09, пластыря на витрине нет
            if (earned * 10 !in income * 4..income * 6) add("7. заработок ≈ 0,4–0,6 · I: $earned из $income")
            if (e.pocketMoney < need) add("8а. нет тупика, карманные ≥ O: ${e.pocketMoney} и $need")
            if (wants.sum() < 2 * freeMax) add("8б. перенос, ΣP ≥ 2 · S_max: ${wants.sum()} и ${2 * freeMax}")
        }
        assertTrue(broken.isEmpty(), "Нарушены неравенства (docs/02-экономика.md):\n" + broken.joinToString("\n"))
    }

    @Test
    fun `онбординг — все обязательные шаги по одному разу, последний ведёт в открытое задание`() {
        val screens = content.onboarding
        assertEquals(screens.size, screens.map { it.id }.toSet().size, "повторяются id экранов")
        for (kind in listOf(ScreenKind.PLAYER_NAME, ScreenKind.LOOK, ScreenKind.PET_NAME, ScreenKind.DIFFICULTY)) {
            assertEquals(1, screens.count { it.kind == kind }, "экран $kind")
        }
        screens.filter { it.kind == ScreenKind.PLAYER_NAME || it.kind == ScreenKind.PET_NAME }.forEach {
            assertTrue(it.input != null && it.input!!.maxLength > 0, "${it.id}: нет поля ввода")
        }
        assertEquals(setOf("easy", "hard"), screens.single { it.kind == ScreenKind.DIFFICULTY }.choices.keys)
        // Игровое имя спрашиваем раньше, чем подставляем, а имя кота — раньше, чем о нём говорим
        val playerAt = screens.indexOfFirst { it.kind == ScreenKind.PLAYER_NAME }
        val petAt = screens.indexOfFirst { it.kind == ScreenKind.PET_NAME }
        screens.forEachIndexed { i, sc ->
            if ("{playerName}" in sc.text) assertTrue(i > playerAt, "${sc.id}: {playerName} до ввода имени")
            if ("{petName}" in sc.text) assertTrue(i > petAt, "${sc.id}: {petName} до ввода имени кота")
        }
        val first = screens.last().next
        assertTrue(tasks.any { it.id == first && it.unlockWeek == 1 }, "после онбординга — задание «$first», его нет или оно закрыто")
    }

    @Test
    fun `словарь — термины уникальны и объяснены`() {
        assertEquals(content.glossary.size, content.glossary.map { it.id }.toSet().size, "повторяются id терминов")
        content.glossary.forEach { assertTrue(it.term.isNotBlank() && it.definition.isNotBlank(), it.id) }
        // Слова из трёх решений онбординга обязаны быть в словаре
        val terms = content.glossary.map { it.term }.toSet()
        content.onboarding.flatMap { it.bullets }.forEach { assertTrue(it.term in terms, "нет в словаре: ${it.term}") }
    }
}
