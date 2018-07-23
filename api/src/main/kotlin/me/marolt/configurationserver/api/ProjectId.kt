package me.marolt.configurationserver.api

import me.marolt.configurationserver.utils.IIdentifiable

sealed class ProjectId
data class ValidProjectId(override val id: Int) : ProjectId(), IIdentifiable<Int>
object InvalidProjectId : ProjectId()