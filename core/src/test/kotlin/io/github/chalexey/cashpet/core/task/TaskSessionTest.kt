package io.github.chalexey.cashpet.core.task

import io.github.chalexey.cashpet.core.model.Difficulty
import io.github.chalexey.cashpet.core.model.EffectKey
import io.github.chalexey.cashpet.core.model.Outcome
import io.github.chalexey.cashpet.core.model.Topic
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TaskSessionTest {

    // Задание 1.1 из образца content/src/test/resources/samples/tasks.json
    private val bowlAndBall = TaskDef(
        id = "1.1", module = 1, title = "Пустая миска и большой мяч", topic = Topic.PURCHASES,
        competence = 2, unlockWeek = 1, reward = Reward(15),
        intro = "{petName} смотрит на пустую миску.",
        steps = listOf(
            StepDef(
                id = "step1", wallet = 30,
                situation = "{petName} ждёт ужина. У тебя 30 монет.",
                options = listOf(
                    OptionDef("food", "Купить корм", mapOf(EffectKey.WALLET to -30, EffectKey.SATIETY to 40),
                        consequence = "Миска полная, {petName} радуется", feedback = "Еда — это нужное",
                        outcome = Outcome.GOOD),
                    OptionDef("ball", "Купить мяч", mapOf(EffectKey.WALLET to -30, EffectKey.MOOD to 15),
                        feedback = "Мяч — это желаемое", outcome = Outcome.RETRY,
                        recovery = "Мяч можно добавить в «Хочу потом»"),
                    OptionDef("half", "Взять по половинке", feedback = "Половинки не продаются", blocked = true,
                        followup = Followup("Что делаем, {playerName}?", listOf("Купить корм", "Мяч — в «Хочу потом»"))),
                ),
            ),
        ),
        hardVariant = HardVariant(
            mapOf(
                "step1" to StepOverride(
                    situation = "Мяч со скидкой: 15 монет.",
                    options = mapOf("ball" to OptionOverride(effects = mapOf(EffectKey.WALLET to -15, EffectKey.MOOD to 15))),
                ),
            ),
        ),
    )

    // Два шага, как в задании 3.3: у первого учебные кошелёк и копилка, у второго — свой кошелёк
    private val twoSteps = TaskDef(
        id = "3.3", module = 3, title = "Снять или подождать?", topic = Topic.SAVINGS,
        competence = 4, unlockWeek = 3, reward = Reward(25),
        steps = listOf(
            StepDef(
                id = "decide", wallet = 10, savings = 70, situation = "Комикс за 20 монет",
                options = listOf(
                    OptionDef("keep", "Не снимать", feedback = "Копилка цела", outcome = Outcome.GOOD),
                    OptionDef("withdraw", "Снять 20 и купить",
                        mapOf(EffectKey.SAVINGS to -20, EffectKey.MOOD to 10),
                        feedback = "Мечта отодвинулась", outcome = Outcome.RETRY, recovery = "Можно докопить"),
                ),
            ),
            StepDef(
                id = "reserve", wallet = 30, situation = "Сколько оставить в запасе?",
                options = listOf(
                    OptionDef("ten", "10 монет", mapOf(EffectKey.WALLET to -10), feedback = "Хороший запас",
                        outcome = Outcome.GOOD),
                    OptionDef("none", "Ничего", feedback = "Запаса нет", outcome = Outcome.OK,
                        recovery = "В следующий раз можно оставить 10"),
                ),
            ),
        ),
    )

    private fun session(task: TaskDef = bowlAndBall, difficulty: Difficulty = Difficulty.EASY) =
        TaskSession(task, difficulty, petName = "Пончик", values = mapOf("playerName" to "Лис"))

    @Test
    fun `первый шаг — с подстановками, вступлением и учебным кошельком`() {
        val step = session().step

        assertEquals(1, step.number)
        assertEquals(1, step.count)
        assertEquals("Пончик смотрит на пустую миску.", step.intro)
        assertEquals("Пончик ждёт ужина. У тебя 30 монет.", step.situation)
        assertEquals(listOf("food", "ball", "half"), step.options.map { it.id })
        assertEquals(30, step.wallet)
        assertNull(step.chosen)
    }

    @Test
    fun `удачный выбор — последствие в числах, без «Попробовать иначе»`() {
        val s = session()

        val choice = s.choose("food") as Choice.Consequence

        assertEquals(Outcome.GOOD, choice.outcome)
        assertEquals(0, choice.wallet)
        assertEquals(40, choice.effects[EffectKey.SATIETY])
        assertEquals("Миска полная, Пончик радуется", choice.consequence)
        assertFalse(choice.canRetry)
        assertEquals(choice, s.step.chosen)
    }

    @Test
    fun `неудачный выбор — путь восстановления и «Попробовать иначе» с исходным кошельком`() {
        val s = session()

        val choice = s.choose("ball") as Choice.Consequence
        assertTrue(choice.canRetry)
        assertEquals("Мяч можно добавить в «Хочу потом»", choice.recovery)
        assertEquals(0, s.step.wallet)

        s.retry()
        assertEquals(30, s.step.wallet)
        assertNull(s.step.chosen)

        assertEquals(Outcome.GOOD, (s.choose("food") as Choice.Consequence).outcome)
    }

    @Test
    fun `вариант-заглушка ничего не меняет и не засчитывается`() {
        val s = session()

        val choice = s.choose("half") as Choice.Blocked

        assertEquals("Что делаем, Лис?", choice.followup!!.prompt)
        assertEquals(30, s.step.wallet)
        assertNull(s.step.chosen)
        assertThrows<IllegalStateException> { s.next() }
        s.choose("food")                                   // выбрать можно снова
    }

    @Test
    fun `посложнее — заменяются ситуация и эффекты, остальное как было`() {
        val s = session(difficulty = Difficulty.HARD)

        assertEquals("Мяч со скидкой: 15 монет.", s.step.situation)
        assertEquals("Купить мяч", s.step.options.single { it.id == "ball" }.label)
        assertEquals(15, (s.choose("ball") as Choice.Consequence).wallet)
    }

    @Test
    fun `попроще замены «посложнее» не применяются`() {
        val s = session(difficulty = Difficulty.EASY)

        assertEquals(0, (s.choose("ball") as Choice.Consequence).wallet)
    }

    @Test
    fun `два шага — у каждого свой кошелёк, итог — исход последнего шага`() {
        val s = session(twoSteps)

        val first = s.choose("withdraw") as Choice.Consequence
        assertEquals(50, first.savings)
        assertEquals(10, first.wallet)

        assertTrue(s.next())
        assertEquals(2, s.step.number)
        assertNull(s.step.intro)
        assertEquals(30, s.step.wallet)
        assertNull(s.step.savings)

        s.choose("none")
        assertFalse(s.next())
        assertTrue(s.isFinished)
        assertEquals(Outcome.OK, s.finalOutcome)
    }

    @Test
    fun `итога нет, пока задание не пройдено`() {
        val s = session()
        s.choose("food")

        assertThrows<IllegalStateException> { s.finalOutcome }
    }

    @Test
    fun `второй выбор в том же шаге без retry — ошибка`() {
        val s = session()
        s.choose("food")

        assertThrows<IllegalStateException> { s.choose("ball") }
    }

    @Test
    fun `незнакомый вариант — ошибка`() {
        assertThrows<IllegalArgumentException> { session().choose("nope") }
    }

    @Test
    fun `после прохождения задание не продолжается`() {
        val s = session()
        s.choose("food")
        s.next()

        assertThrows<IllegalStateException> { s.choose("food") }
        assertThrows<IllegalStateException> { s.retry() }
    }
}
