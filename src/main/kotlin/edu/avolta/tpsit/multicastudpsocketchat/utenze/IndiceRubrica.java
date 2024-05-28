package edu.avolta.tpsit.multicastudpsocketchat.utenze;

import java.net.InetAddress;

/**
 * Generico indice di {@link Rubrica}
 * @param alias l'alias assegnato a un dato {@link Utente}
 * @param UUID l'identificativo univoco dell'utente
 * @param inetAddress l'{@link InetAddress} dell'utente
 * @param porta il numero della porta della {@link java.net.DatagramSocket} dell'utente
 * @author Matteo Bagnoletti Tini
 * @version 1.1
 * @project MulticastUDPSocketChat
 */
public record IndiceRubrica(String alias, String UUID, InetAddress inetAddress, int porta) {}
