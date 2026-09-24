package io.github.chalexey.cashpet.core.engine

import io.github.chalexey.cashpet.core.TestContent
import io.github.chalexey.cashpet.core.model.Difficulty
import io.github.chalexey.cashpet.core.model.FeedbackReason
import io.github.chalexey.cashpet.core.model.GameState
import io.github.chalexey.cashpet.core.model.Outcome
import io.github.chalexey.cashpet.core.model.PetMood
import io.github.chalexey.cashpet.core.model.PetReason
import io.github.chalexey.cashpet.core.model.PetStats
import io.github.chalexey.cashpet.core.model.Plan
import io.github.chalexey.cashpet.core.model.Slot
import io.github.chalexey.cashpet.core.model.Stage
import io.github.chalexey.cashpet.core.model.Stat
import io.github.chalexey.cashpet.core.model.TxSource
import io.github.chalexey.cashpet.core.model.Way
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

private val engine = TestContent.engine

private fun GameState.okResult(action: Action): Result.Ok {
    val result = engine.apply(this, action)
    check(result is Result.Ok) { "Ожидали Ok для $action, а получили $result" }
    return result
}

private fun GameState.act(vararg actions: Action): GameState = actions.fold(this) { s, a -> s.okResult(a).state }

private fun GameState.reject(action: Action): Rejection {
    val result = engine.apply(this, action)
    check(result is Result.Rejected) { "Ожидали отказ для $action, а получили $result" }
    return result.reason
}

/** Новая игра с подтверждённым планом. */
private fun planned(plan: Plan = Plan(50, 25, 25), difficulty: Difficulty = Difficulty.HARD) =
    TestContent.newGame(difficulty).act(Action.SetPlan(plan), Action.ConfirmPlan)

class GameEngineTest {

    @Test
    fun `новая игра — неделя 1, стартовый бюджет с источником`() {
        val s = TestContent.newGame()
        assertEquals(100, s.balance)
        assertEquals(1, s.week.number)
        assertEquals(100, s.week.available)
        assertEquals(PetStats(70, 70, 70), s.pet.stats)
        assertEquals(TxSource.StartBudget, s.transactions.single().source)
    }

    @Nested
    inner class План {
        @Test
        fun `план больше доступного — отказ с превышением`() {
            assertEquals(Rejection.PlanExceedsBudget(15), TestContent.newGame().reject(Action.SetPlan(Plan(50, 40, 25))))
        }

        @Test
        fun `суммы не кратные 5 и отрицательные — отказ`() {
            val s = TestContent.newGame()
            assertEquals(Rejection.InvalidAmount, s.reject(Action.SetPlan(Plan(52, 20, 20))))
            assertEquals(Rejection.InvalidAmount, s.reject(Action.SetPlan(Plan(-5, 20, 20))))
        }

        @Test
        fun `черновик сохраняется, после подтверждения план не меняется`() {
            val draft = TestContent.newGame().act(Action.SetPlan(Plan(50, 30, 20)))
            assertEquals(Plan(50, 30, 20), draft.week.plan)
            assertFalse(draft.week.planConfirmed)
            val confirmed = draft.act(Action.ConfirmPlan)
            assertTrue(confirmed.week.planConfirmed)
            assertEquals(Rejection.PlanAlreadyConfirmed, confirmed.reject(Action.SetPlan(Plan(50, 50, 0))))
            assertEquals(Rejection.PlanAlreadyConfirmed, confirmed.reject(Action.ConfirmPlan))
        }

        @Test
        fun `остаток плана разрешён`() {
            assertEquals(Plan(50, 10, 10), TestContent.newGame().act(Action.SetPlan(Plan(50, 10, 10))).week.plan)
        }
    }

