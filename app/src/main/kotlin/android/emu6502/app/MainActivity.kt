package android.emu6502.app

import android.emu6502.R
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
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
  val fab: FloatingActionButton by bindView(R.id.fab)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    setSupportActionBar(toolbar)

    var ab: ActionBar = getSupportActionBar()
    ab.setDisplayHomeAsUpEnabled(true)
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
