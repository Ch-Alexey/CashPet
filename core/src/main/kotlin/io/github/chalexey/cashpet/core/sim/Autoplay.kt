package io.github.chalexey.cashpet.core.sim

import io.github.chalexey.cashpet.core.engine.Action
import io.github.chalexey.cashpet.core.engine.GameEngine
import io.github.chalexey.cashpet.core.engine.Result
import io.github.chalexey.cashpet.core.model.GameState
import io.github.chalexey.cashpet.core.model.Stage

/** Стратегия игры на неделю: «разумная игра», «транжира», «скопидом» (docs/08-контракты.md, раздел 5а). */
interface Strategy {
    /** Действия одной недели из текущего состояния, последнее — CloseWeek. */
    fun weekActions(state: GameState, engine: GameEngine): List<Action>
}

/** Автопрогон для кнопки «Ускорить» и симулятора: каждое действие — через engine.apply. */
object Autoplay {
    fun run(
        state: GameState,
        engine: GameEngine,
        strategy: Strategy,
        target: Stage,
        maxWeeks: Int = 6,
    ): List<Result.Ok> = TODO("Стратегии и автопрогон — после движка недели")
}
