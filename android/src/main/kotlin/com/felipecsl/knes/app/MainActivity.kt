package com.felipecsl.knes.app

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import com.felipecsl.android.NesGLSurfaceView
import com.felipecsl.knes.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity(), Runnable {
  private val nesGlSurfaceView by lazy { findViewById<NesGLSurfaceView>(R.id.nes_gl_surface_view) }
  private val fabRun by lazy { findViewById<FloatingActionButton>(R.id.fabRun) }
  private val btnReset by lazy { findViewById<AppCompatButton>(R.id.btnReset) }
  private val toolbar by lazy { findViewById<Toolbar>(R.id.toolbar) }
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
  private var isPaused = false
  @Volatile private var shouldSaveState = false
  @Volatile private var shouldRestoreState = false
  private val audioEngine = AudioEngineWrapper()
  private lateinit var director: Director
  private val onButtonTouched = { b: Buttons ->
    View.OnTouchListener { _, e ->
      when (e.action) {
        MotionEvent.ACTION_DOWN -> director.controller1.onButtonDown(b)
        MotionEvent.ACTION_UP -> director.controller1.onButtonUp(b)
      }
      true
    }
  }

  init {
    handlerThread.start()
    handler = Handler(handlerThread.looper)
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    setSupportActionBar(toolbar)
    supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    val glSprite = GLSprite()
    nesGlSurfaceView.setSprite(glSprite)
    fabRun.setOnClickListener {
      onClickPlayPause(glSprite)
    }
    btnReset.setOnClickListener {
      updatePlayPauseIcon()
      resetConsole()
    }

    btnA.setOnTouchListener(onButtonTouched(Buttons.BUTTON_A))
    btnB.setOnTouchListener(onButtonTouched(Buttons.BUTTON_B))
    btnSelect.setOnTouchListener(onButtonTouched(Buttons.BUTTON_SELECT))
    btnStart.setOnTouchListener(onButtonTouched(Buttons.BUTTON_START))
    arrowUp.setOnTouchListener(onButtonTouched(Buttons.ARROW_UP))
    arrowDown.setOnTouchListener(onButtonTouched(Buttons.ARROW_DOWN))
    arrowLeft.setOnTouchListener(onButtonTouched(Buttons.ARROW_LEFT))
    arrowRight.setOnTouchListener(onButtonTouched(Buttons.ARROW_RIGHT))
  }

  private fun onClickPlayPause(glSprite: GLSprite) {
    val cartridgeData = resources.openRawResource(ROM).readBytes()
    updatePlayPauseIcon()
    if (!isRunning) {
      if (!isPaused) {
        startConsole(cartridgeData, glSprite)
      } else {
        resumeConsole()
      }
    } else {
      pauseConsole()
    }
    isRunning = !isRunning
    isPaused = !isRunning
  }

  private fun pauseConsole() {
    director.pause()
    audioEngine.pause()
  }

  private fun resetConsole() {
    director.reset()
    audioEngine.stop()
  }

  private fun resumeConsole() {
    handler.post(this)
    audioEngine.resume()
  }

  private fun updatePlayPauseIcon() {
    val icon = if (!isRunning) R.drawable.ic_stat_name else R.drawable.ic_play_arrow_white_48dp
    fabRun.setImageDrawable(ContextCompat.getDrawable(this, icon))
  }

  private fun startConsole(cartridgeData: ByteArray, glSprite: GLSprite) {
    audioEngine.start()
    director = Director(cartridgeData)
    glSprite.director = director
    staticDirector = director
    handler.post(this)
  }

  override fun run() {
    maybeSaveState()
    maybeRestoreState()
    director.run()
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.menu_main, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem?): Boolean {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    return when (item!!.itemId) {
      R.id.action_save_state -> {
        director.pause()
        shouldSaveState = true
        handler.post(this)
        return true
      }
      R.id.action_restore_state -> {
        director.pause()
        shouldRestoreState = true
        handler.post(this)
        return true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  private fun maybeSaveState() {
    if (shouldSaveState) {
      val stateMap = director.dumpState()
      val sharedPrefs = getSharedPreferences(STATE_PREFS_KEY, Context.MODE_PRIVATE)
      sharedPrefs.edit().also { p ->
        stateMap.map { (k, v) ->
          p.putString(k, v)
        }
      }.apply()
      Snackbar.make(toolbar, "Game state saved", Snackbar.LENGTH_SHORT).show()
      shouldSaveState = false
    }
  }

  private fun maybeRestoreState() {
    if (shouldRestoreState) {
      val sharedPrefs = getSharedPreferences(STATE_PREFS_KEY, Context.MODE_PRIVATE)
      val state = sharedPrefs.all
      if (state.isNotEmpty()) {
        director.restoreState(state)
        Snackbar.make(toolbar, "Game state restored", Snackbar.LENGTH_SHORT).show()
      }
      shouldRestoreState = false
    }
  }

  companion object {
    const val ROM = R.raw.bingo
    private const val STATE_PREFS_KEY = "KTNES_STATE"
    internal var staticDirector: Director? = null

    // Called from JNI AudioEngine
    @Suppress("unused")
    @JvmStatic fun audioBuffer(): FloatArray? {
      return staticDirector?.audioBuffer() ?: FloatArray(0)
    }
  }
}
