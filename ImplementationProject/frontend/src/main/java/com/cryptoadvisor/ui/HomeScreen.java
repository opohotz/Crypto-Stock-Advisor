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
import com.cryptoadvisor.util.TokenManager;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * home screen
 */
public class HomeScreen {
    private Stage primaryStage;
    private final HttpClient httpClient;
    private static final String API_BASE_URL = "http://localhost:3000";
    private VBox contentContainer;
    
    public HomeScreen(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }
    
    public void show() {
        // scroll pane
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #f5f5f5; -fx-background-color: #f5f5f5;");
        
        // main
        VBox mainContainer = new VBox(20);
        mainContainer.setAlignment(Pos.TOP_CENTER);
        mainContainer.setPadding(new Insets(20));
        mainContainer.setStyle("-fx-background-color: #f5f5f5;");
        
        // header
        HBox headerContainer = new HBox(15);
        headerContainer.setAlignment(Pos.CENTER_LEFT);
        headerContainer.setPadding(new Insets(15, 20, 15, 20));
        headerContainer.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        
        // title
        Text title = new Text("📊 CryptoAdvisor Feed");
        title.setFont(Font.font("System", FontWeight.BOLD, 26));
        title.setFill(Color.web("#1976D2"));
        
        // spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // nav buttons
        Button preferencesButton = createNavButton("⚙️ Settings", "#FF9800");
        preferencesButton.setOnAction(e -> showPreferencesScreen());
        
        Button createForumButton = createNavButton("💬 New Post", "#9C27B0");
        createForumButton.setOnAction(e -> showCreateForumDialog());
        
        Button refreshButton = createNavButton("🔄 Refresh", "#4CAF50");
        refreshButton.setOnAction(e -> loadUnifiedFeed());
        
        Button logoutButton = createNavButton("Logout", "#f44336");
        logoutButton.setOnAction(e -> handleLogout());
        
        headerContainer.getChildren().addAll(title, spacer, preferencesButton, createForumButton, refreshButton, logoutButton);
        
        // content
        contentContainer = new VBox(15);
        contentContainer.setAlignment(Pos.TOP_CENTER);
        contentContainer.setPadding(new Insets(30));
        contentContainer.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        
        // loading
        ProgressIndicator loadingIndicator = new ProgressIndicator();
        loadingIndicator.setPrefSize(50, 50);
        Text loadingText = new Text("Loading your personalized feed...");
        loadingText.setFont(Font.font("System", 14));
        loadingText.setFill(Color.web("#666666"));
        
        contentContainer.getChildren().addAll(loadingIndicator, loadingText);
        
        mainContainer.getChildren().addAll(headerContainer, contentContainer);
        scrollPane.setContent(mainContainer);
        
        // scene
        Scene scene = new Scene(scrollPane, 1000, 750);
        primaryStage.setScene(scene);
        primaryStage.setTitle("CryptoAdvisor - Home");
        primaryStage.setResizable(true);
        primaryStage.show();
        
        // load feed
        loadUnifiedFeed();
    }
    
    private Button createNavButton(String text, String color) {
        Button button = new Button(text);
        button.setStyle(String.format(
            "-fx-background-color: %s; -fx-text-fill: white; -fx-font-weight: bold; " +
            "-fx-font-size: 12; -fx-padding: 8 15 8 15; -fx-background-radius: 5; " +
            "-fx-cursor: hand;", color
        ));
        button.setOnMouseEntered(e -> button.setStyle(button.getStyle() + "-fx-opacity: 0.8;"));
        button.setOnMouseExited(e -> button.setStyle(button.getStyle().replace("-fx-opacity: 0.8;", "")));
        return button;
    }
    
