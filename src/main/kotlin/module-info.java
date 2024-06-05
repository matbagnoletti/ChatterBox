module edu.avolta.tpsit {
    requires javafx.controls;
    requires javafx.fxml;
    requires kotlin.stdlib;
    requires javafx.graphics;
    
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires org.kordamp.ikonli.bootstrapicons;
    requires org.kordamp.ikonli.core;
    
    requires atlantafx.base;
    
    requires okhttp3;
    requires com.google.gson;

    opens edu.avolta.tpsit.chatterbox to org.kordamp.ikonli.bootstrapicons, javafx.fxml, com.google.gson, okhttp3;
    exports edu.avolta.tpsit.chatterbox;
    exports edu.avolta.tpsit.security;
    exports edu.avolta.tpsit.multicastudpsocketchat.comunicazione;
    exports edu.avolta.tpsit.multicastudpsocketchat.eccezioni;
    exports edu.avolta.tpsit.multicastudpsocketchat.gestione;
    exports edu.avolta.tpsit.multicastudpsocketchat.host;
    exports edu.avolta.tpsit.multicastudpsocketchat.utenze;
}