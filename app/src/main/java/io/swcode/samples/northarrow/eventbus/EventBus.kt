package io.swcode.samples.northarrow.eventbus

import io.reactivex.rxjava3.core.Observable

interface EventBus {
    fun publish(event: Any)
    fun <T> listen(eventType: Class<T>): Observable<T>
}