package edu.avolta.tpsit.chatterbox

/**
 * Record di risorsa contenente i dati ottenuti dal web service
 * @param idChat Identificativo del gruppo
 * @param nomeChat Nome del gruppo
 * @param pswChat Password del gruppo
 * @param ipChat Indirizzo IP del gruppo
 * @param portaChat Porta del gruppo
 * @param vChat Versione ChatterBox utilizzata
 */
data class RRWebService(val idChat: Int, val nomeChat: String, val pswChat: String, val ipChat: String, val portaChat: String, val vChat: String)
