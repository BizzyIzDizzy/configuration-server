package me.marolt.configurationserver.utils

interface IUnique<T>{
    val typedId: IIdentifiable<T>
    val id: T
        get() = typedId.id
}