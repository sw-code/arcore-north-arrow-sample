package io.swcode.samples.northarrow.eventbus

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject


object SimpleEventBus : EventBus {
    private val publisher = PublishSubject.create<Any>()

    override fun publish(event: Any) {
        publisher.onNext(event)
    }

    override fun <T> listen(eventType: Class<T>): Observable<T> = publisher.ofType(eventType)
}