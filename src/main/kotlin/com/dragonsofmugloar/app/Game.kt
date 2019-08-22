package com.dragonsofmugloar.app

import com.dragonsofmugloar.app.URLBuilder.Companion.getSolveTaskURL
import com.dragonsofmugloar.app.URLBuilder.Companion.getStartGameURL
import com.dragonsofmugloar.app.URLBuilder.Companion.getTasksGameURL
import com.dragonsofmugloar.model.GameModel
import com.dragonsofmugloar.model.SolvedMessage
import com.dragonsofmugloar.model.TaskModel
import org.slf4j.LoggerFactory
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity
import kotlin.system.exitProcess

@Component
class Game(
        restTemplateBuilder: RestTemplateBuilder,
        val improver: Improver) {

    private val restTemplate: RestTemplate = restTemplateBuilder.build()
    private val logger = LoggerFactory.getLogger(Game::class.java)

    private lateinit var game: GameModel

    private var balance: Int = 0
    private var currentScore: Int = 0

    private val sleepTime = 250L
    private val minBalanceForPurchase = 50
    private val improveInAdvanceBalanceThreshold = 200

    private var currentCycle = 1
    private var gameScoreGoal = 1001

    private val taskComparator = Comparator<TaskModel> { task1, task2 ->
        task1.reward.toInt().compareTo(task2.reward.toInt())
    }

    fun startGame() {

        val startGame = restTemplate
                .postForEntity<GameModel>(getStartGameURL())
                .body

        startGame?.let { game ->
            this.game = game
            this.balance = game.gold

            logger.info("\n Game ${game.gameId} has started with ${game.gold} gold \n")

            runGameCycle()
        }
    }

    private fun runGameCycle() {

        logger.info("*** Cycle # ${currentCycle}")

        val tasks = getGameTasks()

        tasks?.let { tasks ->
            val sortedByReward = tasks.sortedWith(taskComparator)
            sortedByReward.forEach {
                if (!solveTask(it)) {
                    // logger.warn("^^^ .. task # ${it.adId} is failed")
                    return@forEach
                }
            }
        }

        sleep()

        // .. able to achieve a score of at least 1000 points
        if (currentScore > gameScoreGoal) {

            logger.info("@@@ Game finished on ${currentCycle} round with ${currentScore} score! \n ")
            exitProcess(0)
        } else {

            currentCycle += 1
            logger.info("*** Cycle # ${currentCycle} finished")
            logger.info("*** Score: ${currentScore}. Gold: ${balance} \n")

            runGameCycle()
        }
    }

    private fun solveTask(task: TaskModel, retry: Boolean = false): Boolean {

        try {

            val result = restTemplate
                    .postForEntity<SolvedMessage>(getSolveTaskURL(game.gameId, task.adId))
                    .body

            result?.let { result ->

                this.balance = result.gold

                if (isFailed(result)) {

                    logger.info("* - Failed, lives: ${result.lives}, gold: ${balance}")

                    if (balance < minBalanceForPurchase) {
                        logger.info("!!! Balance less than ${minBalanceForPurchase}!")
                        return false

                    } else {
                        with(result) { balance = improveChances(gold, lives) }
                        print("")
                    }

                    var isRetryAllow = true
                    if (result.lives <= 2) {
                        isRetryAllow = false
                    }

                    if (!retry && isRetryAllow) {
                        logger.info("*<> Retry task ${task.adId} with reward ${task.reward}")
                        solveTask(task, true)
                    }

                } else {

                    logger.info("* + Success: + ${task.reward}, total score: ${result.score}, gold: ${balance}")
                    currentScore = result.score
                    balance = improveIfPossibleChancesInAdvance(balance)
                }
            }

            shortSleep()

        } catch (e: HttpClientErrorException) {
            logger.warn("::: ${e.statusText}, code: ${e.rawStatusCode}")
            shortSleep()
            return false
        }

        sleep()
        return true
    }

    private fun improveChances(balance: Int, lives: Int): Int {

        val improvementCondition = (lives >= 2)

        return if (improvementCondition) {
            improver.lowImprove(balance, game.gameId)
        } else {
            improver.highImprove(balance, game.gameId)
        }
    }

    private fun improveIfPossibleChancesInAdvance(balance: Int): Int =
        if (balance >= improveInAdvanceBalanceThreshold) {
            logger.info("$>> in advance..")
            val itemCost = if (currentCycle < 7) { 50 } else { 100 }
            improver.improve(balance, game.gameId, itemCost)
        } else {
            balance
        }

    private fun isFailed(result: SolvedMessage) = !result.success

    private fun sleep() = Thread.sleep(sleepTime)

    private fun shortSleep() = Thread.sleep(sleepTime.div(4))

    private fun getGameTasks(): List<TaskModel>? =
            restTemplate
                    .exchange(
                            getTasksGameURL(game.gameId),
                            HttpMethod.GET,
                            null,
                            object : ParameterizedTypeReference<List<TaskModel>>() {}
                    )
                    .body
}