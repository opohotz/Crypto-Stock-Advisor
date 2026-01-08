-- Migration: Add cryptocurrencies column to user_preferences table
-- Run this if your database already exists and doesn't have the cryptocurrencies column

USE crypto;

-- Add cryptocurrencies column
-- Note: If column already exists, this will error - that's okay, just ignore it
ALTER TABLE user_preferences 
ADD COLUMN cryptocurrencies JSON AFTER industries;

SELECT 'Migration completed: cryptocurrencies column added to user_preferences' as message;

