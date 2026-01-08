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
 * news screen
 */
public class NewsScreen {
    private Stage primaryStage;
    private final HttpClient httpClient;
    private static final String API_BASE_URL = "http://localhost:3000";
    
    private VBox newsContainer;
    private Label statusLabel;
    
    public NewsScreen(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }
    
    public void show() {
        // main
        VBox mainContainer = new VBox(20);
        mainContainer.setAlignment(Pos.TOP_CENTER);
        mainContainer.setPadding(new Insets(20));
        mainContainer.setStyle("-fx-background-color: #f5f5f5;");
        
        // header
        HBox headerContainer = new HBox();
        headerContainer.setAlignment(Pos.CENTER_LEFT);
        headerContainer.setSpacing(20);
        headerContainer.setPadding(new Insets(10, 20, 10, 20));
        headerContainer.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        
        Text title = new Text("Personalized News");
        title.setFont(Font.font("System", FontWeight.BOLD, 24));
        title.setFill(Color.web("#1976D2"));
        
        Button refreshButton = new Button("Refresh");
        refreshButton.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
        refreshButton.setOnAction(e -> loadNews());
        
        Button homeButton = new Button("Home");
        homeButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
        homeButton.setOnAction(e -> showHomeScreen());
        
        headerContainer.getChildren().addAll(title, refreshButton, homeButton);
        
        // news container
        newsContainer = new VBox(15);
        newsContainer.setAlignment(Pos.TOP_CENTER);
        newsContainer.setPadding(new Insets(20));
        newsContainer.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        
        // status
        statusLabel = new Label("Loading news...");
        statusLabel.setFont(Font.font("System", 14));
        statusLabel.setTextFill(Color.web("#666666"));
        statusLabel.setAlignment(Pos.CENTER);
        
        newsContainer.getChildren().add(statusLabel);
        
        // add elements
        mainContainer.getChildren().addAll(headerContainer, newsContainer);
        
        // scrollable
        ScrollPane scrollPane = new ScrollPane(mainContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        
        // scene
        Scene scene = new Scene(scrollPane, 1000, 700);
        primaryStage.setScene(scene);
        primaryStage.setTitle("CryptoAdvisor - News");
        primaryStage.setResizable(true);
        primaryStage.show();
        
        // load news
        loadNews();
    }
    
    private void loadNews() {
        statusLabel.setText("Loading news...");
        statusLabel.setTextFill(Color.web("#666666"));
        
        new Thread(() -> {
            try {
                System.out.println("news request");
                System.out.println("token: " + (getAuthToken() != null ? "yes" : "no"));
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_BASE_URL + "/api/news"))
                        .header("Authorization", "Bearer " + getAuthToken())
                        .GET()
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                System.out.println("status: " + response.statusCode());
                System.out.println("body: " + response.body());
                
                Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        try {
                            displayNews(response.body());
                        } catch (Exception e) {
                            System.err.println("parse error");
                            e.printStackTrace();
                            statusLabel.setText("Error parsing news");
                            statusLabel.setTextFill(Color.RED);
                            showSampleNews();
                        }
                    } else if (response.statusCode() == 404) {
                        System.out.println("no prefs");
                        statusLabel.setText("Please set your preferences first to see personalized news");
                        statusLabel.setTextFill(Color.ORANGE);
                        showSampleNews();
                    } else {
                        System.out.println("bad status");
                        statusLabel.setText("Error loading news. Showing sample news.");
                        statusLabel.setTextFill(Color.ORANGE);
                        showSampleNews();
                    }
                });
                
            } catch (IOException | InterruptedException e) {
                System.err.println("connection error");
                e.printStackTrace();
                Platform.runLater(() -> {
                    statusLabel.setText("Connection error. Showing sample news.");
                    statusLabel.setTextFill(Color.ORANGE);
                    showSampleNews();
                });
            }
        }).start();
    }
    
    private void displayNews(String jsonResponse) {
        // clear
        newsContainer.getChildren().clear();
        
        try {
            System.out.println("parsing news");
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonResponse);
            JsonNode newsArray = rootNode.get("news");
            
            if (newsArray == null || !newsArray.isArray() || newsArray.size() == 0) {
                System.out.println("no news");
                statusLabel.setText("No news available at the moment");
                statusLabel.setTextFill(Color.ORANGE);
                return;
            }
            
            System.out.println("found " + newsArray.size());
            
            // header
            Text header = new Text("Latest Market News");
            header.setFont(Font.font("System", FontWeight.BOLD, 18));
            header.setFill(Color.web("#333333"));
            newsContainer.getChildren().add(header);
            
            // parse news
            for (JsonNode newsItem : newsArray) {
                String type = newsItem.has("type") ? newsItem.get("type").asText() : "news";
                String title = newsItem.has("title") ? newsItem.get("title").asText() : "No title";
                String summary = newsItem.has("summary") ? newsItem.get("summary").asText() : "";
                String source = newsItem.has("source") ? newsItem.get("source").asText() : "Unknown";
                
                VBox newsCard = createNewsCard(type, title, summary, source);
                newsContainer.getChildren().add(newsCard);
            }
            
            // hide status
            statusLabel.setVisible(false);
            
        } catch (Exception e) {
            System.err.println("display error");
            e.printStackTrace();
            statusLabel.setText("Error displaying news");
            statusLabel.setTextFill(Color.RED);
        }
    }
    
    
    private void showSampleNews() {
        newsContainer.getChildren().clear();
        
        // sample data
        String[][] sampleNews = {
            {"crypto", "Bitcoin Surges Past $50,000 as Institutional Adoption Grows", "Bitcoin reached new heights as major corporations continue to add BTC to their balance sheets.", "CryptoNews"},
            {"stocks", "Tech Stocks Rally on Strong Earnings Reports", "Major technology companies report better-than-expected quarterly earnings.", "StockNews"},
            {"crypto", "Ethereum 2.0 Upgrade Shows Promising Results", "The latest Ethereum network upgrade is showing significant improvements in transaction speed and costs.", "CryptoNews"},
            {"stocks", "Federal Reserve Signals Potential Rate Changes", "The Fed's latest statements indicate possible adjustments to interest rate policies.", "StockNews"}
        };
        
        Text header = new Text("Latest Market News");
        header.setFont(Font.font("System", FontWeight.BOLD, 18));
        header.setFill(Color.web("#333333"));
        
        newsContainer.getChildren().add(header);
        
        for (String[] news : sampleNews) {
            VBox newsCard = createNewsCard(news[0], news[1], news[2], news[3]);
            newsContainer.getChildren().add(newsCard);
        }
    }
    
    private VBox createNewsCard(String type, String title, String summary, String source) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-border-color: #dee2e6; -fx-border-radius: 8;");
        
        // type badge
        HBox headerRow = new HBox(15);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        
        Label typeBadge = new Label(type.toUpperCase());
        typeBadge.setStyle(getTypeBadgeStyle(type));
        typeBadge.setFont(Font.font("System", FontWeight.BOLD, 10));
        
        headerRow.getChildren().add(typeBadge);
        
        // title
        Text titleText = new Text(title);
        titleText.setFont(Font.font("System", FontWeight.BOLD, 14));
        titleText.setFill(Color.web("#1976D2"));
        titleText.setWrappingWidth(800);
        
        // summary
        Text summaryText = new Text(summary);
        summaryText.setFont(Font.font("System", 12));
        summaryText.setFill(Color.web("#666666"));
        summaryText.setWrappingWidth(800);
        
        // source
        HBox footerRow = new HBox(20);
        footerRow.setAlignment(Pos.CENTER_LEFT);
        
        Text sourceText = new Text("Source: " + source);
        sourceText.setFont(Font.font("System", 11));
        sourceText.setFill(Color.web("#666666"));
        
        footerRow.getChildren().add(sourceText);
        
        card.getChildren().addAll(headerRow, titleText, summaryText, footerRow);
        
        return card;
    }
    
    private String getTypeBadgeStyle(String type) {
        switch (type.toLowerCase()) {
            case "crypto":
                return "-fx-background-color: #f7931a; -fx-text-fill: white; -fx-background-radius: 3; -fx-padding: 2 8;";
            case "stocks":
                return "-fx-background-color: #1976D2; -fx-text-fill: white; -fx-background-radius: 3; -fx-padding: 2 8;";
            default:
                return "-fx-background-color: #6c757d; -fx-text-fill: white; -fx-background-radius: 3; -fx-padding: 2 8;";
        }
    }
    
    private String getAuthToken() {
        return TokenManager.getAuthToken();
    }
    
    private void showHomeScreen() {
        HomeScreen homeScreen = new HomeScreen(primaryStage);
        homeScreen.show();
    }
}
