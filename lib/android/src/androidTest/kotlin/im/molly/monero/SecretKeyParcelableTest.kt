package im.molly.monero

import android.os.Parcel
import com.google.common.truth.Truth
import org.junit.Test
import kotlin.random.Random

class SecretKeyParcelableTest {

    @Test
    fun testParcel() {
        val secret = Random.nextBytes(32)
        val originalKey = SecretKey(secret)

        val parcel = Parcel.obtain()

        originalKey.writeToParcel(parcel, 0)

        parcel.setDataPosition(0)

        val key = SecretKey.create(parcel)
        Truth.assertThat(key == originalKey).isTrue()
    }
}
