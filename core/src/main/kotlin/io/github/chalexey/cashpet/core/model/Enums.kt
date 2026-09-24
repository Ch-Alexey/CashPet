package io.github.chalexey.cashpet.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Перечисления из docs/08-контракты.md, раздел 3. @SerialName — как значения пишутся в JSON

@Serializable
enum class Part {                                         // Нужно · Хочу · Отложить
    @SerialName("need") NEED,
    @SerialName("want") WANT,
    @SerialName("save") SAVE,
}

@Serializable
enum class Difficulty {
    @SerialName("easy") EASY,                              // попроще
    @SerialName("hard") HARD,                              // посложнее
}

@Serializable
enum class Stage {                                        // Малыш · Подросток · Взрослый
    @SerialName("baby") BABY,
    @SerialName("teen") TEEN,
    @SerialName("adult") ADULT,
}

@Serializable
enum class Stat {                                         // Сытость · Уход · Настроение
    @SerialName("satiety") SATIETY,
    @SerialName("care") CARE,
    @SerialName("mood") MOOD,
}

@Serializable
enum class PetMood { HAPPY, CALM, SAD }                    // радуется · спокоен · грустит

@Serializable
enum class Slot { CHILD, DEMO }                            // профиль ребёнка и демо-профиль

@Serializable
enum class Topic {
    @SerialName("intro") INTRO,
    @SerialName("budget") BUDGET,
    @SerialName("savings") SAVINGS,
    @SerialName("purchases") PURCHASES,
    @SerialName("safety") SAFETY,
    @SerialName("holidays") HOLIDAYS,
}

@Serializable
enum class Outcome {
    @SerialName("good") GOOD,
    @SerialName("ok") OK,
    @SerialName("retry") RETRY,
}

@Serializable
enum class GrowthIcon { NEED, PLAN, SAVE }                 // три иконки итога недели

@Serializable
enum class EffectKey {
    @SerialName("wallet") WALLET,
    @SerialName("savings") SAVINGS,
    @SerialName("satiety") SATIETY,
    @SerialName("care") CARE,
    @SerialName("mood") MOOD,
}

/** Что сделал ребёнок — код текста панели «что изменилось» (texts.json → feedback_reasons). */
@Serializable
enum class FeedbackReason {
    PLAN_SAVED, PLAN_CONFIRMED, BOUGHT_NEED, BOUGHT_WANT, WISHLIST_ADDED, GOAL_CHOSEN,
    DEPOSIT, WITHDRAW, GOAL_BOUGHT, TASK_REWARD, TASK_REPEAT, JOB_REWARD, WEEK_CLOSED, LOOK_CHANGED,
}

/**
 * Почему кот сейчас такой. Приоритет — сверху вниз, первое подходящее.
 * Итог недели считается по покупкам недели, карточка кота среди недели — только по текущим показателям.
 */
@Serializable
enum class PetReason { HUNGRY, UNKEMPT, SAD, JOYFUL, WELL }

@Serializable
enum class NeedHint {                                     // когда показывать «коту нужно 50»
    @SerialName("always") ALWAYS,
    @SerialName("first_week") FIRST_WEEK,
    @SerialName("never") NEVER,
}

/** Варианты «Что можно сделать?», когда не хватает монет. */
@Serializable
enum class Way { DO_TASK, DO_JOB, CHEAPER_ITEM, WISHLIST, TAKE_FROM_SAVINGS }
