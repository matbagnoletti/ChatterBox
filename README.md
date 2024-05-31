<h1 align="center">ChatterBox</h1>

<p align="center" style="font-family: monospace">Made by <a href="https://github.com/matbagnoletti">@matbagnoletti</a></p>
<p align="center" style="font-family: monospace">Docenti: prof.ssa <a href="https://github.com/mciuchetti">@mciuchetti</a> e prof.ssa Fioroni</p>
<p align="center" style="font-family: monospace">Corso TPSIT a.s. 2023/2024, <a href="https://www.avoltapg.edu.it/">ITTS A. Volta (PG)</a></p>
<p align="center">
    <img src="https://img.shields.io/github/last-commit/matbagnoletti/ChatterBox?style=for-the-badge" alt="Ultimo commit">
    <img src="https://img.shields.io/github/languages/top/matbagnoletti/ChatterBox?style=for-the-badge" alt="Linguaggio">
</p>

## Descrizione
ChatterBox è un'applicazione di chat che sfrutta il protocollo UDP in modalità multicast per la comunicazione tra più utenti. Il programma permette, grazie a un'apposita API RESTful, la creazione di un gruppo chat e il loro accesso tramite credenziali di autenticazione.
L'interfaccia grafica utilizzata è stata realizzata con JavaFX, mentre la gestione della comunicazione è affidata a Java e Kotlin.

## Requisiti
- [JDK](https://www.oracle.com/it/java/technologies/downloads/) (v22.0.1)
- [JavaFX](https://openjfx.io/) (v22.0.1)
- [Maven](https://maven.apache.org/download.cgi)
- [Git](https://git-scm.com/downloads)
- [Git LFS](https://git-lfs.com/)

È possibile visualizzare le versioni già presenti sul proprio dispositivo mediante i seguenti comandi:
```bash
java -version
mvn -v
```

## Installazione e utilizzo
Per poter utilizzare l'applicazione ChatterBox procedi a:
1. Effettuare il clone della repository con il comando:
   ```bash
   git clone https://www.github.com/matbagnoletti/ChatterBox.git
   ```
   In alternativa, puoi scaricare il file compresso del progetto e decomprimerlo in una cartella locale.
2. Aprire il terminale nella cartella del progetto
3. Dopo aver scaricato [Git LFS](https://git-lfs.com/) procedi con la sua configurazione nella cartella locale. Git LFS gestisce i file .jar eseguibili dell'applicazione a causa delle sue elevate dimensioni (>50MB).
   ```bash
   # Configurazione
   git lfs install
   
   # Download dei file .jar
   git lfs pull
   ```
4. Terminate le operazioni di Git LFS, esegui i comandi per l'avvio dell'applicazione, assicurandoti di sostituire <code>${PATH-TO-FX}</code> con il percorso completo della cartella <code>lib</code> di JavaFX: 
   ```bash
   mvn clean
   mvn package
   mvn install
   java --module-path ${PATH-TO-FX} --add-modules javafx.controls,javafx.fxml,javafx.web,javafx.graphics -cp .\target\ChatterBox-2.0.0-shaded.jar edu.avolta.tpsit.chatterbox.ChatterBox
   ```

In alternativa è possibile eseguire il progetto direttamente da un IDE come IntelliJ IDEA.

## Struttura e funzionamento
Il progetto si compone da 2 packages principali:

- Package [`chatterbox`](src/main/kotlin/edu/avolta/tpsit/chatterbox): contiene le classi di gestione dell'interfaccia grafica JavaFX e la classe principale `ChatterBox`.
- Package [`multicastudpsocketchat`](src/main/kotlin/edu/avolta/tpsit/multicastudpsocketchat): contiene le classi di gestione della chat, il cui progetto di riferimento è disponibile [qui](https://www.github.com/matbagnoletti/MulticastUDPSocketChat).

### Utilizzo
1. All'avvio dell'applicazione, una schermata di setup permetterà all'utente la configurazione del proprio username e le impostazioni di connessione al gruppo chat.
   <div align="center">
      <img src="screenshot/setup-1.PNG" style="max-width: 600px; margin-top: 1.5rem" alt="Pagina di configurazione - 1">
   </div> 
   
   Il nome utente deve essere composto da almeno 3 caratteri. Il nome del gruppo è univoco ed ha una lunghezza minima di 6 caratteri, così come la password. La gestione e amministrazione dei gruppi e realizzata mediante una API RESTful apposita che permette la creazione, la modifica e l'eliminazione di gruppi chat.
   La password del gruppo chat è necessaria per garantire la sicurezza e la privacy dei messaggi scambiati tra gli utenti. Viene memorizzata in forma crittografata e non è possibile recuperarla una volta impostata.   

   <div align="center">
        <img src="screenshot/setup-2.PNG" style="max-width: 600px; margin-top: 1.5rem" alt="Pagina di configurazione - 2">
   </div>
   
   Il TTL (Time To Live) è un valore che determina il numero di router attraverso i quali il datagramma può passare. Un valore di 1 indica che il datagramma non può lasciare la sottorete locale, mentre un valore di 255 indica che il datagramma può attraversare un numero illimitato di router. Il valore di default è 128. Per quanto riguarda il loopback dei datagrammi, invece, è possibile disattivarlo per alleggerire il traffico di rete nel solo caso in cui multiple istanze dell'applicazione NON siano eseguite sullo stesso dispositivo.
   
2. Una volta configurato il gruppo, l'utente verrà reindirizzato alla schermata principale dell'applicazione. Qui sarà possibile visualizzare i messaggi inviati dagli altri utenti del gruppo chat e inviare messaggi propri.
   <div align="center">
        <img src="screenshot/chat-1.PNG" style="max-width: 600px; margin-top: 1.5rem" alt="Chat">
   </div>
   Il pulsante "Termina" permette di chiudere la connessione al gruppo chat.
   Una volta terminata la chat, l'utente potrà riconnettersi al gruppo chat mediante il pulsante "Riconnetti".
   Ogni messaggio inviato o ricevuto è accompagnato dal nome utente, dall'orario di invio e da un'icona che indica se il messaggio è stato ricevuto correttamente da tutti gli utenti online in quell'instante.

   La barra di navigazione in alto permette di visualizzare i dettagli del gruppo chat e di modificare le impostazioni di connessione.
   <div align="center">
      <h4>Info gruppo: informazioni di sistema</h4>
      <img src="screenshot/chat-2.PNG" style="max-width: 600px; margin-top: 1.5rem" alt="Info gruppo">
   </div>

   <div align="center">
      <h4>Pannello di controllo: informazioni sull'esecuzione</h4>
      <img src="screenshot/chat-3.PNG" style="max-width: 600px; margin-top: 1.5rem" alt="Pannello di controllo">
   </div>
   In questo esempio gli indirizzi IP e i numeri di porta sono stati nascondi per motivi di privacy.
   
   <div align="center">
      <h4>Impostazioni: layout e controlli</h4>
      <img src="screenshot/chat-4.PNG" style="max-width: 600px; margin-top: 1.5rem" alt="Impostazioni">
   </div>
   
   <div align="center">
      <h4>Aiuto: a proposito di ChatterBox e comandi</h4>
      <img src="screenshot/chat-5.PNG" style="max-width: 600px; margin-top: 1.5rem" alt="Pagina di aiuto">
   </div>

## Licenza d'uso
Questo progetto (e tutte le sue versioni) sono rilasciate sotto la [MB General Copyleft License](LICENSE).
