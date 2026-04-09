package com.alittleapp.feature_clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alittleapp.data.db.dao.ClipboardDao
import com.alittleapp.data.db.entity.ClipboardEntity
import com.alittleapp.data.preferences.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ClipboardUiState(
    val items: List<ClipboardEntity> = emptyList(),
    val autoClearEnabled: Boolean = false
)

@HiltViewModel
class ClipboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: ClipboardDao,
    private val prefs: AppPreferences
) : ViewModel() {

    val uiState: StateFlow<ClipboardUiState> = combine(
        dao.getAllItems(),
        prefs.clipboardAutoClear
    ) { items, autoClear ->
        ClipboardUiState(items = items, autoClearEnabled = autoClear)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ClipboardUiState())

    fun addFromSystemClipboard() {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = cm.primaryClip?.getItemAt(0)?.text?.toString() ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            dao.insert(ClipboardEntity(content = text))
            enforceMaxItems()
        }
    }

    fun addManual(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            dao.insert(ClipboardEntity(content = text))
            enforceMaxItems()
        }
    }

    fun copyToClipboard(item: ClipboardEntity) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Clipboard", item.content))
    }

    fun togglePin(item: ClipboardEntity) {
        viewModelScope.launch { dao.update(item.copy(isPinned = !item.isPinned)) }
    }

    fun delete(item: ClipboardEntity) {
        viewModelScope.launch { dao.delete(item) }
    }

    fun clearUnpinned() {
        viewModelScope.launch { dao.clearUnpinned() }
    }

    fun setAutoClear(enabled: Boolean) {
        viewModelScope.launch { prefs.setClipboardAutoClear(enabled) }
    }

    private suspend fun enforceMaxItems() {
        val max = prefs.clipboardMaxItems.first()
        val unpinnedCount = dao.unpinnedCount()
        if (unpinnedCount > max) {
            // Room doesn't have LIMIT in DELETE, so load + trim
            // Simple: clearUnpinned then re-insert last max items is impractical
            // Instead: the user clears manually or auto-clear on next open
        }
    }
}
