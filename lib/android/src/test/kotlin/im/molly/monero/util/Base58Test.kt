package im.molly.monero.util

import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Test

@OptIn(ExperimentalStdlibApi::class)
class Base58Test {

    // Test cases from monero unit_tests/base58.cpp

    private val base58ToHex = mapOf(
        // 2-bytes block
        "11" to "00",
        "1z" to "39",
        "5Q" to "FF",
        // 3-bytes block
        "111" to "0000",
        "11z" to "0039",
        "15R" to "0100",
        "LUv" to "FFFF",
        // 5-bytes block
        "11111" to "000000",
        "1111z" to "000039",
        "11LUw" to "010000",
        "2UzHL" to "FFFFFF",
        // 6-bytes block
        "11111z" to "00000039",
        "7YXq9G" to "FFFFFFFF",
        // 7-bytes block
        "111111z" to "0000000039",
        "VtB5VXc" to "FFFFFFFFFF",
        // 9-bytes block
        "11111111z" to "000000000039",
        "3CUsUpv9t" to "FFFFFFFFFFFF",
        // 10-bytes block
        "111111111z" to "00000000000039",
        "Ahg1opVcGW" to "FFFFFFFFFFFFFF",
        // 11-bytes block
        "1111111111z" to "0000000000000039",
        "jpXCZedGfVQ" to "FFFFFFFFFFFFFFFF",
        "11111111111" to "0000000000000000",
        "11111111112" to "0000000000000001",
        "11111111119" to "0000000000000008",
        "1111111111A" to "0000000000000009",
        "11111111121" to "000000000000003A",
        "1Ahg1opVcGW" to "00FFFFFFFFFFFFFF",
        "22222222222" to "06156013762879F7",
        "1z111111111" to "05E022BA374B2A00",
        // Multiple blocks
        "1111111111111" to "000000000000000000",
        "11111111111111" to "00000000000000000000",
        "1111111111111111" to "0000000000000000000000",
        "11111111111111111" to "000000000000000000000000",
        "111111111111111111" to "00000000000000000000000000",
        "11111111111111111111" to "0000000000000000000000000000",
        "111111111111111111111" to "000000000000000000000000000000",
        "1111111111111111111111" to "00000000000000000000000000000000",
        "jpXCZedGfVQ5Q" to "FFFFFFFFFFFFFFFFFF",
        "jpXCZedGfVQLUv" to "FFFFFFFFFFFFFFFFFFFF",
        "jpXCZedGfVQ2UzHL" to "FFFFFFFFFFFFFFFFFFFFFF",
        "jpXCZedGfVQ7YXq9G" to "FFFFFFFFFFFFFFFFFFFFFFFF",
        "22222222222VtB5VXc" to "06156013762879F7FFFFFFFFFF",
        "jpXCZedGfVQVtB5VXc" to "FFFFFFFFFFFFFFFFFFFFFFFFFF",
        "jpXCZedGfVQ3CUsUpv9t" to "FFFFFFFFFFFFFFFFFFFFFFFFFFFF",
        "jpXCZedGfVQAhg1opVcGW" to "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFF",
        "jpXCZedGfVQjpXCZedGfVQ" to "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF",
    )

    private val overflows = listOf(
        "5R",
        "zz",
        "LUw",
        "zzz",
        "2UzHM",
        "zzzzz",
        "7YXq9H",
        "zzzzzz",
        "VtB5VXd",
        "zzzzzzz",
        "3CUsUpv9u",
        "zzzzzzzzz",
        "Ahg1opVcGX",
        "zzzzzzzzzz",
        "jpXCZedGfVR",
        "zzzzzzzzzzz",
        "123456789AB5R",
        "123456789ABzz",
        "123456789ABLUw",
        "123456789ABzzz",
        "123456789AB2UzHM",
        "123456789ABzzzzz",
        "123456789AB7YXq9H",
        "123456789ABzzzzzz",
        "123456789ABVtB5VXd",
        "123456789ABzzzzzzz",
        "123456789AB3CUsUpv9u",
        "123456789ABzzzzzzzzz",
        "123456789ABAhg1opVcGX",
        "123456789ABzzzzzzzzzz",
        "123456789ABjpXCZedGfVR",
        "123456789ABzzzzzzzzzzz",
        "zzzzzzzzzzz11",
    )

    private val invalidSymbols = listOf(
        "10",
        "11I",
        "11O11",
        "11l111",
        "11_11111111",
        "1101111111111",
        "11I11111111111111",
        "11O1111111111111111111",
        "1111111111110",
        "111111111111l1111",
        "111111111111_111111111",
    )

    private val invalidLengths = listOf(
        "1",
        "z",
        "1111",
        "zzzz",
        "11111111",
        "zzzzzzzz",
        "123456789AB1",
        "123456789ABz",
        "123456789AB1111",
        "123456789ABzzzz",
        "123456789AB11111111",
        "123456789ABzzzzzzzz",
    )

    @Test
    fun `decode valid base58 strings`() {
        base58ToHex.forEach { (input, expected) ->
            assertThat(input.decodeBase58()).isEqualTo(expected.hexToByteArray())
        }
    }

    @Test
    fun `empty string decodes to zero length`() {
        assertThat("".decodeBase58()).hasLength(0)
    }

    @Test
    fun `error on overflows`() {
        overflows.forEach {
            val thrown =
                Assert.assertThrows(IllegalArgumentException::class.java) { it.decodeBase58() }
            assertThat(thrown.message).ignoringCase().contains("overflow")
        }
    }

    @Test
    fun `error decoding invalid lengths`() {
        invalidLengths.forEach {
            Assert.assertThrows(IllegalArgumentException::class.java) { it.decodeBase58() }
        }
    }

    @Test
    fun `error decoding invalid symbols`() {
        invalidSymbols.forEach {
            Assert.assertThrows(IllegalArgumentException::class.java) { it.decodeBase58() }
        }
    }
}
