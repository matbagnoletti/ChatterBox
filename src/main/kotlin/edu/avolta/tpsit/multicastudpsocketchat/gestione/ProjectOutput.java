package edu.avolta.tpsit.multicastudpsocketchat.gestione;

import edu.avolta.tpsit.chatterbox.ChatterBoxController;

import edu.avolta.tpsit.multicastudpsocketchat.comunicazione.MsgType;

/**
 * Classe di gestione dell'output del programma
 * @author Matteo Bagnoletti Tini
 * @version 1.0
 * @project MulticastUDPSocketChat
 * @see OutputType
 */
public class ProjectOutput {

    /**
     * Il controller del programma per l'output UI
     */
    public static ChatterBoxController controller = null;

    /**
     * Stampa a video di un dato <code>messaggio</code>
     * @param output il messaggio da stampare
     * @param tipologia la tipologia di output
     * @see OutputType
     */
    public static synchronized void stampa(String output, OutputType tipologia) {
        switch (tipologia) {
            case OutputType.STDOUT -> System.out.println("\033[1;32m> \033[0m" + output);

            case OutputType.STDERR -> System.err.println("\033[1;31m#\033[0m\033[31m " + output + "\033[0m");

            case OutputType.LOG -> System.out.println("\033[1;37m# " + output + "\033[0m");
            
            case OutputType.UIOUT -> {
                if(controller != null && controller.application.isUIattiva()) {
                    try {
                        String mittente = output.split(":")[0];
                        String messaggio = output.split(":")[1];
                        if(!messaggio.trim().equals("DO-NOT-SHOW-THIS-MESSAGE")) controller.nuovoElemChat(messaggio, MsgType.RICEZIONE, null, mittente, null);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        controller.nuovoElemChat(output, MsgType.GESTIONE, null, null, null);
                    }
                } else if (controller != null && !controller.application.isUIattiva()) {
                    stampa(output, OutputType.STDOUT);
                }
            }
            
            case OutputType.UIERR -> {
                if(controller != null && controller.application.isUIattiva()){
                    controller.nuovoElemChat(output, MsgType.GESTIONE, null, null, null);
                } else if (controller != null && !controller.application.isUIattiva()) {
                    stampa(output, OutputType.STDERR);
                }
            }

            case OutputType.UILOG -> {
                if(controller != null && controller.application.isUIattiva()){
                    controller.nuovoLog(output, null);
                } else if (controller != null && !controller.application.isUIattiva()) {
                    stampa(output, OutputType.LOG);
                }
            }
        }
    }
}
