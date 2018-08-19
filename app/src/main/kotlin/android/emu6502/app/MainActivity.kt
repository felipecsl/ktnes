package android.emu6502.app

import android.emu6502.R
import android.emu6502.nes.Console
import android.emu6502.nes.INESFileParser
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.toolbar.*

class MainActivity : AppCompatActivity() {
  private lateinit var console: Console
  private val bgHandlerThread = HandlerThread("CPU Thread")
  private val bgHandler: Handler

  init {
    bgHandlerThread.start()
    bgHandler = Handler(bgHandlerThread.looper)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    setSupportActionBar(toolbar)
    val actionBar: ActionBar = supportActionBar!!
    actionBar.setDisplayHomeAsUpEnabled(true)
    fabRun.setOnClickListener {
      display_wrapper.visibility = View.VISIBLE
      val stream = resources.openRawResource(R.raw.smb3)
      val cpuTemplate = resources.openRawResource(R.raw.template)
      val cartridge = INESFileParser.parseCartridge(stream)
      console = Console.newConsole(cartridge, display, cpuTemplate)
      console.reset()
      bgHandler.post {
        step()
      }
    }

    btnReset.setOnClickListener { console.cpu.reset() }

    val onClickButton = { code: Int ->
//      console.cpu.memory.storeKeypress(code)
    }
    arrowLeft.setOnClickListener { onClickButton(0x61) }
    arrowRight.setOnClickListener { onClickButton(0x64) }
    arrowUp.setOnClickListener { onClickButton(0x77) }
    arrowDown.setOnClickListener { onClickButton(0x73) }
  }

  private fun step() {
    console.step()
    bgHandler.post { step() }
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
}
