package com.expensesplitter.app.ui.util

// Shared sanitizer for every money/numeric-amount text field in the app.
// KeyboardType.Decimal only *hints* a numeric keyboard — it doesn't block
// other input (paste, IME auto-suggest, some third-party keyboards, adb/test
// input all bypass it) — so this filters the actual value: only digits and a
// single decimal point, at most 2 digits after it.
fun sanitizeMoneyInput(value: String): String {
    val digitsAndDot = value.filter { it.isDigit() || it == '.' }
    val firstDotIndex = digitsAndDot.indexOf('.')
    return if (firstDotIndex == -1) {
        digitsAndDot
    } else {
        val whole = digitsAndDot.substring(0, firstDotIndex).filter { it.isDigit() }
        val fraction = digitsAndDot.substring(firstDotIndex + 1).filter { it.isDigit() }.take(2)
        "$whole.$fraction"
    }
}
