package com.felipecsl.android.nes.mappers

class CNROM : Mapper  {
  override fun write(address: Int, value: Int) {
  }

  override fun step() {
  }

  override fun read(address: Int): Int {
    throw NotImplementedError()
  }
}