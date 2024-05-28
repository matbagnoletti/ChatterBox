package edu.avolta.tpsit.chatterbox

import atlantafx.base.theme.Styles
import edu.avolta.tpsit.multicastudpsocketchat.comunicazione.MsgType
import edu.avolta.tpsit.multicastudpsocketchat.gestione.ChatLogger
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Text
import javafx.stage.FileChooser
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons
import org.kordamp.ikonli.javafx.FontIcon
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Controller per la finestra principale dell'applicazione
 * @property application Riferimento all'applicazione principale
 * @property storicoChat Storico dei messaggi della chat
 * @see ChatterBox
 * @author Matteo Bagnoletti Tini
 */
class ChatterBoxController {
    
    /* chat*/
    @FXML
    private lateinit var schermoMessaggi : ListView<HBox>
    @FXML
    private lateinit var boxEsci : HBox
    @FXML
    private lateinit var inputMsg : TextField
    @FXML
    private lateinit var onlineCount : Text
    
    /* info dashboard*/
    @FXML
    private lateinit var impIndirizzoIPLocale: Text
    @FXML
    private lateinit var impPortaLocale: Text
    @FXML
    private lateinit var impIndirizzoIPGruppo: Text
    @FXML
    private lateinit var impPortaGruppo: Text
    @FXML
    private lateinit var impMessaggiOut: Text
    @FXML
    private lateinit var impMessaggiIn: Text
    @FXML
    private lateinit var impStatistiche: Text

    /* icone dashboard */
    @FXML
    private lateinit var iconIndirizzoIPLocale: Label
    @FXML
    private lateinit var iconPortaLocale: Label
    @FXML
    private lateinit var iconIndirizzoIPGruppo: Label
    @FXML
    private lateinit var iconPortaGruppo: Label
    @FXML
    private lateinit var iconMessaggiOut: Label
    @FXML
    private lateinit var iconMessaggiIn: Label
    @FXML
    private lateinit var iconStatistiche: Label
    @FXML
    private lateinit var iconTemaChiaro: Label
    @FXML
    private lateinit var iconTemaScuro: Label
    
    /* impostazioni */
    @FXML
    private lateinit var toggleTema: Toggle
    @FXML
    private lateinit var toggleLog: Toggle
    
    /* log */
    @FXML
    private lateinit var logScreen : ListView<HBox>
    
    /**
     * Riferimento all'applicazione principale
     */
    lateinit var application : ChatterBox
    private val storicoChat : MutableList<String> = mutableListOf()
    private var isChatAttiva = true

    /**
     * Inizializzazione del controller.
     * a) Imposta le icone della dashboard
     * b) Imposta l'azione per l'invio dei messaggi con il tasto INVIO
     * c) Imposta l'azione per la terminazione della chat
     * @see FontIcon
     * @see BootstrapIcons
     */
    fun initialize() {
        iconIndirizzoIPLocale.graphic = FontIcon(BootstrapIcons.WIFI)
        iconPortaLocale.graphic = FontIcon(BootstrapIcons.DOOR_OPEN)
        iconIndirizzoIPGruppo.graphic = FontIcon(BootstrapIcons.WIFI)
        iconPortaGruppo.graphic = FontIcon(BootstrapIcons.DOOR_OPEN)
        iconMessaggiOut.graphic = FontIcon(BootstrapIcons.ARROW_BAR_RIGHT)
        iconMessaggiIn.graphic = FontIcon(BootstrapIcons.ARROW_BAR_LEFT)
        iconStatistiche.graphic = FontIcon(BootstrapIcons.CHECK2_ALL)
        iconTemaChiaro.graphic = FontIcon(BootstrapIcons.SUN)
        iconTemaScuro.graphic = FontIcon(BootstrapIcons.MOON_FILL)
        
        onlineCount.styleClass.addAll(Styles.TEXT, Styles.ACCENT)
        
        aggiornaCtrlBtn()
        aggiornaUtentiOnline("1")
        
        inputMsg.setOnKeyPressed { event ->
            if (event.code == KeyCode.ENTER) {
                nuovoMsgDaInput()
            }
        }
    }

