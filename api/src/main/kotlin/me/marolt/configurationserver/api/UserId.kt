package me.marolt.configurationserver.api

import me.marolt.configurationserver.utils.IIdentifiable

sealed class UserId
data class ValidUserId(override val id: Int) : UserId(), IIdentifiable<Int>
object InvalidUserId : UserId()