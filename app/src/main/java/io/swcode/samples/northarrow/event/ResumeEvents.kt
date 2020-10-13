package io.spotar.tour.filament.sample.event

import io.reactivex.rxjava3.subjects.PublishSubject


interface ResumeEvents {
    val resumeEvents: PublishSubject<Unit>
}