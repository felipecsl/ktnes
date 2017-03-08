# 6502 Android

Ported from the awesome Javascript implementation [Easy 6502 by Nick Morgan](http://skilldrick.github.io/easy6502/).

This project is my first stab at learning Kotlin and doing Emulator programmimg. This emulator was
based on the project mentioned above but not ported verbatim. A Kotlin and Android optimized
solution was implemented. As of now, it is able to assemble and run simple programs, including
the [snake6502](https://gist.github.com/wkjagt/9043907) implementation by Willem van der Jagt, which
was also used in the Easy 6502 "ebook".

## Demo

![](https://raw.githubusercontent.com/felipecsl/6502Android/master/demo.gif)

## Status

There are still some instructions not implemented yet. I will update the CPU class soon with the
implementation for the remaining ones

## License

```
Copyright 2015 Felipe Lima

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