    /**
     * Aggiorna il bottone di controllo della chat
     */
    private fun aggiornaCtrlBtn() {
        if (isChatAttiva) {
            btnDiControllo("Termina", ::preparaRiavvio)
            aggiornaUtentiOnline("0")
        } else {
            btnDiControllo("Riconnetti", ::riavviaChat)
            aggiornaUtentiOnline()
        }
    }

    /**
     * Prepara la chat per un eventuale riavvio
     */
    private fun preparaRiavvio() {
        application.terminaChat()
        isChatAttiva = false
        aggiornaCtrlBtn()
    }

    /**
     * Riavvia la chat
     */
    private fun riavviaChat() {
        application.avviaDaRR()
        isChatAttiva = true
        aggiornaCtrlBtn()
    }

    /**
     * Crea un bottone di controllo per la chat
     */
    private fun btnDiControllo(text : String, azione : () -> Unit) {
        // se il testo è "Termina", il bottone è rosso, altrimenti verde
        boxEsci.children.clear()
        val btn = if(text == "Termina") {
            Button(text, FontIcon(BootstrapIcons.CHEVRON_BAR_RIGHT))
        } else {
            Button(text, FontIcon(BootstrapIcons.ARROW_REPEAT))
        }
        if(text == "Termina") {
            btn.styleClass.add(Styles.DANGER)
            inputMsg.isDisable = false
        } else {
            btn.styleClass.add(Styles.SUCCESS)
            inputMsg.isDisable = true
        }
        btn.contentDisplay = ContentDisplay.RIGHT
        btn.setOnAction { azione() }
        btn.prefWidth = 125.0
        btn.prefHeight = 37.0
        boxEsci.children.add(btn)
    }

    /**
     * Cambia il tema dell'applicazione
     */
    @FXML
    fun cambiaTema() {
        if(toggleTema.isSelected){
            application.cambiaTema("scuro")
        } else {
            application.cambiaTema("chiaro")
        }
    }

    /**
     * Apre il link al repository GitHub del progetto
     */
    @FXML
    fun linkGitHub() {
        application.hostServices.showDocument("https://www.github.com/matbagnoletti/ChatterBox")
    }

    /**
     * Crea un documento TXT con lo storico della chat
     */
    @FXML
    fun downloadChat() {
        try {
            val fileChooser = FileChooser()
            fileChooser.title = "Salva chat"
            fileChooser.extensionFilters.addAll(
                FileChooser.ExtensionFilter("Text Files", "*.txt"),
                FileChooser.ExtensionFilter("All Files", "*.*")
            )
            val selectedFile = fileChooser.showSaveDialog(application.stageCorrente)
            if (selectedFile != null) {
                // Creazione del contenuto del file

                // 1) riga di intestazione con data completa e tutti i dati della chat (indirizzo IP, porta, ecc)
                selectedFile.writeText("ChatterBox - Storico della chat\n\n")
                selectedFile.appendText("Data: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))}\n")
                selectedFile.appendText("Indirizzo IP locale: ${impIndirizzoIPLocale.text}\n")
                selectedFile.appendText("Porta locale: ${impPortaLocale.text}\n")
                selectedFile.appendText("Indirizzo IP gruppo: ${impIndirizzoIPGruppo.text}\n")
                selectedFile.appendText("Porta gruppo: ${impPortaGruppo.text}\n")
                selectedFile.appendText("Messaggi inviati: ${impMessaggiOut.text}\n")
                selectedFile.appendText("Messaggi ricevuti: ${impMessaggiIn.text}\n")
                selectedFile.appendText("Messaggi inviati con successo: ${impStatistiche.text}\n\n")

                // 2) storico della chat
                storicoChat.forEach { 
                    if(!it.contains("DO-NOT-SHOW-THIS-MESSAGE")) selectedFile.appendText("$it\n") 
                }
            }
        } catch (e: Exception) {
            nuovoLog("Errore durante il download dello storico della chat", Color.RED)
        }
        
    }

