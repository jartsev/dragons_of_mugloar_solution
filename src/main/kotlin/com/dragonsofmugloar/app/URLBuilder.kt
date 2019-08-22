package com.dragonsofmugloar.app

class URLBuilder {
    companion object {
        private const val URL = "https://dragonsofmugloar.com/api/v2/"
        fun getStartGameURL() = URL + "game/start"
        fun getTasksGameURL(gameId: String) = URL + gameId + "/messages"
        fun getGameShopItemsURL(gameId: String) = URL + gameId + "/shop"
        fun getPurchaseItemURL(gameId: String, itemId: String) = URL + gameId + "/shop/buy/" + itemId
        fun getSolveTaskURL(gameId: String, taskId: String) = URL + gameId + "/solve/" + taskId
    }
}