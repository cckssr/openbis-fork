package ch.openbis.drive.gui;

import javafx.application.Application;

public class Launcher {
    public static void main(String[] args) {
        System.setProperty("javafx.preloader", Preloader.class.getName());
        Application.launch(OpenDriveApplication.class, args);
    }
}
