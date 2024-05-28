package edu.avolta.tpsit.multicastudpsocketchat.utenze;

import edu.avolta.tpsit.multicastudpsocketchat.gestione.ChatLogger;
import edu.avolta.tpsit.multicastudpsocketchat.gestione.ChatLoggerType;
import edu.avolta.tpsit.multicastudpsocketchat.eccezioni.NoSuchUserException;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

/**
 * Un generico utente umano che utilizza il programma {@code MulticastUDPSocketChat}.
 * <p>
 * Ciascun {@code Utente} viene identificato da uno {@link #username} e da un {@link #IDutente} univoco generato attraverso la classe {@link UUID}.
 * <p>
 * L'assegnazione dell'{@code IDutente} avviene in modo automatico e del tutto trasparente all'utente nel momento in cui l'oggetto {@code Utente} viene istanziato.
 * <p>
 * Implementa {@link Serializable} in modo da poter essere serializzato all'interno di un {@link edu.avolta.tpsit.multicastudpsocketchat.comunicazione.Messaggio} trasmesso in rete.
 * 
 * @author Matteo Bagnoletti Tini
 * @version 1.1
 * @project MulticastUDPSocketChat
 */
public class Utente implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Lo username dell'utente. Non univoco
     */
    private final String username;

    /**
     * L'identificativo univoco dell'utenze
     */
    private final String IDutente;

    /**
     * Crea un oggetto {@link Utente}
     * @param username il nome utenze (username)
     * @throws IllegalArgumentException se il nome utenze è nullo o vuoto
     */
    public Utente(String username) throws IllegalArgumentException {
        if(username == null || username.isBlank()){
            throw new IllegalArgumentException("Nome utenze non valido");
        } else {
            this.username = username;
            this.IDutente = UUID.randomUUID().toString();
            try {
                ChatLogger.log("Utente " + getUtente() + " creato", ChatLoggerType.MANDATORY);
            } catch (NoSuchUserException ignored) {}
        }
    }

    /**
     * Restituisce le info dell'{@link Utente}
     * @return il nome utenze seguito dall'identificativo univoco
     * @throws NoSuchUserException se il nome utenze o l'identificativo sono nulli
     */
    private String getUtente() throws NoSuchUserException {
        if(username != null && IDutente != null) {
            return username + ":" + IDutente;
        } else {
            throw new NoSuchUserException("Utente non inizializzato");
        }
    }

    /**
     * Restituisce lo username dell'{@link Utente}
     * @return lo username dell'utente
     */
    public String getUsername() {
        return username;
    }

    /**
     * Restituisce l'identificativo univoco {@link #IDutente} dell'{@link Utente}
     * @return l'identificativo univoco dell'utente
     */
    public String getIDutente() {
        return IDutente;
    }
    
    @Override
    public String toString() {
        return getIDutente();
    }
}
