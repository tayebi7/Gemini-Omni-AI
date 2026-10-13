package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AIProvider
import com.example.data.network.OfflineAIIntelligence
import com.example.data.repository.AppRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Gemini Omni", appName)
  }

  @Test
  fun `test offline AI synthesizer generates comprehensive content`() {
    val response = OfflineAIIntelligence.generateOfflineResponse("دراسة منتج وتحليل SWOT")
    assertTrue(response.isNotBlank())
    assertTrue(response.contains("SWOT") || response.contains("التحليل"))
  }

  @Test
  fun `test repository session creation and provider switching`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = AppRepository(context)

    val sessionId = repository.createNewSession("جلسة تجريبية")
    assertNotNull(sessionId)

    repository.setProvider(AIProvider.NVIDIA_NIM)
    assertEquals(AIProvider.NVIDIA_NIM, repository.currentProvider.value)

    repository.setProvider(AIProvider.CUSTOM_MANUAL)
    assertEquals(AIProvider.CUSTOM_MANUAL, repository.currentProvider.value)

    repository.setAutoRouting(true)
    assertTrue(repository.isAutoRouting.value)
  }
}