    @Nested
    inner class Покупки {
        @Test
        fun `до подтверждения плана покупать и откладывать нельзя`() {
            val s = TestContent.newGame()
            assertEquals(Rejection.PlanNotConfirmed, s.reject(Action.Buy("food_basic")))
            assertEquals(Rejection.PlanNotConfirmed, s.reject(Action.Deposit(10)))
        }

        @Test
        fun `покупка уменьшает баланс, пишется в факт и меняет кота`() {
            val r = planned().okResult(Action.Buy("food_basic"))
            assertEquals(70, r.state.balance)
            assertEquals(30, r.state.week.spentNeed)
            assertEquals(40, r.state.week.foodPoints)
            assertEquals(100, r.state.pet.stats.satiety)                 // 70 + 40, не выше 100
            assertEquals(mapOf(Stat.SATIETY to 30), r.feedback.statChanges)
            assertEquals(FeedbackReason.BOUGHT_NEED, r.feedback.reason)
            assertEquals(-30, r.state.transactions.last().walletDelta)
        }

        @Test
        fun `для N считается эффект до ограничения в 100`() {
            val s = planned().act(Action.Buy("food_vitamins"), Action.Buy("food_vitamins"))
            assertEquals(40, s.week.foodPoints)
            assertEquals(100, s.pet.stats.satiety)
        }

        @Test
        fun `нехватка монет — отказ с суммой и вариантами`() {
            val s = planned(Plan(50, 50, 0)).act(Action.Buy("want_jacket"))   // 100 − 55 = 45
            val reason = s.reject(Action.Buy("want_scooter")) as Rejection.NotEnoughMoney
            assertEquals(35, reason.missing)
            assertTrue(Way.CHEAPER_ITEM in reason.options)
            assertTrue(Way.WISHLIST in reason.options)
            assertTrue(Way.DO_TASK in reason.options)
            assertTrue(Way.DO_JOB in reason.options)
            assertFalse(Way.TAKE_FROM_SAVINGS in reason.options)               // копилка пуста
            assertEquals(45, s.balance)                                        // баланс не тронут
        }

        @Test
        fun `взять из копилки предлагается, только если её хватает`() {
            val s = planned(Plan(50, 0, 50)).act(Action.Deposit(50), Action.Buy("want_bed"))   // кошелёк 10, копилка 50
            val reason = s.reject(Action.Buy("want_ball")) as Rejection.NotEnoughMoney
            assertEquals(5, reason.missing)
            assertTrue(Way.TAKE_FROM_SAVINGS in reason.options)
        }

        @Test
        fun `Хочу потом не меняет баланс и факт, покупка убирает из списка`() {
            val s = planned().act(Action.AddToWishlist("want_ball"))
            assertEquals(listOf("want_ball"), s.wishlist)
            assertEquals(100, s.balance)
            assertEquals(0, s.week.spentWant)
            assertEquals(emptyList<String>(), s.act(Action.RemoveFromWishlist("want_ball")).wishlist)
            assertEquals(emptyList<String>(), s.act(Action.Buy("want_ball")).wishlist)
        }

        @Test
        fun `неизвестный товар — отказ`() {
            assertEquals(Rejection.UnknownId("nope"), planned().reject(Action.Buy("nope")))
        }
    }

