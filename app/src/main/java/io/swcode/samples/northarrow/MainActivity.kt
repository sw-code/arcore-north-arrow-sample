package io.swcode.samples.northarrow

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.tbruyelle.rxpermissions3.RxPermissions
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startArButton.setOnClickListener {
            startAr()
            startArButton.isVisible = false
        }
    }

    private fun startAr() {
        RxPermissions(this)
            .request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.CAMERA)
            .subscribe { granted ->
                if (granted) {
                    val fragment = ArCoreFragment()
                    val transaction = supportFragmentManager.beginTransaction()
                    transaction.add(R.id.fragmentContainer, fragment, "arCore").addToBackStack(null)
                    transaction.commit()
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show()
                }
            }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        startArButton.isVisible = true
    }
}