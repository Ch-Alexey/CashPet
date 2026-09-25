package io.github.chalexey.cashpet.app.vm

import io.github.chalexey.cashpet.app.FakeGameRepository
import io.github.chalexey.cashpet.app.store.GameStore
import io.github.chalexey.cashpet.content.ContentLoader
import io.github.chalexey.cashpet.core.engine.Action
import io.github.chalexey.cashpet.core.engine.GameEngine
import io.github.chalexey.cashpet.core.engine.Rejection
import io.github.chalexey.cashpet.core.model.Difficulty
import io.github.chalexey.cashpet.core.model.FeedbackReason
import io.github.chalexey.cashpet.core.model.Part
import io.github.chalexey.cashpet.core.model.PetLook
import io.github.chalexey.cashpet.core.model.Plan
import io.github.chalexey.cashpet.core.model.Profile
import io.github.chalexey.cashpet.core.model.Slot
import io.github.chalexey.cashpet.core.model.Stat
import io.github.chalexey.cashpet.core.model.Way
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShopViewModelTest {

    private val content = ContentLoader().load()
    private val engine = GameEngine(content.economy, content.catalog)
    private val store = GameStore(engine, FakeGameRepository())
    private lateinit var vm: ShopViewModel

    private val shop get() = vm.uiState.value!!
    private val game get() = store.state.value!!

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        vm = ShopViewModel(content, store)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun newGame() = runBlocking {
        store.newGame(Profile("Лис", Difficulty.EASY, Slot.CHILD), "Пончик", PetLook("fluffy", "ginger"))
    }

    private fun act(vararg actions: Action) = runBlocking { actions.forEach { store.dispatch(it) } }

    private fun text(reason: FeedbackReason, item: String) =
        content.texts.feedback.getValue(reason).replace("{item}", item).replace("{petName}", "Пончик")

    @Test
    fun `профиля нет — магазина нет`() {
        assertNull(vm.uiState.value)
    }

    @Test
    fun `витрина — нужное и желаемое из каталога, до плана «Сначала план»`() {
        newGame()

        assertEquals(content.catalog.items.filter { it.part == Part.NEED }.map { it.id }, shop.need.map { it.id })
        assertEquals(content.catalog.items.filter { it.part == Part.WANT }.map { it.id }, shop.want.map { it.id })
        assertEquals(mapOf(Stat.SATIETY to 40), shop.need.single { it.id == "food_basic" }.effects)
        assertTrue(shop.wishlist.isEmpty())
        assertFalse(shop.planConfirmed)
        assertNull(shop.planLeftNeed)
        assertNull(shop.planLeftWant)
    }

    @Test
    fun `покупка до плана — отказ, баланс прежний`() {
        newGame()

        vm.buy("food_basic")

        assertEquals(Rejection.PlanNotConfirmed, shop.rejection)
        assertEquals(100, shop.top.balance)
    }

    @Test
    fun `покупка — панель «было → стало», эффект на кота, остаток по плану`() {
        newGame()
        act(Action.SetPlan(Plan(50, 25, 25)), Action.ConfirmPlan)

        vm.buy("food_basic")

        val f = shop.feedback!!
        assertEquals(100 to 70, f.balanceBefore to f.balanceAfter)
        assertEquals(mapOf(Stat.SATIETY to 30), f.statChanges)          // 70 + 40, но не больше 100
        assertEquals(text(FeedbackReason.BOUGHT_NEED, "Пакет корма"), f.reasonText)
        assertEquals(20, shop.planLeftNeed)
        assertEquals(25, shop.planLeftWant)
    }

    @Test
    fun `дороже кошелька — товар помечен, покупка — отказ с вариантами`() {
        newGame()
        act(
            Action.SetPlan(Plan(60, 40, 0)), Action.ConfirmPlan,
            Action.Buy("food_premium"),                                   // 100 → 40
        )
        assertFalse(shop.want.single { it.id == "want_scooter" }.affordable)
        assertTrue(shop.want.single { it.id == "want_bed" }.affordable)

        vm.buy("want_scooter")

        val rejection = shop.rejection as Rejection.NotEnoughMoney
        assertEquals(40, rejection.missing)
        assertTrue(Way.WISHLIST in rejection.options)
        assertEquals(40, shop.top.balance)
    }

    @Test
    fun `«взять из копилки» — снять недостающее и купить, одна панель на оба шага`() {
        newGame()
        act(
            Action.SetPlan(Plan(60, 15, 25)), Action.ConfirmPlan,
            Action.Deposit(25), Action.Buy("food_premium"),               // кошелёк 15, копилка 25
        )
        vm.buy("want_bed")
        assertTrue(Way.TAKE_FROM_SAVINGS in (shop.rejection as Rejection.NotEnoughMoney).options)

        vm.buyWithSavings("want_bed")

        val f = shop.feedback!!
        assertEquals(15 to 0, f.balanceBefore to f.balanceAfter)
        assertEquals(25 to 0, f.savingsBefore to f.savingsAfter)
        assertEquals(text(FeedbackReason.BOUGHT_WANT, "Лежанка"), f.reasonText)
        assertNull(shop.rejection)
        assertEquals(40, game.week.spentWant)
    }

    @Test
    fun `«взять из копилки» до плана — отказ, копилку не трогаем`() {
        newGame()

        vm.buyWithSavings("want_bed")

        assertEquals(Rejection.PlanNotConfirmed, shop.rejection)
        assertEquals(0, game.week.withdrawn)
    }

    @Test
    fun `«Хочу потом» — в список и из списка, покупка убирает из списка`() {
        newGame()

        vm.addToWishlist("want_scooter")
        assertEquals(listOf("want_scooter"), shop.wishlist.map { it.id })
        assertEquals(text(FeedbackReason.WISHLIST_ADDED, "Самокат"), shop.feedback!!.reasonText)

        vm.removeFromWishlist("want_scooter")
        assertTrue(shop.wishlist.isEmpty())
        assertEquals(text(FeedbackReason.WISHLIST_REMOVED, "Самокат"), shop.feedback!!.reasonText)

        vm.addToWishlist("want_ball")
        act(Action.SetPlan(Plan(50, 25, 25)), Action.ConfirmPlan)
        vm.buy("want_ball")
        assertTrue(shop.wishlist.isEmpty())
    }

    @Test
    fun `сообщение показано — панель убирается`() {
        newGame()
        vm.buy("food_basic")

        vm.onMessageShown()

        assertNull(shop.feedback)
        assertNull(shop.rejection)
    }
}
