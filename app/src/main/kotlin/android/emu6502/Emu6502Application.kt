package android.emu6502

import android.app.Application
import com.facebook.stetho.Stetho

class Emu6502Application : Application() {
  override fun onCreate() {
    super.onCreate()
    Stetho.initialize(Stetho.newInitializerBuilder(this)
        .enableWebKitInspector(Stetho.defaultInspectorModulesProvider(this))
        .build())
  }
}

