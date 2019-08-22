package com.dragonsofmugloar.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class App

fun main(args: Array<String>) {
	val appCtx = runApplication<App>(*args)
	val gameBean = appCtx.getBean(Game::class.java)
	gameBean.startGame()
}
