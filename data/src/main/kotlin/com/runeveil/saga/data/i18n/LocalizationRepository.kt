package com.runeveil.saga.data.i18n

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import dagger.hilt.android.qualifiers.ApplicationContext
import com.runeveil.saga.data.di.IoDispatcher
import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves the content string keys (`species_glutwelp_name`, `mv_glutklaue_desc`)
 * that the domain models carry.
 *
 * Why not `strings.xml`? Because content ships ~4 500 generated strings, and
 * regenerating an XML resource file on every content change would make content
 * edits require a full app rebuild. UI chrome ("Speichern", "Abbrechen") still
 * lives in `strings.xml` — this class is only for *content* text.
 *
 * Missing keys return the key itself, which makes gaps immediately obvious in
 * the UI instead of silently rendering nothing.
 */
@Singleton
class LocalizationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val mutex = Mutex()
    private val _language = MutableStateFlow(DEFAULT_LANGUAGE)

    /** The language currently in use; drives recomposition when switched. */
    val language: StateFlow<String> = _language.asStateFlow()

    @Volatile
    private var strings: Map<String, String> = emptyMap()

    @Volatile
    private var fallback: Map<String, String> = emptyMap()

    /**
     * Loads [languageTag] (or the system language when null) plus the English
     * fallback. Safe to call repeatedly.
     */
    suspend fun load(languageTag: String? = null) {
        val requested = resolveLanguage(languageTag)
        if (requested == _language.value && strings.isNotEmpty()) return
        mutex.withLock {
            withContext(ioDispatcher) {
                if (fallback.isEmpty()) {
                    fallback = readLanguage(FALLBACK_LANGUAGE)
                }
                strings = if (requested == FALLBACK_LANGUAGE) fallback else readLanguage(requested)
            }
            _language.value = requested
        }
    }

    /** Resolves [key]; falls back to English, then to the key itself. */
    operator fun get(key: String): String =
        strings[key] ?: fallback[key] ?: key

    /** Resolves [key] and substitutes `{0}`, `{1}` … with [args]. */
    fun format(key: String, vararg args: Any): String {
        var text = get(key)
        args.forEachIndexed { index, value ->
            text = text.replace("{$index}", value.toString())
        }
        return text
    }

    /** True when the key exists — used by debug tooling and tests. */
    fun contains(key: String): Boolean = key in strings || key in fallback

    val keyCount: Int get() = strings.size

    private fun readLanguage(languageTag: String): Map<String, String> =
        runCatching {
            context.assets.open("$I18N_PATH/$languageTag.json")
                .bufferedReader()
                .use { reader -> json.decodeFromString<Map<String, String>>(reader.readText()) }
        }.getOrElse { emptyMap() }

    private fun resolveLanguage(languageTag: String?): String {
        val tag = languageTag?.takeIf { it != "system" }
            ?: Locale.getDefault().language
        return if (tag in SUPPORTED_LANGUAGES) tag else FALLBACK_LANGUAGE
    }

    private companion object {
        const val I18N_PATH = "i18n"
        const val DEFAULT_LANGUAGE = "de"
        const val FALLBACK_LANGUAGE = "en"
        val SUPPORTED_LANGUAGES = setOf("de", "en")
    }
}
