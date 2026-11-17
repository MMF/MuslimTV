package com.mfouad.muslimtv.core.model

enum class AzkarCategory { MORNING, EVENING, AFTER_PRAYER, SLEEP, WAKING, OCCASIONS, IFTAR }

/** One remembrance as written in Hisn al-Muslim, with how many times to say it and its source. */
data class Zikr(val text: String, val count: Int, val reference: String)

data class AzkarChapter(val id: Int, val title: String, val items: List<Zikr>)

data class DivineName(val number: Int, val arabic: String, val transliteration: String, val english: String, val explanation: String)

/** Tasbeeh counting: a target per round; finishing a round triggers the completion animation. Pure. */
data class Tasbeeh(val count: Int = 0, val target: Int = 33, val rounds: Int = 0) {
    val inRound: Int get() = if (target <= 0) count else count - rounds * target
    val justCompleted: Boolean get() = target > 0 && count > 0 && count % target == 0

    fun tap(): Tasbeeh {
        val next = count + 1
        return copy(count = next, rounds = if (target > 0) next / target else 0)
    }

    fun withTarget(t: Int): Tasbeeh = Tasbeeh(count = 0, target = t.coerceIn(0, 9999), rounds = 0)
    fun reset(): Tasbeeh = copy(count = 0, rounds = 0)
}
