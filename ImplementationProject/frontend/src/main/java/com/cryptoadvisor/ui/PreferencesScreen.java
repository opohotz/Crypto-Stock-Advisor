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
import java.util.ArrayList;
import java.util.List;

import com.cryptoadvisor.util.TokenManager;

/**
 * preferences screen
 */
public class PreferencesScreen {
    private Stage primaryStage;
    private final HttpClient httpClient;
    private static final String API_BASE_URL = "http://localhost:3000";
    
    private Label statusLabel;
    
    public PreferencesScreen(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }
    
    // Store UI elements for updating
    private RadioButton stocksOption;
    private RadioButton cryptoOption;
    private RadioButton bothOption;
    private RadioButton longTermOption;
    private RadioButton dayTradeOption;
    private List<CheckBox> industryCheckBoxes;
    private ToggleGroup assetToggleGroup;
    private ToggleGroup investmentToggleGroup;
    
    public void show() {
        // Main container with scroll pane
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #f5f5f5; -fx-background-color: #f5f5f5;");
        
        VBox mainContainer = new VBox(25);
        mainContainer.setAlignment(Pos.TOP_CENTER);
        mainContainer.setPadding(new Insets(30));
        mainContainer.setStyle("-fx-background-color: #f5f5f5;");
        
        // Title
        Text title = new Text("🎯 Set Your Investment Preferences");
        title.setFont(Font.font("System", FontWeight.BOLD, 28));
        title.setFill(Color.web("#1976D2"));
        
        // Description
        Text description = new Text("Tell us about your investment interests so we can personalize your experience.");
        description.setFont(Font.font("System", 14));
        description.setFill(Color.web("#666666"));
        description.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        
        // ===== SECTION 1: Asset Type =====
        VBox assetSection = new VBox(15);
        assetSection.setAlignment(Pos.CENTER_LEFT);
        assetSection.setPadding(new Insets(20));
        assetSection.setMaxWidth(700);
        assetSection.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        
        Text assetTitle = new Text("1. Asset Type");
        assetTitle.setFont(Font.font("System", FontWeight.BOLD, 18));
        assetTitle.setFill(Color.web("#1976D2"));
        
        assetToggleGroup = new ToggleGroup();
        stocksOption = new RadioButton("📈 Stocks");
        stocksOption.setToggleGroup(assetToggleGroup);
        stocksOption.setUserData("stocks");
        stocksOption.setFont(Font.font("System", 14));
        
        cryptoOption = new RadioButton("🪙 Cryptocurrency");
        cryptoOption.setToggleGroup(assetToggleGroup);
        cryptoOption.setUserData("crypto");
        cryptoOption.setFont(Font.font("System", 14));
        
        bothOption = new RadioButton("🔄 Both");
        bothOption.setToggleGroup(assetToggleGroup);
        bothOption.setUserData("both");
        bothOption.setFont(Font.font("System", 14));
        
        // Default to stocks (will be overridden if preferences exist)
        stocksOption.setSelected(true);
        
        assetSection.getChildren().addAll(assetTitle, stocksOption, cryptoOption, bothOption);
        
        // ===== SECTION 2: Investment Type =====
        VBox investmentSection = new VBox(15);
        investmentSection.setAlignment(Pos.CENTER_LEFT);
        investmentSection.setPadding(new Insets(20));
        investmentSection.setMaxWidth(700);
        investmentSection.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        
        Text investmentTitle = new Text("2. Investment Strategy");
        investmentTitle.setFont(Font.font("System", FontWeight.BOLD, 18));
        investmentTitle.setFill(Color.web("#1976D2"));
        
        investmentToggleGroup = new ToggleGroup();
        longTermOption = new RadioButton("🏦 Long-Term Investment");
        longTermOption.setToggleGroup(investmentToggleGroup);
        longTermOption.setUserData("Long-Term");
        longTermOption.setFont(Font.font("System", 14));
        
        dayTradeOption = new RadioButton("⚡ Day Trade");
        dayTradeOption.setToggleGroup(investmentToggleGroup);
        dayTradeOption.setUserData("Day Trade");
        dayTradeOption.setFont(Font.font("System", 14));
        
        // Default to Long-Term (will be overridden if preferences exist)
        longTermOption.setSelected(true);
        
        investmentSection.getChildren().addAll(investmentTitle, longTermOption, dayTradeOption);
        
        // ===== SECTION 3: Industries =====
        VBox industrySection = new VBox(15);
        industrySection.setAlignment(Pos.CENTER_LEFT);
        industrySection.setPadding(new Insets(20));
        industrySection.setMaxWidth(700);
        industrySection.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        
        Text industryTitle = new Text("3. Industries (Select up to 3)");
        industryTitle.setFont(Font.font("System", FontWeight.BOLD, 18));
        industryTitle.setFill(Color.web("#1976D2"));
        
        // Industry checkboxes
        industryCheckBoxes = new ArrayList<>();
        String[] industries = {"Technology", "Healthcare", "Energy", "Finance", "Consumer", "Automotive"};
        
        FlowPane industryPane = new FlowPane();
        industryPane.setHgap(15);
        industryPane.setVgap(10);
        
        for (String industry : industries) {
            CheckBox checkBox = new CheckBox(industry);
            checkBox.setFont(Font.font("System", 14));
            checkBox.setOnAction(e -> {
                // Limit to 3 selections
                long selectedCount = industryCheckBoxes.stream().filter(CheckBox::isSelected).count();
                if (selectedCount > 3) {
                    checkBox.setSelected(false);
                    showMessage("You can select up to 3 industries only.", true);
                }
            });
            industryCheckBoxes.add(checkBox);
            industryPane.getChildren().add(checkBox);
        }
        
        industrySection.getChildren().addAll(industryTitle, industryPane);
        
        // Status label
        statusLabel = new Label();
        statusLabel.setTextFill(Color.RED);
        statusLabel.setVisible(false);
        statusLabel.setFont(Font.font("System", 14));
        
        // Buttons
        HBox buttonContainer = new HBox(20);
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.setPadding(new Insets(10, 0, 0, 0));
        
        Button saveButton = new Button("Save Preferences");
        saveButton.setPrefWidth(200);
        saveButton.setPrefHeight(45);
        saveButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14; -fx-background-radius: 5;");
        saveButton.setOnAction(e -> handleSavePreferences(assetToggleGroup, investmentToggleGroup, industryCheckBoxes));
        
        Button skipButton = new Button("Skip for Now");
        skipButton.setPrefWidth(150);
        skipButton.setPrefHeight(45);
        skipButton.setStyle("-fx-background-color: #757575; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14; -fx-background-radius: 5;");
        skipButton.setOnAction(e -> showHomeScreen());
        
        buttonContainer.getChildren().addAll(saveButton, skipButton);
        
        // Add all sections to main container
        mainContainer.getChildren().addAll(
            title, 
            description, 
            assetSection, 
            investmentSection, 
            industrySection,
            statusLabel,
            buttonContainer
        );
        
        scrollPane.setContent(mainContainer);
        
        // Create scene
        Scene scene = new Scene(scrollPane, 800, 700);
        primaryStage.setScene(scene);
        primaryStage.setTitle("CryptoAdvisor - Set Preferences");
        primaryStage.setResizable(true);
        primaryStage.show();
        
        // Load existing preferences after UI is shown
        loadExistingPreferences();
    }
    
