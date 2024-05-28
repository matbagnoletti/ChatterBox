package edu.avolta.tpsit.chatterbox

import edu.avolta.tpsit.multicastudpsocketchat.comunicazione.Cronologia
import edu.avolta.tpsit.multicastudpsocketchat.utenze.Rubrica
import edu.avolta.tpsit.multicastudpsocketchat.utenze.Utente

/**
 * Record di risorsa contenente i dati per la chat
 * @param username Nome utente
 * @param indirizzoIP Indirizzo IP del gruppo
 * @param porta Porta del gruppo
 * @param ttl TTL da impostare per i datagrammi del gruppo
 * @param loopbackOff Flag per disattivare il loopback
 * @param utente Utente associato al RR
 * @param rubrica Rubrica associata al RR
 * @param cronologia Cronologia associata al RR
 */
data class RR(val username : String, val indirizzoIP : String, val porta : String, val ttl : String, val loopbackOff : Boolean, var utente : Utente? = null, var rubrica : Rubrica? = null, var cronologia : Cronologia? = null)
