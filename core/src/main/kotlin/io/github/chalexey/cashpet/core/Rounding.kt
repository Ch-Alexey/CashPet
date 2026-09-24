package io.github.chalexey.cashpet.core

/**
 * Доля [percent] процентов от [value], округлённая до целого «половина вверх»
 * (правило из docs/02-экономика.md). Только для неотрицательных значений.
 */
fun percentOf(value: Int, percent: Int): Int {
    require(value >= 0 && percent >= 0) { "Отрицательные значения не поддерживаются" }
    return (value * percent + 50) / 100
}
