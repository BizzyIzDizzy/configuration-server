package me.marolt.configurationserver.services

import me.marolt.configurationserver.utils.IIdentifiable

sealed class UserId
data class ValidUserId(override val id: Int) : UserId(), IIdentifiable<Int>
object InvalidUserId : UserId()

sealed class UserRole
data class ValidUserRole(override val id: String): UserRole(), IIdentifiable<String>
object InvalidUserROle : UserRole()

data class User(
        val id: ValidUserId,
        val username: String,
        val roles: Set<ValidUserRole>)