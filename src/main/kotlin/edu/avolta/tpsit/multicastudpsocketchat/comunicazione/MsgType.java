package edu.avolta.tpsit.multicastudpsocketchat.comunicazione;

/**
 * Enumerazione per i tipi di messaggi gestiti dall'applicazione (UI).
 */
public enum MsgType {
    /**
     * Messaggio inviato dall'utente corrente. Nella UI viene visualizzato a destra.
     */
    INVIO,

    /**
     * Messaggio ricevuto da un altro utente. Nella UI viene visualizzato a sinistra.
     */
    RICEZIONE,

    /**
     * Messaggio di sistema. Nella UI viene visualizzato al centro.
     */
    GESTIONE
}
