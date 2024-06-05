package edu.avolta.tpsit.chatterbox

import atlantafx.base.controls.Notification
import atlantafx.base.theme.Styles
import atlantafx.base.util.Animations
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.geometry.Insets
import javafx.scene.control.*
import javafx.scene.layout.AnchorPane
import javafx.scene.text.Text
import javafx.util.Duration
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons
import org.kordamp.ikonli.javafx.FontIcon
import kotlin.math.roundToInt

/**
 * Controller per la configurazione della chat
 */
class ChatSetupController {
    @FXML
    lateinit var btnCreaGruppo : Button
    @FXML
    lateinit var btnStart : Button
    @FXML
    lateinit var username : TextField
    @FXML
    lateinit var nomeGruppo : TextField
    @FXML
    lateinit var passwordGruppo : PasswordField
    @FXML
    lateinit var TTL : Slider
    @FXML
    lateinit var ttlImpostato : Text
    @FXML
    lateinit var loopbackOff : CheckBox
    @FXML
    lateinit var schermo : AnchorPane

    /**
     * Riferimento all'applicazione
     */
    lateinit var application : ChatterBox

    /**
     * Web service handler
     */
    private val ws = WSHandler()
    
    /*
    fun initialize() {
        btnInfo.graphic = FontIcon(BootstrapIcons.INFO_CIRCLE)
    }

    @FXML
    fun info() {
        val infoMsg = "ChatterBox sfrutta la tecnologia Multicast UDP per scambiare messaggi. Per poter accedere ad un gruppo sono necessari un indirizzo IP di classe D (da 224.0.1.0 - 238.255.255.255 esclusi per il multicast globale) e un numero di porta (da 0 a 65535) validi."
        val alert = Alert(Alert.AlertType.INFORMATION, infoMsg, ButtonType.OK)
        alert.isResizable = false
        alert.title = "ChatterBox"
        alert.headerText = "Info su ChatterBox"
        alert.showAndWait()
        alert.close()
    }
     */

    /**
     * Aggiorna il valore del TTL in base alla posizione dello slider
     */
    @FXML
    fun aggiornaTTL() {
        ttlImpostato.text = TTL.value.roundToInt().toString()
    }

    /**
     * Controlla la validità dei dati inseriti
     */
    @FXML
    fun controllaForm() {
        /*
        val segmentiIP = indirizzoIP.text.split(".")
        val isValidMulticast = segmentiIP.size == 4 && segmentiIP[0].toIntOrNull() in 224..238
                && segmentiIP[1].toIntOrNull() in 0..255
                && segmentiIP[2].toIntOrNull() in 1..255
                && segmentiIP[3].toIntOrNull() in 1..254
                && !indirizzoIP.text.startsWith("224.0.0.")

        val isValidPort = try {
            val port = porta.text.toInt()
            port in 1024..65535
        } catch (e: NumberFormatException) {
            false
        }
         */

        btnStart.isDisable = !(username.text.isNotBlank() && nomeGruppo.text.isNotBlank() && passwordGruppo.text.isNotBlank())
    }

    /**
     * Crea un nuovo gruppo e avvia la chat
     */
    @FXML
    fun creaGruppo() {
        
        if (!validaInput()) return
        
        val nome = nomeGruppo.text
        val password = passwordGruppo.text
        val username = username.text
        val ttl = ttlImpostato.text
        val loopbackOff = loopbackOff.isSelected
        disabilitaUI(true)
        
        ws.creaGruppo(nome, password) { esito ->
            if (esito is RRWebService) {
                popup("successo", "Gruppo creato con successo")
                val resourceRecord = RRConfig(esito, username = username, sGateKey = password, indirizzoIP = esito.ipChat, porta = esito.portaChat, ttl = ttl, loopbackOff = loopbackOff)
                Thread.sleep(1500)
                Platform.runLater{
                    application.avviaChat(resourceRecord, esito.nomeChat)
                }
            } else if (esito is APIError) {
                popup("errore", esito.messaggio)
                disabilitaUI(false)
            }
        }
    }

