package im.molly.monero.sdk.internal

import android.os.ParcelFileDescriptor
import com.google.common.truth.Truth.assertThat
import im.molly.monero.sdk.WalletDataStore
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.junit4.MockKRule
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException

class DataStoreAdapterTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    private val dataStore = mockk<WalletDataStore>(relaxed = true)
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private val testIOException = IOException("Test IO Exception")

    private lateinit var adapter: DataStoreAdapter

    @Before
    fun setUp() {
        adapter = DataStoreAdapter(dataStore, ioDispatcher = testDispatcher)
    }

    @Test
    fun overwriteIsPassedToDataStore() = runTest(testDispatcher) {
        coEvery { dataStore.save(any(), any()) } returns Unit

        listOf(true, false).forEach {
            adapter.saveWithFd(overwrite = it) {}
            coVerify(exactly = 1) { dataStore.save(any(), overwrite = it) }
        }
    }

    @Test
    fun propagatesIOExceptionWhenLoadFails() = runTest(testDispatcher) {
        coEvery { dataStore.load() } throws testIOException

        val exception = runCatching {
            adapter.loadWithFd {}
        }.exceptionOrNull()

        assertThat(exception).isEqualTo(testIOException)
    }

    @Test
    fun propagatesIOExceptionWhenSaveFails() = runTest(testDispatcher) {
        coEvery { dataStore.save(any(), any()) } throws testIOException

        val exception = runCatching {
            adapter.saveWithFd(overwrite = true) {}
        }.exceptionOrNull()

        assertThat(exception).isEqualTo(testIOException)
    }

    @Test
    fun pipeIsAlwaysClosedAfterLoad() = runTest(testDispatcher) {
        val (readFd, writeFd) = mockPipe()

        coEvery { dataStore.load() } returns ByteArrayInputStream(byteArrayOf(1, 2, 3))

        adapter.loadWithFd({})
        coVerify { readFd.close() }
        coVerify { writeFd.close() }

        clearAllMocks(answers = false)

        runCatching {
            adapter.loadWithFd({ throw RuntimeException() })
        }
        coVerify { readFd.close() }
        coVerify { writeFd.close() }
    }

    @Test
    fun pipeIsAlwaysClosedAfterSave() = runTest(testDispatcher) {
        val (readFd, writeFd) = mockPipe()

        coEvery { dataStore.save(any(), any()) } returns Unit

        adapter.saveWithFd(overwrite = true, {})
        coVerify { readFd.close() }
        coVerify { writeFd.close() }

        clearAllMocks(answers = false)

        runCatching {
            adapter.saveWithFd(overwrite = true, { throw RuntimeException() })
        }
        coVerify { readFd.close() }
        coVerify { writeFd.close() }
    }

    private fun mockPipe(): Pair<ParcelFileDescriptor, ParcelFileDescriptor> {
        val readFd = mockk<ParcelFileDescriptor>(relaxed = true)
        val writeFd = mockk<ParcelFileDescriptor>(relaxed = true)

        mockkStatic(ParcelFileDescriptor::class)
        coEvery { ParcelFileDescriptor.createPipe() } returns arrayOf(readFd, writeFd)

        return readFd to writeFd
    }
}
