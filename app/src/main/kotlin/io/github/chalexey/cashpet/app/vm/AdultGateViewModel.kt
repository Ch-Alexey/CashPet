package io.github.chalexey.cashpet.app.vm

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

/**
 * Барьер раздела взрослого (docs/01-функционал.md, раздел 8): пример «двузначное × однозначное»,
 * например 47 × 6. Ребёнок 7–11 лет в уме его не решит, взрослый — за несколько секунд.
 * Экран показывает «[a] × [b] = ?» и поле ответа; тексты — в strings.xml.
 */
data class AdultGateUiState(
    val a: Int,
    val b: Int,
    val input: String = "",
    val wrongAttempt: Boolean = false,        // прошлый ответ не подошёл — показан новый пример
    val passed: Boolean = false,              // можно открывать раздел взрослого
)

class AdultGateViewModel(private val random: Random = Random.Default) : ViewModel() {

    private val _uiState = MutableStateFlow(newExample())
    val uiState: StateFlow<AdultGateUiState> = _uiState.asStateFlow()

    /** Только цифры и не длиннее ответа: 99 × 9 = 891. */
    fun onInputChange(text: String) {
        _uiState.update { it.copy(input = text.filter(Char::isDigit).take(MAX_ANSWER_LENGTH)) }
    }

    /** Верно — барьер пройден. Ошибка — просто новый пример, без счётчиков и блокировок. */
    fun submit() {
        val state = _uiState.value
        val answer = state.input.toIntOrNull() ?: return
        _uiState.value = if (answer == state.a * state.b) {
            state.copy(passed = true)
        } else {
            newExample().copy(wrongAttempt = true)
        }
    }

    // a — от 23, без круглых десятков и одинаковых цифр; b — от 4: 11 × 3, 30 × 2, 22 × 3 третьеклассник решит в уме
    private fun newExample(): AdultGateUiState {
        var a: Int
        do {
            a = random.nextInt(MIN_A, 100)
        } while (a % 10 == 0 || a % 11 == 0)
        return AdultGateUiState(a = a, b = random.nextInt(MIN_B, 10))
    }

    private companion object {
        const val MAX_ANSWER_LENGTH = 3
        const val MIN_A = 23
        const val MIN_B = 4
    }
}
