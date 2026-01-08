package com.cryptoadvisor.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import com.cryptoadvisor.util.TokenManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * login screen
 */
public class LoginScreen {
    private Stage primaryStage;
    private final HttpClient httpClient;
    // api url
    private static final String API_BASE_URL = "http://localhost:3000";
    
    public LoginScreen(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }
    
    public void show() {
        // Main container
        VBox mainContainer = new VBox(20);
        mainContainer.setAlignment(Pos.CENTER);
        mainContainer.setPadding(new Insets(40));
        mainContainer.setStyle("-fx-background-color: #f5f5f5;");
        
        // Title
        Text title = new Text("🚀 CryptoAdvisor");
        title.setFont(Font.font("System", FontWeight.BOLD, 32));
        title.setFill(Color.web("#1976D2"));
        
        // Login form container
        VBox formContainer = new VBox(15);
        formContainer.setAlignment(Pos.CENTER);
        formContainer.setMaxWidth(400);
        formContainer.setPadding(new Insets(30));
        formContainer.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        
        // Form title
        Text formTitle = new Text("Login");
        formTitle.setFont(Font.font("System", FontWeight.BOLD, 24));
        formTitle.setFill(Color.web("#333333"));
        
        // Email field
        Label emailLabel = new Label("Email:");
        emailLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        TextField emailField = new TextField();
        emailField.setPromptText("Enter your email");
        emailField.setPrefHeight(35);
        emailField.setStyle("-fx-border-radius: 5; -fx-border-color: #cccccc;");
        
        // Password field
        Label passwordLabel = new Label("Password:");
        passwordLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password");
        passwordField.setPrefHeight(35);
        passwordField.setStyle("-fx-border-radius: 5; -fx-border-color: #cccccc;");
        
        // Login button
        Button loginButton = new Button("Login");
        loginButton.setPrefWidth(200);
        loginButton.setPrefHeight(40);
        loginButton.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
        loginButton.setOnAction(e -> handleLogin(emailField.getText(), passwordField.getText()));
        
        // Error label
        Label errorLabel = new Label();
        errorLabel.setTextFill(Color.RED);
        errorLabel.setVisible(false);
        
        // Register link
        HBox registerContainer = new HBox(5);
        registerContainer.setAlignment(Pos.CENTER);
        Text registerText = new Text("Don't have an account?");
        Hyperlink registerLink = new Hyperlink("Register here");
        registerLink.setOnAction(e -> showRegisterScreen());
        registerContainer.getChildren().addAll(registerText, registerLink);
        
        // Add elements to form container
        formContainer.getChildren().addAll(
            formTitle,
            emailLabel, emailField,
            passwordLabel, passwordField,
            loginButton,
            errorLabel,
            registerContainer
        );
        
        // Add elements to main container
        mainContainer.getChildren().addAll(title, formContainer);
        
        // Create scene
        Scene scene = new Scene(mainContainer, 500, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("CryptoAdvisor - Login");
        primaryStage.setResizable(false);
        primaryStage.show();
        
        // Handle Enter key for login
        emailField.setOnAction(e -> passwordField.requestFocus());
        passwordField.setOnAction(e -> handleLogin(emailField.getText(), passwordField.getText()));
    }
    
    private void handleLogin(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            showMessage("Please fill in all fields", true);
            return;
        }
        
        // wait for API
        showMessage("Logging in...", false);
        
        // Make API call to backend
        new Thread(() -> {
            try {
                String requestBody = String.format("{\"user_email\":\"%s\",\"user_password\":\"%s\"}", email, password);
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_BASE_URL + "/api/auth/login"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();
                //341 logic
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        try {
                            // grab token
                            ObjectMapper mapper = new ObjectMapper();
                            JsonNode jsonNode = mapper.readTree(response.body());
                            String token = jsonNode.get("token").asText();
                            
                            //  local storage
                            TokenManager.setAuthToken(token);
                            
                            showMessage("Login successful!", false);
                            
                            // check preferences
                            new Thread(() -> {
                                try {
                                    Thread.sleep(500);
                                    checkPreferencesAndRedirect(token);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            }).start();
                        } catch (Exception e) {
                            showMessage("Error processing login response", true);
                        }
                    } else {
                        // Login failed
                        showMessage("Invalid email or password", true);
                    }
                });
                
            } catch (IOException | InterruptedException e) {
                Platform.runLater(() -> {
                    showMessage("Connection error. Please check if backend server is running.", true);
                });
            }
        }).start();
    }
    
    private void checkPreferencesAndRedirect(String token) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE_URL + "/api/user/preferences"))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            Platform.runLater(() -> {
                if (response.statusCode() == 200) {
                    // prefs exist
                    showHomeScreen();
                } else {
                    // no prefs
                    showPreferencesScreen();
                }
            });
        } catch (IOException | InterruptedException e) {
            Platform.runLater(() -> showPreferencesScreen());
        }
    }
    
    private void showMessage(String message, boolean isError) {
        // Find the error/success label in the current scene
        VBox mainContainer = (VBox) primaryStage.getScene().getRoot();
        VBox formContainer = (VBox) mainContainer.getChildren().get(1);
        
        for (var child : formContainer.getChildren()) {
            if (child instanceof Label) {
                Label label = (Label) child;
                if (isError && label.getTextFill().equals(Color.RED)) {
                    label.setText(message);
                    label.setVisible(true);
                    break;
                } else if (!isError && label.getTextFill().equals(Color.GREEN)) {
                    label.setText(message);
                    label.setVisible(true);
                    break;
                }
            }
        }
    }
    
    private void showRegisterScreen() {
        RegisterScreen registerScreen = new RegisterScreen(primaryStage);
        registerScreen.show();
    }
    
    private void showHomeScreen() {
        HomeScreen homeScreen = new HomeScreen(primaryStage);
        homeScreen.show();
    }
    
    private void showPreferencesScreen() {
        PreferencesScreen preferencesScreen = new PreferencesScreen(primaryStage);
        preferencesScreen.show();
    }
}
