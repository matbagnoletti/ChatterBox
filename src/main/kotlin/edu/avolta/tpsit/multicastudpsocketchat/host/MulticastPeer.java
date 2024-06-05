package edu.avolta.tpsit.multicastudpsocketchat.host;

import edu.avolta.tpsit.chatterbox.ChatterBoxController;
import edu.avolta.tpsit.chatterbox.RRConfig;
import edu.avolta.tpsit.multicastudpsocketchat.comunicazione.MsgType;
import edu.avolta.tpsit.multicastudpsocketchat.comunicazione.Protocollo;
import edu.avolta.tpsit.multicastudpsocketchat.eccezioni.ProtocolException;
import edu.avolta.tpsit.multicastudpsocketchat.gestione.ChatLogger;
import edu.avolta.tpsit.multicastudpsocketchat.gestione.ChatLoggerType;
import edu.avolta.tpsit.multicastudpsocketchat.gestione.OutputType;
import edu.avolta.tpsit.multicastudpsocketchat.gestione.ProjectOutput;
import edu.avolta.tpsit.multicastudpsocketchat.comunicazione.Cronologia;
import edu.avolta.tpsit.multicastudpsocketchat.comunicazione.Messaggio;
import edu.avolta.tpsit.multicastudpsocketchat.eccezioni.CommunicationException;
import edu.avolta.tpsit.multicastudpsocketchat.eccezioni.MsgException;
import edu.avolta.tpsit.multicastudpsocketchat.eccezioni.NoSuchUserException;
import edu.avolta.tpsit.multicastudpsocketchat.utenze.*;
import edu.avolta.tpsit.security.SecurityGate;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Generico membro di un {@link GroupChat} multicast
 * @author Matteo Bagnoletti Tini
 * @version 1.0
 * @project MulticastUDPSocketChat
 */
public class MulticastPeer {
    
    private final ChatterBoxController controller;

    /**
     * L'utente umano che utilizza il programma
     */
    private final Utente utente;

    /**
     * Socket per la comunicazione unicast
     */
    private DatagramSocket unicastSocket;

    /**
     * Variabile per lo status dell'host. <code>true</code> se operativo, <code>false</code> altrimenti.
     */
    private volatile boolean online;

    /**
     * {@link Rubrica} degli utenti conosciuti
     */
    private final Rubrica rubrica;

    /**
     * {@link Cronologia} dei messaggi
     */
    private final Cronologia cronologia;

    /**
     * Il {@link GroupChat} utilizzato
     */
    private final GroupChat gruppoUDP;
    
    private final SecurityGate securityGate;

    /**
     * Crea un oggetto <code>multicastPeer</code> e configura le strutture di gestione e funzionamento associate
     * @see Utente
     * @see Rubrica
     * @see Cronologia
     * @param username il nome utenze
     * @param abilitaLog indica se abilitare le funzioni di logging
     * @throws IllegalArgumentException nei casi previsti dalla creazione dell'{@link Utente}
     */
    public MulticastPeer(final String username, final boolean abilitaLog, final GroupChat gruppo, final ChatterBoxController controller, final RRConfig resourceRecord) throws IllegalArgumentException {
        /* recupero dati da RRConfig */
        if(resourceRecord.getUtente() != null){
            utente = resourceRecord.getUtente();
        } else {
            utente = new Utente(username);
        }
        if(resourceRecord.getRubrica() != null){
            rubrica = resourceRecord.getRubrica();
        } else {
            rubrica = new Rubrica(utente);
        }
        if(resourceRecord.getCronologia() != null){
            cronologia = resourceRecord.getCronologia();
        } else {
            cronologia = new Cronologia(utente);
        }
        gruppoUDP = gruppo;

        /* aggiornamento RRConfig */
        resourceRecord.setUtente(utente);
        resourceRecord.setRubrica(rubrica);
        resourceRecord.setCronologia(cronologia);
        
        setOnline(false);
        ChatLogger.abilita(abilitaLog);
        this.controller = controller;
        this.securityGate = new SecurityGate();
        this.securityGate.generaChiave(resourceRecord.getSGateKey());
    }

    /**
     * Getter di {@link #online}
     * @return se l'<code>host</code> è online oppure no
     */
    private synchronized boolean isOnline() {
        return online;
    }

    /**
     * Setter di {@link #online}
     * @param online imposta lo status del <code>multicastPeer</code>
     */
    private synchronized void setOnline(boolean online) {
        this.online = online;
    }
    