    @Nested
    inner class Копилка {
        @Test
        fun `взнос и снятие двигают монеты между кошельком и копилкой`() {
            val s = planned().act(Action.Deposit(25))
            assertEquals(75, s.balance)
            assertEquals(25, s.savings.total)
            assertEquals(-25, s.transactions.last().walletDelta)
            assertEquals(25, s.transactions.last().savingsDelta)
            val w = s.act(Action.Withdraw(10))
            assertEquals(85, w.balance)
            assertEquals(15, w.savings.total)
        }

        @Test
        fun `снять больше, чем есть, — отказ`() {
            assertEquals(Rejection.NotEnoughSavings(5), planned().act(Action.Deposit(20)).reject(Action.Withdraw(25)))
        }

        @Test
        fun `не хватает на взнос — «взять из копилки» не предлагается`() {
            val s = planned(Plan(50, 0, 50)).act(Action.Deposit(60))          // кошелёк 40, копилка 60
            val reason = s.reject(Action.Deposit(60)) as Rejection.NotEnoughMoney
            assertEquals(20, reason.missing)
            assertFalse(Way.TAKE_FROM_SAVINGS in reason.options)
        }

        @Test
        fun `купленную мечту выбрать снова нельзя, другую — можно`() {
            val s = TestContent.newGame().act(Action.SetPlan(Plan(30, 0, 70)), Action.ConfirmPlan,
                Action.ChooseGoal("goal_scratcher"), Action.Deposit(60), Action.BuyGoal)
            assertEquals(Rejection.GoalAlreadyBought, s.reject(Action.ChooseGoal("goal_scratcher")))
            assertEquals("goal_house", s.act(Action.ChooseGoal("goal_house")).savings.activeGoalId)
        }

        @Test
        fun `сумма взноса не кратная 5 — отказ`() {
            assertEquals(Rejection.InvalidAmount, planned().reject(Action.Deposit(12)))
            assertEquals(Rejection.InvalidAmount, planned().reject(Action.Deposit(0)))
        }

        @Test
        fun `купить мечту — из копилки, кошелёк не меняется`() {
            val s = TestContent.newGame().act(Action.SetPlan(Plan(30, 0, 70)), Action.ConfirmPlan,
                Action.ChooseGoal("goal_scratcher"), Action.Deposit(50))
            assertEquals(Rejection.GoalNotReached(10), s.reject(Action.BuyGoal))
            val bought = s.act(Action.Deposit(20)).okResult(Action.BuyGoal)
            assertEquals(10, bought.state.savings.total)                    // 70 − 60
            assertEquals(30, bought.state.balance)                          // кошелёк не тронут
            assertEquals(FeedbackReason.GOAL_BOUGHT, bought.feedback.reason)
            assertNull(bought.state.savings.activeGoalId)
            assertEquals(listOf("goal_scratcher"), bought.state.savings.boughtGoalIds)
            assertTrue(bought.state.week.goalBought)
            assertEquals(0, bought.state.week.withdrawn)                    // покупка мечты — не снятие
            assertEquals(0, bought.state.week.spentWant)                    // и не факт «Хочу»
        }

        @Test
        fun `без цели купить мечту нельзя`() {
            assertEquals(Rejection.NoActiveGoal, planned().reject(Action.BuyGoal))
        }

        @Test
        fun `срок — по среднему за неделю, а не по отдельным взносам`() {
            // Три взноса по 5 за неделю — это 15 в неделю: Когтеточка 60 → 4 недели, а не 12
            val s = planned().act(Action.ChooseGoal("goal_scratcher"), Action.Deposit(5), Action.Deposit(5), Action.Deposit(5))
            assertEquals(3, engine.weeksToGoal(s))                           // осталось 45 при 15 в неделю
            val nextWeek = s.act(Action.CloseWeek)
            assertEquals(listOf(15), nextWeek.savings.recentWeeklySaved)
            assertEquals(3, engine.weeksToGoal(nextWeek))
        }

        @Test
        fun `срок не показываем, пока откладывать нечего`() {
            assertNull(engine.weeksToGoal(planned().act(Action.ChooseGoal("goal_house"))))
            assertNull(engine.weeksToGoal(planned()))                        // нет цели
        }

        @Test
        fun `предпросмотр снятия совпадает с тем, что будет после снятия`() {
            val s = planned().act(Action.ChooseGoal("goal_house"), Action.Deposit(25), Action.CloseWeek)
                .act(Action.SetPlan(Plan(50, 30, 30)), Action.ConfirmPlan, Action.Deposit(25), Action.CloseWeek)
            val preview = engine.previewWithdraw(s, 20)
            val after = s.act(Action.Withdraw(20))
            assertEquals(after.savings.total, preview.savedAfter)
            assertEquals(engine.weeksToGoal(after), preview.weeksAfter)
            assertEquals(engine.weeksToGoal(s), preview.weeksBefore)
            assertTrue(preview.weeksAfter!! >= preview.weeksBefore!!)
        }
    }

