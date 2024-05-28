package edu.avolta.tpsit.multicastudpsocketchat.host;

import edu.avolta.tpsit.multicastudpsocketchat.eccezioni.CommunicationException;
import edu.avolta.tpsit.multicastudpsocketchat.gestione.ChatLogger;
import edu.avolta.tpsit.multicastudpsocketchat.gestione.ChatLoggerType;

import java.io.IOException;
import java.net.*;
import java.util.Enumeration;

/**
 * Generico <code>gruppo</code> UDP multicast.
 * <p>
 * Fornisce la {@link MulticastSocket} necessaria alla comunicazione multicast e i metodi di gestione.    
 *     
 * @author Matteo Bagnoletti Tini
 * @version 1.0
 * @project MulticastUDPSocketChat
 */
public class GroupChat {
    
    /**
     * Socket per la comunicazione con un <code>gruppo</code> multicast
     */
    private MulticastSocket multicastSocket;
    
    /**
     * L'{@link  InetAddress} del gruppo
     */
    private InetAddress indirizzoMulticast;

    /**
     * L'{@link InetSocketAddress} del gruppo
     */
    private InetSocketAddress gruppo;

    /**
     * La porta del gruppo
     */
    private int portaGruppo;

    /**
     * L'interfaccia di rete utilizzata dalla {@link #multicastSocket}
     * @see #identificaNet()
     */
    private NetworkInterface interfacciaDiRete;
    
    /**
     * Crea e configura opportunamente la {@link #multicastSocket}
     * @param indirizzoMulticast l'{@link InetAddress} del <code>gruppo</code>
     * @param portaMulticast la porta del <code>gruppo</code>
     * @throws IllegalArgumentException se uno dei due parametri non risulta valido
     * @throws IOException se si verifica un errore di I/O
     * @throws CommunicationException se il programma non è riuscito a identificare una {@link NetworkInterface} disponibile
     */
    public GroupChat(final String indirizzoMulticast, final int portaMulticast, final int ttl, final boolean loopbackOff) throws IOException, CommunicationException, IllegalArgumentException {
        ChatLogger.log("Forniti -> IPv4 o nome di dominio del gruppo: " + indirizzoMulticast + " | Numero di porta del gruppo: " + portaMulticast, ChatLoggerType.OPTIONAL);
        if(portaMulticast < 1024 || portaMulticast > 65535 ) {
            throw new IllegalArgumentException("Porta non nel range valido (1024-65535)");
        }

        this.interfacciaDiRete = identificaNet();
        if (interfacciaDiRete == null) throw new CommunicationException("Nessuna interfaccia di rete abilitata per IPv4 e multicast trovata");
        
        try {
            this.portaGruppo = portaMulticast;
            this.multicastSocket = new MulticastSocket(portaGruppo);
            this.multicastSocket.setTimeToLive(ttl);
            this.multicastSocket.setOption(StandardSocketOptions.IP_MULTICAST_LOOP, !loopbackOff);
            
            this.indirizzoMulticast = InetAddress.getByName(indirizzoMulticast);
            
            if(!this.indirizzoMulticast.isMulticastAddress()) throw new IllegalArgumentException("L'indirizzo IP fornito per la configurazione del gruppo non è di tipo multicast");
            
            this.gruppo = new InetSocketAddress(this.indirizzoMulticast, this.portaGruppo);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Indirizzo IP o di dominio non valido: " + e.getMessage());
        } catch (IOException e) {
            throw new IOException("Errore nella creazione del Socket Multicast: " + e.getMessage());
        }
    }

    /**
     * Avvia il {@link GroupChat} accedendo al gruppo opportunamente configurato in precedenza
     * @throws IOException se si verificano errori di I/O unendosi al gruppo
     */
    public void avvia() throws IOException {
        this.multicastSocket.joinGroup(gruppo, interfacciaDiRete);
    }

    /**
     * Restituisce l'oggetto di {@link MulticastSocket}
     * @return l'oggetto {@link MulticastSocket} se esiste, null altrimenti
     */
    public MulticastSocket getMulticastSocket() {
        return multicastSocket;
    }

    /**
     * Metodo di scrittura di messaggi di tipo multicast
     * @throws CommunicationException se si verifica un errore legato alla chat multicast
     */
    public synchronized void multicast(byte[] buffer) throws CommunicationException {
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, indirizzoMulticast, portaGruppo);
        try {
            multicastSocket.send(packet);
            ChatLogger.log("Messaggio multicast inviato con successo", ChatLoggerType.OPTIONAL);
        } catch (IOException e) {
            throw new CommunicationException("Impossibile inviare il messaggio al gruppo: " + e.getMessage(), e.getCause());
        }
    }

    /**
     * Determina la prima {@link NetworkInterface} che supporta IPv4 multicast e con almeno un indirizzo associato
     * @return la {@link NetworkInterface} identificata, null se non presente
     * @throws SocketException se non sono presenti interfacce
     */
    private synchronized NetworkInterface identificaNet() throws SocketException {
        ChatLogger.log("Ricerca interfacce di rete disponibili...", ChatLoggerType.OPTIONAL);
        Enumeration<NetworkInterface> interfacce = NetworkInterface.getNetworkInterfaces();
        NetworkInterface ultimaNetDisponibile = null;
        String infoNet = "Interfacce di rete rilevate: ";
        int contaNetUtilizzabili = 0;
        int contaNetInutilizzabili = 0;

        while (interfacce.hasMoreElements()) {
            NetworkInterface net = interfacce.nextElement();
            /* l'interfaccia deve essere multicast e deve avere almeno un indirizzo associato */
            if (net.supportsMulticast() && net.isUp()) {
                ultimaNetDisponibile = net;
                contaNetUtilizzabili++;
            } else {
                contaNetInutilizzabili++;
            }
        }

        infoNet += (contaNetUtilizzabili + contaNetInutilizzabili) + " elementi. Utilizzabili per IPv4 e multicast: " + contaNetUtilizzabili;

        ChatLogger.log(infoNet, ChatLoggerType.OPTIONAL);
        if(ultimaNetDisponibile != null) {
            ChatLogger.log("Interfaccia di rete selezionata: (" + ultimaNetDisponibile.getIndex() + ") " + ultimaNetDisponibile.getDisplayName(), ChatLoggerType.OPTIONAL);
        } else {
            ChatLogger.log("Nessuna interfaccia di rete selezionabile", ChatLoggerType.OPTIONAL);
        }
        return ultimaNetDisponibile;
    }

    /**
     * Restituisce l'indirizzo multicast del gruppo
     * @return l'indirizzo multicast del gruppo
     */
    public InetAddress getIndirizzoMulticast() {
        return indirizzoMulticast;
    }

    /**
     * Restituisce la porta del gruppo
     * @return la porta del gruppo
     */
    public int getPortaGruppo() {
        return portaGruppo;
    }

    /**
     * Abbandona il gruppo e chiude la {@link #multicastSocket}, rilasciando le risorse
     * @throws IOException se si verificano errori di I/O abbandonando il gruppo
     */
    public void chiudi() throws IOException {
        if(multicastSocket != null && !multicastSocket.isClosed()) {
            try {
                multicastSocket.leaveGroup(gruppo, interfacciaDiRete);
            } catch (IOException e) {
                ChatLogger.log("Impossibile abbandonare il gruppo", ChatLoggerType.OPTIONAL);
            }
            multicastSocket.close();
        }
    }
}
