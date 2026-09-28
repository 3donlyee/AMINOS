package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AmInoSManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AmInoS", appName)
  }

  @Test
  fun `verify AmInoSManager initialization`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = AmInoSManager(context)
    val status = manager.status.value
    assertNotNull(status)
    assertEquals("13.5.4", status.version)
  }
}
