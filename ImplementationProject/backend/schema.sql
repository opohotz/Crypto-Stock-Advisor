-- Crypto/Stock Market Advisor - Database Schema
-- Group 2 - Scenario #2 Implementation

-- Create database
CREATE DATABASE IF NOT EXISTS crypto;
USE crypto;

-- Users table
CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(36) PRIMARY KEY,
    user_name VARCHAR(100) NOT NULL UNIQUE,
    user_email VARCHAR(255) NOT NULL UNIQUE,
    user_password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_email (user_email),
    INDEX idx_username (user_name)
);

-- User preferences table with enhanced fields
CREATE TABLE IF NOT EXISTS user_preferences (
    preference_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    preferred_asset_type ENUM('crypto', 'stocks', 'both') NOT NULL,
    investment_type VARCHAR(50),
    industries JSON,
    cryptocurrencies JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id)
);

-- Recommendations table
CREATE TABLE IF NOT EXISTS recommendations (
    recommendation_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    asset_type VARCHAR(20) NOT NULL,
    asset_symbol VARCHAR(20) NOT NULL,
    asset_name VARCHAR(255) NOT NULL,
    current_price DECIMAL(10, 2),
    recommendation_type VARCHAR(50),
    confidence_score DECIMAL(5, 2),
    reasoning TEXT,
    news_summary TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_asset_symbol (asset_symbol),
    INDEX idx_expires_at (expires_at)
);

-- Forums table
CREATE TABLE IF NOT EXISTS forums (
    forum_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
);

-- Forum replies table
CREATE TABLE IF NOT EXISTS forum_replies (
    reply_id VARCHAR(36) PRIMARY KEY,
    forum_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (forum_id) REFERENCES forums(forum_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_forum_id (forum_id),
    INDEX idx_user_id (user_id)
);

-- Sample data for testing (optional)
-- Insert a test user (password: test123)
INSERT IGNORE INTO users (user_id, user_name, user_email, user_password) 
VALUES 
    (UUID(), 'Jane_invests', 'jane@example.com', 'securePassword700'),
    (UUID(), 'test_user', 'test@example.com', 'test123');

-- Note: The application will automatically generate recommendations 
-- when users set their preferences and access the home screen

SELECT 'Database schema created successfully!' as message;

