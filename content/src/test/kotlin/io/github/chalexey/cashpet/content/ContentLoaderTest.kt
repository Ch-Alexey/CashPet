package io.github.chalexey.cashpet.content

import io.github.chalexey.cashpet.core.model.Difficulty
import io.github.chalexey.cashpet.core.model.EffectKey
import io.github.chalexey.cashpet.core.model.FeedbackReason
import io.github.chalexey.cashpet.core.model.Outcome
import io.github.chalexey.cashpet.core.model.Part
import io.github.chalexey.cashpet.core.model.PetReason
import io.github.chalexey.cashpet.core.model.Stat
import io.github.chalexey.cashpet.core.model.Topic
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

// Образцы в test/resources/samples — формат, который Ярик переносит в src/main/resources/content
class ContentLoaderTest {

    private fun sample(name: String): String =
        javaClass.getResource("/samples/$name")?.readText() ?: error("Нет образца $name")

    private val content = ContentLoader(::sample).load()

    @Test
    fun `экономика читается`() {
        assertEquals(100, content.economy.startBudget)
        assertEquals(50, content.economy.needMin)
        assertEquals(20, content.economy.tolerance[Difficulty.EASY])
    }

    @Test
    fun `магазин — 8 нужных и 8 желаемых, эффекты по показателям`() {
        val items = content.catalog.items
        assertEquals(8, items.count { it.part == Part.NEED })
        assertEquals(8, items.count { it.part == Part.WANT })

        val fish = content.catalog.item("food_fish")!!
        assertEquals(45, fish.price)
        assertEquals(mapOf(Stat.SATIETY to 50, Stat.MOOD to 5), fish.effects)
        assertTrue(content.catalog.item("want_cap")!!.accessory)
    }

    @Test
    fun `товары только для заданий не попадают на витрину`() {
        assertEquals(null, content.catalog.item("want_comic"))
    }

    @Test
    fun `цели, подработки и сведения о заданиях — в каталоге`() {
        assertEquals(listOf(60, 100, 150), content.catalog.goals.map { it.cost })
        assertEquals(10, content.catalog.job("help_home")!!.reward)

        val meta = content.catalog.task("1.1")!!
        assertEquals(Topic.PURCHASES, meta.topic)
        assertEquals(1, meta.unlockWeek)
        assertEquals(15, meta.rewardCoins)
    }

    @Test
    fun `задание читается целиком`() {
        val task = content.tasks.single { it.id == "1.1" }
        val step = task.steps.single()
        assertEquals(30, step.wallet)

        val ball = step.options.single { it.id == "ball" }
        assertEquals(Outcome.RETRY, ball.outcome)
        assertEquals(-30, ball.effects[EffectKey.WALLET])
        assertTrue(ball.recovery!!.isNotBlank())

        val half = step.options.single { it.id == "half" }
        assertTrue(half.blocked)
        assertEquals(2, half.followup!!.buttons.size)

        val hardBall = task.hardVariant!!.steps.getValue("step1").options.getValue("ball")
        assertEquals(-15, hardBall.effects!![EffectKey.WALLET])
    }

    @Test
    fun `вводное задание без компетенции`() {
        val intro = content.tasks.single { it.id == "0" }
        assertEquals(Topic.INTRO, intro.topic)
        assertEquals(null, intro.competence)
    }

    @Test
    fun `коты 3 × 3`() {
        assertEquals(3, content.pets.breeds.size)
        assertEquals(3, content.pets.colors.size)
    }

    @Test
    fun `фраза есть для каждой причины`() {
        assertEquals(FeedbackReason.entries.toSet(), content.texts.feedback.keys)
        assertEquals(PetReason.entries.toSet(), content.texts.petWeek.keys)
        assertEquals(PetReason.entries.toSet(), content.texts.petNow.keys)
        assertEquals("Покупка: {item}. Это нужное", content.texts.feedback[FeedbackReason.BOUGHT_NEED])
    }

    @Test
    fun `сломанный JSON — ошибка с именем файла`() {
        val loader = ContentLoader { name -> if (name == ContentLoader.SHOP) "{ \"items\": [" else sample(name) }

        val error = assertThrows<ContentException> { loader.load() }

        assertEquals(ContentLoader.SHOP, error.file)
    }

    @Test
    fun `нет файла — ошибка с именем файла`() {
        val loader = ContentLoader { name -> if (name == ContentLoader.JOBS) error("нет") else sample(name) }

        val error = assertThrows<ContentException> { loader.load() }

        assertEquals(ContentLoader.JOBS, error.file)
    }

    @Test
    fun `неизвестная причина в texts_json — ошибка с ключом`() {
        val broken = sample(ContentLoader.TEXTS).replace("\"bought_need\"", "\"bougth_need\"")
        val loader = ContentLoader { name -> if (name == ContentLoader.TEXTS) broken else sample(name) }

        val error = assertThrows<ContentException> { loader.load() }

        assertEquals(ContentLoader.TEXTS, error.file)
        assertTrue("bougth_need" in error.message!!)
    }
}