    /**
     * Configura opportunamente il <code>multicastPeer</code>
     * @throws IOException se si verifica un errore di I/O
     */
    public synchronized void configura() throws IOException {
        try {
            this.unicastSocket = new DatagramSocket();
            ChatLogger.log("Determinati -> IPv4 locale: " + InetAddress.getLocalHost() + " | porta locale : " + unicastSocket.getLocalPort(), ChatLoggerType.OPTIONAL);
            setOnline(true);
            ChatLogger.log("Socket unicast creato con successo", ChatLoggerType.OPTIONAL);
        } catch (IOException e){
            setOnline(false);
            throw new IOException("Errore nella creazione del Socket unicast: " + e.getMessage());
        }
    }

    /**
     * Avvia le funzioni generali del {@link MulticastPeer}
     * @throws CommunicationException
     * @throws MsgException
     * @throws ProtocolException
     * @throws IllegalArgumentException
     */
    public synchronized void avvia() throws CommunicationException, MsgException, ProtocolException, IOException {
        gruppoUDP.avvia();
        /* inputUtente(); */
        leggiUnicast();
        leggiGruppo();
        invia("join-group");
        keepAlive();
    }

    /**
     * Avvia il {@link Thread} di ricezione unicast
     */
    private void leggiUnicast() {
        threadRicezione(unicastSocket);
        ChatLogger.log("Thread ricezione unicast avviato", ChatLoggerType.OPTIONAL);
    }

    /**
     * Avvio il {@link Thread} di ricezione multicast
     */
    private void leggiGruppo() {
        threadRicezione(gruppoUDP.getMulticastSocket());
        ChatLogger.log("Thread ricezione multicast avviato", ChatLoggerType.OPTIONAL);
    }

    /**
     * Crea un generico {@link Thread} di ricezione.
     * <p>
     * Il funzionamento in ricezione di un {@link MulticastPeer} è identico sia per socket di tipo <code>unicast</code>, che di tipo <code>multicast</code>:
     * <ol>
     *     <li>Il {@link Thread} viene creato e configurato con un proprio nome specifico per una più efficace gestione</li>
     *     <li>Procedendo in un loop che termina nel solo momento in cui l'<code>host</code> diventa offline o la <code>socket</code> viene chiusa, viene creato un buffer (array) di byte da utilizzare per il costruttore del {@link DatagramPacket} di ricezione</li>
     *     <li>Ricevuto un {@link DatagramPacket}, viene estratto il messaggio, salvato nella {@link Cronologia} e segnalato l'utente mittente alla {@link Rubrica}</li>
     *     <li>Nel caso in cui il messaggio sia di tipo <code>ACK</code>, viene avviata la procedura per la memorizzazione dell'avvenuta conferma di ricezione</li>
     *     <li>In caso contrario 3 situazioni vengono verificate:
     *       <ul>
     *           <li>Se il messaggio segnala che un utente ha abbandonato il gruppo: <code>left-group</code></li>
     *           <li>Se il messaggio segnala che un utente si è unit* al gruppo: <code>join-group</code></li>
     *           <li>Se il messaggio contiene un generico testo, a cui si provvedere ad inviare un messaggio <code>ACK</code> di risposta</li>
     *       </ul>
     *     </li>
     * </ol>
     */
    private synchronized void threadRicezione(final DatagramSocket tipoSocket) {
        new Thread(()->{
            
            if(tipoSocket != null) {
                if (tipoSocket instanceof MulticastSocket) {
                    Thread.currentThread().setName("Thread di ricezione multicast");
                } else {
                    Thread.currentThread().setName("Thread di ricezione unicast");
                }
                
                while(this.isOnline() && !tipoSocket.isClosed()) {
                    try {
                        byte[] buffer = new byte[1024];
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        tipoSocket.receive(packet);
                        Messaggio msgRicevuto = securityGate.decifraMessaggio(packet.getData(), packet.getLength());

                        cronologia.nuovoMessaggio(msgRicevuto);
                        rubrica.aggiungiUtente(msgRicevuto.getUtente(), packet.getAddress(), msgRicevuto.getPortaMittente(), controller);

                        if (msgRicevuto.isACK() && !msgRicevuto.getIDutente().equals(this.utente.getIDutente())) {
                            cronologia.confermaDiLettura(msgRicevuto, controller);
                        } else if (!msgRicevuto.getIDutente().equals(this.utente.getIDutente())) {
                            if (msgRicevuto.getMsg().equals("left-group")) {
                                String utenteRimosso = rubrica.rimuoviUtente(msgRicevuto.getIDutente(), controller);
                                ProjectOutput.stampa(utenteRimosso + " ha abbandonato il gruppo", OutputType.UIOUT);
                            } else if (msgRicevuto.getMsg().equals("join-group")) {
                                ProjectOutput.stampa(rubrica.ottieniAliasDaUUID(msgRicevuto.getUtente().getIDutente()) + " si è unito/a al gruppo", OutputType.UIOUT);
                                ChatLogger.log("Tentativo di invio del messaggio di saluto in corso...", ChatLoggerType.OPTIONAL);
                                invia("DO-NOT-SHOW-THIS-MESSAGE");
                            } else {
                                ProjectOutput.stampa(msgRicevuto.estraiUI(rubrica), OutputType.UIOUT);
                                /* invio ACK */
                                preparaACK(String.valueOf(msgRicevuto.getID()), msgRicevuto.getIDutente());
                            }
                        }
                        controller.aggiornaDashboard(InetAddress.getLocalHost().getHostAddress(), String.valueOf(this.unicastSocket.getLocalPort()), this.gruppoUDP.getIndirizzoMulticast().toString(), String.valueOf(this.gruppoUDP.getPortaGruppo()), String.valueOf(this.cronologia.getMessaggiInviati()), String.valueOf(this.cronologia.getMessaggiRicevuti()), this.cronologia.getSimpleStat());
                    } catch (SocketException e) {
                        setOnline(false);
                        chiudi();
                        break;
                    } catch (IOException e) {
                        setOnline(false);
                        ProjectOutput.stampa("Errore di I/O: " + e.getMessage(), OutputType.STDERR);
                        break;
                    } catch (MsgException e) {
                        ProjectOutput.stampa("Formato messaggio non valido", OutputType.STDERR);
                    } catch (NoSuchUserException e) {
                        ProjectOutput.stampa("Utente non inizializzato", OutputType.STDERR);
                    } catch (CommunicationException e) {
                        ProjectOutput.stampa(e.getMessage(), OutputType.STDERR);
                    } catch (ProtocolException e){
                        ProjectOutput.stampa(e.getMessage() + ". Il programma verrà terminato", OutputType.STDERR);
                        chiudi();
                    }
                }
            }

        }).start();
    }

