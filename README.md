# ktnes

A NES emulator implemented in Kotlin using multiplatform support and Kotlin/Native.

Inspired on the following prior work:

* Go emulator [nes by @fogleman](https://github.com/fogleman/nes)
* JS emulator [jsnes by @bfirsh](https://github.com/bfirsh/jsnes)

## Demo

![super mario bros 3](https://raw.githubusercontent.com/felipecsl/ktnes/master/smb3.gif)
![the legend of zelda](https://raw.githubusercontent.com/felipecsl/ktnes/master/zelda.gif)

## Building/Running

You'll need to pull the oboe git submodule to run ktnes:
```
git submodule init && git submodule update
```

## Android implementation

You should be able to build and run directly from Android Studio.

If you'd rather use Gradle, run:
```
./gradlew android:installDebug
```

## Javascript implementation

To build/run the Javascript version of the emulator, run:
```
./gradlew web:run
```

Then, open `http://localhost:8088` on your browser.

## Status

Still under active development.

Sound works but is still a bit garbled/distorted.

### Implemented mappers:

* Mapper 0
* Mapper 1
* Mapper 4 (MMC3)

## License

```
Copyright 2018 Felipe Lima

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
