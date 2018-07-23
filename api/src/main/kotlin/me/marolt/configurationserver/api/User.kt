package me.marolt.configurationserver.api

data class User(
        val id: ValidUserId,
        val username: String,
        val roles: Set<ValidUserRole>)