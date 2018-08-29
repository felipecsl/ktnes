package com.felipecsl.android

import java.util.*

class Labels(private val assembler: Assembler,
    private val symbols: Map<String, String>) : HashMap<String, Int>() {
  private val labelIndex: ArrayList<String> = ArrayList()

  fun indexLines(lines: List<String>) {
    lines.forEach { line ->
      indexLine(line)
    }
  }

  override fun get(label: String): Int {
    return super.get(label) ?: -1
  }

  // Extract label if line contains one and calculate position in memory.
  // Return false if label already exists.
  private fun indexLine(input: String) {
    // Figure out how many bytes this instruction takes
    val currentPC = assembler.defaultCodePC
    // TODO: find a better way for Labels to have access to assembler
    assembler.assembleLine(input);

    // Find command or label
    if (input.matches("^\\w+:".toRegex())) {
      val label = input.replace("(^\\w+):.*$".toRegex(), "$1")

      if (symbols.get(label) != null) {
        throw RuntimeException(
            "**Label ${label}is already used as a symbol; please rename one of them**")
      }

      put(label, currentPC)
    }
  }
}