    /**
     * Abilita/disabilita il log
     */
    @FXML
    fun cambiaLog(){
        ChatLogger.abilita(toggleLog.isSelected)
    }

    /**
     * Invia un nuovo messaggio dalla casella di input
     */
    @FXML
    fun nuovoMsgDaInput() {
        if (inputMsg.text.isNotBlank()) {
            application.nuovoMsgDaInput(inputMsg.text)
            inputMsg.text = ""
        }
    }

    /**
     * Aggiunge un nuovo messaggio alla chat
     */
    @Synchronized
    fun nuovoElemChat(
        messaggio: String,
        tipo: MsgType,
        msgID: Int? = null,
        mittente: String? = null,
        timestamp: String? = null
    ) {
        // Formato per storico: [hh:mm] mittente: messaggio. Se mittente è null, scrive 'TU'
        val mittenteStorico = mittente ?: "TU"
        val orarioStorico = timestamp ?: LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        val msgStorico = "[$orarioStorico] $mittenteStorico: $messaggio"
        storicoChat.add(msgStorico)
        
        Platform.runLater {
            // Creazione HBox per il messaggio con padding e spacing
            val hBox = HBox().apply {
                padding = Insets(8.0)
                spacing = 8.0
            }

            // Creazione VBox per la struttura del messaggio
            val vBox = VBox().apply {
                alignment = Pos.CENTER_RIGHT
                spacing = 1.5
            }

            // HBox per il mittente e il messaggio
            val hBoxMittenteMsg = HBox().apply { spacing = 1.5 }

            // Aggiunta del mittente se presente
            mittente?.let {
                val mit = Label("$it: ").apply { styleClass.addAll(Styles.TEXT, Styles.TEXT_BOLD) }
                hBoxMittenteMsg.children.add(mit)
            }

            // Creazione del messaggio
            val msg = Label(messaggio).apply { isWrapText = true }

            // Impostazione degli allineamenti in base al tipo di messaggio
            when (tipo) {
                MsgType.INVIO -> {
                    hBoxMittenteMsg.alignment = Pos.CENTER_RIGHT
                    hBox.alignment = Pos.CENTER_RIGHT
                    inputMsg.text = ""
                }
                MsgType.RICEZIONE -> {
                    hBoxMittenteMsg.alignment = Pos.CENTER_LEFT
                    hBox.alignment = Pos.CENTER_LEFT
                }
                MsgType.GESTIONE -> {
                    msg.styleClass.add(Styles.TEXT_MUTED)
                    hBoxMittenteMsg.alignment = Pos.CENTER
                    hBox.alignment = Pos.CENTER
                }
            }

            // Aggiunta del messaggio all'HBox
            hBoxMittenteMsg.children.add(msg)
            vBox.children.add(hBoxMittenteMsg)

            // Aggiunta del timestamp e dell'icona per messaggi di invio e ricezione
            if (tipo != MsgType.GESTIONE) {
                val hBoxTempoVisto = HBox().apply {
                    alignment = when (tipo) {
                        MsgType.INVIO -> Pos.CENTER_RIGHT
                        MsgType.RICEZIONE -> Pos.CENTER_LEFT
                        else -> Pos.CENTER
                    }
                    spacing = 2.0
                }

                // Aggiunta del timestamp
                val tempo = Label(orarioStorico).apply {
                    styleClass.addAll(Styles.TEXT, Styles.TEXT_SMALL)
                }
                hBoxTempoVisto.children.add(tempo)

                // Aggiunta dell'icona per i messaggi di invio
                if (tipo == MsgType.INVIO) {
                    val icona = FontIcon(BootstrapIcons.CHECK).apply {
                        iconSize = 10
                        styleClass.addAll(Styles.TEXT, Styles.ACCENT)
                        id = "msg-$timestamp-$msgID"
                    }
                    hBoxTempoVisto.children.add(icona)
                }

                vBox.children.add(hBoxTempoVisto)
            }

            // Aggiunta della struttura del messaggio all'HBox
            hBox.children.add(vBox)
            schermoMessaggi.items.add(hBox)

            // Scorrimento automatico alla fine dei messaggi
            Platform.runLater {
                schermoMessaggi.scrollTo(schermoMessaggi.items.size - 1)
            }
        }
    }

