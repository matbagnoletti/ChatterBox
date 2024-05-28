/**
 * Fornisce le classi-entità per la gestione della comunicazione tra {@link edu.avolta.tpsit.multicastudpsocketchat.host.MulticastPeer}.
 * <p>
 * La comunicazione prevede uno scambio di oggetti {@link edu.avolta.tpsit.multicastudpsocketchat.comunicazione.Messaggio} dedicati.
 * La gestione e organizzazione di ciascun <code>messaggio</code> è demandata all'<code>host</code> che lo ha generato o ricevuto, attraverso opportuni metodi di cui è fornito, e in particolare mediante un oggetto di tipo {@link edu.avolta.tpsit.multicastudpsocketchat.comunicazione.Cronologia}.
 * <p>
 * Dalla versione <code>v1.2</code> ciascun <code>messaggio</code> deve contenere il {@link edu.avolta.tpsit.multicastudpsocketchat.comunicazione.Protocollo} utilizzato.   
 * <p>
 * Contiene:
 * <ul>
 *     <li>{@link edu.avolta.tpsit.multicastudpsocketchat.comunicazione.Cronologia}</li>
 *     <li>{@link edu.avolta.tpsit.multicastudpsocketchat.comunicazione.Messaggio}</li>
 *     <li>{@link edu.avolta.tpsit.multicastudpsocketchat.comunicazione.Protocollo}</li>
 * </ul>
 * 
 * @author Matteo Bagnoletti Tini
 * @version 1.1
 * @since 1.0
 * @project MulticastUDPSocketChat
 */
package edu.avolta.tpsit.multicastudpsocketchat.comunicazione;