    /**
     * Avvia il {@link Thread} per la lettura e interpretazione dell'input dell'utente da tastiera. Specifici comandi preceduti dal carattere <code>$</code> possono essere visualizzati attraverso il comando <code>$help</code>
     */
    private synchronized void inputUtente() {
        new Thread(()->{
            Thread.currentThread().setName("Thread per l'input dell'utente");
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

            Scanner inUtente = new Scanner(System.in);
            ChatLogger.log("Terminale pronto all'invio di messaggi", ChatLoggerType.MANDATORY);
            ChatLogger.log("Digita una qualsiasi sequenza di caratteri per comunicare con il GRUPPO", ChatLoggerType.MANDATORY);
            ChatLogger.log("Per poter comunicare privatamente con un utente digitare: 'testo > aliasUtente'", ChatLoggerType.MANDATORY);
            ChatLogger.log("Digita '$exit' per terminare l'esecuzione", ChatLoggerType.MANDATORY);
            ChatLogger.log("Digita '$help' per visualizzare l'elenco dei comandi", ChatLoggerType.MANDATORY);
            
            while(this.isOnline()) {
                try {
                    String input = inUtente.nextLine().trim();
                    
                    String[] arrayInput;
                    
                    if(!input.isBlank()){
                        /* comandi */
                        if(input.contains("$")){
                            arrayInput = input.split(" ");
                            
                            switch (arrayInput[0].trim()){
                                case "$exit" -> {
                                    chiudi();
                                }
                                
                                case "$utenti" -> ChatLogger.log(rubrica.getRubrica(), ChatLoggerType.MANDATORY);
                                
                                case "$stat" -> ChatLogger.log(cronologia.getStatistiche(), ChatLoggerType.MANDATORY);
                                
                                case "$rn" -> {
                                    if(arrayInput.length == 3){
                                        rubrica.rinomina(arrayInput[1].trim(), arrayInput[2].trim());
                                    } else {
                                        ProjectOutput.stampa("Parametri <alias> e <nuovoAlias> assenti o non validi. Digita $help per l'elenco dei comandi", OutputType.STDERR);
                                    }
                                }
                                
                                case "$help" -> {
                                    ChatLogger.log("Digita '$exit' per terminare l'esecuzione", ChatLoggerType.MANDATORY);
                                    ChatLogger.log("Digita '$help' per visualizzare l'elenco dei comandi", ChatLoggerType.MANDATORY);
                                    ChatLogger.log("Digita '$utenti' per visualizzare la rubrica memorizzata", ChatLoggerType.MANDATORY);
                                    ChatLogger.log("Digita '$stat' per visualizzare le statistiche di output", ChatLoggerType.MANDATORY);
                                    ChatLogger.log("Digita '$rn <alias> <nuovoAlias>' per rinominare l'alias di un utente in rubrica", ChatLoggerType.MANDATORY);
                                    ChatLogger.log("Digita '$log' per attivare/disattivare la modalità di logging", ChatLoggerType.MANDATORY);
                                }
                                
                                case "$log" -> ChatLogger.abilita(!ChatLogger.isAbilitato());
                                
                                default -> ProjectOutput.stampa("Comando " + arrayInput[0].trim() + " non riconosciuto. Digita $help per l'elenco dei comandi", OutputType.STDERR);
                            }
                            
                        } else if (input.contains(">")){
                            arrayInput = input.split(">");

                            if(arrayInput.length == 2) {
                                /* scrittura unicast */
                                try {
                                    preparaInvio(arrayInput[0].trim(), arrayInput[1].trim());
                                } catch (MsgException | IllegalArgumentException e) {
                                    ProjectOutput.stampa(e.getMessage(), OutputType.STDERR);
                                }

                            } else {
                                ProjectOutput.stampa("Formato per invio messaggio non valido", OutputType.STDERR);
                            }
                        } else {
                            /* scrittura multicast */
                            try {
                                invia(input);
                            } catch (MsgException | IllegalArgumentException | CommunicationException e) {
                                ProjectOutput.stampa(e.getMessage(), OutputType.STDERR);
                            }
                        }
                    } else {
                        ProjectOutput.stampa("Formato input non valido: null", OutputType.STDERR);
                    }
                } catch (Exception e){
                    setOnline(false);
                    ProjectOutput.stampa("Errore: " + e.getMessage(), OutputType.STDERR);
                    break;
                }
            }
            chiudi();
        }).start();
    }

