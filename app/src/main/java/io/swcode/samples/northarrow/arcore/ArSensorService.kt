package io.swcode.samples.northarrow.arcore

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.google.ar.core.Frame
import com.google.ar.core.TrackingState
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.subjects.PublishSubject
import io.swcode.samples.northarrow.eventbus.SimpleEventBus
import io.swcode.samples.northarrow.math.MathHelper
import kotlin.math.atan2

class ArSensorService constructor(context: Context) : SensorEventListener {

    companion object {
        private const val DECAY_RATE = 0.9f
    }

    private val sensorEventSubject: PublishSubject<SensorEvent> = PublishSubject.create()
    private val doFrameEvent: PublishSubject<Frame> = PublishSubject.create()
    private val compositeDisposable = CompositeDisposable()
    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    fun onResume() {
        Observable.combineLatest(sensorEventSubject, doFrameEvent, {sensor, frame, ->
            val rotated = FloatArray(3)

            val accumulated = FloatArray(3)

            val deviceToWorldPose = frame.androidSensorPose.extractRotation()
            deviceToWorldPose.rotateVector(sensor.values, 0, rotated, 0)
            for (i in 0..2) {
                accumulated[i] = accumulated[i] * DECAY_RATE + rotated[i]
            }

            val eastX = accumulated[0]
            val eastZ = accumulated[2]
            // negative because positive rotation about Y rotates X away from Z
            val xToEastAngle = (-atan2(eastZ.toDouble(), eastX.toDouble())).toFloat()
            val pose = MathHelper.axisRotation(1, xToEastAngle)
            SimpleEventBus.publish(CompassPoseEvent(pose, frame))
        })
            .subscribe()
            .let {
                compositeDisposable.add(it)
            }

        sensorManager.registerListener(
            this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
            200000 /* 5Hz */
        )
    }

    fun onPause() {
        sensorManager.unregisterListener(this)
        compositeDisposable.clear()
    }

    override fun onSensorChanged(sensorEvent: SensorEvent) {
        if (sensorEvent.sensor.type != Sensor.TYPE_MAGNETIC_FIELD) {
            return
        }
        sensorEventSubject.onNext(sensorEvent)
    }

    fun onUpdate(frame: Frame) {
        if(frame.camera.trackingState == TrackingState.TRACKING) {
            doFrameEvent.onNext(frame)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // NOP
    }
}