package edu.avolta.tpsit.multicastudpsocketchat.comunicazione;

/**
 * Rappresenta il <code>protocollo</code> di trasporto utilizzato nella comunicazione.
 * 
 * @author Matteo Bagnoletti Tini
 * @version 1.2
 * @project MulticastUDPSocketChat
 */
public class Protocollo {

    /**
     * Protocollo di trasporto TCP
     */
    public enum TCP {
        unicast
    }

    /**
     * Protocollo di trasporto UDP
     */
    public enum UDP {
        unicast,
        multicast,
        broadcast
    }
}
