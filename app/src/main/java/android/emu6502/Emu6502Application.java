package android.emu6502;

import android.app.Application;

import com.facebook.stetho.Stetho;

public class Emu6502Application extends Application {

  @Override public void onCreate() {
    super.onCreate();
    Stetho.initialize(
        Stetho.newInitializerBuilder(this)
            .enableWebKitInspector(Stetho.defaultInspectorModulesProvider(this))
            .build());
  }
}
