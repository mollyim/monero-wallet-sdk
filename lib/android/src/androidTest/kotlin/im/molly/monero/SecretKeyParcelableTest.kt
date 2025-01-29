package im.molly.monero

import android.os.Parcel
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.random.Random

class SecretKeyParcelableTest {

    @Test
    fun testParcel() {
        val secret = Random.nextBytes(32)
        val originalKey = SecretKey(secret)

        val parcel = Parcel.obtain().apply {
            originalKey.writeToParcel(this, 0)
            setDataPosition(0)
        }

        val recreatedKey = SecretKey.CREATOR.createFromParcel(parcel)

        assertThat(recreatedKey).isEqualTo(originalKey)
    }
}
