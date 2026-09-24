package io.github.chalexey.cashpet.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Состояние игры — docs/08-контракты.md, раздел 3. Деньги — Int, в монетах.
// У каждого поля значение по умолчанию: так старое сохранение читается после обновления приложения.

@Serializable
data class PetLook(val breedId: String = "", val colorId: String = "")

@Serializable
data class PetStats(val satiety: Int = 70, val care: Int = 70, val mood: Int = 70)

@Serializable
data class Pet(
    val name: String = "",
    val look: PetLook = PetLook(),
    val stats: PetStats = PetStats(),
    val totalGp: Int = 0,
    val stage: Stage = Stage.BABY,
)

@Serializable
data class Profile(
    val playerName: String = "",
    val difficulty: Difficulty = Difficulty.EASY,
    val slot: Slot = Slot.CHILD,
)

@Serializable
data class Plan(val need: Int = 0, val want: Int = 0, val save: Int = 0) {
    val total: Int get() = need + want + save
}

@Serializable
data class Savings(
    val total: Int = 0,                              // общий счётчик копилки
    val activeGoalId: String? = null,                // цель — только отметка с ценой
    val recentWeeklySaved: List<Int> = emptyList(),  // «отложено» за последние 3 закрытые недели — для срока
    val boughtGoalIds: List<String> = emptyList(),
)

@Serializable
data class Week(
    val number: Int = 1,
    val available: Int = 0,                          // доступно на начало недели
    val plan: Plan? = null,                          // черновик или подтверждённый; null — ещё не трогали
    val planConfirmed: Boolean = false,
    val spentNeed: Int = 0,
    val spentWant: Int = 0,
    val deposited: Int = 0,
    val withdrawn: Int = 0,
    val earned: Int = 0,                             // задания и подработки этой недели
    val foodPoints: Int = 0,                         // сумма эффектов покупок до ограничения в 100 — для N
    val carePoints: Int = 0,
    val jobsDone: Int = 0,
    val goalBought: Boolean = false,
)

@Serializable
data class WeekResult(
    val number: Int,
    val plan: Plan,
    val planConfirmed: Boolean,
    val factNeed: Int,
    val factWant: Int,
    val factSaved: Int,                              // max(0, пополнения − снятия)
    val earned: Int,
    val nPct: Int,                                   // 0..100 — для иконок
    val mPct: Int,
    val sPct: Int,
    val gp: Int,
    val totalGp: Int,
    val stageBefore: Stage,
    val stageAfter: Stage,
    val statsBefore: PetStats,                       // до падения в конце недели
    val statsAfter: PetStats,                        // после падения
    val petReason: PetReason,
)

// Короткие имена в JSON сохранения: без них там полное имя класса с пакетом, и переименование
// пакета или класса сломало бы все сохранения. Имена ниже не менять — это формат сохранения
@Serializable
sealed interface TxSource {
    @Serializable @SerialName("start_budget") data object StartBudget : TxSource
    @Serializable @SerialName("pocket_money") data object PocketMoney : TxSource
    @Serializable @SerialName("task_reward") data class TaskReward(val taskId: String) : TxSource
    @Serializable @SerialName("job") data class Job(val jobId: String) : TxSource
    @Serializable @SerialName("purchase") data class Purchase(val itemId: String) : TxSource
    @Serializable @SerialName("deposit") data object Deposit : TxSource
    @Serializable @SerialName("withdraw") data object Withdraw : TxSource
    @Serializable @SerialName("goal_purchase") data class GoalPurchase(val goalId: String) : TxSource  // из копилки, кошелёк не меняется
}

@Serializable
data class Transaction(
    val week: Int,
    val source: TxSource,
    val walletDelta: Int,                            // у взноса −25, у покупки мечты 0
    val savingsDelta: Int,                           // у взноса +25, у покупки мечты −100
    val balanceAfter: Int,
    val savingsAfter: Int,
)

@Serializable
data class TaskRecord(val taskId: String, val topic: Topic, val firstOutcome: Outcome, val week: Int)

@Serializable
data class GameState(
    val profile: Profile = Profile(),
    val pet: Pet = Pet(),
    val balance: Int = 0,                            // кошелёк, никогда < 0
    val savings: Savings = Savings(),
    val week: Week = Week(),
    val history: List<WeekResult> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val wishlist: List<String> = emptyList(),        // «Хочу потом», id товаров
    val tasks: Map<String, TaskRecord> = emptyMap(),
)