    /**
     * Prepara l'output di messaggi unicast
     * @param messaggioUnicast il contenuto del messaggio
     * @param destinatario il destinatario (UUID o username)
     * @throws NoSuchUserException se si verifica un errore legato agli utenti e la {@link #rubrica}
     * @throws MsgException se si verifica un errore nella creazione del {@link Messaggio}
     * @throws IOException se si verifica un errore nell'invio del datagramma
     * @throws ProtocolException se il parametro <code>protocollo</code> non è un valido {@link Protocollo}
     */
    private synchronized void preparaInvio(String messaggioUnicast, String destinatario) throws MsgException, NoSuchUserException, ProtocolException, IOException {
        IndiceRubrica infoDestinatario;
        
        if(destinatario.contains("-")) {
            String aliasDaUUID = rubrica.ottieniAliasDaUUID(destinatario);
            infoDestinatario = rubrica.ottieniInfoUtente(aliasDaUUID);
        } else {
            infoDestinatario = rubrica.ottieniInfoUtente(destinatario);
        }
        int id = cronologia.getNewID();
        Messaggio messaggio = new Messaggio(id, utente, unicastSocket.getLocalPort(), 1, messaggioUnicast, false, Protocollo.UDP.unicast);
        ChatLogger.log("Invio messaggio unicast per " + utente.getIDutente() + " con msgID " + messaggioUnicast + " in corso...", ChatLoggerType.OPTIONAL);
        cronologia.storicizzaMessaggio(messaggio);
        
        byte[] out = securityGate.cifraMessaggio(messaggio);
        DatagramPacket packet = new DatagramPacket(out, out.length, infoDestinatario.inetAddress(), infoDestinatario.porta());
        invia(packet);
        controller.nuovoElemChat(messaggioUnicast, MsgType.INVIO, id, null, messaggio.getTimestamp());
    }

