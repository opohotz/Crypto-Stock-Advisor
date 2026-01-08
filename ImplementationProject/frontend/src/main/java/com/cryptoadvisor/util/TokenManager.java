package com.cryptoadvisor.util;

/**
 * token manager
 */
public class TokenManager {
    private static String authToken = null;
    private static String userId = null;
    private static String userName = null;
    private static String userEmail = null;
    
    public static void setAuthToken(String token) {
        authToken = token;
    }
    
    public static String getAuthToken() {
        return authToken;
    }
    
    public static void setUserId(String id) {
        userId = id;
    }
    
    public static String getUserId() {
        return userId;
    }
    
    public static void setUserName(String name) {
        userName = name;
    }
    
    public static String getUserName() {
        return userName;
    }
    
    public static void setUserEmail(String email) {
        userEmail = email;
    }
    
    public static String getUserEmail() {
        return userEmail;
    }
    
    public static void clearAll() {
        authToken = null;
        userId = null;
        userName = null;
        userEmail = null;
    }
    
    public static boolean isLoggedIn() {
        return authToken != null && !authToken.isEmpty();
    }
}

