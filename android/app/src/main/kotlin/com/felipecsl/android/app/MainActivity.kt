package com.felipecsl.android.app

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Switch
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.felipecsl.android.NesGLSurfaceView
import com.felipecsl.android.R
import com.felipecsl.knes.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import java.util.concurrent.Executors
import java.util.logging.Logger

class MainActivity : AppCompatActivity() {
  private val executor = Executors.newSingleThreadExecutor()
  private val nesGlSurfaceView by lazy { findViewById<NesGLSurfaceView>(R.id.nes_gl_surface_view) }
  private val surfaceView by lazy { findViewById<SurfaceView>(R.id.surface_view) }
  private val fabRun by lazy { findViewById<FloatingActionButton>(R.id.fabRun) }
  private val toolbar by lazy { findViewById<Toolbar>(R.id.toolbar) }
  private val implSwitch by lazy { findViewById<Switch>(R.id.implementation_switch) }
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    setSupportActionBar(toolbar)
    val actionBar: ActionBar = supportActionBar!!
    actionBar.setDisplayHomeAsUpEnabled(true)
    val sprite = Sprite(GLSprite())
    nesGlSurfaceView.setSprite(sprite)
    fabRun.setOnClickListener {
      val cartridgeData = resources.openRawResource(R.raw.smb3).readBytes()
      executor.submit {
        if (implSwitch.isChecked) {
          Snackbar.make(implSwitch, "Using Kotlin/Native implementation",
              BaseTransientBottomBar.LENGTH_SHORT).show()
          startConsole(cartridgeData)
        } else {
          Snackbar.make(implSwitch, "Using JVM implementation",
              BaseTransientBottomBar.LENGTH_SHORT).show()
          Director.startConsole(cartridgeData, sprite)
        }
      }
    }

//    btnReset.setOnClickListener { console.cpu.reset() }
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    // Inflate the menu; this adds items to the action bar if it is present.
    menuInflater.inflate(R.menu.menu_main, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem?): Boolean {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    val id = item!!.itemId
    return if (id == R.id.action_settings) {
      true
    } else {
      super.onOptionsItemSelected(item)
    }
  }

  companion object {
    init {
      System.loadLibrary("knes")
    }
  }
}