    /**
     * Prepara l'output di messaggi ACK (unicast)
     * @param msgIDxACK il contenuto del messaggio
     * @param UUID l'identificativo univoco del destinatario
     * @throws MsgException se si verifica un errore nella creazione del {@link Messaggio}
     * @throws IOException se si verifica un errore nell'invio del datagramma
     * @throws ProtocolException se il parametro <code>protocollo</code> non è un valido {@link Protocollo}
     */
    private synchronized void preparaACK(String msgIDxACK, String UUID) throws MsgException, IOException, ProtocolException, NoSuchUserException {
        String aliasDaUUID = rubrica.ottieniAliasDaUUID(UUID);
        IndiceRubrica infoDestinatario = rubrica.ottieniInfoUtente(aliasDaUUID);
        
        Messaggio messaggio = new Messaggio(cronologia.getNewID(), utente, unicastSocket.getLocalPort(), true, msgIDxACK, Protocollo.UDP.unicast);
        ChatLogger.log("Invio messaggio ACK per " + utente.getIDutente() + " con msgID " + msgIDxACK + " in corso...", ChatLoggerType.OPTIONAL);
        cronologia.storicizzaMessaggio(messaggio);
        
        byte[] out = securityGate.cifraMessaggio(messaggio);
        DatagramPacket packet = new DatagramPacket(out, out.length, infoDestinatario.inetAddress(), infoDestinatario.porta());
        invia(packet);
    }

    /**
     * Invia i datagrammi unicast al destinatario
     * @param datagramPacket il datagramma da inviare
     * @throws IOException se si verifica un errore nell'invio del datagramma
     */
    private synchronized void invia(DatagramPacket datagramPacket) throws IOException {
        try {
            unicastSocket.send(datagramPacket);
        } catch (IOException e) {
            throw new IOException("Impossibile inviare ACK: " + e.getMessage(), e.getCause());
        }
        controller.aggiornaDashboard(InetAddress.getLocalHost().getHostAddress(), String.valueOf(this.unicastSocket.getLocalPort()), this.gruppoUDP.getIndirizzoMulticast().toString(), String.valueOf(this.gruppoUDP.getPortaGruppo()), String.valueOf(this.cronologia.getMessaggiInviati()), String.valueOf(this.cronologia.getMessaggiRicevuti()), this.cronologia.getSimpleStat());
    }

    /**
     * Metodo di scrittura di messaggi a un <code>gruppo</code> multicast
     * @param messaggioMulticast il contenuto del messaggio
     * @throws MsgException se si verifica un errore nella creazione del {@link Messaggio}
     * @throws CommunicationException se si verifica un errore legato alla chat multicast
     * @throws ProtocolException se il parametro <code>protocollo</code> non è un valido {@link Protocollo}
     */
    private synchronized void invia(String messaggioMulticast) throws MsgException, CommunicationException, ProtocolException, UnknownHostException {
        ChatLogger.log("Invio messaggio multicast in corso...", ChatLoggerType.OPTIONAL);
        int id = cronologia.getNewID();
        Messaggio messaggio;
        if (messaggioMulticast.equals("left-group")) {
            ProjectOutput.stampa("Hai abbandonato il gruppo", OutputType.UIOUT);
            messaggio = new Messaggio(id, utente, unicastSocket.getLocalPort(), 0, messaggioMulticast, true, Protocollo.UDP.multicast);
        } else if (messaggioMulticast.equals("join-group")) {
            ProjectOutput.stampa("Ti sei unito/a al gruppo", OutputType.UIOUT);
            messaggio = new Messaggio(id, utente, unicastSocket.getLocalPort(), 0, messaggioMulticast, true, Protocollo.UDP.multicast);
        } else if (!messaggioMulticast.equals("DO-NOT-SHOW-THIS-MESSAGE")) {
            messaggio = new Messaggio(id, utente, unicastSocket.getLocalPort(), rubrica.partecipantiGruppo(), messaggioMulticast, true, Protocollo.UDP.multicast);
            controller.nuovoElemChat(messaggioMulticast, MsgType.INVIO, id, null, messaggio.getTimestamp());
        } else {
            messaggio = new Messaggio(id, utente, unicastSocket.getLocalPort(), rubrica.partecipantiGruppo(), messaggioMulticast, true, Protocollo.UDP.multicast);
        }
        cronologia.storicizzaMessaggio(messaggio);
        byte[] out = securityGate.cifraMessaggio(messaggio);
        gruppoUDP.multicast(out);
        controller.aggiornaDashboard(InetAddress.getLocalHost().getHostAddress(), String.valueOf(this.unicastSocket.getLocalPort()), this.gruppoUDP.getIndirizzoMulticast().toString(), String.valueOf(this.gruppoUDP.getPortaGruppo()), String.valueOf(this.cronologia.getMessaggiInviati()), String.valueOf(this.cronologia.getMessaggiRicevuti()), this.cronologia.getSimpleStat());
    }

