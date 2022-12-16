package com.example.courseapp.resources

import java.util.concurrent.Executor

class ResourceManager<R>(
    private val executor: Executor,
    private val errorHandler: ErrorHandler<R>

) {
    private val consumers = mutableListOf<Consumer<R>>()
    private var resource: R? = null
    private var destroyed = false


    fun setResource(resource: R) = synchronized(this) {
        if (destroyed) return
        var localConsumers: List<Consumer<R>>
        do {
            localConsumers = ArrayList(consumers)
            consumers.clear()
            localConsumers.forEach { consumer ->
                processResource(consumer, resource)
            }
        } while (consumers.isNotEmpty())
        this.resource = resource
    }

    fun clearResource() = synchronized(this) {
        if (destroyed) return
        this.resource = null
    }

    fun consumeResource(consumer: Consumer<R>) = synchronized(this) {
        if (destroyed) return@synchronized
        val resource = this.resource
        if (resource != null) {
            processResource(consumer, resource)
        } else {
            consumers.add(consumer)
        }
    }

    fun destroy() = synchronized(this) {
        destroyed = true
        consumers.clear()
        resource = null
    }

    private fun processResource(consumer: Consumer<R>, resource: R) {
        executor.execute {
            try {
                consumer.invoke(resource)
            } catch (e: Exception) {
                errorHandler.onError(e, resource)
            }
        }
    }
}