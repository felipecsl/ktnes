package android.emu6502.app

import android.emu6502.Display
import android.emu6502.Emulator
import android.emu6502.R
import android.os.Bundle
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import butterknife.bindView

public class MainActivity : AppCompatActivity() {

  val toolbar: Toolbar by bindView(R.id.toolbar)
  val txtA: TextView by bindView(R.id.A)
  val txtX: TextView by bindView(R.id.X)
  val txtY: TextView by bindView(R.id.Y)
  val txtSP: TextView by bindView(R.id.SP)
  val txtPC: TextView by bindView(R.id.PC)
  val txtFlags: TextView by bindView(R.id.PC)
  val displayWrapper: FrameLayout by bindView(R.id.display_wrapper)
  val display: Display by bindView(R.id.display)
  val txtInstructions: TextView by bindView(R.id.txtInstructions)
  val fabRun: FloatingActionButton by bindView(R.id.fabRun)
  val layoutContent: CoordinatorLayout by bindView(R.id.layout_content)
  val btnLeft: Button by bindView(R.id.arrowLeft)
  val btnRight: Button by bindView(R.id.arrowRight)
  val btnUp: Button by bindView(R.id.arrowUp)
  val btnDown: Button by bindView(R.id.arrowDown)
  val btnReset: Button by bindView(R.id.btnReset)

  private var emulator: Emulator? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    setSupportActionBar(toolbar)

    var ab: ActionBar = getSupportActionBar()
    ab.setDisplayHomeAsUpEnabled(true)

    fabRun.setOnClickListener {
      displayWrapper.setVisibility(View.VISIBLE)
      emulator = Emulator(display)
      val emu: Emulator = emulator as Emulator
      emu.assembler.assembleCode(txtInstructions.getText().toString().splitBy("\n"))
      Snackbar.make(layoutContent,
          "Code assembled successfully, ${emu.assembler.codeLen} bytes.",
          Snackbar.LENGTH_SHORT).show()
      emu.cpu.run()
    }

    btnReset.setOnClickListener {
      val emu: Emulator = emulator as Emulator
      emu.reset()
    }

    val onClickButton = { code: Int ->
      if (emulator != null) {
        val emu = emulator as Emulator
        emu.cpu.memory.storeKeypress(code)
      }
    }
    btnLeft.setOnClickListener  { onClickButton(0x61) }
    btnRight.setOnClickListener { onClickButton(0x64) }
    btnUp.setOnClickListener    { onClickButton(0x77) }
    btnDown.setOnClickListener  { onClickButton(0x73) }
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem?): Boolean {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    val id = item!!.getItemId()

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true
    }

    return super.onOptionsItemSelected(item)
  }
}