    private void loadUnifiedFeed() {
        contentContainer.getChildren().clear();
        
        // loading
        ProgressIndicator loadingIndicator = new ProgressIndicator();
        loadingIndicator.setPrefSize(50, 50);
        Text loadingText = new Text("Loading your personalized feed...");
        loadingText.setFont(Font.font("System", 14));
        loadingText.setFill(Color.web("#666666"));
        
        contentContainer.getChildren().addAll(loadingIndicator, loadingText);
        
        new Thread(() -> {
            try {
                // fetch recs
                HttpRequest recRequest = HttpRequest.newBuilder()
                        .uri(URI.create(API_BASE_URL + "/api/recommendations"))
                        .header("Authorization", "Bearer " + TokenManager.getAuthToken())
                        .GET()
                        .build();
                
                HttpResponse<String> recResponse = httpClient.send(recRequest, HttpResponse.BodyHandlers.ofString());
                
                // fetch news
                HttpRequest newsRequest = HttpRequest.newBuilder()
                        .uri(URI.create(API_BASE_URL + "/api/news"))
                        .header("Authorization", "Bearer " + TokenManager.getAuthToken())
                        .GET()
                        .build();
                
                HttpResponse<String> newsResponse = httpClient.send(newsRequest, HttpResponse.BodyHandlers.ofString());
                
                // fetch forums
                HttpRequest forumsRequest = HttpRequest.newBuilder()
                        .uri(URI.create(API_BASE_URL + "/api/forums"))
                        .GET()
                        .build();
                
                HttpResponse<String> forumsResponse = httpClient.send(forumsRequest, HttpResponse.BodyHandlers.ofString());
                
                String finalRecResponse = recResponse.body();
                String finalNewsResponse = newsResponse.body();
                String finalForumsResponse = forumsResponse.body();
                
                Platform.runLater(() -> displayUnifiedFeed(finalRecResponse, finalNewsResponse, finalForumsResponse));
                
            } catch (IOException | InterruptedException e) {
                Platform.runLater(() -> showError("Connection error. Please check if backend is running."));
                e.printStackTrace();
            }
        }).start();
    }
    
