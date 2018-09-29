package com.felipecsl.knes.app

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.Switch
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
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
  private var isRunning = false
  private lateinit var director: Director
  private val buttons = BooleanArray(8)
  private val onButtonTouched = { i: Int ->
    View.OnTouchListener { _, e ->
      when (e.action) {
        MotionEvent.ACTION_DOWN -> buttons[i] = true
        MotionEvent.ACTION_UP -> buttons[i] = false
      }
      true
    }
  }

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
    val cartridgeData = resources.openRawResource(ROM).readBytes()
    val glSprite = GLSprite { buttons }
    nesGlSurfaceView.setSprite(glSprite)
    fabRun.setOnClickListener {
      val icon = if (!isRunning) R.drawable.ic_stat_name else R.drawable.ic_play_arrow_white_48dp
      fabRun.setImageDrawable(ContextCompat.getDrawable(this, icon))
      if (!isRunning) {
        startConsole(cartridgeData, glSprite)
      } else {
        director.reset()
        stopEngine()
      }
      isRunning = !isRunning
    }

    btnA.setOnTouchListener(onButtonTouched(0))
    btnB.setOnTouchListener(onButtonTouched(1))
    btnSelect.setOnTouchListener(onButtonTouched(2))
    btnStart.setOnTouchListener(onButtonTouched(3))
    arrowUp.setOnTouchListener(onButtonTouched(4))
    arrowDown.setOnTouchListener(onButtonTouched(5))
    arrowLeft.setOnTouchListener(onButtonTouched(6))
    arrowRight.setOnTouchListener(onButtonTouched(7))
  }

  private fun startConsole(cartridgeData: ByteArray, glSprite: GLSprite) {
    if (implSwitch.isChecked) {
      Snackbar.make(implSwitch, "Using Kotlin/Native implementation",
          BaseTransientBottomBar.LENGTH_SHORT).show()
      nativeStartConsole(cartridgeData)
    } else {
      Snackbar.make(implSwitch, "Using JVM implementation",
          BaseTransientBottomBar.LENGTH_SHORT).show()
      director = Director(cartridgeData, AudioSink())
      glSprite.director = director
      handler.post(this)
    }
  }

  override fun run() {
    staticConsole = director.console
    startEngine(getExclusiveCores())
    director.run()
  }

  // Obtain CPU cores which are reserved for the foreground app. The audio thread can be
  // bound to these cores to avoids the risk of it being migrated to slower or more contended
  // core(s).
  private fun getExclusiveCores(): IntArray {
    var exclusiveCores = intArrayOf()
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
      Log.w("MainActivity", "getExclusiveCores() not supported. Only available on API " +
          Build.VERSION_CODES.N + "+")
    } else {
      exclusiveCores = android.os.Process.getExclusiveCores()

    }
    return exclusiveCores
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
      System.loadLibrary("ktnes-audio")
    }

    const val ROM = R.raw.legend_of_zelda
    internal var staticConsole: Console? = null

    // Called from JNI AudioEngine
    @Suppress("unused")
    @JvmStatic fun audioBuffer(): FloatArray? {
      return staticConsole?.audioBuffer() ?: FloatArray(0)
    }
  }
}
