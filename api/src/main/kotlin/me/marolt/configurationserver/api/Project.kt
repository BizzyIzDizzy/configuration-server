package me.marolt.configurationserver.api

import me.marolt.configurationserver.utils.IUnique

data class Project(
  override val typedId: ValidProjectId,
  val configurations: List<IConfiguration>) : IUnique<Int>