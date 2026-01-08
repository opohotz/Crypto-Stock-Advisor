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
 * register screen
 */
public class RegisterScreen {
    private Stage primaryStage;
    private final HttpClient httpClient;
    private static final String API_BASE_URL = "http://localhost:3000";
    
    public RegisterScreen(Stage primaryStage) {
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
        
        // Register form container
        VBox formContainer = new VBox(15);
        formContainer.setAlignment(Pos.CENTER);
        formContainer.setMaxWidth(400);
        formContainer.setPadding(new Insets(30));
        formContainer.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        
        // Form title
        Text formTitle = new Text("Create Account");
        formTitle.setFont(Font.font("System", FontWeight.BOLD, 24));
        formTitle.setFill(Color.web("#333333"));
        
        // Username field
        Label usernameLabel = new Label("Username:");
        usernameLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        TextField usernameField = new TextField();
        usernameField.setPromptText("Choose a username");
        usernameField.setPrefHeight(35);
        usernameField.setStyle("-fx-border-radius: 5; -fx-border-color: #cccccc;");
        
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
        passwordField.setPromptText("Choose a password");
        passwordField.setPrefHeight(35);
        passwordField.setStyle("-fx-border-radius: 5; -fx-border-color: #cccccc;");
        
        // Confirm password field
        Label confirmPasswordLabel = new Label("Confirm Password:");
        confirmPasswordLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm your password");
        confirmPasswordField.setPrefHeight(35);
        confirmPasswordField.setStyle("-fx-border-radius: 5; -fx-border-color: #cccccc;");
        
        // Register button
        Button registerButton = new Button("Create Account");
        registerButton.setPrefWidth(200);
        registerButton.setPrefHeight(40);
        registerButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
        registerButton.setOnAction(e -> handleRegister(usernameField.getText(), emailField.getText(), 
            passwordField.getText(), confirmPasswordField.getText()));
        
        // Error label
        Label errorLabel = new Label();
        errorLabel.setTextFill(Color.RED);
        errorLabel.setVisible(false);
        
        // Success label
        Label successLabel = new Label();
        successLabel.setTextFill(Color.GREEN);
        successLabel.setVisible(false);
        
        // Login link
        HBox loginContainer = new HBox(5);
        loginContainer.setAlignment(Pos.CENTER);
        Text loginText = new Text("Already have an account?");
        Hyperlink loginLink = new Hyperlink("Login here");
        loginLink.setOnAction(e -> showLoginScreen());
        loginContainer.getChildren().addAll(loginText, loginLink);
        
        // Add elements to form container
        formContainer.getChildren().addAll(
            formTitle,
            usernameLabel, usernameField,
            emailLabel, emailField,
            passwordLabel, passwordField,
            confirmPasswordLabel, confirmPasswordField,
            registerButton,
            errorLabel,
            successLabel,
            loginContainer
        );
        
        // Add elements to main container
        mainContainer.getChildren().addAll(title, formContainer);
        
        // Create scene
        Scene scene = new Scene(mainContainer, 500, 700);
        primaryStage.setScene(scene);
        primaryStage.setTitle("CryptoAdvisor - Register");
        primaryStage.setResizable(false);
        primaryStage.show();
        
        // Handle Enter key for registration
        usernameField.setOnAction(e -> emailField.requestFocus());
        emailField.setOnAction(e -> passwordField.requestFocus());
        passwordField.setOnAction(e -> confirmPasswordField.requestFocus());
        confirmPasswordField.setOnAction(e -> handleRegister(usernameField.getText(), emailField.getText(), 
            passwordField.getText(), confirmPasswordField.getText()));
    }
    
    private void handleRegister(String username, String email, String password, String confirmPassword) {
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showMessage("Please fill in all fields", true);
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            showMessage("Passwords do not match", true);
            return;
        }
        
        // if (password.length() < 6) {
        //     showMessage("Password must be at least 6 characters long", true);
        //     return;
        // }
        
        // Show loading state
        showMessage("Creating account...", false);
        
        // Make API call to backend
        new Thread(() -> {
            try {
                String requestBody = String.format("{\"user_name\":\"%s\",\"user_email\":\"%s\",\"user_password\":\"%s\"}", 
                    username, email, password);
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_BASE_URL + "/api/auth/register"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                Platform.runLater(() -> {
                    if (response.statusCode() == 201) {
                        try {
                            // Parse the response to get the token
                            ObjectMapper mapper = new ObjectMapper();
                            JsonNode jsonNode = mapper.readTree(response.body());
                            String token = jsonNode.get("token").asText();
                            
                            // Store the token
                            TokenManager.setAuthToken(token);
                            
                            showMessage("Account created successfully! Redirecting...", false);
                            
                            // Navigate to preferences screen after registration
                            new Thread(() -> {
                                try {
                                    Thread.sleep(1500);
                                    Platform.runLater(() -> showPreferencesScreen());
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            }).start();
                        } catch (Exception e) {
                            showMessage("Error processing registration response", true);
                        }
                    } else {
                        // Registration failed
                        String errorMessage = "Registration failed";
                        if (response.statusCode() == 409) {
                            errorMessage = "User with this email or username already exists";
                        }
                        showMessage(errorMessage, true);
                    }
                });
                
            } catch (IOException | InterruptedException e) {
                Platform.runLater(() -> {
                    showMessage("Connection error. Please check if backend server is running.", true);
                });
            }
        }).start();
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
    
    private void showLoginScreen() {
        LoginScreen loginScreen = new LoginScreen(primaryStage);
        loginScreen.show();
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
