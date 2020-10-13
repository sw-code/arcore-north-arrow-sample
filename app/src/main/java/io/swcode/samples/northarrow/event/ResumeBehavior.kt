package io.spotar.tour.filament.sample.event

import io.reactivex.rxjava3.subjects.BehaviorSubject

interface ResumeBehavior {
    val resumeBehavior: BehaviorSubject<Boolean>
}