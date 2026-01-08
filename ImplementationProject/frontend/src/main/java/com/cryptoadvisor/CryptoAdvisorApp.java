package com.cryptoadvisor;

import com.cryptoadvisor.ui.LoginScreen;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * main app
 */
public class CryptoAdvisorApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        System.out.println("javafx start");

        // login screen
        LoginScreen loginScreen = new LoginScreen(primaryStage);
        loginScreen.show();

        System.out.println("app visible");
    }

    public static void main(String[] args) {
        System.out.println("launching app");
        launch(args);
    }
}