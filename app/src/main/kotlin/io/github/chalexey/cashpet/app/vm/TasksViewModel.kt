package io.github.chalexey.cashpet.app.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.chalexey.cashpet.app.appContainer
import io.github.chalexey.cashpet.app.store.GameStore
import io.github.chalexey.cashpet.content.GameContent
import io.github.chalexey.cashpet.core.engine.Action
import io.github.chalexey.cashpet.core.engine.GameEngine
import io.github.chalexey.cashpet.core.engine.Result
import io.github.chalexey.cashpet.core.model.GameState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Задания и подработки (docs/01-функционал.md, раздел 4.3). Открытые задания — по 2 в неделю, в демо все;
 * пройденные не исчезают, их можно пройти ещё раз без монет. Само задание — [TaskPlayViewModel].
 * Подработка, пока мини-игр нет, — одно нажатие «Помочь по дому» с той же оплатой.
 */
class TasksViewModel(
    private val content: GameContent,
    private val engine: GameEngine,
    private val store: GameStore,
) : ViewModel() {

    private val message = MutableStateFlow(ActionMessage())

    val uiState: StateFlow<TasksUiState?> = combine(store.state, message) { state, msg ->
        state?.let { toUi(it, msg) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, store.state.value?.let { toUi(it, ActionMessage()) })

    /** «Помочь по дому»: оплата — сразу в кошелёк, не больше jobs_per_week за неделю. */
    fun doJob() {
        val job = content.catalog.jobs.firstOrNull() ?: return
        viewModelScope.launch {
            message.value = when (val result = store.dispatch(Action.DoJob(job.id))) {
                is Result.Ok -> ActionMessage(feedback = feedbackUi(result, content, amount = job.reward))
                is Result.Rejected -> ActionMessage(rejection = result.reason)
            }
        }
    }

    /** Панель или сообщение показаны — убрать. */
    fun onMessageShown() {
        message.value = ActionMessage()
    }

    private fun toUi(state: GameState, msg: ActionMessage): TasksUiState {
        val job = content.catalog.jobs.firstOrNull()
        return TasksUiState(
            top = topBar(state, content),
            open = openTasks(state, content, engine),
            done = doneTasks(state, content),
            jobsLeft = if (job == null) 0 else maxOf(0, content.economy.jobsPerWeek - state.week.jobsDone),
            jobReward = job?.reward ?: 0,
            feedback = msg.feedback,
            rejection = msg.rejection,
        )
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = this[APPLICATION_KEY]!!.appContainer
                TasksViewModel(c.content, c.engine, c.gameStore)
            }
        }
    }
}
