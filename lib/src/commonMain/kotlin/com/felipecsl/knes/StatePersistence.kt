package com.felipecsl.knes

/** Poor man's very simple state serialization */
internal object StatePersistence {
  internal class State(private val data: List<Any>) {
    private var pos = 0

    fun <T> next(): T {
      @Suppress("UNCHECKED_CAST")
      return data[pos++] as T
    }
  }

  fun dumpState(vararg data: Any) =
    data.joinToString("\n") {
      when (it) {
        is Int -> "Int:$it"
        is Long -> "Long:$it"
        is Float -> "Float:$it"
        is String -> "String:$it"
        is Double -> "Double:$it"
        is Boolean -> "Boolean:$it"
        is IntArray -> it.joinToString(prefix = "IntArray:")
        else -> throw RuntimeException("Unsupported serialization type ${it::class}")
      }
    }

  @Suppress("IMPLICIT_CAST_TO_ANY")
  fun restoreState(state: String) =
    State(state.split("\n").map {
      val (type, value) = it.split(":")
      when (type) {
        "String" -> value
        "Int" -> value.toInt()
        "Long" -> value.toLong()
        "Float" -> value.toFloat()
        "Double" -> value.toDouble()
        "Boolean" -> value.toBoolean()
        "IntArray" -> value.toIntArray()
        else -> throw RuntimeException("Unsupported deserialization type $type")
      }
    })

  private fun String.toIntArray() =
    split(", ").map(String::toInt).toIntArray()
}