    private void loadExistingPreferences() {
        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_BASE_URL + "/api/user/preferences"))
                        .header("Authorization", "Bearer " + getAuthToken())
                        .GET()
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        try {
                            String responseBody = response.body();
                            System.out.println("Preferences response: " + responseBody);
                            
                            // Parse preferences from JSON
                            String preferredAssetType = extractValue(responseBody, "\"preferred_asset_type\":\"");
                            String investmentType = extractValue(responseBody, "\"investment_type\":\"");
                            String industriesJson = extractValue(responseBody, "\"industries\":");
                            
                            // Set asset type
                            if (!preferredAssetType.equals("N/A")) {
                                if ("stocks".equals(preferredAssetType)) {
                                    stocksOption.setSelected(true);
                                } else if ("crypto".equals(preferredAssetType)) {
                                    cryptoOption.setSelected(true);
                                } else if ("both".equals(preferredAssetType)) {
                                    bothOption.setSelected(true);
                                }
                            }
                            
                            // Set investment type
                            if (!investmentType.equals("N/A")) {
                                if ("Long-Term".equals(investmentType)) {
                                    longTermOption.setSelected(true);
                                } else if ("Day Trade".equals(investmentType)) {
                                    dayTradeOption.setSelected(true);
                                }
                            }
                            
                            // Parse and set industries
                            if (!industriesJson.equals("N/A") && industriesJson != null && !industriesJson.isEmpty()) {
                                // Extract industries array from JSON
                                if (industriesJson.trim().startsWith("[")) {
                                    // Find the full array
                                    int arrayStart = responseBody.indexOf("\"industries\":[");
                                    if (arrayStart != -1) {
                                        arrayStart = responseBody.indexOf("[", arrayStart);
                                        int arrayEnd = responseBody.indexOf("]", arrayStart);
                                        if (arrayEnd != -1) {
                                            String industriesArrayStr = responseBody.substring(arrayStart + 1, arrayEnd);
                                            // Split by comma and clean up quotes
                                            String[] industriesArray = industriesArrayStr.split(",");
                                            
                                            for (String industry : industriesArray) {
                                                industry = industry.trim().replace("\"", "").replace("'", "");
                                                if (!industry.isEmpty()) {
                                                    for (CheckBox checkBox : industryCheckBoxes) {
                                                        if (checkBox.getText().equals(industry)) {
                                                            checkBox.setSelected(true);
                                                            break;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            System.out.println("Loaded preferences: AssetType=" + preferredAssetType + ", InvestmentType=" + investmentType);
                            
                        } catch (Exception e) {
                            System.err.println("Error parsing preferences: " + e.getMessage());
                            e.printStackTrace();
                        }
                    } else if (response.statusCode() == 404) {
                        // No preferences found - that's okay, use defaults
                        System.out.println("No existing preferences found, using defaults");
                    } else {
                        System.err.println("Failed to load preferences: " + response.statusCode() + " - " + response.body());
                    }
                });
                
            } catch (IOException | InterruptedException e) {
                Platform.runLater(() -> {
                    System.err.println("Error loading preferences: " + e.getMessage());
                });
                e.printStackTrace();
            }
        }).start();
    }
    
    private String extractValue(String json, String key) {
        try {
            int start = json.indexOf(key);
            if (start == -1) return "N/A";
            start += key.length();
            
            int end;
            if (key.contains("\"")) {
                end = json.indexOf("\"", start);
            } else {
                // For arrays, find the closing bracket
                if (json.charAt(start) == '[') {
                    int bracketCount = 0;
                    end = start;
                    while (end < json.length()) {
                        if (json.charAt(end) == '[') bracketCount++;
                        if (json.charAt(end) == ']') {
                            bracketCount--;
                            if (bracketCount == 0) {
                                end++;
                                break;
                            }
                        }
                        end++;
                    }
                } else {
                    end = json.indexOf(",", start);
                    if (end == -1) end = json.indexOf("}", start);
                }
            }
            
            if (end == -1) return "N/A";
            return json.substring(start, end).trim();
        } catch (Exception e) {
            return "N/A";
        }
    }
    
    private void handleSavePreferences(ToggleGroup assetToggleGroup, ToggleGroup investmentToggleGroup, List<CheckBox> industryCheckBoxes) {
        // Get asset type
        RadioButton selectedAsset = (RadioButton) assetToggleGroup.getSelectedToggle();
        if (selectedAsset == null) {
            showMessage("Please select an asset type", true);
            return;
        }
        String assetType = (String) selectedAsset.getUserData();
        
        // Get investment type
        RadioButton selectedInvestment = (RadioButton) investmentToggleGroup.getSelectedToggle();
        if (selectedInvestment == null) {
            showMessage("Please select an investment strategy", true);
            return;
        }
        String investmentType = (String) selectedInvestment.getUserData();
        
        // Get selected industries
        List<String> selectedIndustries = new ArrayList<>();
        for (CheckBox checkBox : industryCheckBoxes) {
            if (checkBox.isSelected()) {
                selectedIndustries.add(checkBox.getText());
            }
        }
        
        if (selectedIndustries.isEmpty()) {
            showMessage("Please select at least one industry", true);
            return;
        }
        
        // Show loading state
        showMessage("Saving preferences...", false);
        
        // Make API call
        new Thread(() -> {
            try {
                // Build JSON manually
                StringBuilder industriesJson = new StringBuilder("[");
                for (int i = 0; i < selectedIndustries.size(); i++) {
                    industriesJson.append("\"").append(selectedIndustries.get(i)).append("\"");
                    if (i < selectedIndustries.size() - 1) {
                        industriesJson.append(",");
                    }
                }
                industriesJson.append("]");
                
                String requestBody = String.format(
                    "{\"preferred_asset_type\":\"%s\",\"investment_type\":\"%s\",\"industries\":%s}",
                    assetType, investmentType, industriesJson.toString()
                );
                
                System.out.println("Sending preferences: " + requestBody);
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_BASE_URL + "/api/user/preferences"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + getAuthToken())
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                Platform.runLater(() -> {
                    if (response.statusCode() == 201) {
                        showMessage("✓ Preferences saved successfully!", false);
                        statusLabel.setTextFill(Color.GREEN);
                        
                        // Navigate to home screen after a short delay
                        new Thread(() -> {
                            try {
                                Thread.sleep(1500);
                                Platform.runLater(() -> showHomeScreen());
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }).start();
                    } else {
                        showMessage("Failed to save preferences: " + response.body(), true);
                    }
                });
                
            } catch (IOException | InterruptedException e) {
                Platform.runLater(() -> {
                    showMessage("Connection error. Please check if backend server is running.", true);
                });
                e.printStackTrace();
            }
        }).start();
    }
    
    private String getAuthToken() {
        return TokenManager.getAuthToken();
    }
    
    private void showMessage(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setTextFill(isError ? Color.RED : Color.GREEN);
        statusLabel.setVisible(true);
    }
    
    private void showHomeScreen() {
        HomeScreen homeScreen = new HomeScreen(primaryStage);
        homeScreen.show();
    }
}