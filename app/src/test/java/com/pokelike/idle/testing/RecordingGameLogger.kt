package com.pokelike.idle.testing

import com.pokelike.idle.util.GameLogger

/**
 * Protokoll, das seine Meldungen behaelt.
 *
 * Notwendig fuer Faelle, in denen die Meldung selbst das erwartete Verhalten
 * ist - etwa eine verfallene Werbebelohnung. Sie darf nicht stillschweigend
 * geschehen, und ohne diese Nachbildung liesse sich genau das nicht pruefen.
 */
class RecordingGameLogger : GameLogger {

    private val _warnings = mutableListOf<String>()
    private val _errors = mutableListOf<String>()

    val warnings: List<String> get() = _warnings.toList()
    val errors: List<String> get() = _errors.toList()

    override fun warn(tag: String, message: String, throwable: Throwable?) {
        _warnings += "$tag: $message"
    }

    override fun error(tag: String, message: String, throwable: Throwable?) {
        _errors += "$tag: $message"
    }
}
