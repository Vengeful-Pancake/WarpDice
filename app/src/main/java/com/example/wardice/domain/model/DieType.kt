// domain/model/DieType.kt
package com.example.wardice.domain.model

enum class DieType(val sides: Int) {
    D2(2),
    D3(3),
    D4(4),
    D6(6),
    D8(8),
    D10(10),
    D12(12),
    D20(20),
    D100(100);

    override fun toString(): String = "d$sides"
}
