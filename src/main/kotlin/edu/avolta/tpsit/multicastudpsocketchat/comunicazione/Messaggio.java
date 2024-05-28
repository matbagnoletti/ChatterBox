package edu.avolta.tpsit.multicastudpsocketchat.comunicazione;

import edu.avolta.tpsit.multicastudpsocketchat.eccezioni.MsgException;
import edu.avolta.tpsit.multicastudpsocketchat.eccezioni.NoSuchUserException;
import edu.avolta.tpsit.multicastudpsocketchat.eccezioni.ProtocolException;
import edu.avolta.tpsit.multicastudpsocketchat.utenze.Rubrica;
import edu.avolta.tpsit.multicastudpsocketchat.utenze.Utente;

import java.io.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Generico messaggio scambiato in una comunicazione di rete tra end-point {@link edu.avolta.tpsit.multicastudpsocketchat.host.MulticastPeer}.
 * <p>
 * Fornisce una collezione di metodi per la gestione di ciascun <code>messaggio</code> e l'ottenimento delle informazioni contenute in esso.
 * <p>
 * Ogni <code>messaggio</code> è identificato da un {@link #ID} univoco incrementale, ottenuto attraverso il metodo {@link Cronologia#getNewID()} invocato dall'<code>host</code> che lo ha generato.
 * <p>
 * L'attributo {@link #utente} contiene l'oggetto <code>utente</code> che ha generato il <code>messaggio</code>, mentre il metodo {@link #isMsgGruppo()} permette di identificare messaggi provenienti da gruppi di <code>host</code>.
 * <p>
 * Dalla versione <code>v1.2</code> è necessario specificare per ciascun <code>messaggio</code> il {@link Protocollo} impiegato per il suo invio e ricezione. 
 *     
 * @author Matteo Bagnoletti Tini
 * @version 1.2
 * @project MulticastUDPSocketChat
 */
public class Messaggio implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * L'identificativo univoco incrementale del messaggio
     */
    private final int ID;

    /**
     * L'{@link Utente} mittente del messaggio
     */
    private final Utente utente;

    /**
     * Variabile numerica che identifica la porta utilizzata dal {@link edu.avolta.tpsit.multicastudpsocketchat.host.MulticastPeer} per scambiare messaggi unicast. Essendo scelta in modo dinamico deve essere nota agli altri <code>multicastPeer</code> in modo da poter comunicare tra loro in modo diretto, al di fuori del {@link edu.avolta.tpsit.multicastudpsocketchat.host.GroupChat}.
     */
    private final int portaMittente;

    /**
     * Indica se il messaggio proviene dalla chat multicast del gruppo
     */
    private final boolean msgGruppo;

    /**
     * Indica se il messaggio è un ACK
     */
    private final boolean ACK;

    /**
     * Il contenuto del messaggio
     */
    private final String msg;

    /**
     * Il numero di ACK ricevuti
     */
    private int contaACK;

    /**
     * Il numero di ACK attesi
     */
    private final int targetACK;

    /**
     * Protocollo e tipologia di comunicazione utilizzato. Inserito per scopi futuri.
     */
    private final Enum<?> protocollo;

    /**
     * L'orario esatto in cui il messaggio è stato generato
     */
    private final LocalTime timestamp;
    
    /**
     * Crea un'istanza di {@link Messaggio}
     * @param ID l'ID univoco incrementale del messaggio
     * @param utente l'utente mittente del messaggio
     * @param portaMittente il numero di porta su cui il mittente è in ascolto per eventuali datagrammi unicast
     * @param targetACK il numero di ACK attesi
     * @param msg il contenuto del messaggio
     * @param isDaGruppo indica se il messaggio proviene da {@link edu.avolta.tpsit.multicastudpsocketchat.host.GroupChat}
     * @param protocollo il {@link Protocollo} utilizzato per l'invio e la ricezione del <code>messaggio</code>
     * @throws ProtocolException se il parametro <code>protocollo</code> non è un valido {@link Protocollo}
     */
    public Messaggio(final int ID, final Utente utente, int portaMittente, final int targetACK, final String msg, final boolean isDaGruppo, final Enum<?> protocollo) throws ProtocolException {
        this.ID = ID;
        this.utente = utente;
        this.portaMittente = portaMittente;
        this.ACK = false;
        this.targetACK = targetACK;
        this.contaACK = 0;
        this.msg = msg;
        this.msgGruppo = isDaGruppo;
        this.timestamp = LocalTime.now();
        
        if(protocollo == null || protocollo.getClass().getEnclosingClass() == Protocollo.class){
            this.protocollo = protocollo;
        } else {
            throw new ProtocolException("Il parametro " + protocollo + " non è un valido Protocollo");
        }
    }

    /**
     * Crea un'istanza di {@link Messaggio} di tipo <code>acknowledge</code>
     * @param ID l'identificativo univoco incrementale del messaggio
     * @param utente il mittente del messaggio
     * @param portaMittente il numero di porta su cui il mittente è in ascolto per eventuali datagrammi unicast
     * @param isACK indica se il messaggio è un ACK
     * @param msgID l'identificativo del messaggio di cui è ACK
     * @param protocollo il {@link Protocollo} utilizzato per l'invio e la ricezione del <code>messaggio</code>
     * @throws MsgException se il messaggio non è ACK
     * @throws ProtocolException se il parametro <code>protocollo</code> non è un valido {@link Protocollo}
     */
    public Messaggio(final int ID, final Utente utente, final int portaMittente, final boolean isACK, final String msgID, final Enum<?> protocollo) throws MsgException, ProtocolException {
        if(!isACK) throw new MsgException("Formato ACK non valido");
        this.ID = ID;
        this.utente = utente;
        this.portaMittente = portaMittente;
        this.ACK = true;
        this.targetACK = 0;
        this.contaACK = 0;
        this.msg = msgID;
        this.msgGruppo = false;
        this.timestamp = LocalTime.now();

        if(protocollo == null || protocollo.getClass().getEnclosingClass() == Protocollo.class){
            this.protocollo = protocollo;
        } else {
            throw new ProtocolException("Il parametro " + protocollo + " non è un valido Protocollo");
        }
    }

    /**
     * Restituisce l'{@link #ID} univoco incrementale del <code>messaggio</code>
     * @return l'{@link #ID} univoco incrementale del <code>messaggio</code>
     */
    public int getID() {
        return ID;
    }

    /**
     * Restituisce l'{@link Utente} che ha generato il messaggio
     * @return l'{@link Utente} che ha generato il messaggio
     */
    public Utente getUtente() {
        return utente;
    }
    
    /**
     * Restituisce il numero di porta utilizzata datta {@link java.net.DatagramSocket} unicast del mittente
     * @return il numero di porta utilizzata datta {@link java.net.DatagramSocket} unicast del mittente
     */
    public int getPortaMittente() {
        return portaMittente;
    }

    /**
     * Verifica se il messaggio proviene dal gruppo
     * @return se il messaggio proviene dal gruppo
     */
    public boolean isMsgGruppo() {
        return msgGruppo;
    }

    /**
     * Restituisce lo status del <code>messaggio</code>
     * @return <code>true</code> se il <code>messaggio</code> è di tipo ACK, <code>false</code> altrimenti
     */
    public boolean isACK() {
        return ACK;
    }

    /**
     * Restituisce il contenuto del <code>messaggio</code>
     * @return il contenuto del <code>messaggio</code>
     */
    public String getMsg() {
        return msg;
    }

    /**
     * Restituisce il numero di ACK ricevuti
     * @return il numero di ACK ricevuti
     */
    public int getContaACK() {
        return contaACK;
    }

    /**
     * Restituisce il numero di ACK attesi
     * @return il numero di ACK attesi
     */
    public int getTargetACK() {
        return targetACK;
    }

    /**
     * Restituisce lo username dell'utente
     * @return lo username dell'utente
     * @throws NoSuchUserException se l'utente non è inizializzato
     */
    public String getUsername() throws NoSuchUserException {
        if(utente == null) throw new NoSuchUserException("Utente non inizializzato");
        return utente.getUsername();
    }

    /**
     * Restituisce l'identificativo univoco dell'utente
     * @return l'identificativo univoco dell'utente
     * @throws NoSuchUserException se l'utente non è inizializzato
     */
    public String getIDutente() throws NoSuchUserException {
        if(utente == null) throw new NoSuchUserException("Utente non inizializzato");
        return utente.getIDutente();
    }

    /**
     * Verifica della ricezione degli ACK richiesti
     * @return true se sono stati ottenuti tutti gli ACK richiesti
     */
    public int isInviatoCorrettamente() {
        return contaACK == targetACK ? 1 : 0;
    }

    /**
     * Restituisce il protocollo e la tipologia di comunicazione utilizzata per trasmettere il <code>messaggio</code>
     * @return il protocollo e la tipologia di comunicazione utilizzata per trasmettere il <code>messaggio</code>
     */
    public Enum<?> getProtocollo() {
        return this.protocollo;
    }

    /**
     * Restituisce l'orario esatto in cui il <code>messaggio</code> è stato generato
     * @return l'orario esatto in cui il <code>messaggio</code> è stato generato
     */
    public String getTimestamp() {
        return this.timestamp.format(DateTimeFormatter.ofPattern("HH:mm"));
    }
    
    /**
     * Aumenta il contatore di ACK ricevuti fino al valore atteso, {@link #targetACK}.
     * @see #contaACK
     * @see #targetACK
     */
    public void ACK() {
        this.contaACK += 1;
    }
    
    /**
     * Restituisce il contenuto formattato del <code>messaggio</code>
     * @param rubrica la rubrica dell'utente per ottenere l'alias dell'{@link Utente} dal quale si è ricevuto il messaggio
     * @return il contenuto formattato del messaggio
     * @throws NoSuchUserException se l'utente non è inizializzato
     */
    public String estrai(Rubrica rubrica) throws NoSuchUserException {
        if(utente == null) throw new NoSuchUserException("Utente non inizializzato");

        String msg = "";
        String aliasMittente = rubrica.ottieniAliasDaUUID(utente.getIDutente());
        if(isMsgGruppo()){
            msg += "[" + getTimestamp() + "]\033[1;36m " + aliasMittente + " (gruppo)\033[0m: " + this.msg;
        } else {
            msg += "[" + getTimestamp() + "]\033[1;35m " + aliasMittente + " (privato)\033[0m: " + this.msg;
        }

        return msg;
    }

    public String estraiUI(Rubrica rubrica) throws NoSuchUserException {
        if(utente == null) throw new NoSuchUserException("Utente non inizializzato");

        String msg = "";
        String aliasMittente = rubrica.ottieniAliasDaUUID(utente.getIDutente());
        if(isMsgGruppo()){
            msg += aliasMittente + ":" + this.msg;
        } else {
            msg += aliasMittente + " (privato):" + this.msg;
        }

        return msg;
    }

    /**
     * Serializza il <code>messaggio</code> fornito
     * @param messaggio il messaggio da serializzare
     * @return l'array di byte del messaggio serializzato
     * @throws MsgException se si verifica un problema durante la serializzazione o se il <code>messaggio</code> fornito è <code>null</code>
     */
    public static byte[] configMsg(Messaggio messaggio) throws MsgException {
        if(messaggio == null) throw new MsgException("Impossibile serializzare un messaggio nullo");
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(outputStream);
            os.writeObject(messaggio);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new MsgException("Errore nella serializzazione del messaggio");
        }
    }

    /**
     * Restituisce il messaggio ricevuto a partire da un array di byte serializzato
     * @param arrayInput l'array di byte che rappresenta un <code>messaggio</code> serializzato
     * @return il messaggio originale
     * @throws MsgException se si verifica un problema durante la serializzazione
     */
    public static Messaggio configMsg(byte[] arrayInput) throws MsgException {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(arrayInput);
            ObjectInputStream is = new ObjectInputStream(inputStream);
            Object test = is.readObject();
            if (test instanceof Messaggio){
                return (Messaggio) test;
            } else {
                throw new MsgException("Il messaggio ricevuto non è valido");
            }
        } catch (ClassNotFoundException e) {
            throw new MsgException("Errore nella deserializzazione del messaggio: impossibile ricostruire il messaggio");
        } catch (IOException e) {
            throw new MsgException("Errore nella deserializzazione del messaggio: errore di I/O");
        }
    }
}
