package io.swcode.samples.northarrow.orientation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.appcompat.app.AlertDialog
import io.spotar.tour.filament.sample.orientation.Orientation
import io.swcode.samples.northarrow.eventbus.SimpleEventBus
import io.swcode.samples.northarrow.math.radianToDegree


class OrientationService constructor(private val context: Context) : SensorEventListener {

    private var sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private var filteredVector: FloatArray? = null
    private val rotationMatrix: FloatArray = FloatArray(9)
    private val remappedRotationMatrix: FloatArray = FloatArray(9)
    private val orientationVector: FloatArray = FloatArray(3)

    fun onResume() {
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), 200000 /* 5Hz */)
    }

    fun onPause() {
        sensorManager.unregisterListener(this)
    }

    private fun showErrorDialog() {
        val alertDialog = AlertDialog.Builder(context)
            .setTitle("Sensor nicht verfügbar")
            .setMessage("Sensor ist nicht verfügbar")
            .create()
        alertDialog.show()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // NOP
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR)
            {
                var vector = event.values
                SensorManager.getRotationMatrixFromVector(rotationMatrix, vector)

                // Remap coordinate System to compensate the camera front position
                var success = SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, remappedRotationMatrix)
                if (success)
                {
                    SensorManager.getOrientation(remappedRotationMatrix, orientationVector)

                    filteredVector = applyLowPass(orientationVector, filteredVector)

                    val x = ((radianToDegree(filteredVector!![0].toDouble()) + 360) % 360).toInt()
                    val y = radianToDegree(filteredVector!![1].toDouble()).toInt()
                    val z = ((radianToDegree(filteredVector!![2].toDouble()) + 360) % 360).toInt()

                    Log.i("OrientationService", "onSensorChanged: " + Orientation( x, y, z))
                    SimpleEventBus.publish(Orientation( x, y, z))
                }
            }
        }
    }

    private fun applyLowPass(input: FloatArray, output: FloatArray?, alpha: Float = 0.5f) : FloatArray {
        if (output == null) return input

        for (i in input.indices) {
            output[i] = output[i] + alpha * (input[i] - output[i])
        }

        return output
    }
}