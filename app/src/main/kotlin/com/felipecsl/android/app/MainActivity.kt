package com.felipecsl.android.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import com.felipecsl.android.R
import com.felipecsl.android.nes.Console
import com.felipecsl.android.nes.INESFileParser
import com.felipecsl.android.toHexString
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.toolbar.*
import java.util.concurrent.Executors
import java.util.logging.Logger

class MainActivity : AppCompatActivity() {
  private lateinit var console: Console
  private val LOG = Logger.getLogger("NesGLRenderer")
  private var lastSecond: Long = 0
  private var totalCycles: Long = 0
  private val executor = Executors.newSingleThreadExecutor()
  private val cpuRunnable = object : Runnable {
    @SuppressLint("SetTextI18n")
    override fun run() {
      totalCycles += console.step()
      val cycleEnd = System.currentTimeMillis()
      if (cycleEnd - lastSecond >= 1000) {
        val finalCycles = totalCycles
        // a second has passed
        runOnUiThread {
          frequency.text = "Frequency=${finalCycles / 1000}KHz"
          A.text = "A=$${console.cpu.A.toHexString()}"
          X.text = "X=$${console.cpu.X.toHexString()}"
          Y.text = "Y=$${console.cpu.Y.toHexString()}"
          SP.text = "SP=$${console.cpu.SP.toHexString()}"
          PC.text = "PC=$${console.cpu.PC.toHexString()}"
          flags.text = "flags=$${console.cpu.flags().toHexString()}"
        }
        lastSecond = System.currentTimeMillis()
        totalCycles = 0
      }
      executor.submit(this)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    setSupportActionBar(toolbar)
    val actionBar: ActionBar = supportActionBar!!
    actionBar.setDisplayHomeAsUpEnabled(true)
    fabRun.setOnClickListener {
      val cartridge = INESFileParser.parseCartridge(resources.openRawResource(R.raw.smb3))
      console = Console.newConsole(cartridge, surface_view)
      console.reset()
      lastSecond = System.currentTimeMillis()
      executor.submit(cpuRunnable)
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
