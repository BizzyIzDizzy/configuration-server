package me.marolt.configurationserver.utils

interface IIdentifiable<T> {
    val id: T
}

interface IUnique<T>{
    val typedId: IIdentifiable<T>
    val id: T
        get() = typedId.id
}