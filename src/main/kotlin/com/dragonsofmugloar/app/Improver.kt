package com.dragonsofmugloar.app

import com.dragonsofmugloar.app.URLBuilder.Companion.getGameShopItemsURL
import com.dragonsofmugloar.app.URLBuilder.Companion.getPurchaseItemURL
import com.dragonsofmugloar.model.PurchaseItemResponse
import com.dragonsofmugloar.model.ShopItem
import org.slf4j.LoggerFactory
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity

@Component
class Improver(restTemplateBuilder: RestTemplateBuilder) {

    private val restTemplate: RestTemplate = restTemplateBuilder.build()
    private val logger = LoggerFactory.getLogger(Improver::class.java)

    private val shopItems: MutableList<ShopItem> = mutableListOf()
    private val sortedShopItems: MutableList<ShopItem> = mutableListOf()

    private val shopItemComparator = Comparator<ShopItem> { item1, item2 ->
        item1.cost.compareTo(item2.cost)
    }

    private val shopItemReverseComparator = Comparator<ShopItem> { item1, item2 ->
        item2.cost.compareTo(item1.cost)
    }

    fun lowImprove(balance: Int, gameId: String): Int {

        val items = updateAndReturnAvailableShopItems(gameId, balance,false)

        return when (balance) {
            in 51..149 -> purchaseShopItem(items.first(), gameId, balance)
            in 150..299 -> purchaseShopItem(items.first { it.cost >= 100 }, gameId, balance)
            in 300..10000 -> purchaseShopItems(
                    items.filter { it.cost == 100 }.subList(0,1),
                    gameId,
                    balance
            )
            else -> balance
        }
    }

    fun highImprove(balance: Int, gameId: String): Int {

        val sortedItems = updateAndReturnAvailableShopItems(gameId, balance,true)

        val predicate = when (balance) {
            in 0..50 ->  { item: ShopItem -> item.cost == 50 }
            in 51..153 ->  { item: ShopItem -> item.cost == 50 }
            in 154..354 ->  { item: ShopItem -> item.cost == 100 }
            in 355..1001 ->  { item: ShopItem -> item.cost == 300 }
            else ->  { item: ShopItem -> item.cost <= 100 }
        }

        val itemsForPurchase = if (balance > 100) {
            sortedItems.filter(predicate)
        } else {
            listOf(sortedItems.first(predicate))
        }

        return purchaseShopItems(
                itemsForPurchase,
                gameId,
                balance)
    }

    fun improve(balance: Int, gameId: String, itemCost: Int): Int {
        val items = updateAndReturnAvailableShopItems(gameId, balance,true)
        return purchaseShopItem(items.first { it.cost == itemCost }, gameId, balance)
    }

    private fun purchaseShopItems(items: List<ShopItem>, gameId: String, balance: Int): Int =
        items.map { purchaseShopItem(it, gameId, balance) }.toSet().sum()

    private fun purchaseShopItem(item: ShopItem, gameId: String, balance: Int): Int {

        var balance = 0

        val purchaseURL = getPurchaseItemURL(gameId, item.id)
        val transactionStatus = restTemplate
                .postForEntity<PurchaseItemResponse>(purchaseURL)
                .body

        transactionStatus?.let { status ->
            balance = status.gold
            if (status.shoppingSuccess.toBoolean()) {
                logger.info("$$$ Improved by ${item.name}, ${item.cost}")
            }
        }

        return balance
    }

    private fun updateAndReturnAvailableShopItems(gameId: String, balance: Int, descSort: Boolean): MutableList<ShopItem> {

        shopItems.clear()
        shopItems.addAll(getShopItems(gameId))
        sortedShopItems.clear()

        val comparator = if (descSort) { shopItemReverseComparator } else { shopItemComparator }
        sortedShopItems.addAll(
                shopItems
                        .filter { it.cost <= balance }
                        .sortedWith(comparator)
        )

        return sortedShopItems

    }

    private fun getShopItems(gameId: String) =
            restTemplate
                    .exchange(
                            getGameShopItemsURL(gameId),
                            HttpMethod.GET,
                            null,
                            object : ParameterizedTypeReference<List<ShopItem>>() {}
                    )
                    .body!!

}