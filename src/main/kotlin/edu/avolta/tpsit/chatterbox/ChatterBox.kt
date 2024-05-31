package edu.avolta.tpsit.chatterbox

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
    
    override fun start(stage: Stage) {
        /* CSS AtlantaFX*/
        setUserAgentStylesheet(PrimerDark().userAgentStylesheet)
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
        ctrlSetup = fxmlLoader.getController<ChatSetupController>()
        
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
        ctrlChat = fxmlLoader.getController<ChatterBoxController>()
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
    }

    /**
     * Crea una scena con un determinato fxml, dimensioni e foglio di stile
     */
    private fun creaScena(fxmlPath: String, width: Double, height: Double, cssPath: String? = "style/mbstyle.css"): Scene {
        val fxmlLoader = FXMLLoader(ChatterBox::class.java.getResource(fxmlPath))
        val scene = Scene(fxmlLoader.load(), width, height)
        val cssResource = javaClass.getResource(cssPath)
        if (cssResource != null) {
            scene.stylesheets.add(cssResource.toExternalForm())
        }
        this.fxmlLoader = fxmlLoader
        return scene
    }

    /**
     * Cambia il tema dell'applicazione
     */
    fun cambiaTema(tema : String){
        when(tema) {
            "scuro" -> {
                setUserAgentStylesheet(PrimerDark().userAgentStylesheet)
            }
            "chiaro" -> {
                setUserAgentStylesheet(PrimerLight().userAgentStylesheet)
            }
        }
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
        ctrlChat.aggiornaInfoGruppo(resourceRecord.username, resourceRecord.ws.nomeChat, resourceRecord.ws.pswChat)
    }
}

fun main() {
    Application.launch(ChatterBox::class.java)
}