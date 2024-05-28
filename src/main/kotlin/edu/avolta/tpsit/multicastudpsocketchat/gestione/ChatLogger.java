package edu.avolta.tpsit.multicastudpsocketchat.gestione;

/**
 * Classe per il logging di eventi
 * @author Matteo Bagnoletti Tini
 * @version 1.0
 * @project MulticastUDPSocketChat
 */
public class ChatLogger {

    /**
     * Indica se il {@link ChatLogger} è abilitato
     */
    private static volatile boolean abilita;

    /**
     * Registra ed eventualmente segnala eventi durante l'esecuzione del programma
     * @param messaggio il messaggio di cui effettuare il log
     * @param tipologia la tipologia di log
     * @see ChatLoggerType
     */
    public synchronized static void log(String messaggio, ChatLoggerType tipologia) {
        if ((tipologia == ChatLoggerType.MANDATORY) || (tipologia== ChatLoggerType.OPTIONAL && abilita)) {
            ProjectOutput.stampa(messaggio, OutputType.UILOG);
        }
    }

    /**
     * Abilita o disabilita il {@link ChatLogger}
     * @param abilita true per abilitare il logging, false altrimenti
     */
    public synchronized static void abilita(boolean abilita) {
        ChatLogger.abilita = abilita;
        if(abilita) {
            log("Logging: ON", ChatLoggerType.MANDATORY);
        } else {
            log("Logging: OFF", ChatLoggerType.MANDATORY);
        }
        log("Digita '$log' per attivare/disattivare la modalità di logging", ChatLoggerType.MANDATORY);
    }

    /**
     * Restituisce lo status di operatività del {@link ChatLogger}
     * @return true se abilitato, false altrimenti
     */
    public synchronized static boolean isAbilitato() {
        return ChatLogger.abilita;
    }
}
