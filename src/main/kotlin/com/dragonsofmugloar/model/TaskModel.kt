package com.dragonsofmugloar.model

class TaskModel(
    val adId: String,
    val message: String,
    val reward: String,
    val expiresIn: String
) {
    override fun toString(): String {
        return "TaskModel(adId='$adId', message='$message', reward='$reward', expiresIn='$expiresIn')"
    }
}