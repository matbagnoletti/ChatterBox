package edu.avolta.tpsit.multicastudpsocketchat.gestione;

/**
 * Tipologia di output
 * @author Matteo Bagnoletti Tini
 * @version 1.0
 * @since 1.0
 * @project MulticastUDPSocketChat
 * @see ProjectOutput#stampa(String, OutputType) 
 */
public enum OutputType {

    /**
     * Output standard
     */
    STDOUT,

    /**
     * Errore standard
     */
    STDERR,

    /**
     * Commento di log ad uso esclusivo del {@link ChatLogger}
     */
    LOG,

    /**
     * Output UI
     */
    UIOUT,

    /**
     * Errore UI
     */
    UIERR,
    
    /**
     * Commento di log ad uso esclusivo del {@link ChatLogger} in UI
     */
    UILOG
}
