package com.empowermom.app.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// DataStore 实例（每个 Context 只创建一次）
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "editor_draft")

@Singleton
class DraftRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val KEY_CONTENT = stringPreferencesKey("draft_content")
    private val KEY_CATEGORY = stringPreferencesKey("draft_category")
    private val KEY_TAGS = stringPreferencesKey("draft_tags") // 用逗号分隔

    // 读取草稿（返回 Flow，自动更新）
    val draftContent: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_CONTENT] ?: ""
    }
    val draftCategory: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_CATEGORY] ?: ""
    }
    val draftTags: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_TAGS] ?: ""
    }

    // 保存草稿
    suspend fun saveDraft(content: String, category: String, tags: List<String>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CONTENT] = content
            prefs[KEY_CATEGORY] = category
            prefs[KEY_TAGS] = tags.joinToString(",")
        }
    }

    // 清空草稿（发布成功后调用）
    suspend fun clearDraft() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_CONTENT)
            prefs.remove(KEY_CATEGORY)
            prefs.remove(KEY_TAGS)
        }
    }
}
