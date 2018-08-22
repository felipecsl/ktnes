package com.felipecsl.android.app

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import com.felipecsl.android.R
import com.felipecsl.android.nes.Console
import com.felipecsl.android.nes.INESFileParser
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.toolbar.*
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
  private lateinit var console: Console
  private val executor = Executors.newSingleThreadExecutor()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    setSupportActionBar(toolbar)
    val actionBar: ActionBar = supportActionBar!!
    actionBar.setDisplayHomeAsUpEnabled(true)
    fabRun.setOnClickListener {
      display_wrapper.visibility = View.VISIBLE
      val cartridge = INESFileParser.parseCartridge(resources.openRawResource(R.raw.smb3))
      console = Console.newConsole(cartridge, surface_view)
      console.reset()
      executor.submit { step() }
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
    executor.submit { step() }
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
