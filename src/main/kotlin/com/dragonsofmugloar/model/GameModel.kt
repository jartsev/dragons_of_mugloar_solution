package com.dragonsofmugloar.model

data class GameModel(
        val gameId: String,
        val lives: Int,
        val gold: Int,
        val level: Int,
        val score: Int,
        val highScore: Int,
        val turn: Int) {

    override fun toString(): String {
        return "GameModel(gameId='$gameId', lives=$lives, gold=$gold, level=$level, score=$score, highScore=$highScore, turn=$turn)"
    }
}