    /**
     * Aggiorna l'icona di un messaggio inviato con successo
     */
    @Synchronized
    fun aggiornaIconaMsg(timestamp: String, msgID: Int) {
        Platform.runLater {
            schermoMessaggi.layout()
            schermoMessaggi.items.forEach { hBox ->
                val vBox = hBox.children[0] as VBox
                try {
                    val hBoxTempoVisto = vBox.children[1] as HBox
                    val icona = hBoxTempoVisto.children[1] as FontIcon
                    if(icona.id == "msg-$timestamp-$msgID"){
                        icona.iconCode = BootstrapIcons.CHECK_ALL
                    }
                } catch (ignored: Exception) {
                    /* Se non trova l'icona, non fa nulla */
                }
            }
        }
    }

    /**
     * Aggiorna il numero di utenti online
     */
    @Synchronized
    fun aggiornaUtentiOnline(numUtenti : String? = null) {
        Platform.runLater {
            if(numUtenti == null) onlineCount.text = "Info chat non disponibili"
            else if(numUtenti.toInt() == 0) onlineCount.text = "Caricamento info in corso..."
            else if(numUtenti.toInt() == 1) onlineCount.text = "1 utente online (tu)"
            else if(numUtenti.toInt() > 1) onlineCount.text = "Utenti online: $numUtenti"
        }
    }

    /**
     * Rinomina un utente nella chat
     */
    @Synchronized
    fun rinominaUtente(ex: String, nuovo: String){
        storicoChat.replaceAll { it.replace(ex, nuovo) }
        Platform.runLater {
            /* ricerca tutti i messaggi con il vecchio nome e li sostituisce con il nuovo */
            schermoMessaggi.items.forEach { hBox ->
                val vBox = hBox.children[0] as VBox
                val hBoxMittenteMsg = vBox.children[0] as HBox
                val mittente = hBoxMittenteMsg.children[0] as Label
                if(mittente.text.contains(ex)){
                    mittente.text = mittente.text.replace(ex, nuovo)
                }
            }
        }
    }

    /**
     * Aggiunge un nuovo log alla schermata
     */
    @Synchronized
    fun nuovoLog(testo : String, colore : Color?) {
        Platform.runLater {
            val label = Label("[${LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))}] $testo")
            if(colore != null) label.textFill = colore
            label.isWrapText = true
            val hBox = HBox()
            hBox.alignment = Pos.CENTER_LEFT
            hBox.children.add(label)
            logScreen.items.add(hBox)
            logScreen.scrollTo(logScreen.items.size - 1)
        }
    }
    
    /**
     * Aggiorna i dati della dashboard
     */
    @Synchronized
    fun aggiornaDashboard(indirizzoIPLocale : String? = "non disponibile", portaLocale  : String? = "non disponibile", indirizzoIPGruppo : String? = "non disponibile", portaGruppo : String? = "non disponibile", messaggiOut : String? = "non disponibile", messaggiIn : String ? = "non disponibile", statistiche : String? = "non disponibile") {
        Platform.runLater {
            impIndirizzoIPLocale.text = "Indirizzo IP locale: $indirizzoIPLocale"
            impPortaLocale.text = "Porta locale: $portaLocale"
            impIndirizzoIPGruppo.text = "Indirizzo IP gruppo: $indirizzoIPGruppo"
            impPortaGruppo.text = "Porta gruppo: $portaGruppo"
            impMessaggiOut.text = "Messaggi inviati: $messaggiOut"
            impMessaggiIn.text = "Messaggi ricevuti: $messaggiIn"
            impStatistiche.text = "Messaggi inviati con successo: $statistiche"
        }
    }
}