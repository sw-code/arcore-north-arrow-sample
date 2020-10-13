package io.spotar.tour.filament.sample.event

import android.content.res.Configuration
import io.reactivex.rxjava3.subjects.PublishSubject

interface ConfigurationChangedEvents {
    val configurationChangedEvents: PublishSubject<Configuration>
}