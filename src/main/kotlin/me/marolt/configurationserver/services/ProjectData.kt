package me.marolt.configurationserver.services

import me.marolt.configurationserver.utils.IUnique
import me.marolt.configurationserver.utils.IIdentifiable

sealed class ProjectId
data class ValidProjectId(override val id: Int) : ProjectId(), IIdentifiable<Int>
object InvalidProjectId : ProjectId()

data class Project(
        override val typedId: ValidProjectId,
        val configurations: List<IConfiguration>) : IUnique<Int>