    @Nested
    inner class Заработок {
        @Test
        fun `задание даёт монеты один раз, повтор — без монет`() {
            val first = planned().okResult(Action.CompleteTask("1.1", Outcome.RETRY))
            assertEquals(115, first.state.balance)
            assertEquals(15, first.state.week.earned)
            assertEquals(FeedbackReason.TASK_REWARD, first.feedback.reason)
            val repeat = first.state.okResult(Action.CompleteTask("1.1", Outcome.GOOD))
            assertEquals(115, repeat.state.balance)
            assertEquals(FeedbackReason.TASK_REPEAT, repeat.feedback.reason)
            assertEquals(Outcome.RETRY, repeat.state.tasks.getValue("1.1").firstOutcome)
        }

        @Test
        fun `правильность ответа не влияет на монеты`() {
            val good = planned().act(Action.CompleteTask("1.1", Outcome.GOOD)).balance
            val retry = planned().act(Action.CompleteTask("1.1", Outcome.RETRY)).balance
            assertEquals(good, retry)
        }

        @Test
        fun `задание следующей недели закрыто, в демо — открыто`() {
            assertEquals(Rejection.TaskClosed, planned().reject(Action.CompleteTask("4.2", Outcome.GOOD)))
            val demo = TestContent.newGame(slot = Slot.DEMO)
            assertTrue(engine.isTaskOpen(demo, "4.2"))
        }

        @Test
        fun `подработок не больше лимита за неделю, на новой неделе лимит снова`() {
            val s = planned().act(Action.DoJob("help_home"), Action.DoJob("help_home"))
            assertEquals(120, s.balance)
            assertEquals(Rejection.JobLimitReached, s.reject(Action.DoJob("help_home")))
            assertEquals(1, s.act(Action.CloseWeek, Action.DoJob("help_home")).week.jobsDone)
        }
    }