    /**
     * Ricevuto un input dall'utente, lo processa e gestisce opportunamente.
     * <p>
     * Per la versione GUI del programma, i comandi possibili sono stati ridotti a:
     * <ul>
     *     <li><code>$exit</code> per terminare l'esecuzione</li>
     *     <li><code>$rn</code> per rinominare un utente in rubrica</li>
     *     <li><code>$log</code> per attivare/disattivare la modalità di logging</li>
     * </ul>
     * @param inputUtente l'input dell'utente
     */
    public synchronized void inputDaUI(String inputUtente){
        try {
            String[] arrayInput;

            /* comandi */
            if(inputUtente.contains("$")){
                arrayInput = inputUtente.split(" ");

                switch (arrayInput[0].trim()){
                    case "$exit" -> chiudi();
                    
                    case "$rn" -> {
                        if(arrayInput.length == 3){
                            rubrica.rinomina(arrayInput[1].trim(), arrayInput[2].trim(), controller);
                        } else {
                            ProjectOutput.stampa("Parametri <alias> e <nuovoAlias> assenti o non validi", OutputType.UIERR);
                        }
                    }
                    
                    case "$log" -> ChatLogger.abilita(!ChatLogger.isAbilitato());

                    default -> ProjectOutput.stampa("Comando " + arrayInput[0].trim() + " non riconosciuto", OutputType.UIERR);
                }

            } else if (inputUtente.contains(">")){
                arrayInput = inputUtente.split(">");

                if(arrayInput.length == 2) {
                    /* scrittura unicast */
                    try {
                        preparaInvio(arrayInput[0].trim(), arrayInput[1].trim());
                    } catch (MsgException | IllegalArgumentException e) {
                        ProjectOutput.stampa(e.getMessage(), OutputType.UIERR);
                    }

                } else {
                    ProjectOutput.stampa("Formato per invio messaggio non valido", OutputType.UIERR);
                }
            } else {
                /* scrittura multicast */
                try {
                    invia(inputUtente);
                } catch (MsgException | IllegalArgumentException | CommunicationException e) {
                    ProjectOutput.stampa(e.getMessage(), OutputType.STDERR);
                }
            }
        } catch (Exception e){
            setOnline(false);
            ProjectOutput.stampa("Errore: " + e.getMessage(), OutputType.STDERR);
        }
    }

    /**
     * Avvia il {@link Thread} di keepAlive per mantenere attiva la connessione
     */
    private synchronized void keepAlive() {
        new Thread(()->{
            Thread.currentThread().setName("Thread di keepAlive");
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            
            while(this.isOnline()) {
                try {
                    invia("DO-NOT-SHOW-THIS-MESSAGE");
                    Thread.sleep(10000);
                } catch (InterruptedException | MsgException | CommunicationException | ProtocolException | UnknownHostException e) {
                    ProjectOutput.stampa("Errore: " + e.getMessage(), OutputType.STDERR);
                }
            }
        }).start();
    }

    /**
     * Chiude la {@link #unicastSocket}, rilasciando le risorse
     */
    public synchronized void chiudi() {
        if(isOnline()){
            try {
                invia("left-group");
            } catch (Exception e){
                ProjectOutput.stampa("Impossibile inviare il messaggio di uscita dal gruppo", OutputType.STDERR);
            }
            
            // TODO: implementare l'eliminazione del gruppo dal Web Service
            
            setOnline(false);
            ChatLogger.log("Terminazione in corso...", ChatLoggerType.MANDATORY);

            try {
                gruppoUDP.chiudi();
            } catch (IOException e) {
                ChatLogger.log("Impossibile abbandonare correttamente il gruppo", ChatLoggerType.OPTIONAL);
            }
            if(unicastSocket != null && !unicastSocket.isClosed()) unicastSocket.close();
        }
    }
}
