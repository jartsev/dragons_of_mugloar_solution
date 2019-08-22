package com.dragonsofmugloar.model

class SolvedMessage(
        val success: Boolean,
        val lives: Int,
        val gold: Int,
        val score: Int,
        val highScore: Int,
        val turn: Int,
        val message: String
) {
    override fun toString(): String {
        return "SolvedMessage(success=$success, lives=$lives, gold=$gold, score=$score, highScore=$highScore, turn=$turn, message=$message)"
    }
}