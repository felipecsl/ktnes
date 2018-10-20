package com.felipecsl.knes

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StatePersistenceTest {
  @Test fun `test dump and restore`() {
    val persisted = StatePersistence.dumpState(0, 1, 2, 4.5F, 2.3, intArrayOf(5, 6), false)
    val restored = StatePersistence.restoreState(persisted)
    assertThat(restored.next() as Int).isEqualTo(0)
    assertThat(restored.next() as Int).isEqualTo(1)
    assertThat(restored.next() as Int).isEqualTo(2)
    assertThat(restored.next() as Float).isEqualTo(4.5F)
    assertThat(restored.next() as Double).isEqualTo(2.3)
    assertThat(restored.next() as IntArray).isEqualTo(intArrayOf(5, 6))
    assertThat(restored.next() as Boolean).isEqualTo(false)
  }
}