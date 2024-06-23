package edu.avolta.tpsit.chatterbox

import atlantafx.base.theme.CupertinoDark
import atlantafx.base.theme.CupertinoLight
import atlantafx.base.theme.Dracula
import atlantafx.base.theme.NordDark
import atlantafx.base.theme.NordLight
import atlantafx.base.theme.PrimerDark
import atlantafx.base.theme.PrimerLight
import edu.avolta.tpsit.multicastudpsocketchat.gestione.OutputType
import edu.avolta.tpsit.multicastudpsocketchat.gestione.ProjectOutput
import edu.avolta.tpsit.multicastudpsocketchat.gestione.ProjectOutput.stampa
import edu.avolta.tpsit.multicastudpsocketchat.host.GroupChat
import edu.avolta.tpsit.multicastudpsocketchat.host.MulticastPeer
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Stage
import kotlin.jvm.Throws


/**
 * Classe principale dell'applicazione ChatterBox
 * @author Matteo Bagnoletti Tini
 * @version 1.0.0-alpha
 */
class ChatterBox : Application() {
    /**
     * Riferimento alla scena corrente
     */
    lateinit var stageCorrente : Stage
    
    /**
     * Caricatore FXML
     */
    private lateinit var fxmlLoader : FXMLLoader

    /**
     * Flag per verificare se l'interfaccia è attiva
     */
    @Volatile
    var isUIattiva : Boolean = false

    /**
     * Chat multicast
     */
    private lateinit var groupChat : GroupChat

    /**
     * Peer multicast del gruppo (utente corrente)
     */
    private lateinit var multicastPeer: MulticastPeer

    /**
     * RRConfig con i dati per la chat
     */
    private lateinit var resourceRecord : RRConfig

    /**
     * Controller del setup
     */
    private lateinit var ctrlSetup : ChatSetupController
    
    /**
     * Controller della chat
     */
    private lateinit var ctrlChat : ChatterBoxController

    /**
     * Configurazione dell'applicazione
     */
    private lateinit var config: CBConfig
    
    override fun start(stage: Stage) {
        /* CSS AtlantaFX*/
        setUserAgentStylesheet(CupertinoDark().userAgentStylesheet)
        val scene = creaScena("fxml/chatsetup-view.fxml", 500.0, 350.0)
        fxmlLoader.getController<ChatSetupController>().application = this
        stage.title = "ChatterBox Setup"
        stage.scene = scene
        stage.isResizable = false
        val iconImage = Image(ChatterBox::class.java.getResourceAsStream("img/chatterbox.png"))
        stage.icons.add(iconImage)
        stage.show()
        isUIattiva = true
        stageCorrente = stage
        ctrlSetup = fxmlLoader.getController()
        
        stage.setOnCloseRequest {
            ctrlSetup.disconnettiWS()
        }
    }

    /**
     * Avvia la scena con la chat principale
     * @param resourceRecord RRConfig con i dati per la chat
     * @param nomeChat il nome della chat
     */
    fun avviaChat(resourceRecord : RRConfig, nomeChat: String) {
        this.resourceRecord = resourceRecord
        stageCorrente.close()
        isUIattiva = false
        val scene = creaScena("fxml/chatterbox-view.fxml", 650.0, 445.0)
        val stage = Stage()
        stage.title = "ChatterBox: $nomeChat"
        scene.also { stage.scene = it }
        stage.isResizable = false
        val iconImage = Image(ChatterBox::class.java.getResourceAsStream("img/chatterbox.png"))
        stage.icons.add(iconImage)
        stage.show()
        isUIattiva = true
        stageCorrente = stage
        ctrlChat = fxmlLoader.getController()
        ctrlChat.application = this
        ProjectOutput.controller = ctrlChat
        try {
            avviaDaRR()
        } catch (e: Exception) {
            stampa(e.message, OutputType.STDERR)
        }
        
        stage.setOnCloseRequest {
            ctrlSetup.disconnettiWS()
            isUIattiva = false
            multicastPeer.chiudi()
        }
        
        config = CBConfig(resourceRecord.username)
        ctrlChat.config = config
        if (config.getConfig("tema").isNotBlank() && config.getConfig("darkmode").isNotBlank()) {
            setStileUI(config.getConfig("tema"), config.getConfig("darkmode").toBoolean()) 
        }

        ctrlChat.popup("avviso", "A partire versione v2.1.0 ogni conversazione è protetta da crittografia End-to-End (AES).")
        ctrlChat.popup("avviso", "A partire versione v2.1.1 le preferenze utente sono salvate in un file di configurazione in modo che possano essere recuperate al prossimo avvio.")
    }

    /**
     * Crea una scena con un determinato fxml, dimensioni e foglio di stile
     */
    private fun creaScena(fxmlPath: String, width: Double, height: Double, cssPath: String? = "style/mbstyle.css"): Scene {
        val fxmlLoader = FXMLLoader(ChatterBox::class.java.getResource(fxmlPath))
        val scene = Scene(fxmlLoader.load(), width, height)
        val cssResource = cssPath?.let { javaClass.getResource(it) }
        if (cssResource != null) {
            scene.stylesheets.add(cssResource.toExternalForm())
        }
        this.fxmlLoader = fxmlLoader
        return scene
    }

    /**
     * Cambia il tema dell'applicazione in base al tema e al dark mode
     */
    fun setStileUI(tema: String, darkMode: Boolean) {
        val stile = when {
            tema == "Default" && darkMode -> CupertinoDark().userAgentStylesheet
            tema == "Default" -> CupertinoLight().userAgentStylesheet
            tema == "Nord" && darkMode -> NordDark().userAgentStylesheet
            tema == "Nord" -> NordLight().userAgentStylesheet
            tema == "Primer" && darkMode -> PrimerDark().userAgentStylesheet
            tema == "Primer" -> PrimerLight().userAgentStylesheet
            tema == "Dracula" -> Dracula().userAgentStylesheet
            else -> null
        }
        
        ctrlChat.toggleTema.isSelected = darkMode
        ctrlChat.sceltaStileUI.value = tema

        stile?.let { setUserAgentStylesheet(it) }
        config.setConfig("tema", tema, "")
        config.setConfig("darkmode", darkMode.toString(), "")
    }

    /**
     * Ricevuto un messaggio dall'utente, lo invia al gruppo o privatamente
     */
    fun nuovoMsgDaInput(text: String) {
        multicastPeer.inputDaUI(text)
    }

    /**
     * Termina l'esecuzione della chat
     */
    fun terminaChat() {
        multicastPeer.chiudi()
    }

    /**
     * Avvia la chat da un RRConfig
     * @throws Exception se ci sono errori nella configurazione
     */
    @Throws(Exception::class)
    fun avviaDaRR() {
        groupChat = GroupChat(resourceRecord.ws.idChat, resourceRecord.ws.pswChat, resourceRecord.indirizzoIP, resourceRecord.porta.toInt(), resourceRecord.ttl.toInt(), resourceRecord.loopbackOff)
        multicastPeer = MulticastPeer(resourceRecord.username, true, groupChat, ctrlChat, resourceRecord)
        multicastPeer.configura()
        multicastPeer.avvia()
        ctrlChat.aggiornaInfoGruppo(resourceRecord.username, resourceRecord.ws.nomeChat, resourceRecord.sGateKey)
    }
}

fun main() {
    Application.launch(ChatterBox::class.java)
}