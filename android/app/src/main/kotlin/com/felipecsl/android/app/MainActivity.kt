package com.felipecsl.android.app

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import com.felipecsl.android.R
import com.felipecsl.knes.Director
import com.felipecsl.knes.Surface
import com.felipecsl.knes.startConsole
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.toolbar.*
import java.util.concurrent.Executors
import java.util.logging.Logger

class MainActivity : AppCompatActivity() {
  private val LOG = Logger.getLogger("NesGLRenderer")
  private val executor = Executors.newSingleThreadExecutor()
  private val surfaceView by lazy { surface_view }
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    setSupportActionBar(toolbar)
    val actionBar: ActionBar = supportActionBar!!
    actionBar.setDisplayHomeAsUpEnabled(true)
    fabRun.setOnClickListener {
      val cartridgeData = resources.openRawResource(R.raw.smb3).readBytes()
      executor.submit {
        if (switch1.isChecked) {
          Snackbar.make(switch1, "Using Kotlin/Native implementation",
              BaseTransientBottomBar.LENGTH_SHORT).show()
          startConsole(cartridgeData)
        } else {
          Snackbar.make(switch1, "Using JVM implementation",
              BaseTransientBottomBar.LENGTH_SHORT).show()
          Director.startConsole(cartridgeData, Surface(surfaceView))
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
    private const val USE_NATIVE_CONSOLE_IMPL = true

    init {
      System.loadLibrary("knes")
    }
  }
}
