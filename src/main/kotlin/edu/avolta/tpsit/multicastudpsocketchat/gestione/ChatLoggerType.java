package edu.avolta.tpsit.multicastudpsocketchat.gestione;

/**
 * Tipologia di log
 * @author Matteo Bagnoletti Tini
 * @version 1.0
 * @project MulticastUDPSocketChat
 * @see ChatLogger#log(String, ChatLoggerType) 
 */
public enum ChatLoggerType {
    
    /**
     * {@link ChatLoggerType} che indica un log da segnalare obbligatoriamente
     */
    MANDATORY,

    /**
     * {@link ChatLoggerType} che indica un log non necessariamente da segnalare.
     * La segnalazione avverrà solo nel caso in cui il {@link ChatLogger} sia abilitato.
     * @see ChatLogger#isAbilitato() 
     */
    OPTIONAL
}
