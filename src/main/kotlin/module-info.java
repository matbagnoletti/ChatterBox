module edu.avolta.tpsit {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires kotlin.stdlib;
    requires javafx.graphics;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires atlantafx.base;
    requires org.kordamp.ikonli.bootstrapicons;
    requires org.kordamp.ikonli.core;
    requires java.desktop;

    opens edu.avolta.tpsit.chatterbox to javafx.fxml;
    exports edu.avolta.tpsit.chatterbox;
    exports edu.avolta.tpsit.multicastudpsocketchat.comunicazione;
    exports edu.avolta.tpsit.multicastudpsocketchat.eccezioni;
    exports edu.avolta.tpsit.multicastudpsocketchat.gestione;
    exports edu.avolta.tpsit.multicastudpsocketchat.host;
    exports edu.avolta.tpsit.multicastudpsocketchat.utenze;
}