package io.swcode.samples.northarrow

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.tbruyelle.rxpermissions3.RxPermissions
import io.swcode.samples.northarrow.arcore.BaseArCoreFragment
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        R.drawable.sceneform_plane
        startArButton.setOnClickListener { startAr() }
    }

    private fun startAr() {
        RxPermissions(this)
            .request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.CAMERA)
            .subscribe { granted ->
                if (granted) {
                    val fragment = BaseArCoreFragment()
                    val transaction = supportFragmentManager.beginTransaction()
                    transaction.add(R.id.fragmentContainer, fragment, "arCore")
                    transaction.commit()
                    startArButton.visibility = View.INVISIBLE
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show()
                }
            }
    }
}