    /**
     * Recupera un gruppo esistente e avvia la chat
     */
    @FXML
    fun avviaChat(){
        
        if (!validaInput()) return
        
        val nome = nomeGruppo.text
        val password = passwordGruppo.text
        val username = username.text
        val ttl = ttlImpostato.text
        val loopbackOff = loopbackOff.isSelected
        disabilitaUI(true)


        ws.cercaGruppo(nome, password) { esito ->
            if (esito is RRWebService) {
                popup("successo", "Accesso al gruppo in corso...")
                val resourceRecord = RRConfig(esito, username = username, sGateKey = password, indirizzoIP = esito.ipChat, porta = esito.portaChat, ttl = ttl, loopbackOff = loopbackOff)
                Thread.sleep(1500)
                Platform.runLater{
                    application.avviaChat(resourceRecord, esito.nomeChat)
                }
            } else if (esito is APIError) {
                popup("errore", esito.messaggio)
                disabilitaUI(false)
            }
        }
    }

    /**
     * Mostra a schermo un popup generico
     * @param tipo tipo di popup (successo o errore)
     * @param msg messaggio da visualizzare
    */
    @Synchronized
    fun popup(tipo: String, msg: String) {
        Platform.runLater {
            val avvisoErrore = Notification(
                msg,
                try {
                    FontIcon(BootstrapIcons.EXCLAMATION_CIRCLE)
                } catch (e: Exception) {
                    FontIcon("bi-exclamation-circle")
                },
            )
            if(tipo == "successo") {
                avvisoErrore.styleClass.addAll(
                    Styles.SUCCESS, Styles.ELEVATED_1
                )
            } else {
                avvisoErrore.styleClass.addAll(
                    Styles.DANGER, Styles.ELEVATED_1
                )
            }
            avvisoErrore.onClose = EventHandler {
                val out = Animations.fadeOut(avvisoErrore, Duration.seconds(0.5))
                out.onFinished = EventHandler {
                    if(schermo.children.contains(avvisoErrore))
                        schermo.children.remove(avvisoErrore)
                }
                out.play()
            }
            avvisoErrore.prefHeight = 50.0
            avvisoErrore.maxWidth = 250.0 + msg.length
            avvisoErrore.padding = Insets(5.0)
            avvisoErrore.layoutX = schermo.width - avvisoErrore.maxWidth - 1
            schermo.children.add(avvisoErrore)
            Animations.slideInDown(avvisoErrore, Duration.seconds(1.5)).play()
        }
    }
    
    /**
     * Controlla la validità dei dati inseriti
     */
    private fun validaInput(): Boolean {
        if (username.text.length < 3) {
            popup("errore", "L'username deve contenere almeno 3 caratteri")
            return false
        }

        if (nomeGruppo.text.length < 5) {
            popup("errore", "Il nome del gruppo deve contenere almeno 5 caratteri")
            return false
        }

        if (passwordGruppo.text.length < 5) {
            popup("errore", "La password del gruppo deve contenere almeno 5 caratteri")
            return false
        }

        if (TTL.value.roundToInt() !in 1..255) {
            popup("errore", "Il TTL deve essere compreso tra 1 e 255")
            return false
        }
        
        return true
    }

    /**
     * Disabilita o abilita la UI
     * @param disabilita true per disabilitare, false per abilitare
     */
    private fun disabilitaUI(disabilita: Boolean) {
        Platform.runLater {
            btnCreaGruppo.isDisable = disabilita
            btnStart.isDisable = disabilita
            username.isDisable = disabilita
            nomeGruppo.isDisable = disabilita
            passwordGruppo.isDisable = disabilita
            TTL.isDisable = disabilita
            loopbackOff.isDisable = disabilita
        }
    }
    
    fun disconnettiWS() {
        ws.disconnetti()
    }
}