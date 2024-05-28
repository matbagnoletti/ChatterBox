package edu.avolta.tpsit.chatterbox

import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.text.Text
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons
import org.kordamp.ikonli.javafx.FontIcon
import kotlin.math.roundToInt


class ChatSetupController {
    @FXML
    lateinit var btnInfo : Button
    @FXML
    lateinit var btnStart : Button
    @FXML
    lateinit var username : TextField
    @FXML
    lateinit var indirizzoIP : TextField
    @FXML
    lateinit var porta : TextField
    @FXML
    lateinit var TTL : Slider
    @FXML
    lateinit var ttlImpostato : Text
    @FXML
    lateinit var loopbackOff : CheckBox

    /**
     * Riferimento all'applicazione
     */
    lateinit var application : ChatterBox
    
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
    
    @FXML
    fun aggiornaTTL() {
        ttlImpostato.text = TTL.value.roundToInt().toString()
    }

    @FXML
    fun controllaForm() {
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

        btnStart.isDisable = !(username.text.isNotEmpty() && isValidMulticast && isValidPort)
    }
    
    @FXML
    fun avviaChat(){
        val resourceRecord = RR(username = username.text, indirizzoIP = indirizzoIP.text, porta = porta.text, ttl = ttlImpostato.text, loopbackOff = loopbackOff.isSelected)
        application.avviaChat(resourceRecord)
    }
}