package com.opennotes.settings.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.opennotes.settings.domain.model.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class WidgetSettingsTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun persistsPureBlackWidgetsAndExposesItToBothReaders() =
        runBlocking {
            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            try {
                val store = PreferenceDataStoreFactory.create(scope = scope) { File(temporaryFolder.root, "settings.preferences_pb") }
                val repository = DataStoreRepository(store)
                assertFalse(repository.getSettings().pureBlackWidgets)
                repository.saveSettings(Settings(pureBlackWidgets = true))
                assertTrue(repository.getSettings().pureBlackWidgets)
                assertTrue(repository.getSettingsFlow().first().pureBlackWidgets)
                repository.saveSettings(repository.getSettings().copy(pureBlackWidgets = false))
                assertFalse(repository.getSettingsFlow().first().pureBlackWidgets)
                assertEquals(false, repository.getSettings().blackTheme)
            } finally {
                scope.cancel()
            }
        }
}
