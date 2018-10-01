# ktnes

A NES emulator implemented in Kotlin using multiplatform support and Kotlin/Native.

Ported from the awesome Golang implementation [nes by @fogleman](https://github.com/fogleman/nes).

## Demo

![super mario bros 3](https://raw.githubusercontent.com/felipecsl/ktnes/master/smb3.gif)
![the legend of zelda](https://raw.githubusercontent.com/felipecsl/ktnes/master/zelda.gif)

## Building/Running

You'll need https://github.com/google/oboe to run ktnes.
Please clone Oboe and put it in the same directory as ktnes, eg.:
`~/code/ktnes` and `~/code/oboe`.

## Kotlin/Native implementation

The emulator implementation using Kotlin/Native is not functional yet.
To build the Android native library implementation: 

- Set `ANDROID_HOME` to your Android SDK path (e.g. `/Users/jenny/Library/Android/sdk`). 
- Run `./gradlew android:native-lib:copyLibs`

If you did that correctly, you should be able to build and run directly from Android Studio.

## Status

Still under active development.

Sound works but is still a bit garbled/distorted.

Implemented mappers:

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
