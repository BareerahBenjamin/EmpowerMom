package com.empowermom.app.feature.dailylog.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.empowermom.app.core.data.repository.DailyLogRepository
import com.empowermom.app.core.data.repository.SupabaseRepository
import com.empowermom.app.feature.dailylog.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── UiState ───────────────────────────────────────────────────────────────────

data class DailyLogUiState(
    // 当前随机到的题目
    val currentCore: CoreQuestion = QuestionBank.coreQuestions.first(),
    val currentLife: LifeQuestion = QuestionBank.lifeQuestions.first(),
    val currentOpen: OpenQuestion = QuestionBank.openQuestions.first(),

    // 用户选择
    val q1Selected: QuestionOption? = null,
    val q2Selected: QuestionOption? = null,
    val q3Text: String = "",

    // 卡片
    val isCardVisible: Boolean = false,
    val aiCardText: String = "",
    val isGenerating: Boolean = false,

    // 历史 & 小结
    val allLogs: List<DailyLog> = emptyList(),
    val weeklySummary: String = "",
    val isSummaryLoading: Boolean = false,

    // 今天是否已记录
    val todayLogId: Long? = null,
    val isPrivate: Boolean = false,
    val syncStatus: String = ""
)

// ── Intent ────────────────────────────────────────────────────────────────────

sealed class DailyLogIntent {
    data object RefreshQ1 : DailyLogIntent()
    data object RefreshQ2 : DailyLogIntent()
    data object RefreshQ3 : DailyLogIntent()
    data class SelectQ1(val option: QuestionOption) : DailyLogIntent()
    data class SelectQ2(val option: QuestionOption) : DailyLogIntent()
    data class UpdateQ3Text(val text: String) : DailyLogIntent()
    data object GenerateCard : DailyLogIntent()
    data object TogglePrivacy : DailyLogIntent()
    data object LoadWeeklySummary : DailyLogIntent()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class DailyLogViewModel @Inject constructor(
    private val repository: DailyLogRepository,
    private val supabaseRepository: SupabaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DailyLogUiState())
    val uiState: StateFlow<DailyLogUiState> = _uiState.asStateFlow()

    init {
        // 随机初始化题目
        refreshAllQuestions()
        // 订阅历史记录
        viewModelScope.launch {
            repository.observeAll().collect { logs ->
                _uiState.update { it.copy(allLogs = logs) }
            }
        }
        // 检查今天是否已记录
        viewModelScope.launch {
            val today = repository.getTodayLog()
            if (today != null) {
                _uiState.update { it.copy(todayLogId = today.id, isPrivate = today.isPrivate) }
            }
        }
        // 订阅同步状态
        viewModelScope.launch {
            repository.lastSyncResult.collect { status ->
                _uiState.update { it.copy(syncStatus = status) }
            }
        }
        // 从 Supabase 拉取远程速记
        viewModelScope.launch {
            try {
                // 诊断表访问
                val diag = supabaseRepository.testTableAccess()
                Log.d("DailyLog", "Supabase 表诊断: $diag")

                repository.refreshFromRemote()
            } catch (e: Exception) {
                Log.e("DailyLog", "拉取远程速记失败: ${e.message}")
            }
        }
    }

    fun handleIntent(intent: DailyLogIntent) {
        when (intent) {
            DailyLogIntent.RefreshQ1 -> refreshQ1()
            DailyLogIntent.RefreshQ2 -> refreshQ2()
            DailyLogIntent.RefreshQ3 -> refreshQ3()
            is DailyLogIntent.SelectQ1 -> _uiState.update { it.copy(q1Selected = intent.option) }
            is DailyLogIntent.SelectQ2 -> _uiState.update { it.copy(q2Selected = intent.option) }
            is DailyLogIntent.UpdateQ3Text -> _uiState.update { it.copy(q3Text = intent.text) }
            DailyLogIntent.GenerateCard -> generateCard()
            DailyLogIntent.TogglePrivacy -> togglePrivacy()
            DailyLogIntent.LoadWeeklySummary -> loadWeeklySummary()
        }
    }

    private fun refreshAllQuestions() {
        _uiState.update {
            it.copy(
                currentCore = QuestionBank.coreQuestions.random(),
                currentLife = QuestionBank.lifeQuestions.random(),
                currentOpen = QuestionBank.openQuestions.random(),
                q1Selected = null, q2Selected = null, q3Text = "",
                isCardVisible = false, aiCardText = ""
            )
        }
    }

    private fun refreshQ1() {
        _uiState.update {
            it.copy(
                currentCore = QuestionBank.coreQuestions.random(),
                q1Selected = null
            )
        }
    }

    private fun refreshQ2() {
        _uiState.update {
            it.copy(
                currentLife = QuestionBank.lifeQuestions.random(),
                q2Selected = null
            )
        }
    }

    private fun refreshQ3() {
        _uiState.update {
            it.copy(
                currentOpen = QuestionBank.openQuestions.random(),
                q3Text = ""
            )
        }
    }

    private fun generateCard() {
        val state = _uiState.value
        Log.d("DailyLog", "generateCard: q1=${state.q1Selected?.text}, q2=${state.q2Selected?.text}, q3=${state.q3Text}")
        _uiState.update { it.copy(isGenerating = true, isCardVisible = true) }

        viewModelScope.launch {
            try {
                // 先保存到数据库
                val log = DailyLog(
                    q1Type     = state.currentCore.type,
                    q1Answer   = state.q1Selected?.text ?: "未选择",
                    q1Color    = state.q1Selected?.colorToken ?: "",
                    q2Question = state.currentLife.title,
                    q2Answer   = state.q2Selected?.text ?: "未选择",
                    q3Question = state.currentOpen.title,
                    q3Text     = state.q3Text
                )
                val id = repository.save(log)
                Log.d("DailyLog", "保存成功 id=$id")
                _uiState.update { it.copy(todayLogId = id) }

                // 异步请求 AI 文案
                val aiText = repository.generateCardText(
                    q1Type     = state.currentCore.type,
                    q1Answer   = state.q1Selected?.text ?: "未选择",
                    q2Question = state.currentLife.title,
                    q2Answer   = state.q2Selected?.text ?: "未选择",
                    q3Question = state.currentOpen.title,
                    q3Text     = state.q3Text
                )
                Log.d("DailyLog", "AI 文案: $aiText")
                repository.updateAiCardText(id, aiText)
                _uiState.update { it.copy(aiCardText = aiText, isGenerating = false) }
            } catch (e: Exception) {
                Log.e("DailyLog", "生成卡片失败: ${e.message}", e)
                _uiState.update { it.copy(isGenerating = false, aiCardText = "今天也辛苦了，你已经很棒了 💛") }
            }
        }
    }

    private fun togglePrivacy() {
        val id = _uiState.value.todayLogId ?: return
        val newPrivate = !_uiState.value.isPrivate
        _uiState.update { it.copy(isPrivate = newPrivate) }
        viewModelScope.launch {
            repository.togglePrivacy(id, newPrivate)
        }
    }

    private fun loadWeeklySummary() {
        // 少于 7 条不生成
        val logs = _uiState.value.allLogs
        if (logs.size < 7) return
        _uiState.update { it.copy(isSummaryLoading = true) }
        viewModelScope.launch {
            val summary = repository.generateWeeklySummary(
                repository.getRecentLogs()
            )
            _uiState.update { it.copy(weeklySummary = summary, isSummaryLoading = false) }
        }
    }
}
