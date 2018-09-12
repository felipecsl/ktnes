package com.felipecsl.knes.app

import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Switch
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.Toolbar
import com.felipecsl.android.NesGLSurfaceView
import com.felipecsl.knes.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity(), Runnable {
  private val nesGlSurfaceView by lazy { findViewById<NesGLSurfaceView>(R.id.nes_gl_surface_view) }
  private val fabRun by lazy { findViewById<FloatingActionButton>(R.id.fabRun) }
  private val btnReset by lazy { findViewById<AppCompatButton>(R.id.btnReset) }
  private val toolbar by lazy { findViewById<Toolbar>(R.id.toolbar) }
  private val implSwitch by lazy { findViewById<Switch>(R.id.implementation_switch) }
  private val btnStart by lazy { findViewById<AppCompatButton>(R.id.btnStart) }
  private val btnSelect by lazy { findViewById<AppCompatButton>(R.id.btnSelect) }
  private val btnA by lazy { findViewById<AppCompatButton>(R.id.btnA) }
  private val btnB by lazy { findViewById<AppCompatButton>(R.id.btnB) }
  private val arrowUp by lazy { findViewById<AppCompatButton>(R.id.arrowUp) }
  private val arrowDown by lazy { findViewById<AppCompatButton>(R.id.arrowDown) }
  private val arrowLeft by lazy { findViewById<AppCompatButton>(R.id.arrowLeft) }
  private val arrowRight by lazy { findViewById<AppCompatButton>(R.id.arrowRight) }
  private val handlerThread = HandlerThread("Console Thread")
  private var handler: Handler
  private lateinit var director: Director
  private val buttons = BooleanArray(8)

  init {
    handlerThread.start()
    handler = Handler(handlerThread.looper)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    setSupportActionBar(toolbar)
    val actionBar: ActionBar = supportActionBar!!
    actionBar.setDisplayHomeAsUpEnabled(true)
    val cartridgeData = resources.openRawResource(R.raw.smb3).readBytes()
    val glSprite = GLSprite {
      val state = buttons.copyOf()
      buttons.forEachIndexed { i, _ -> buttons[i] = false }
      state
    }
    nesGlSurfaceView.setSprite(glSprite)
    fabRun.setOnClickListener {
      if (implSwitch.isChecked) {
        Snackbar.make(implSwitch, "Using Kotlin/Native implementation",
            BaseTransientBottomBar.LENGTH_SHORT).show()
        nativeStartConsole(cartridgeData)
      } else {
        Snackbar.make(implSwitch, "Using JVM implementation",
            BaseTransientBottomBar.LENGTH_SHORT).show()
        director = Director(cartridgeData)
        glSprite.director = director
        handler.post(this)
      }
    }

    btnStart.setOnClickListener(this::onStart)
    btnSelect.setOnClickListener(this::onSelect)
    arrowUp.setOnClickListener(this::onArrowUp)
    arrowDown.setOnClickListener(this::onArrowDown)
    arrowLeft.setOnClickListener(this::onArrowLeft)
    arrowRight.setOnClickListener(this::onArrowRight)
    btnA.setOnClickListener(this::onBtnA)
    btnB.setOnClickListener(this::onBtnB)
  }

  private fun onBtnA(v: View) {
    buttons[0] = true
  }

  private fun onBtnB(v: View) {
    buttons[1] = true
  }

  private fun onSelect(v: View) {
    buttons[2] = true
  }

  private fun onStart(v: View) {
    buttons[3] = true
  }

  private fun onArrowUp(v: View) {
    buttons[4] = true
  }

  private fun onArrowDown(v: View) {
    buttons[5] = true
  }

  private fun onArrowLeft(v: View) {
    buttons[6] = true
  }

  private fun onArrowRight(v: View) {
    buttons[7] = true
  }

  override fun run() {
    director.run()
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
