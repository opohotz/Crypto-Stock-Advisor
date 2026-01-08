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
import javafx.stage.Modality;

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
 * forums screen
 */
public class ForumsScreen {
    private Stage primaryStage;
    private final HttpClient httpClient;
    private static final String API_BASE_URL = "http://localhost:3000";
    
    private VBox forumsContainer;
    
    public ForumsScreen(Stage primaryStage) {
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
        
        Text title = new Text("Forums");
        title.setFont(Font.font("System", FontWeight.BOLD, 24));
        title.setFill(Color.web("#1976D2"));
        
        Button createButton = new Button("New Post");
        createButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
        createButton.setOnAction(e -> showCreateDialog());
        
        Button homeButton = new Button("Home");
        homeButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
        homeButton.setOnAction(e -> showHomeScreen());
        
        headerContainer.getChildren().addAll(title, createButton, homeButton);
        
        // forums container
        forumsContainer = new VBox(15);
        forumsContainer.setAlignment(Pos.TOP_CENTER);
        forumsContainer.setPadding(new Insets(20));
        forumsContainer.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        
        Label loadingLabel = new Label("Loading forums...");
        forumsContainer.getChildren().add(loadingLabel);
        
        // add elements
        mainContainer.getChildren().addAll(headerContainer, forumsContainer);
        
        // scrollable
        ScrollPane scrollPane = new ScrollPane(mainContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        
        // scene
        Scene scene = new Scene(scrollPane, 1000, 700);
        primaryStage.setScene(scene);
        primaryStage.setTitle("CryptoAdvisor - Forums");
        primaryStage.setResizable(true);
        primaryStage.show();
        
        // load
        loadForums();
    }
    
    private void loadForums() {
        new Thread(() -> {
            try {
                System.out.println("loading forums");
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_BASE_URL + "/api/forums"))
                        .GET()
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                System.out.println("forums status: " + response.statusCode());
                
                Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        try {
                            displayForums(response.body());
                        } catch (Exception e) {
                            System.err.println("parse error");
                            e.printStackTrace();
                        }
                    }
                });
                
            } catch (IOException | InterruptedException e) {
                System.err.println("connection error");
                e.printStackTrace();
            }
        }).start();
    }
    
    private void displayForums(String jsonResponse) {
        forumsContainer.getChildren().clear();
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonResponse);
            JsonNode forumsArray = rootNode.get("forums");
            
            if (forumsArray == null || !forumsArray.isArray() || forumsArray.size() == 0) {
                Label noForums = new Label("No forum posts yet. Be the first to create one!");
                forumsContainer.getChildren().add(noForums);
                return;
            }
            
            System.out.println("forums found: " + forumsArray.size());
            
            for (JsonNode forum : forumsArray) {
                String forumId = forum.get("forum_id").asText();
                String forumTitle = forum.get("title").asText();
                String content = forum.get("content").asText();
                String authorName = forum.get("author_name").asText();
                
                VBox forumCard = createForumCard(forumId, forumTitle, content, authorName);
                forumsContainer.getChildren().add(forumCard);
            }
            
        } catch (Exception e) {
            System.err.println("display error");
            e.printStackTrace();
        }
    }
    
    private VBox createForumCard(String forumId, String title, String content, String author) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-border-color: #dee2e6; -fx-border-radius: 8; -fx-cursor: hand;");
        
        // title
        Text titleText = new Text(title);
        titleText.setFont(Font.font("System", FontWeight.BOLD, 16));
        titleText.setFill(Color.web("#1976D2"));
        
        // content preview
        Text contentText = new Text(content.length() > 150 ? content.substring(0, 150) + "..." : content);
        contentText.setFont(Font.font("System", 12));
        contentText.setFill(Color.web("#666666"));
        contentText.setWrappingWidth(900);
        
        // author
        Text authorText = new Text("By: " + author);
        authorText.setFont(Font.font("System", 11));
        authorText.setFill(Color.web("#999999"));
        
        card.getChildren().addAll(titleText, contentText, authorText);
        
        // click to view
        card.setOnMouseClicked(e -> showForumDetails(forumId, title, content, author));
        
        return card;
    }
    
    private void showForumDetails(String forumId, String title, String content, String author) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(primaryStage);
        
        VBox dialogVbox = new VBox(20);
        dialogVbox.setPadding(new Insets(20));
        dialogVbox.setStyle("-fx-background-color: white;");
        
        // title
        Text titleText = new Text(title);
        titleText.setFont(Font.font("System", FontWeight.BOLD, 18));
        titleText.setWrappingWidth(550);
        
        // content
        Text contentText = new Text(content);
        contentText.setFont(Font.font("System", 14));
        contentText.setWrappingWidth(550);
        
        // author
        Text authorText = new Text("By: " + author);
        authorText.setFont(Font.font("System", 12));
        authorText.setFill(Color.web("#666666"));
        
        Separator separator = new Separator();
        
        // replies
        VBox repliesContainer = new VBox(10);
        Label repliesLabel = new Label("Replies:");
        repliesLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        repliesContainer.getChildren().add(repliesLabel);
        
        // new reply
        TextArea replyArea = new TextArea();
        replyArea.setPromptText("Write a reply...");
        replyArea.setPrefRowCount(3);
        
        Button submitButton = new Button("Submit Reply");
        submitButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        submitButton.setOnAction(e -> postReply(forumId, replyArea.getText(), repliesContainer, dialog));
        
        dialogVbox.getChildren().addAll(titleText, contentText, authorText, separator, repliesContainer, replyArea, submitButton);
        
        // load replies
        loadReplies(forumId, repliesContainer);
        
        ScrollPane scrollPane = new ScrollPane(dialogVbox);
        scrollPane.setFitToWidth(true);
        
        Scene dialogScene = new Scene(scrollPane, 600, 500);
        dialog.setScene(dialogScene);
        dialog.setTitle("Forum Post");
        dialog.show();
    }
    
    private void loadReplies(String forumId, VBox container) {
        new Thread(() -> {
            try {
                System.out.println("loading replies");
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_BASE_URL + "/api/forums/" + forumId + "/replies"))
                        .GET()
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        try {
                            //341 logic for forums to display
                            ObjectMapper mapper = new ObjectMapper();
                            JsonNode rootNode = mapper.readTree(response.body());
                            JsonNode repliesArray = rootNode.get("replies");
                            
                            if (repliesArray != null && repliesArray.isArray()) {
                                System.out.println("replies: " + repliesArray.size());
                                
                                for (JsonNode reply : repliesArray) {
                                    String replyContent = reply.get("content").asText();
                                    String replyAuthor = reply.get("author_name").asText();
                                    
                                    VBox replyCard = new VBox(5);
                                    replyCard.setPadding(new Insets(10));
                                    replyCard.setStyle("-fx-background-color: #e9ecef; -fx-background-radius: 5;");
                                    
                                    Text contentText = new Text(replyContent);
                                    contentText.setWrappingWidth(500);
                                    
                                    Text authorText = new Text("- " + replyAuthor);
                                    authorText.setFont(Font.font("System", 10));
                                    authorText.setFill(Color.web("#666666"));
                                    
                                    replyCard.getChildren().addAll(contentText, authorText);
                                    container.getChildren().add(replyCard);
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("replies parse error");
                        }
                    }
                });
                
            } catch (IOException | InterruptedException e) {
                System.err.println("replies load error");
            }
        }).start();
    }
    
    private void postReply(String forumId, String content, VBox repliesContainer, Stage dialog) {
        if (content == null || content.trim().isEmpty()) {
            System.out.println("empty reply");
            return;
        }
        
        new Thread(() -> {
            try {
                System.out.println("posting reply");
                
                String requestBody = String.format("{\"content\":\"%s\"}", content.replace("\"", "\\\""));
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_BASE_URL + "/api/forums/" + forumId + "/replies"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + TokenManager.getAuthToken())
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                Platform.runLater(() -> {
                    if (response.statusCode() == 201) {
                        System.out.println("reply posted");
                        dialog.close();
                        loadForums();
                    }
                });
                
            } catch (IOException | InterruptedException e) {
                System.err.println("post error");
            }
        }).start();
    }
    
    private void showCreateDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(primaryStage);
        
        VBox dialogVbox = new VBox(15);
        dialogVbox.setPadding(new Insets(20));
        
        Label titleLabel = new Label("Title:");
        TextField titleField = new TextField();
        titleField.setPromptText("Enter forum title");
        
        Label contentLabel = new Label("Content:");
        TextArea contentArea = new TextArea();
        contentArea.setPromptText("Enter forum content");
        contentArea.setPrefRowCount(5);
        
        Button submitButton = new Button("Create Post");
        submitButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        submitButton.setOnAction(e -> createForum(titleField.getText(), contentArea.getText(), dialog));
        
        dialogVbox.getChildren().addAll(titleLabel, titleField, contentLabel, contentArea, submitButton);
        
        Scene dialogScene = new Scene(dialogVbox, 400, 350);
        dialog.setScene(dialogScene);
        dialog.setTitle("Create Forum Post");
        dialog.show();
    }
    
    private void createForum(String title, String content, Stage dialog) {
        if (title == null || title.trim().isEmpty() || content == null || content.trim().isEmpty()) {
            System.out.println("empty fields");
            return;
        }
        
        new Thread(() -> {
            try {
                System.out.println("creating forum");
                
                String requestBody = String.format("{\"title\":\"%s\",\"content\":\"%s\"}", 
                    title.replace("\"", "\\\""), content.replace("\"", "\\\""));
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_BASE_URL + "/api/forums"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + TokenManager.getAuthToken())
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                Platform.runLater(() -> {
                    if (response.statusCode() == 201) {
                        System.out.println("forum created");
                        dialog.close();
                        loadForums();
                    }
                });
                
            } catch (IOException | InterruptedException e) {
                System.err.println("create error");
            }
        }).start();
    }
    
    private void showHomeScreen() {
        HomeScreen homeScreen = new HomeScreen(primaryStage);
        homeScreen.show();
    }
}