    private void displayUnifiedFeed(String recommendationsJson, String newsJson, String forumsJson) {
        contentContainer.getChildren().clear();
        
        // header
        Text feedTitle = new Text("🌟 Your Personalized Feed");
        feedTitle.setFont(Font.font("System", FontWeight.BOLD, 24));
        feedTitle.setFill(Color.web("#1976D2"));
        contentContainer.getChildren().add(feedTitle);
        
        VBox feedContainer = new VBox(15);
        feedContainer.setPadding(new Insets(10, 0, 0, 0));
        
        boolean hasContent = false;
        
        // recommendations - separate stocks and crypto
        if (recommendationsJson.contains("\"asset_symbol\"")) {
            // Parse all recommendations first
            java.util.List<RecommendationItem> stockRecs = new java.util.ArrayList<>();
            java.util.List<RecommendationItem> cryptoRecs = new java.util.ArrayList<>();
            
            // Better parsing: find each recommendation object
            String[] recs = recommendationsJson.split("\"asset_symbol\":\"");
            for (int i = 1; i < recs.length; i++) {
                String rec = recs[i];
                try {
                    String symbol = rec.substring(0, rec.indexOf("\""));
                    
                    // Need to look backwards to find asset_type - it comes before asset_symbol in JSON
                    // Get the full recommendation object by looking at previous part + current part
                    String fullRec = (i > 0 ? recs[i-1] : "") + "\"asset_symbol\":\"" + rec;
                    
                    String name = extractValue(rec, "\"asset_name\":\"");
                    
                    // Try multiple strategies to find asset_type
                    String assetType = extractValue(fullRec, "\"asset_type\":\"");
                    if (assetType.equals("N/A") || assetType.isEmpty()) {
                        // Try searching in current rec as well (in case it's after asset_symbol)
                        assetType = extractValue(rec, "\"asset_type\":\"");
                    }
                    if (assetType.equals("N/A") || assetType.isEmpty()) {
                        // Last resort: search in the original JSON around this position
                        int symbolPos = recommendationsJson.indexOf("\"asset_symbol\":\"" + symbol);
                        if (symbolPos > 0) {
                            // Look backwards up to 200 chars to find asset_type
                            int searchStart = Math.max(0, symbolPos - 200);
                            String searchArea = recommendationsJson.substring(searchStart, symbolPos);
                            assetType = extractValue(searchArea, "\"asset_type\":\"");
                        }
                    }
                    
                    // Extract CoinGecko ID for crypto
                    String coingeckoId = extractValue(fullRec, "\"coingecko_id\":\"");
                    if (coingeckoId.equals("N/A") || coingeckoId.isEmpty()) {
                        coingeckoId = extractValue(rec, "\"coingecko_id\":\"");
                    }
                    
                    // Extract prediction message
                    String predictionMessage = extractValue(fullRec, "\"prediction_message\":\"");
                    if (predictionMessage.equals("N/A") || predictionMessage.isEmpty()) {
                        predictionMessage = extractValue(rec, "\"prediction_message\":\"");
                    }
                    
                    // Debug output
                    System.out.println("Parsing recommendation - Symbol: " + symbol + ", AssetType: " + assetType + ", Name: " + name + ", CoinGeckoID: " + coingeckoId);
                    String type = extractValue(rec, "\"recommendation_type\":\"");
                    String reasoning = extractValue(rec, "\"reasoning\":\"");
                    
                    // extract price
                    String price = "N/A";
                    try {
                        int priceIdx = rec.indexOf("\"current_price\":");
                        if (priceIdx != -1) {
                            int start = priceIdx + "\"current_price\":".length();
                            int end = rec.indexOf(",", start);
                            if (end == -1) end = rec.indexOf("}", start);
                            if (end != -1) {
                                String priceStr = rec.substring(start, end).trim();
                                // remove quotes
                                priceStr = priceStr.replace("\"", "");
                                double priceVal = Double.parseDouble(priceStr);
                                price = String.format("%.2f", priceVal);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error parsing price for " + symbol + ": " + e.getMessage());
                    }
                    
                    RecommendationItem item = new RecommendationItem(symbol, name, price, type, reasoning, assetType, coingeckoId, predictionMessage);
                    
                    // Default to stocks if asset_type is not found (backward compatibility)
                    if (assetType.equals("N/A") || assetType.isEmpty()) {
                        System.out.println("Warning: asset_type not found for " + symbol + ", defaulting to stocks");
                        assetType = "stocks";
                    }
                    
                    if ("crypto".equalsIgnoreCase(assetType)) {
                        cryptoRecs.add(item);
                        System.out.println("✓ Added Crypto: " + symbol + " | Price: $" + price + " | Type: " + assetType);
                    } else {
                        stockRecs.add(item);
                        System.out.println("✓ Added Stock: " + symbol + " | Price: $" + price + " | Type: " + assetType);
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing recommendation: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            // Display stocks
            if (!stockRecs.isEmpty()) {
                Text stockHeader = new Text("💼 Recommended Stocks");
                stockHeader.setFont(Font.font("System", FontWeight.BOLD, 18));
                stockHeader.setFill(Color.web("#333333"));
                feedContainer.getChildren().add(stockHeader);
                
                for (RecommendationItem item : stockRecs) {
                    VBox recCard = createRecommendationCard(item.symbol, item.name, item.price, item.type, item.reasoning, "stocks", null, item.predictionMessage);
                    feedContainer.getChildren().add(recCard);
                    hasContent = true;
                }
            }
            
            // Display crypto
            if (!cryptoRecs.isEmpty()) {
                if (!stockRecs.isEmpty()) {
                    Separator sep = new Separator();
                    sep.setPadding(new Insets(10, 0, 10, 0));
                    feedContainer.getChildren().add(sep);
                }
                
                Text cryptoHeader = new Text("🪙 Recommended Cryptocurrencies");
                cryptoHeader.setFont(Font.font("System", FontWeight.BOLD, 18));
                cryptoHeader.setFill(Color.web("#333333"));
                feedContainer.getChildren().add(cryptoHeader);
                
                for (RecommendationItem item : cryptoRecs) {
                    VBox recCard = createRecommendationCard(item.symbol, item.name, item.price, item.type, item.reasoning, "crypto", item.coingeckoId, item.predictionMessage);
                    feedContainer.getChildren().add(recCard);
                    hasContent = true;
                }
            }
        } else {
            VBox emptyRecs = createEmptyStateCard("📊", "No recommendations yet!", 
                "Set your investment preferences to receive personalized recommendations.");
            feedContainer.getChildren().add(emptyRecs);
        }
        
        // separator
        Separator sep1 = new Separator();
        sep1.setPadding(new Insets(10, 0, 10, 0));
        feedContainer.getChildren().add(sep1);
        
        // articles
        Text newsHeader = new Text("📰 Latest Articles");
        newsHeader.setFont(Font.font("System", FontWeight.BOLD, 18));
        newsHeader.setFill(Color.web("#333333"));
        feedContainer.getChildren().add(newsHeader);
        
        if (newsJson.contains("\"title\"")) {
            String[] articles = newsJson.split("\"title\":\"");
            for (int i = 1; i < articles.length; i++) {
                String article = articles[i];
                try {
                    String title = article.substring(0, article.indexOf("\""));
                    String summary = extractValue(article, "\"summary\":\"");
                    String source = extractValue(article, "\"source\":\"");
                    String type = extractValue(article, "\"type\":\"");
                    String url = extractValue(article, "\"url\":\"");
                    
                    VBox articleCard = createArticleCard(title, summary, source, type, url);
                    feedContainer.getChildren().add(articleCard);
                    hasContent = true;
                } catch (Exception e) {
                    System.err.println("Error parsing article: " + e.getMessage());
                }
            }
        } else {
            Text noNews = new Text("No articles available at the moment.");
            noNews.setFont(Font.font("System", 14));
            noNews.setFill(Color.web("#999999"));
            feedContainer.getChildren().add(noNews);
        }
        
        // separator
        Separator sep2 = new Separator();
        sep2.setPadding(new Insets(10, 0, 10, 0));
        feedContainer.getChildren().add(sep2);
        
        // forums
        Text forumsHeader = new Text("💬 Community Forums");
        forumsHeader.setFont(Font.font("System", FontWeight.BOLD, 18));
        forumsHeader.setFill(Color.web("#333333"));
        feedContainer.getChildren().add(forumsHeader);
        
        if (forumsJson.contains("\"forum_id\"")) {
            String[] forums = forumsJson.split("\"title\":\"");
            for (int i = 1; i < forums.length; i++) {
                String forum = forums[i];
                try {
                    String title = forum.substring(0, forum.indexOf("\""));
                    String content = extractValue(forum, "\"content\":\"");
                    String author = extractValue(forum, "\"author_name\":\"");
                    
                    VBox forumCard = createForumCard(title, content, author);
                    feedContainer.getChildren().add(forumCard);
                    hasContent = true;
                } catch (Exception e) {
                    System.err.println("Error parsing forum: " + e.getMessage());
                }
            }
        } else {
            Text noForums = new Text("No forum posts available. Be the first to start a discussion!");
            noForums.setFont(Font.font("System", 14));
            noForums.setFill(Color.web("#999999"));
            feedContainer.getChildren().add(noForums);
        }
        
        contentContainer.getChildren().add(feedContainer);
        
        if (!hasContent) {
            VBox emptyState = createEmptyStateCard("🌟", "Welcome to CryptoAdvisor!", 
                "Set your preferences to get personalized recommendations and explore articles and forums.");
            contentContainer.getChildren().add(emptyState);
        }
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
                end = json.indexOf(",", start);
                if (end == -1) end = json.indexOf("}", start);
            }
            
            if (end == -1) return "N/A";
            return json.substring(start, end).trim();
        } catch (Exception e) {
            return "N/A";
        }
    }
    
    // Helper class for recommendations
    private static class RecommendationItem {
        String symbol, name, price, type, reasoning, assetType, coingeckoId, predictionMessage;
        RecommendationItem(String symbol, String name, String price, String type, String reasoning, String assetType, String coingeckoId, String predictionMessage) {
            this.symbol = symbol;
            this.name = name;
            this.price = price;
            this.type = type;
            this.reasoning = reasoning;
            this.assetType = assetType;
            this.coingeckoId = coingeckoId;
            this.predictionMessage = predictionMessage;
        }
    }
    
    private VBox createRecommendationCard(String symbol, String name, String price, String type, String reasoning, String assetType, String coingeckoId, String predictionMessage) {
        // Different colors for stocks vs crypto
        boolean isCrypto = "crypto".equalsIgnoreCase(assetType);
        String bgColor = isCrypto ? "#FFF3E0" : "#E8F5E9"; // Orange for crypto, green for stocks
        String borderColor = isCrypto ? "#FF9800" : "#4CAF50"; // Orange for crypto, green for stocks
        String tagBgColor = isCrypto ? "#FF9800" : "#4CAF50";
        String tagText = isCrypto ? "🪙 CRYPTO RECOMMENDATION" : "📈 STOCK RECOMMENDATION";
        
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle(String.format("-fx-background-color: %s; -fx-background-radius: 8; -fx-border-color: %s; -fx-border-radius: 8; -fx-border-width: 2; -fx-cursor: hand;", bgColor, borderColor));
        
        // click to view chart - different URLs for stocks vs crypto
        card.setOnMouseClicked(e -> {
            try {
                String url;
                if (isCrypto) {
                    // CoinGecko URL for crypto - use CoinGecko ID if available, otherwise fallback to symbol
                    if (coingeckoId != null && !coingeckoId.equals("N/A") && !coingeckoId.isEmpty()) {
                        url = "https://www.coingecko.com/en/coins/" + coingeckoId.toLowerCase();
                    } else {
                        // Fallback: try to construct from symbol (may not work for all cryptos)
                        url = "https://www.coingecko.com/en/coins/" + symbol.toLowerCase();
                    }
                } else {
                    // Yahoo Finance for stocks
                    url = "https://finance.yahoo.com/quote/" + symbol;
                }
                System.out.println("Opening URL: " + url);
                java.awt.Desktop.getDesktop().browse(new URI(url));
            } catch (Exception ex) {
                System.err.println("Error opening chart: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
        
        // tag
        Label tagLabel = new Label(tagText);
        tagLabel.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: white; -fx-padding: 4 10; -fx-background-radius: 4; -fx-font-size: 10; -fx-font-weight: bold;", tagBgColor));
        
        // header
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(5, 0, 0, 0));
        
        Text symbolText = new Text(symbol);
        symbolText.setFont(Font.font("System", FontWeight.BOLD, 20));
        symbolText.setFill(Color.web("#1976D2"));
        
        Text nameText = new Text(name);
        nameText.setFont(Font.font("System", 14));
        nameText.setFill(Color.web("#666666"));
        
        Region spacer3 = new Region();
        HBox.setHgrow(spacer3, Priority.ALWAYS);
        
        // price top right
        VBox priceBox = new VBox(2);
        priceBox.setAlignment(Pos.CENTER_RIGHT);
        
        Text priceLabel = new Text("Current Price");
        priceLabel.setFont(Font.font("System", 10));
        priceLabel.setFill(Color.web("#888888"));
        
        Text priceText = new Text("$" + price);
        priceText.setFont(Font.font("System", FontWeight.BOLD, 20));
        priceText.setFill(Color.web("#2E7D32"));
        
        priceBox.getChildren().addAll(priceLabel, priceText);
        
        header.getChildren().addAll(symbolText, nameText, spacer3, priceBox);
        
        // type
        Label typeLabel = new Label(type);
        typeLabel.setStyle("-fx-background-color: #BBDEFB; -fx-text-fill: #0D47A1; -fx-padding: 5 10; -fx-background-radius: 12; -fx-font-size: 11; -fx-font-weight: bold;");
        
        // reasoning
        Text reasoningText = new Text(reasoning);
        reasoningText.setFont(Font.font("System", 13));
        reasoningText.setFill(Color.web("#555555"));
        reasoningText.setWrappingWidth(900);
        
        // hint - different for stocks vs crypto
        String hintMessage = isCrypto ? "💡 Click to view on CoinGecko" : "💡 Click to view chart on Yahoo Finance";
        String hintColor = isCrypto ? "#E65100" : "#2E7D32";
        Text hintText = new Text(hintMessage);
        hintText.setFont(Font.font("System", 11));
        hintText.setFill(Color.web(hintColor));
        hintText.setStyle("-fx-font-style: italic;");
        
        card.getChildren().addAll(tagLabel, header, typeLabel, reasoningText, hintText);
        
        return card;
    }
    
    private VBox createArticleCard(String title, String summary, String source, String type, String url) {
        // blue card
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #E3F2FD; -fx-background-radius: 8; -fx-border-color: #2196F3; -fx-border-radius: 8; -fx-border-width: 2;");
        
        // tag
        String tagText = type.equalsIgnoreCase("stocks") ? "📊 STOCKS ARTICLE" : "🪙 CRYPTO ARTICLE";
        String tagColor = type.equalsIgnoreCase("stocks") ? "#2196F3" : "#FF9800";
        
        Label tagLabel = new Label(tagText);
        tagLabel.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: white; -fx-padding: 4 10; -fx-background-radius: 4; -fx-font-size: 10; -fx-font-weight: bold;", tagColor));
        
        // clickable title
        Hyperlink titleLink = new Hyperlink(title);
        titleLink.setFont(Font.font("System", FontWeight.BOLD, 16));
        titleLink.setTextFill(Color.web("#0D47A1"));
        titleLink.setStyle("-fx-border-width: 0; -fx-padding: 0;");
        titleLink.setWrapText(true);
        titleLink.setMaxWidth(900);
        titleLink.setOnAction(e -> {
            try {
                if (url != null && !url.isEmpty()) {
                    java.awt.Desktop.getDesktop().browse(new URI(url));
                }
            } catch (Exception ex) {
                System.err.println("Error opening URL: " + ex.getMessage());
            }
        });
        
        // summary
        Text summaryText = new Text(summary);
        summaryText.setFont(Font.font("System", 13));
        summaryText.setFill(Color.web("#555555"));
        summaryText.setWrappingWidth(900);
        
        // source
        Text sourceText = new Text("Source: " + source);
        sourceText.setFont(Font.font("System", 11));
        sourceText.setFill(Color.web("#888888"));
        sourceText.setStyle("-fx-font-style: italic;");
        
        card.getChildren().addAll(tagLabel, titleLink, summaryText, sourceText);
        
        return card;
    }
    
    private VBox createForumCard(String title, String content, String author) {
        // purple card
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #F3E5F5; -fx-background-radius: 8; -fx-border-color: #9C27B0; -fx-border-radius: 8; -fx-border-width: 2;");
        
        // tag
        Label tagLabel = new Label("💬 FORUM POST");
        tagLabel.setStyle("-fx-background-color: #9C27B0; -fx-text-fill: white; -fx-padding: 4 10; -fx-background-radius: 4; -fx-font-size: 10; -fx-font-weight: bold;");
        
        // title
        Text titleText = new Text(title);
        titleText.setFont(Font.font("System", FontWeight.BOLD, 16));
        titleText.setFill(Color.web("#6A1B9A"));
        titleText.setWrappingWidth(900);
        
        // content
        String displayContent = content.length() > 200 ? content.substring(0, 200) + "..." : content;
        Text contentText = new Text(displayContent);
        contentText.setFont(Font.font("System", 13));
        contentText.setFill(Color.web("#555555"));
        contentText.setWrappingWidth(900);
        
        // author
        Text authorText = new Text("Posted by: " + author);
        authorText.setFont(Font.font("System", 11));
        authorText.setFill(Color.web("#888888"));
        authorText.setStyle("-fx-font-style: italic;");
        
        // button
        Button viewButton = new Button("View Discussion");
        viewButton.setStyle("-fx-background-color: #9C27B0; -fx-text-fill: white; -fx-font-size: 11; -fx-padding: 5 15; -fx-background-radius: 4;");
        viewButton.setOnAction(e -> showForumsScreen());
        
        card.getChildren().addAll(tagLabel, titleText, contentText, authorText, viewButton);
        
        return card;
    }
    
    private VBox createEmptyStateCard(String icon, String message, String hint) {
        // yellow card
        VBox emptyState = new VBox(15);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setPadding(new Insets(30));
        emptyState.setStyle("-fx-background-color: #FFF8E1; -fx-background-radius: 8; -fx-border-color: #FFC107; -fx-border-radius: 8; -fx-border-width: 2;");
        
        Text emptyIcon = new Text(icon);
        emptyIcon.setFont(Font.font(48));
        
        Text emptyMessage = new Text(message);
        emptyMessage.setFont(Font.font("System", FontWeight.BOLD, 16));
        emptyMessage.setFill(Color.web("#666666"));
        
        Text emptyHint = new Text(hint);
        emptyHint.setFont(Font.font("System", 13));
        emptyHint.setFill(Color.web("#888888"));
        emptyHint.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        emptyHint.setWrappingWidth(400);
        
        emptyState.getChildren().addAll(emptyIcon, emptyMessage, emptyHint);
        
        return emptyState;
    }
    
    private void showError(String message) {
        contentContainer.getChildren().clear();
        
        VBox errorBox = new VBox(15);
        errorBox.setAlignment(Pos.CENTER);
        errorBox.setPadding(new Insets(40));
        
        Text errorIcon = new Text("⚠️");
        errorIcon.setFont(Font.font(48));
        
        Text errorText = new Text(message);
        errorText.setFont(Font.font("System", 14));
        errorText.setFill(Color.RED);
        errorText.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        errorText.setWrappingWidth(400);
        
        Button retryButton = new Button("Retry");
        retryButton.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
        retryButton.setOnAction(e -> loadUnifiedFeed());
        
        errorBox.getChildren().addAll(errorIcon, errorText, retryButton);
        contentContainer.getChildren().add(errorBox);
    }
    
    private void handleLogout() {
        // token management
        TokenManager.clearAll();
        
        // login screen
        LoginScreen loginScreen = new LoginScreen(primaryStage);
        loginScreen.show();
    }
    
    private void showPreferencesScreen() {
        PreferencesScreen preferencesScreen = new PreferencesScreen(primaryStage);
        preferencesScreen.show();
    }
    
    private void showCreateForumDialog() {
        // dialog
        Stage dialog = new Stage();
        dialog.initOwner(primaryStage);
        dialog.setTitle("Create Forum Post");
        
        VBox dialogContent = new VBox(15);
        dialogContent.setPadding(new Insets(20));
        dialogContent.setStyle("-fx-background-color: white;");
        
        Text dialogTitle = new Text("💬 Create New Forum Post");
        dialogTitle.setFont(Font.font("System", FontWeight.BOLD, 18));
        dialogTitle.setFill(Color.web("#1976D2"));
        
        // title field
        Label titleLabel = new Label("Title:");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        TextField titleField = new TextField();
        titleField.setPromptText("Enter post title");
        titleField.setPrefHeight(35);
        
        // content field
        Label contentLabel = new Label("Content:");
        contentLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        TextArea contentArea = new TextArea();
        contentArea.setPromptText("Enter post content");
        contentArea.setPrefRowCount(5);
        contentArea.setWrapText(true);
        
        // buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        
        Button submitButton = new Button("Create Post");
        submitButton.setStyle("-fx-background-color: #9C27B0; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
        submitButton.setPrefWidth(120);
        submitButton.setOnAction(e -> {
            String title = titleField.getText().trim();
            String content = contentArea.getText().trim();
            
            if (title.isEmpty() || content.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Missing Fields");
                alert.setHeaderText(null);
                alert.setContentText("Please fill in both title and content");
                alert.showAndWait();
                return;
            }
            
            createForumPost(title, content, dialog);
        });
        
        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle("-fx-background-color: #757575; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
        cancelButton.setPrefWidth(120);
        cancelButton.setOnAction(e -> dialog.close());
        
        buttonBox.getChildren().addAll(submitButton, cancelButton);
        
        dialogContent.getChildren().addAll(dialogTitle, titleLabel, titleField, contentLabel, contentArea, buttonBox);
        
        Scene dialogScene = new Scene(dialogContent, 450, 400);
        dialog.setScene(dialogScene);
        dialog.setResizable(false);
        dialog.show();
    }
    
    private void createForumPost(String title, String content, Stage dialog) {
        new Thread(() -> {
            try {
                String requestBody = String.format(
                    "{\"title\":\"%s\",\"content\":\"%s\"}",
                    title.replace("\"", "\\\""),
                    content.replace("\"", "\\\"").replace("\n", "\\n")
                );
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_BASE_URL + "/api/forums"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + TokenManager.getAuthToken())
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                Platform.runLater(() -> {
                    if (response.statusCode() == 201) {
                        dialog.close();
                        loadUnifiedFeed(); // refresh feed
                        
                        Alert success = new Alert(Alert.AlertType.INFORMATION);
                        success.setTitle("Success");
                        success.setHeaderText(null);
                        success.setContentText("Forum post created successfully!");
                        success.showAndWait();
                    } else {
                        Alert error = new Alert(Alert.AlertType.ERROR);
                        error.setTitle("Error");
                        error.setHeaderText(null);
                        error.setContentText("Failed to create post: " + response.body());
                        error.showAndWait();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    Alert error = new Alert(Alert.AlertType.ERROR);
                    error.setTitle("Error");
                    error.setHeaderText(null);
                    error.setContentText("Connection error: " + e.getMessage());
                    error.showAndWait();
                });
            }
        }).start();
    }
    
    private void showNewsScreen() {
        NewsScreen newsScreen = new NewsScreen(primaryStage);
        newsScreen.show();
    }
    
    private void showForumsScreen() {
        ForumsScreen forumsScreen = new ForumsScreen(primaryStage);
        forumsScreen.show();
    }
}
