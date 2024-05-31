package edu.avolta.tpsit.multicastudpsocketchat.utenze;

import edu.avolta.tpsit.chatterbox.ChatterBoxController;
import edu.avolta.tpsit.multicastudpsocketchat.gestione.ChatLogger;
import edu.avolta.tpsit.multicastudpsocketchat.eccezioni.NoSuchUserException;
import edu.avolta.tpsit.multicastudpsocketchat.gestione.ChatLoggerType;

import java.net.InetAddress;
import java.util.ArrayList;

/**
 * Classe con funzione di rubrica per gli utenti noti nella chat. 
 * Fornisce i metodi di gestione e recupero delle informazioni di tali utenti.
 * @author Matteo Bagnoletti Tini
 * @version 1.1
 * @project MulticastUDPSocketChat
 */
public class Rubrica {

    /**
     * Elenco rubrica
     */
    private ArrayList<IndiceRubrica> rubricaList;
    private ArrayList<IndiceRubrica> cestinoRubrica;

    /**
     * L'utente possessore della rubrica
     */
    private final Utente utente;

    public Rubrica(Utente utente) {
        this.rubricaList = new ArrayList<>();
        this.cestinoRubrica = new ArrayList<>();
        this.utente = utente;
    }

    /**
     * Metodo per l'inserimento di nuovi utenti
     * @param utente il nome utente e il codice univoco del nuovo {@link Utente}
     * @param inetAddress l'{@link InetAddress} della socket del nuovo utente
     * @param porta il numero di porta della socket del nuovo utente
     * @throws NoSuchUserException nei casi previsti dalla classe {@link Utente}
     */
    public synchronized void aggiungiUtente(Utente utente, InetAddress inetAddress, int porta, ChatterBoxController ctrl) throws NoSuchUserException {
        if (!this.utente.getIDutente().equals(utente.getIDutente()) && !isGiaInRubrica(utente.getIDutente()) && !isInCestino(utente.getIDutente(), inetAddress, porta, ctrl)) {
            int nDuplicato = 0;
            while (isDuplicato(utente.getUsername(), nDuplicato)) {
                nDuplicato++;
            }
            
            String alias;
            if (nDuplicato == 0){
                alias = utente.getUsername();
            } else {
                alias = utente.getUsername() + "(" + nDuplicato + ")";
            }
            
            rubricaList.add(new IndiceRubrica(alias, utente.getIDutente(), inetAddress, porta));
            ChatLogger.log("(Rubrica) tentativo di inserimento riuscito: memorizzato come " + alias, ChatLoggerType.OPTIONAL);
        } else {
            ChatLogger.log("(Rubrica) tentativo di inserimento fallito: l'utente potrebbe corrispondere all'utente corrente o è già presente in rubrica", ChatLoggerType.OPTIONAL);
        }
        ctrl.aggiornaUtentiOnline(String.valueOf(rubricaList.size()+1));
    }

    /**
     * Rimuove un {@link Utente} dalla rubrica
     * @param IDutente l'identificativo univoco dell'{@link Utente} da ricercare e rimuovere
     * @return lo username dell'{@link Utente} rimosso
     */
    public synchronized String rimuoviUtente(String IDutente, ChatterBoxController ctrl) {
        String alias = ottieniAliasDaUUID(IDutente);
        ChatLogger.log("(Rubrica) richiesta rimozione dalla rubrica di " + alias, ChatLoggerType.OPTIONAL);
        /* prima di eliminarlo lo inserisco nel gestino */
        for(IndiceRubrica i : rubricaList){
            if(i.UUID().equals(IDutente)){
                cestinoRubrica.add(i);
                ChatLogger.log("(Rubrica) utente con UUID: " + IDutente + " spostato nel cestino", ChatLoggerType.OPTIONAL);
                break;
            }
        }
        rubricaList.removeIf(i -> i.alias().equals(alias));
        ctrl.aggiornaUtentiOnline(String.valueOf(rubricaList.size()+1));
        return alias;
    }

    /**
     * Verifica di eventuali duplicati nella rubrica
     * @param utente il nome utente da ricercare in rubrica
     * @param nDuplicato il valore di crescita per la rinomina di duplicati (e.g. "Mario(2)")
     * @return true se vi è un duplicato, false altrimenti
     * @see #rinomina(String, String)
     */
    private synchronized boolean isDuplicato(String utente, int nDuplicato) {
        ChatLogger.log("(Rubrica) ricerca duplicati in corso...", ChatLoggerType.OPTIONAL);
        for(IndiceRubrica i : rubricaList){
            if(nDuplicato == 0){
                if(i.alias().equals(utente.trim())){
                    ChatLogger.log("(Rubrica) duplicato trovato: " + i.alias(), ChatLoggerType.OPTIONAL);
                    return true;
                }
            } else {
                if(i.alias().equals(utente.trim() + "(" + nDuplicato + ")")){
                    ChatLogger.log("(Rubrica) duplicato trovato: " + i.alias(), ChatLoggerType.OPTIONAL);
                    return true;
                }
            }
        }
        
        ChatLogger.log("(Rubrica) " + utente + " risulta univoco", ChatLoggerType.OPTIONAL);
        return false;
    }

    /**
     * Verifica se un utente è già in rubrica
     * @param UUID l'identificativo univoco dell'utente
     * @return true se già in rubrica, false altrimenti
     */
    private synchronized boolean isGiaInRubrica(String UUID) {
        for(IndiceRubrica i : rubricaList){
            if(i.UUID().equals(UUID)){
                ChatLogger.log("(Rubrica) utente con UUID: " + UUID + " già in rubrica", ChatLoggerType.OPTIONAL);
                return true;
            }
        }
        
        ChatLogger.log("(Rubrica) utente con UUID: " + UUID + " non in rubrica", ChatLoggerType.OPTIONAL);
        return false;
    }
    