    @Nested
    inner class КонецНедели {
        @Test
        fun `идеальная неделя — 100 очков роста`() {
            val s = planned().act(Action.Buy("food_basic"), Action.Buy("care_shampoo"), Action.Buy("want_bow"), Action.Deposit(25))
            val r = s.okResult(Action.CloseWeek).feedback.weekResult!!
            assertEquals(100, r.nPct)
            assertEquals(100, r.mPct)
            assertEquals(100, r.sPct)
            assertEquals(100, r.gp)
        }

        @Test
        fun `неделя без плана не даёт «План удался»`() {
            val r = TestContent.newGame().okResult(Action.CloseWeek).feedback.weekResult!!
            assertFalse(r.planConfirmed)
            assertEquals(0, r.mPct)
            assertEquals(0, r.gp)
            assertEquals(Plan(), r.plan)
        }

        @Test
        fun `перерасход сверх допуска снижает M, в допуске — нет`() {
            // Хочу: план 10, потрачено 35, допуск 10 → D = 15 → M = 1 − 15/20 = 0,25
            val hard = planned(Plan(50, 10, 10)).act(Action.Buy("want_cap"), Action.Deposit(10))
            assertEquals(25, hard.okResult(Action.CloseWeek).feedback.weekResult!!.mPct)
            // «Попроще»: допуск 20 → D = 5 → M = 1 − 5/40 = 0,875
            val easy = planned(Plan(50, 10, 10), Difficulty.EASY).act(Action.Buy("want_cap"), Action.Deposit(10))
            assertEquals(88, easy.okResult(Action.CloseWeek).feedback.weekResult!!.mPct)
            // Потратить меньше плана — не ошибка
            val less = planned(Plan(50, 40, 10)).act(Action.Deposit(10))
            assertEquals(100, less.okResult(Action.CloseWeek).feedback.weekResult!!.mPct)
        }

        @Test
        fun `копилка меньше плана — S частично, мечта куплена — S = 1`() {
            val half = planned(Plan(50, 0, 40)).act(Action.Deposit(20))
            assertEquals(50, half.okResult(Action.CloseWeek).feedback.weekResult!!.sPct)
            // Отложено 60 при плане 70 — без покупки мечты было бы 86 %, с покупкой — 100 %
            val goal = TestContent.newGame().act(Action.SetPlan(Plan(30, 0, 70)), Action.ConfirmPlan,
                Action.ChooseGoal("goal_scratcher"), Action.Deposit(60), Action.BuyGoal)
            assertEquals(100, goal.okResult(Action.CloseWeek).feedback.weekResult!!.sPct)
        }

        @Test
        fun `N — половина за еду и половина за уход`() {
            val onlyFood = planned().act(Action.Buy("food_basic"))
            assertEquals(50, onlyFood.okResult(Action.CloseWeek).feedback.weekResult!!.nPct)
        }

        @Test
        fun `показатели падают в конце недели, половина вверх, не ниже 25`() {
            val s = planned()   // 70 / 70 / 70
            val after = s.act(Action.CloseWeek).pet.stats
            assertEquals(PetStats(satiety = 53, care = 56, mood = 49), after)   // 52,5 → 53
            var low = s
            repeat(6) { low = low.act(Action.CloseWeek) }
            assertEquals(PetStats(25, 25, 25), low.pet.stats)
        }

        @Test
        fun `новая неделя — остаток плюс карманные, заработок уходит в доступное`() {
            val s = planned().act(Action.DoJob("help_home"), Action.Buy("food_basic"))   // 100 + 10 − 30 = 80
            val next = s.okResult(Action.CloseWeek)
            assertEquals(140, next.state.balance)
            assertEquals(2, next.state.week.number)
            assertEquals(140, next.state.week.available)
            assertFalse(next.state.week.planConfirmed)
            assertEquals(0, next.state.week.jobsDone)
            assertEquals(TxSource.PocketMoney, next.state.transactions.last().source)
            assertEquals(FeedbackReason.WEEK_CLOSED, next.feedback.reason)
        }

        @Test
        fun `стадии по порогам и никогда не понижаются`() {
            var s = TestContent.newGame()
            fun goodWeek() {
                s = s.act(Action.SetPlan(Plan(50, 0, 10)), Action.ConfirmPlan,
                    Action.Buy("food_basic"), Action.Buy("care_shampoo"), Action.Deposit(10), Action.CloseWeek)
            }
            goodWeek(); assertEquals(Stage.BABY, s.pet.stage)     // 100
            goodWeek(); assertEquals(Stage.TEEN, s.pet.stage)     // 200
            s = s.act(Action.CloseWeek)                            // неделя без плана: 0 очков
            assertEquals(Stage.TEEN, s.pet.stage)
            assertEquals(200, s.pet.totalGp)
            goodWeek(); goodWeek(); assertEquals(Stage.ADULT, s.pet.stage)   // 400
        }

        @Test
        fun `причина для кота на итоге недели — по покупкам`() {
            assertEquals(PetReason.HUNGRY, planned().okResult(Action.CloseWeek).feedback.weekResult!!.petReason)
            val fed = planned().act(Action.Buy("food_basic"))
            assertEquals(PetReason.UNKEMPT, fed.okResult(Action.CloseWeek).feedback.weekResult!!.petReason)
            val all = planned().act(Action.Buy("food_basic"), Action.Buy("care_shampoo"), Action.Buy("want_bow"))
            assertEquals(PetReason.JOYFUL, all.okResult(Action.CloseWeek).feedback.weekResult!!.petReason)
        }
    }

    @Nested
    inner class Кот {
        @Test
        fun `общее состояние по порогам`() {
            assertEquals(PetMood.HAPPY, engine.petMood(PetStats(75, 75, 75)))
            assertEquals(PetMood.CALM, engine.petMood(PetStats(70, 70, 70)))
            assertEquals(PetMood.SAD, engine.petMood(PetStats(40, 40, 40)))
        }

        @Test
        fun `в начале недели карточка кота не пугает «без еды»`() {
            val start = TestContent.newGame().act(Action.CloseWeek).pet.stats   // 53 / 56 / 49
            assertEquals(PetReason.WELL, engine.petReasonNow(start))
            assertEquals(PetReason.HUNGRY, engine.petReasonNow(PetStats(satiety = 45, care = 80, mood = 80)))
            assertEquals(PetReason.UNKEMPT, engine.petReasonNow(PetStats(satiety = 80, care = 45, mood = 80)))
        }
    }
}
