package com.felipecsl.knes

interface PPUStepCallback {
  fun step(cycle: Int, scanLine: Int, frame: Int, paletteData: IntArray,
      nameTableData: IntArray, oamData: IntArray, v: Int, t: Int, x: Int, w: Int, f: Int,
      register: Int, nmiOccurred: Boolean, nmiOutput: Boolean,
      nmiPrevious: Boolean, nmiDelay: Int, nameTableByte: Int, attributeTableByte: Int,
      lowTileByte: Int, highTileByte: Int, tileData: Int, spriteCount: Int, spritePatterns: IntArray,
      spritePositions: IntArray, spritePriorities: IntArray, spriteIndexes: IntArray, flagNameTable: Int,
      flagIncrement: Int, flagSpriteTable: Int, flagBackgroundTable: Int, flagSpriteSize: Int,
      flagMasterSlave: Int, flagGrayscale: Int, flagShowLeftBackground: Int, flagShowLeftSprites: Int,
      flagShowBackground: Int, flagShowSprites: Int, flagRedTint: Int, flagGreenTint: Int,
      flagBlueTint: Int, flagSpriteZeroHit: Int, flagSpriteOverflow: Int, oamAddress: Int,
      bufferedData: Int
  )
}