    private synchronized boolean isInCestino(String UUID, InetAddress inetAddress, int porta, ChatterBoxController ctrl) {
        for(IndiceRubrica i : cestinoRubrica){
            if(i.UUID().equals(UUID)){
                ChatLogger.log("(Rubrica) utente con UUID: " + UUID + " recuperato", ChatLoggerType.OPTIONAL);
                /* lo rimuovo dal cestino */
                cestinoRubrica.remove(i);
                /* aggiorno l'indice con il nuovo IP e porta */
                i = new IndiceRubrica(i.alias(), i.UUID(), inetAddress, porta);
                /* lo re-inserisco in rubrica*/
                rubricaList.add(i);
                ctrl.aggiornaUtentiOnline(String.valueOf(rubricaList.size()+1));
                return true;
            }
        }
        
        ChatLogger.log("(Rubrica) utente con UUID: " + UUID + " non nel cestino", ChatLoggerType.OPTIONAL);
        return false;
    }
    

    /**
     * Restituisce le informazioni note di un dato {@link Utente}
     * @param utente il nome utente da ricercare
     * @return l'{@link IndiceRubrica} contenente le informazioni dell'utente
     * @throws NoSuchUserException se l'utente non è presente in rubrica
     */
    public synchronized IndiceRubrica ottieniInfoUtente(String utente) throws NoSuchUserException {
        ChatLogger.log("(Rubrica) ricerca informazioni per: " + utente + " in corso...", ChatLoggerType.OPTIONAL);
        for(IndiceRubrica i : rubricaList){
            if(i.alias().equals(utente)){
                ChatLogger.log("(Rubrica) utente con alias: " + i.alias() + " trovato", ChatLoggerType.OPTIONAL);
                return i;
            }
        }
        
        throw new NoSuchUserException("Utente non in rubrica");
    }

    /**
     * Restituisce il numero di partecipanti al gruppo (e dunque memorizzati in rubrica)
     * @return il numero di partecipanti al gruppo
     */
    public synchronized int partecipantiGruppo() {
        return rubricaList.size();
    }

    /**
     * Rinomina un {@link Utente}
     * @param utente il nome utente da rinominare
     * @param rinominaIn il nuovo nome utente
     */
    public synchronized void rinomina(String utente, String rinominaIn) {
        ChatLogger.log("(Rubrica) tentativo di rinomina di " + utente + " in " + rinominaIn + " in corso...", ChatLoggerType.OPTIONAL);
        for(IndiceRubrica i : rubricaList){
            if(i.alias().equals(utente.trim())){
                int indice = rubricaList.indexOf(i);
                rubricaList.set(indice, new IndiceRubrica(rinominaIn, i.UUID(), i.inetAddress(), i.porta()));
                ChatLogger.log("(Rubrica) rinomina riuscita", ChatLoggerType.OPTIONAL);
                
                return;
            }
        }
        ChatLogger.log("(Rubrica) rinomina fallita", ChatLoggerType.OPTIONAL);
    }

    /**
     * Rinomina un {@link Utente}
     * @param utente il nome utente da rinominare
     * @param rinominaIn il nuovo nome utente
     * @param ctrl il controller per la gestione dell'interfaccia grafica
     */
    public synchronized void rinomina(String utente, String rinominaIn, ChatterBoxController ctrl) {
        ChatLogger.log("(Rubrica) tentativo di rinomina di " + utente + " in " + rinominaIn + " in corso...", ChatLoggerType.OPTIONAL);
        for(IndiceRubrica i : rubricaList){
            if(i.alias().equals(utente.trim())){
                int indice = rubricaList.indexOf(i);
                rubricaList.set(indice, new IndiceRubrica(rinominaIn, i.UUID(), i.inetAddress(), i.porta()));
                ChatLogger.log("(Rubrica) rinomina riuscita", ChatLoggerType.OPTIONAL);
                ctrl.rinominaUtente(utente, rinominaIn);
                return;
            }
        }
        ChatLogger.log("(Rubrica) rinomina fallita", ChatLoggerType.OPTIONAL);
    }

    /**
     * Restituisce la {@link #rubricaList} memorizzata
     * @return la rubrica memorizzata
     */
    public synchronized String getRubrica() {
        String rubrica = "Rubrica:\n";
        if(rubricaList.isEmpty()){
            rubrica += "Nessun utente memorizzato";
        } else {
            for (IndiceRubrica indiceRubrica: rubricaList) {
                rubrica += "(*) " + indiceRubrica.alias() + " [" + indiceRubrica.UUID() + "] connesso tramite " + indiceRubrica.inetAddress() + ":" + indiceRubrica.porta() + "\n";
            }
            rubrica += "Digita $rn <alias> <nuovoAlias> per rinominare un utente";
        }
        
        return rubrica;
    }

    /**
     * Restituisce l'alias di un {@link Utente} dato il suo identificativo univoco
     * @param UUID l'identificativo univo dell'utente
     * @return l'alias dell'utente se trovato, null altrimenti
     */
    public synchronized String ottieniAliasDaUUID(String UUID){
        ChatLogger.log("(Rubrica) richiesta alias per UUID: " + UUID + " in corso...", ChatLoggerType.OPTIONAL);
        for(IndiceRubrica i : rubricaList){
            if(i.UUID().equals(UUID)){
                ChatLogger.log("(Rubrica) alias di UUID: " + UUID + " risolto in: " + i.alias(), ChatLoggerType.OPTIONAL);
                return i.alias();
            }
        }
        
        return null;
    }
}
