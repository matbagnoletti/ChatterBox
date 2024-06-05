package edu.avolta.tpsit.security

import edu.avolta.tpsit.multicastudpsocketchat.comunicazione.Messaggio
import edu.avolta.tpsit.multicastudpsocketchat.gestione.OutputType
import edu.avolta.tpsit.multicastudpsocketchat.gestione.ProjectOutput
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Classe che si occupa di gestire la sicurezza dei dati.
 */
class SecurityGate {

    private lateinit var chiave: SecretKeySpec
    /**
     * Genera una chiave di cifratura a partire da una stringa.
     * La stringa può avere qualsiasi lunghezza, e verrà poi convertita in una chiave di 128 bit a partire da un hash SHA-256.
     * @param chiave la stringa da cui generare la chiave.
     * @return la chiave generata.
     */
    @Synchronized
    fun generaChiave(chiave: String) {
        val sha = MessageDigest.getInstance("SHA-256")
        var key = chiave.toByteArray(Charsets.UTF_8)
        key = sha.digest(key)
        key = key.copyOf(16)
        this.chiave = SecretKeySpec(key, "AES")
    }
    
    @Synchronized
    fun cifra(byteMsg: ByteArray): ByteArray {
        try {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
            val iv = ByteArray(16)
            SecureRandom().nextBytes(iv)
            val ivSpec = IvParameterSpec(iv)

            cipher.init(Cipher.ENCRYPT_MODE, chiave, ivSpec)

            val encrypted = cipher.doFinal(byteMsg)
            return iv + encrypted
        } catch (e: Exception) {
            ProjectOutput.stampa("Errore nella cifratura: ${e.message}", OutputType.STDERR)
            return ByteArray(0)
        }
    }
    
    @Synchronized
    fun decifra(byteMsg: ByteArray): ByteArray {
        try {
            val iv = byteMsg.copyOfRange(0, 16)
            val encrypted = byteMsg.copyOfRange(16, byteMsg.size)

            val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
            val ivSpec = IvParameterSpec(iv)

            cipher.init(Cipher.DECRYPT_MODE, chiave, ivSpec)

            return cipher.doFinal(encrypted)
        } catch (e: Exception) {
            ProjectOutput.stampa("Errore nella decifrazione: ${e.message}", OutputType.STDERR)
            return ByteArray(0)
        }
    }

    @Synchronized
    fun cifraMessaggio(messaggio: Messaggio): ByteArray {
        val messaggioSerializzato = Messaggio.configMsg(messaggio)
        return cifra(messaggioSerializzato)
    }

    @Synchronized
    fun decifraMessaggio(msgCifrato: ByteArray, lunghezza: Int): Messaggio {
        val messaggioDecifrato = decifra(msgCifrato.copyOfRange(0, lunghezza))
        return Messaggio.configMsg(messaggioDecifrato) as Messaggio
    }
}