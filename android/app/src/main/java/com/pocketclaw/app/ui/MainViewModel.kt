package com.pocketclaw.app.ui

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocketclaw.app.PocketClawApplication
import com.pocketclaw.app.brain.BrainEngine
import com.pocketclaw.app.brain.DashScopeProvider
import com.pocketclaw.app.brain.LocalLlmProvider
import com.pocketclaw.app.brain.SkillRouter
import com.pocketclaw.app.data.CustomSkill
import com.pocketclaw.app.data.Memory
import com.pocketclaw.app.data.Preferences
import com.pocketclaw.app.ui.chat.ChatMessage
import com.pocketclaw.app.voice.KokoroTtsEngine
import com.pocketclaw.app.voice.LlamaEngine
import com.pocketclaw.app.voice.WhisperEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "MainVM"
    }

    private val app = application as PocketClawApplication
    private val memoryDao = app.database.memoryDao()
    private val skillDao = app.database.customSkillDao()

    val whisperEngine = WhisperEngine(application)
    val ttsEngine = KokoroTtsEngine(application)
    val llamaEngine = LlamaEngine(application)
    private val localProvider = LocalLlmProvider(llamaEngine)
    private val apiProvider = DashScopeProvider()

    private val brain = BrainEngine(
        if (Preferences.llmMode == "api") apiProvider else localProvider
    )

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    val memories: StateFlow<List<Memory>> = memoryDao.getAllFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customSkills: StateFlow<List<CustomSkill>> = skillDao.getAllFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _interactionCount = MutableStateFlow(0)
    val interactionCount: StateFlow<Int> = _interactionCount.asStateFlow()

    private val _skillUsage = MutableStateFlow<Map<String, Int>>(emptyMap())
    val skillUsage: StateFlow<Map<String, Int>> = _skillUsage.asStateFlow()

    private val _llmMode = MutableStateFlow(Preferences.llmMode)
    val llmMode: StateFlow<String> = _llmMode.asStateFlow()

    private val _llmReady = MutableStateFlow(false)
    val llmReady: StateFlow<Boolean> = _llmReady.asStateFlow()

    val growthStage: StateFlow<Int> = combine(
        _interactionCount, memories
    ) { interactions, mems ->
        when {
            interactions >= 500 && mems.size >= 50 -> 4
            interactions >= 200 && mems.size >= 30 -> 3
            interactions >= 50 && mems.size >= 10 -> 2
            interactions >= 10 -> 1
            else -> 0
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        initEngines()
        viewModelScope.launch {
            skillDao.getAllEnabledFlow().collect { skills ->
                SkillRouter.updateCustomSkills(skills)
            }
        }
    }

    private fun initEngines() {
        viewModelScope.launch {
            if (whisperEngine.isModelDownloaded) {
                whisperEngine.loadModel()
                Log.i(TAG, "Whisper STT ready: ${whisperEngine.isReady}")
            } else {
                Log.w(TAG, "Whisper model not found")
            }
            if (ttsEngine.isModelDownloaded) {
                ttsEngine.loadModel()
                Log.i(TAG, "Kokoro TTS ready: ${ttsEngine.isReady}")
            } else {
                Log.w(TAG, "Kokoro TTS models not found")
            }
            if (llamaEngine.isModelDownloaded) {
                val loaded = llamaEngine.loadModel()
                _llmReady.value = loaded || apiProvider.isReady
                if (loaded) Log.i(TAG, "Local LLM ready")
            } else {
                _llmReady.value = apiProvider.isReady
            }
        }
    }

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun switchLlmMode(mode: String) {
        Preferences.llmMode = mode
        _llmMode.value = mode
        when (mode) {
            "local" -> brain.switchProvider(localProvider)
            "api" -> brain.switchProvider(apiProvider)
        }
        _llmReady.value = brain.isReady
    }

    fun sendTextMessage(text: String) {
        val userMsg = ChatMessage(text = text, isUser = true)
        _messages.update { it + userMsg }

        if (SkillRouter.isAddSkillRequest(text)) {
            handleSkillCreation(text)
        } else {
            processWithBrain(text)
        }
    }

    private fun handleSkillCreation(text: String) {
        _isProcessing.value = true
        viewModelScope.launch {
            try {
                val skillDef = brain.generateSkillDefinition(text)
                if (skillDef != null) {
                    val skillId = skillDef.name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
                    val existing = skillDao.findBySkillId(skillId)
                    if (existing != null) {
                        skillDao.update(existing.copy(
                            name = skillDef.name,
                            description = skillDef.description,
                            keywords = skillDef.keywords.joinToString(","),
                        ))
                    } else {
                        skillDao.insert(CustomSkill(
                            skillId = skillId,
                            name = skillDef.name,
                            description = skillDef.description,
                            keywords = skillDef.keywords.joinToString(","),
                        ))
                    }
                    val reply = ChatMessage(
                        text = "Skill added: **${skillDef.name}**\n\n${skillDef.description}\n\nKeywords: ${skillDef.keywords.joinToString(", ")}",
                        isUser = false,
                    )
                    _messages.update { it + reply }
                    _interactionCount.update { it + 1 }
                } else {
                    val reply = ChatMessage(
                        text = "I couldn't parse a skill definition from that. Try: \"Add a skill for [name] that does [description]\"",
                        isUser = false,
                    )
                    _messages.update { it + reply }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Skill creation failed: ${e.message}", e)
                _messages.update { it + ChatMessage(text = "Failed to create skill: ${e.message}", isUser = false) }
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun newTopic() {
        _messages.value = emptyList()
        _inputText.value = ""
    }

    fun deleteMessage(message: ChatMessage) {
        _messages.update { list -> list.filter { it.id != message.id } }
    }

    fun startRecording() {
        if (!whisperEngine.isModelDownloaded) {
            showToast("Whisper model not installed. Push ggml-small-q5_1.bin via ADB or download in-app.")
            return
        }
        if (!whisperEngine.isReady) {
            viewModelScope.launch {
                whisperEngine.loadModel()
                if (whisperEngine.isReady) doRecord() else showToast("Failed to load Whisper model")
            }
            return
        }
        if (!whisperEngine.hasRecordPermission()) {
            showToast("Microphone permission required")
            return
        }
        doRecord()
    }

    private fun doRecord() {
        _isRecording.value = true
        viewModelScope.launch {
            val transcript = whisperEngine.recordAndTranscribe()
            _isRecording.value = false
            if (transcript.isNotBlank()) {
                sendTextMessage(transcript.trim())
            }
        }
    }

    fun stopRecording() {
        whisperEngine.stopRecording()
        _isRecording.value = false
    }

    private fun processWithBrain(text: String) {
        _isProcessing.value = true
        viewModelScope.launch {
            try {
                val currentMemories = memoryDao.getAll()
                val response = brain.processUserMessage(text, currentMemories)

                _interactionCount.update { it + 1 }

                val skillId = response.meta.skillUsed
                _skillUsage.update { map ->
                    map + (skillId to ((map[skillId] ?: 0) + 1))
                }

                val replyText = response.actions
                    .filter { it.type == "notify" }
                    .joinToString("\n") { "${it.body ?: ""}".trim() }
                    .ifBlank { "Done." }

                val assistantMsg = ChatMessage(text = replyText, isUser = false)
                _messages.update { it + assistantMsg }

                response.memories?.forEach { mem ->
                    val existing = memoryDao.findByKey(mem.key)
                    if (existing != null) {
                        memoryDao.update(existing.copy(
                            value = mem.value,
                            confidence = mem.confidence,
                            updatedAt = Instant.now(),
                        ))
                    } else {
                        memoryDao.insert(Memory(
                            type = mem.type,
                            key = mem.key,
                            value = mem.value,
                            confidence = mem.confidence,
                            source = text.take(50),
                        ))
                    }
                }

                // TTS is now manual — user taps the speak button per message
            } catch (e: Exception) {
                Log.e(TAG, "Brain processing failed: ${e.message}", e)
                val errorMsg = ChatMessage(
                    text = "Processing failed: ${e.message}",
                    isUser = false,
                )
                _messages.update { it + errorMsg }
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun speakMessage(text: String) {
        if (!ttsEngine.isReady) {
            if (!ttsEngine.isModelDownloaded) {
                showToast("TTS model not installed")
                return
            }
            viewModelScope.launch {
                ttsEngine.loadModel()
                if (ttsEngine.isReady) ttsEngine.speakAndPlay(text)
                else showToast("Failed to load TTS model")
            }
            return
        }
        viewModelScope.launch { ttsEngine.speakAndPlay(text) }
    }

    fun deleteMemory(memory: Memory) {
        viewModelScope.launch { memoryDao.delete(memory) }
    }

    fun deleteSkill(skill: CustomSkill) {
        viewModelScope.launch { skillDao.delete(skill) }
    }

    fun toggleSkill(skill: CustomSkill) {
        viewModelScope.launch { skillDao.update(skill.copy(enabled = !skill.enabled)) }
    }

    private fun showToast(msg: String) {
        Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
    }

    override fun onCleared() {
        super.onCleared()
        whisperEngine.release()
        ttsEngine.release()
        llamaEngine.release()
    }
}
