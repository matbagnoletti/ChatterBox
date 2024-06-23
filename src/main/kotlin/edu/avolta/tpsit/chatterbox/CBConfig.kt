package edu.avolta.tpsit.chatterbox

import edu.avolta.tpsit.security.SecurityGate
import java.io.File
import java.util.*


/**
 * Classe di configurazione di ChatterBox
 */
class CBConfig(username: String) {
    private var _dir: File
    private val _app = "ChatterBox"

    /**
     * Inizializza la directory di configurazione
     */
    init {
        val userHome = System.getProperty("user.home")
        val os = System.getProperty("os.name").lowercase(Locale.getDefault())

        val configDir = if (os.contains("win")) {
            File(userHome, "AppData\\Local\\$_app\\$username")
        } else if (os.contains("mac")) {
            File(userHome, "Library/Application Support/$_app/$username")
        } else if (os.contains("nix") || os.contains("nux")) {
            File(userHome, ".$_app/$username")
        } else {
            throw UnsupportedOperationException("Sistema operativo non supportato: $os")
        }

        if (!configDir.exists()) {
            configDir.mkdirs()
        }

        _dir = File(configDir, "config.properties")
    }

    /**
     * Recupera la configurazione da file
     */
    private fun getConfig(): Properties {
        val config = Properties()
        if (_dir.exists()) {
            config.load(_dir.inputStream())
        }
        return config
    }

    /**
     * Restituisce il valore di una proprietà di configurazione
     * @param chiave Chiave della proprietà
     */
    fun getConfig(chiave: String): String {
        val config = getConfig()
        return config.getProperty(chiave, "")
    }

    /**
     * Imposta una proprietà di configurazione
     * @param chiave Chiave della proprietà
     * @param valore Valore della proprietà
     * @param commento Commento della proprietà
     */
    fun setConfig(chiave: String, valore: String, commento: String) {
        val config = getConfig()
        config.setProperty(chiave, valore)
        config.store(_dir.outputStream(), commento)
    }
}