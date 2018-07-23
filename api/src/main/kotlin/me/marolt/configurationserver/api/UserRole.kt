package me.marolt.configurationserver.api

import me.marolt.configurationserver.utils.IIdentifiable

sealed class UserRole
data class ValidUserRole(override val id: String): UserRole(), IIdentifiable<String>
object InvalidUserROle : UserRole()