// backend
require('dotenv').config();
const express = require('express');
const cors = require('cors');
const jwt = require('jsonwebtoken');
const { v4: uuidv4 } = require('uuid');
const axios = require('axios');

// db setup
let db = null;
const dbType = 'MySQL';

if (process.env.DB_HOST && process.env.DB_USER && process.env.DB_PASSWORD && process.env.DB_NAME) {
    const mysql = require('mysql2/promise');
    
    const dbConfig = {
        host: process.env.DB_HOST,
        port: parseInt(process.env.DB_PORT) || 3306,
        database: 'crypto',
        user: process.env.DB_USER,
        password: process.env.DB_PASSWORD,
        connectTimeout: 10000,
        acquireTimeout: 10000,
        timeout: 10000,
    };
    
    // pool
    db = mysql.createPool(dbConfig);
}

// app
const app = express();
const PORT = process.env.PORT || 3000;

// middleware
app.use(cors());
app.use(express.json());

// auth token
const authenticateToken = (req, res, next) => {
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.split(' ')[1];

    if (!token) {
        return res.status(401).json({ message: 'Access token required' });
    }

    jwt.verify(token, process.env.JWT_SECRET, (err, user) => {
        if (err) {
            return res.status(403).json({ message: 'Invalid or expired token' });
        }
        req.user = user;
        next();
    });
};

// routes
app.get('/', (req, res) => {
    res.json({
        message: 'Hello World from CryptoAdvisor Backend! 🚀',
        status: 'success',
        timestamp: new Date().toISOString()
    });
});

// get prefs
app.get('/api/user/preferences', authenticateToken, async (req, res) => {
    try {
        const [rows] = await db.execute(
            'SELECT * FROM user_preferences WHERE user_id = ?',
            [req.user.user_id]
        );
        
        if (rows.length === 0) {
            return res.status(404).json({ message: 'Preferences not found. Please set your preferences.' });
        }
        
        // parse industries and cryptocurrencies
        const prefs = rows[0];
        if (prefs.industries && typeof prefs.industries === 'string') {
            try {
                prefs.industries = JSON.parse(prefs.industries);
            } catch (e) {
                prefs.industries = [];
            }
        }
        
        if (prefs.cryptocurrencies && typeof prefs.cryptocurrencies === 'string') {
            try {
                prefs.cryptocurrencies = JSON.parse(prefs.cryptocurrencies);
            } catch (e) {
                prefs.cryptocurrencies = [];
            }
        }
        
        res.json({ preferences: prefs });
    } catch (error) {
        console.error('Error fetching user preferences:', error);
        res.status(500).json({ message: 'Internal server error' });
    }
});

// save prefs
app.post('/api/user/preferences', authenticateToken, async (req, res) => {
    try {
        const { preferred_asset_type, investment_type, industries, cryptocurrencies } = req.body;
        
        console.log('save prefs:', req.user.user_id, preferred_asset_type, investment_type, industries, cryptocurrencies);
        
        // validate
        if (!preferred_asset_type || !['crypto', 'stocks', 'both'].includes(preferred_asset_type)) {
            console.log('invalid type');
            return res.status(400).json({ message: 'Preferred asset type is required and must be crypto, stocks, or both' });
        }
        
        // validate type
        if (investment_type && !['Day Trade', 'Long-Term'].includes(investment_type)) {
            return res.status(400).json({ message: 'Investment type must be Day Trade or Long-Term' });
        }
        
        // validate industries
        if (industries && (!Array.isArray(industries) || industries.length > 3)) {
            return res.status(400).json({ message: 'Industries must be an array with max 3 items' });
        }
        
        // validate cryptocurrencies
        if (cryptocurrencies && (!Array.isArray(cryptocurrencies) || cryptocurrencies.length > 5)) {
            return res.status(400).json({ message: 'Cryptocurrencies must be an array with max 5 items' });
        }
        
        const preference_id = uuidv4();
        const industriesJson = industries ? JSON.stringify(industries) : null;
        const cryptocurrenciesJson = cryptocurrencies ? JSON.stringify(cryptocurrencies) : null;
        
        // check existing
        const [existing] = await db.execute(
            'SELECT preferred_asset_type, investment_type, industries, cryptocurrencies FROM user_preferences WHERE user_id = ?',
            [req.user.user_id]
        );
        
        let preferencesChanged = false;
        let assetTypeChanged = false;
        
        if (existing.length > 0) {
            const oldPrefs = existing[0];
            
            // Check if preferences have changed
            const oldAssetType = oldPrefs.preferred_asset_type;
            const oldIndustries = oldPrefs.industries ? (typeof oldPrefs.industries === 'string' ? JSON.parse(oldPrefs.industries) : oldPrefs.industries) : null;
            const oldCryptos = oldPrefs.cryptocurrencies ? (typeof oldPrefs.cryptocurrencies === 'string' ? JSON.parse(oldPrefs.cryptocurrencies) : oldPrefs.cryptocurrencies) : null;
            
            // Check if asset type changed
            if (oldAssetType !== preferred_asset_type) {
                assetTypeChanged = true;
                preferencesChanged = true;
                console.log(`Asset type changed from ${oldAssetType} to ${preferred_asset_type}`);
            }
            
            // Check if industries changed
            const industriesChanged = JSON.stringify(oldIndustries) !== JSON.stringify(industries);
            if (industriesChanged) {
                preferencesChanged = true;
                console.log('Industries changed');
            }
            
            // Check if cryptos changed
            const cryptosChanged = JSON.stringify(oldCryptos) !== JSON.stringify(cryptocurrencies);
            if (cryptosChanged) {
                preferencesChanged = true;
                console.log('Cryptocurrencies changed');
            }
            
            // Check if investment type changed
            if (oldPrefs.investment_type !== investment_type) {
                preferencesChanged = true;
                console.log('Investment type changed');
            }
            
            // If preferences changed, delete old recommendations
            if (preferencesChanged) {
                console.log('Preferences changed, deleting old recommendations...');
                await db.execute(
                    'DELETE FROM recommendations WHERE user_id = ?',
                    [req.user.user_id]
                );
                console.log('Old recommendations deleted');
            }
            
            // update - check if cryptocurrencies column exists, if not we'll need to add it
            try {
                await db.execute(
                    'UPDATE user_preferences SET preferred_asset_type = ?, investment_type = ?, industries = ?, cryptocurrencies = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?',
                    [preferred_asset_type, investment_type, industriesJson, cryptocurrenciesJson, req.user.user_id]
                );
            } catch (error) {
                // If cryptocurrencies column doesn't exist, update without it
                if (error.message.includes('cryptocurrencies')) {
                    console.log('cryptocurrencies column not found, updating without it');
                    await db.execute(
                        'UPDATE user_preferences SET preferred_asset_type = ?, investment_type = ?, industries = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?',
                        [preferred_asset_type, investment_type, industriesJson, req.user.user_id]
                    );
                } else {
                    throw error;
                }
            }
        } else {
            // create
            console.log('creating prefs');
            try {
                await db.execute(
                    'INSERT INTO user_preferences (preference_id, user_id, preferred_asset_type, investment_type, industries, cryptocurrencies) VALUES (?, ?, ?, ?, ?, ?)',
                    [preference_id, req.user.user_id, preferred_asset_type, investment_type, industriesJson, cryptocurrenciesJson]
                );
            } catch (error) {
                // If cryptocurrencies column doesn't exist, insert without it
                if (error.message.includes('cryptocurrencies')) {
                    console.log('cryptocurrencies column not found, inserting without it');
                    await db.execute(
                        'INSERT INTO user_preferences (preference_id, user_id, preferred_asset_type, investment_type, industries) VALUES (?, ?, ?, ?, ?)',
                        [preference_id, req.user.user_id, preferred_asset_type, investment_type, industriesJson]
                    );
                } else {
                    throw error;
                }
            }
        }
        
        console.log('prefs saved:', req.user.user_id);
        res.status(201).json({ 
            message: 'Preferences saved successfully',
            recommendationsCleared: preferencesChanged
        });
    } catch (error) {
        console.error('prefs error:', error);
        res.status(500).json({ message: 'Internal server error' });
    }
});

// get recs
app.get('/api/recommendations', authenticateToken, async (req, res) => {
    try {
        // check prefs
        const [prefs] = await db.execute(
            'SELECT preferred_asset_type, investment_type, industries, cryptocurrencies FROM user_preferences WHERE user_id = ?',
            [req.user.user_id]
        );
        
        if (prefs.length === 0) {
            return res.json({ 
                recommendations: [], 
                message: 'Please set your investment preferences to receive recommendations.' 
            });
        }
        
        // check existing
        const [rows] = await db.execute(
            `SELECT * FROM recommendations 
             WHERE user_id = ? AND (expires_at IS NULL OR expires_at > NOW())
             ORDER BY confidence_score DESC, created_at DESC
             LIMIT 20`,
            [req.user.user_id]
        );
        
        // generate if none
        if (rows.length === 0) {
            console.log('No recommendations found, generating new ones');
            await generateRecommendations(req.user.user_id, prefs[0]);
            
            // fetch new
            const [newRows] = await db.execute(
                `SELECT * FROM recommendations 
                 WHERE user_id = ? AND (expires_at IS NULL OR expires_at > NOW())
                 ORDER BY confidence_score DESC, created_at DESC
                 LIMIT 20`,
                [req.user.user_id]
            );
            
            // Add CoinGecko IDs for crypto recommendations
            const recommendationsWithIds = newRows.map(rec => {
                if (rec.asset_type === 'crypto') {
                    // Find the CoinGecko ID from the mapping
                    const cryptoId = Object.keys(cryptoMapping).find(id => 
                        cryptoMapping[id].symbol === rec.asset_symbol
                    );
                    if (cryptoId) {
                        rec.coingecko_id = cryptoId;
                    }
                }
                return rec;
            });
            
            return res.json({ recommendations: recommendationsWithIds });
        }
        
        // Add CoinGecko IDs for existing crypto recommendations
        const recommendationsWithIds = rows.map(rec => {
            if (rec.asset_type === 'crypto') {
                // Find the CoinGecko ID from the mapping
                const cryptoId = Object.keys(cryptoMapping).find(id => 
                    cryptoMapping[id].symbol === rec.asset_symbol
                );
                if (cryptoId) {
                    rec.coingecko_id = cryptoId;
                }
            }
            return rec;
        });
        
        res.json({ recommendations: recommendationsWithIds });
    } catch (error) {
        console.error('Error fetching recommendations:', error);
        res.status(500).json({ message: 'Internal server error' });
    }
});

// create rec
app.post('/api/recommendations', authenticateToken, async (req, res) => {
    try {
        const { asset_type, asset_symbol, asset_name, current_price, recommendation_type, confidence_score, reasoning, news_summary } = req.body;
        
        if (!asset_type || !asset_symbol || !asset_name || !recommendation_type) {
            return res.status(400).json({ message: 'Required fields missing' });
        }
        
        const recommendation_id = uuidv4();
        
        await db.execute(
            `INSERT INTO recommendations (recommendation_id, user_id, asset_type, asset_symbol, asset_name, 
             current_price, recommendation_type, confidence_score, reasoning, news_summary, expires_at) 
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, DATE_ADD(NOW(), INTERVAL 24 HOUR))`,
            [recommendation_id, req.user.user_id, asset_type, asset_symbol, asset_name,
             current_price, recommendation_type, confidence_score, reasoning, news_summary]
        );
        
        res.status(201).json({ message: 'Recommendation created successfully' });
    } catch (error) {
        console.error('Error creating recommendation:', error);
        res.status(500).json({ message: 'Internal server error' });
    }
});

// get forums
app.get('/api/forums', async (req, res) => {
    try {
        console.log('get forums');
        
        const [rows] = await db.execute(`
            SELECT f.*, u.user_name as author_name
            FROM forums f
            JOIN users u ON f.user_id = u.user_id
            ORDER BY f.created_at DESC
            LIMIT 20
        `);
        
        console.log('forums:', rows.length);
        res.json({ forums: rows });
    } catch (error) {
        console.error('forums error:', error);
        res.status(500).json({ message: 'Internal server error' });
    }
});

// new forum
app.post('/api/forums', authenticateToken, async (req, res) => {
    try {
        const { title, content } = req.body;
        
        console.log('create forum:', req.user.user_id);
        
        if (!title || !content) {
            console.log('missing fields');
            return res.status(400).json({ message: 'Title and content are required' });
        }
        
        const forum_id = uuidv4();
        
        await db.execute(
            'INSERT INTO forums (forum_id, user_id, title, content) VALUES (?, ?, ?, ?)',
            [forum_id, req.user.user_id, title, content]
        );
        
        console.log('forum created:', forum_id);
        res.status(201).json({ message: 'Forum post created successfully', forum_id });
    } catch (error) {
        console.error('create error:', error);
        res.status(500).json({ message: 'Internal server error' });
    }
});

// get replies
app.get('/api/forums/:forumId/replies', async (req, res) => {
    try {
        const { forumId } = req.params;
        
        console.log('get replies:', forumId);
        
        const [rows] = await db.execute(
            `SELECT fr.*, u.user_name as author_name 
             FROM forum_replies fr 
             JOIN users u ON fr.user_id = u.user_id 
             WHERE fr.forum_id = ? 
             ORDER BY fr.created_at ASC`,
            [forumId]
        );
        
        console.log('replies:', rows.length);
        res.json({ replies: rows });
    } catch (error) {
        console.error('replies error:', error);
        res.status(500).json({ message: 'Internal server error' });
    }
});

// new reply
app.post('/api/forums/:forumId/replies', authenticateToken, async (req, res) => {
    try {
        const { forumId } = req.params;
        const { content } = req.body;
        
        console.log('create reply:', forumId, req.user.user_id);
        
        if (!content) {
            console.log('no content');
            return res.status(400).json({ message: 'Content is required' });
        }
        
        const reply_id = uuidv4();
        
        await db.execute(
            'INSERT INTO forum_replies (reply_id, forum_id, user_id, content) VALUES (?, ?, ?, ?)',
            [reply_id, forumId, req.user.user_id, content]
        );
        
        console.log('reply created:', reply_id);
        res.status(201).json({ message: 'Reply created successfully', reply_id });
    } catch (error) {
        console.error('reply error:', error);
        res.status(500).json({ message: 'Internal server error' });
    }
});

// news
app.get('/api/news', authenticateToken, async (req, res) => {
    try {
        console.log('news request:', req.user.user_id);
        
        // get prefs
        const [prefs] = await db.execute(
            'SELECT preferred_asset_type FROM user_preferences WHERE user_id = ?',
            [req.user.user_id]
        );
        
        console.log('prefs:', prefs);
        
        if (prefs.length === 0) {
            console.log('no prefs');
            return res.status(404).json({ message: 'Please set your preferences first' });
        }
        
        const assetType = prefs[0].preferred_asset_type;
        console.log('type:', assetType);
        const news = [];
        
        // fetch
        if (assetType === 'crypto' || assetType === 'both') {
            console.log('fetching crypto');
            try {
                const cryptoNews = await fetchCryptoNews();
                console.log(`crypto: ${cryptoNews.length}`);
                news.push(...cryptoNews.map(item => ({ ...item, type: 'crypto' })));
            } catch (error) {
                console.error('crypto error:', error);
            }
        }
        
        if (assetType === 'stocks' || assetType === 'both') {
            console.log('fetching stocks');
            try {
                const stockNews = await fetchStockNews();
                console.log(`stocks: ${stockNews.length}`);
                news.push(...stockNews.map(item => ({ ...item, type: 'stocks' })));
            } catch (error) {
                console.error('stock error:', error);
            }
        }
        
        // sort by date
        news.sort((a, b) => new Date(b.date) - new Date(a.date));
        
        const timestamp = new Date().toLocaleString();
        console.log(`sending ${news.length} at ${timestamp}`);
        res.json({ news: news.slice(0, 20), assetType, lastUpdated: timestamp });
    } catch (error) {
        console.error('news error:', error);
        res.status(500).json({ message: 'Internal server error' });
    }
});

// price cache
const priceCache = {};
const CACHE_DURATION = 5 * 60 * 1000;

// fetch price
async function fetchStockPrice(symbol) {
    // check cache
    if (priceCache[symbol] && Date.now() - priceCache[symbol].timestamp < CACHE_DURATION) {
        console.log(`using cached price for ${symbol}`);
        return priceCache[symbol].data;
    }
    
    const apiKey = process.env.STOCK_KEY;
    if (!apiKey) {
        console.log('no stock api key');
        return null;
    }
    
    try {
        console.log(`fetching price for ${symbol}`);
        const url = `https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=${symbol}&apikey=${apiKey}`;
        const response = await axios.get(url);
        
        if (response.data['Global Quote'] && response.data['Global Quote']['05. price']) {
            const quote = response.data['Global Quote'];
            const data = {
                price: parseFloat(quote['05. price']),
                change: parseFloat(quote['09. change']),
                changePercent: parseFloat(quote['10. change percent'].replace('%', ''))
            };
            
            // cache
            priceCache[symbol] = {
                data: data,
                timestamp: Date.now()
            };
            
            return data;
        }
        
        console.log(`no data for ${symbol}`);
        return null;
    } catch (error) {
        console.error(`error fetching ${symbol}:`, error.message);
        return null;
    }
}

// fetch crypto price using CoinGecko API
async function fetchCryptoPrice(cryptoId) {
    const cacheKey = `crypto_${cryptoId}`;
    
    // check cache
    if (priceCache[cacheKey] && Date.now() - priceCache[cacheKey].timestamp < CACHE_DURATION) {
        console.log(`using cached price for ${cryptoId}`);
        return priceCache[cacheKey].data;
    }
    
    const apiKey = process.env.COINGECKO_API_KEY || 'CG-ZxbuGMEZxjhNQNGb34zVFAit';
    
    try {
        console.log(`fetching crypto price for ${cryptoId}`);
        const url = `https://api.coingecko.com/api/v3/simple/price?vs_currencies=usd&ids=${cryptoId}&x_cg_demo_api_key=${apiKey}`;
        const response = await axios.get(url);
        
        if (response.data && response.data[cryptoId] && response.data[cryptoId].usd) {
            const price = response.data[cryptoId].usd;
            
            // For change calculation, we'll use a placeholder since CoinGecko simple/price doesn't provide 24h change
            // In a real implementation, you might want to use the /coins/markets endpoint for more data
            const data = {
                price: parseFloat(price),
                change: 0, // Placeholder - would need additional API call for 24h change
                changePercent: 0 // Placeholder
            };
            
            // Try to get 24h change from markets endpoint if available
            try {
                const marketsUrl = `https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&ids=${cryptoId}&x_cg_demo_api_key=${apiKey}`;
                const marketsResponse = await axios.get(marketsUrl);
                if (marketsResponse.data && marketsResponse.data[0]) {
                    const marketData = marketsResponse.data[0];
                    data.change = marketData.price_change_24h || 0;
                    data.changePercent = marketData.price_change_percentage_24h || 0;
                }
            } catch (e) {
                console.log(`Could not fetch 24h change for ${cryptoId}, using defaults`);
            }
            
            // cache
            priceCache[cacheKey] = {
                data: data,
                timestamp: Date.now()
            };
            
            return data;
        }
        
        console.log(`no data for ${cryptoId}`);
        return null;
    } catch (error) {
        console.error(`error fetching ${cryptoId}:`, error.message);
        return null;
    }
}

// crypto mapping (CoinGecko IDs to display names) - defined globally for use in routes
const cryptoMapping = {
    'bitcoin': { name: 'Bitcoin', symbol: 'BTC' },
    'ethereum': { name: 'Ethereum', symbol: 'ETH' },
    'binancecoin': { name: 'BNB', symbol: 'BNB' },
    'ripple': { name: 'XRP', symbol: 'XRP' },
    'cardano': { name: 'Cardano', symbol: 'ADA' },
    'dogecoin': { name: 'Dogecoin', symbol: 'DOGE' },
    'solana': { name: 'Solana', symbol: 'SOL' },
    'matic-network': { name: 'Polygon', symbol: 'MATIC' },
    'polkadot': { name: 'Polkadot', symbol: 'DOT' },
    'avalanche-2': { name: 'Avalanche', symbol: 'AVAX' },
    'litecoin': { name: 'Litecoin', symbol: 'LTC' },
    'chainlink': { name: 'Chainlink', symbol: 'LINK' },
    'uniswap': { name: 'Uniswap', symbol: 'UNI' },
    'tether': { name: 'Tether', symbol: 'USDT' },
    'usd-coin': { name: 'USD Coin', symbol: 'USDC' }
};

// generate recs
async function generateRecommendations(userId, preferences) {
    console.log('Generating recommendations for user:', userId);
    
    const { preferred_asset_type, investment_type, industries, cryptocurrencies } = preferences;
    
    // parse
    let industriesList = [];
    if (industries) {
        try {
            industriesList = typeof industries === 'string' ? JSON.parse(industries) : industries;
        } catch (e) {
            console.error('Error parsing industries:', e);
        }
    }
    
    // parse cryptocurrencies
    let cryptocurrenciesList = [];
    if (cryptocurrencies) {
        try {
            cryptocurrenciesList = typeof cryptocurrencies === 'string' ? JSON.parse(cryptocurrencies) : cryptocurrencies;
        } catch (e) {
            console.error('Error parsing cryptocurrencies:', e);
        }
    }
    
    // stocks by industry
    const stocksByIndustry = {
        'Technology': [
            { symbol: 'AAPL', name: 'Apple Inc.', price: 178.50, change: 2.3 },
            { symbol: 'MSFT', name: 'Microsoft Corporation', price: 378.91, change: 1.8 },
            { symbol: 'GOOGL', name: 'Alphabet Inc.', price: 141.80, change: 3.1 },
            { symbol: 'NVDA', name: 'NVIDIA Corporation', price: 495.22, change: 5.2 },
            { symbol: 'META', name: 'Meta Platforms Inc.', price: 338.54, change: 1.5 }
        ],
        'Healthcare': [
            { symbol: 'PFE', name: 'Pfizer Inc.', price: 28.45, change: 0.8 },
            { symbol: 'JNJ', name: 'Johnson & Johnson', price: 156.32, change: 1.2 },
            { symbol: 'UNH', name: 'UnitedHealth Group', price: 524.18, change: 2.1 },
            { symbol: 'ABBV', name: 'AbbVie Inc.', price: 168.90, change: 1.4 },
            { symbol: 'MRK', name: 'Merck & Co.', price: 112.43, change: 0.9 }
        ],
        'Energy': [
            { symbol: 'XOM', name: 'ExxonMobil Corp.', price: 112.67, change: 1.9 },
            { symbol: 'CVX', name: 'Chevron Corporation', price: 163.84, change: 2.4 },
            { symbol: 'COP', name: 'ConocoPhillips', price: 118.92, change: 1.7 },
            { symbol: 'SLB', name: 'Schlumberger Limited', price: 54.78, change: 3.2 },
            { symbol: 'EOG', name: 'EOG Resources Inc.', price: 129.45, change: 2.8 }
        ],
        'Finance': [
            { symbol: 'JPM', name: 'JPMorgan Chase & Co.', price: 158.76, change: 1.3 },
            { symbol: 'BAC', name: 'Bank of America Corp.', price: 34.52, change: 0.7 },
            { symbol: 'WFC', name: 'Wells Fargo & Company', price: 52.89, change: 1.1 },
            { symbol: 'GS', name: 'Goldman Sachs Group', price: 384.21, change: 2.2 },
            { symbol: 'MS', name: 'Morgan Stanley', price: 95.34, change: 1.6 }
        ],
        'Consumer': [
            { symbol: 'AMZN', name: 'Amazon.com Inc.', price: 178.35, change: 2.9 },
            { symbol: 'WMT', name: 'Walmart Inc.', price: 166.84, change: 0.6 },
            { symbol: 'HD', name: 'Home Depot Inc.', price: 362.19, change: 1.4 },
            { symbol: 'NKE', name: 'Nike Inc.', price: 93.76, change: 2.1 },
            { symbol: 'SBUX', name: 'Starbucks Corporation', price: 95.43, change: 1.8 }
        ],
        'Automotive': [
            { symbol: 'TSLA', name: 'Tesla Inc.', price: 242.84, change: 4.5 },
            { symbol: 'F', name: 'Ford Motor Company', price: 12.45, change: 2.3 },
            { symbol: 'GM', name: 'General Motors Company', price: 38.67, change: 1.9 },
            { symbol: 'TM', name: 'Toyota Motor Corporation', price: 238.92, change: 1.2 },
            { symbol: 'RIVN', name: 'Rivian Automotive Inc.', price: 18.34, change: 6.7 }
        ]
    };
    
    // default cryptos if none specified
    const defaultCryptos = ['bitcoin', 'ethereum', 'solana'];
    
    const recommendations = [];
    
    // Generate stock recommendations if user prefers stocks or both
    if (preferred_asset_type === 'stocks' || preferred_asset_type === 'both') {
        // fetch prices
        if (industriesList && industriesList.length > 0) {
            for (const industry of industriesList) {
                const stocks = stocksByIndustry[industry] || [];
                
                // pick stocks
                for (const stock of stocks.slice(0, 3)) {
                    // fetch price
                    const realData = await fetchStockPrice(stock.symbol);
                    const currentPrice = realData ? realData.price : stock.price;
                    const changePercent = realData ? realData.changePercent : stock.change;
                    
                    const reasoning = investment_type === 'Day Trade' 
                        ? `${stock.name} shows ${changePercent > 0 ? 'strong' : 'significant'} intraday momentum with ${changePercent.toFixed(2)}% ${changePercent > 0 ? 'gain' : 'change'}. ${changePercent > 2 ? 'Good for day trading opportunities.' : 'Watch for volatility.'}`
                        : `${stock.name} is a solid ${industry} stock with strong fundamentals, suitable for long-term investment. Current price: $${currentPrice.toFixed(2)}.`;
                    
                    recommendations.push({
                        recommendation_id: uuidv4(),
                        user_id: userId,
                        asset_type: 'stocks',
                        asset_symbol: stock.symbol,
                        asset_name: stock.name,
                        current_price: currentPrice,
                        recommendation_type: investment_type || 'Long-Term',
                        confidence_score: 70 + Math.random() * 30,
                        reasoning: reasoning,
                        news_summary: `${stock.name} (${stock.symbol}) is ${changePercent > 0 ? 'up' : 'down'} ${Math.abs(changePercent).toFixed(2)}% today. Analysts remain ${changePercent > 0 ? 'optimistic' : 'cautious'} about ${industry} sector performance.`,
                        expires_at: new Date(Date.now() + 24 * 60 * 60 * 1000)
                    });
                    
                    // delay
                    await new Promise(resolve => setTimeout(resolve, 100));
                }
            }
        } else {
            // fallback
            const generalStocks = [
                { symbol: 'AAPL', name: 'Apple Inc.', price: 178.50, change: 2.3, industry: 'Technology' },
                { symbol: 'MSFT', name: 'Microsoft Corporation', price: 378.91, change: 1.8, industry: 'Technology' },
                { symbol: 'JNJ', name: 'Johnson & Johnson', price: 156.32, change: 1.2, industry: 'Healthcare' }
            ];
            
            for (const stock of generalStocks) {
                const realData = await fetchStockPrice(stock.symbol);
                const currentPrice = realData ? realData.price : stock.price;
                const changePercent = realData ? realData.changePercent : stock.change;
                
                const reasoning = investment_type === 'Day Trade' 
                    ? `${stock.name} shows ${changePercent > 0 ? 'strong' : 'significant'} momentum with ${changePercent.toFixed(2)}% ${changePercent > 0 ? 'gain' : 'change'}.`
                    : `${stock.name} is a blue-chip stock with strong fundamentals, suitable for long-term investment.`;
                
                recommendations.push({
                    recommendation_id: uuidv4(),
                    user_id: userId,
                    asset_type: 'stocks',
                    asset_symbol: stock.symbol,
                    asset_name: stock.name,
                    current_price: currentPrice,
                    recommendation_type: investment_type || 'Long-Term',
                    confidence_score: 75 + Math.random() * 25,
                    reasoning: reasoning,
                    news_summary: `${stock.name} (${stock.symbol}) is ${changePercent > 0 ? 'up' : 'down'} ${Math.abs(changePercent).toFixed(2)}% today.`,
                    expires_at: new Date(Date.now() + 24 * 60 * 60 * 1000)
                });
                
                await new Promise(resolve => setTimeout(resolve, 100));
            }
        }
    }
    
    // Generate crypto recommendations if user prefers crypto or both
    if (preferred_asset_type === 'crypto' || preferred_asset_type === 'both') {
        const cryptosToRecommend = cryptocurrenciesList.length > 0 ? cryptocurrenciesList : defaultCryptos;
        
        for (const cryptoId of cryptosToRecommend.slice(0, 5)) {
            const cryptoInfo = cryptoMapping[cryptoId] || { name: cryptoId.charAt(0).toUpperCase() + cryptoId.slice(1), symbol: cryptoId.toUpperCase() };
            
            // fetch price
            const realData = await fetchCryptoPrice(cryptoId);
            const currentPrice = realData ? realData.price : 0;
            const changePercent = realData ? realData.changePercent : 0;
            
            if (!realData) {
                console.log(`Warning: Could not fetch price for ${cryptoId}, using default price 0`);
            } else {
                console.log(`Fetched price for ${cryptoId}: $${currentPrice}, change: ${changePercent}%`);
            }
            
            const reasoning = investment_type === 'Day Trade' 
                ? `${cryptoInfo.name} shows ${changePercent > 0 ? 'strong' : 'significant'} momentum with ${changePercent.toFixed(2)}% ${changePercent > 0 ? 'gain' : 'change'}. ${changePercent > 2 ? 'Good for day trading opportunities.' : 'Watch for volatility.'}`
                : `${cryptoInfo.name} is a ${cryptoId === 'bitcoin' || cryptoId === 'ethereum' ? 'major' : 'promising'} cryptocurrency with strong fundamentals, suitable for long-term investment. Current price: $${currentPrice.toFixed(2)}.`;
            
            recommendations.push({
                recommendation_id: uuidv4(),
                user_id: userId,
                asset_type: 'crypto',
                asset_symbol: cryptoInfo.symbol,
                asset_name: cryptoInfo.name,
                current_price: currentPrice,
                recommendation_type: investment_type || 'Long-Term',
                confidence_score: 70 + Math.random() * 30,
                reasoning: reasoning,
                news_summary: `${cryptoInfo.name} (${cryptoInfo.symbol}) is ${changePercent > 0 ? 'up' : 'down'} ${Math.abs(changePercent).toFixed(2)}% today. Market sentiment is ${changePercent > 0 ? 'positive' : 'mixed'}.`,
                expires_at: new Date(Date.now() + 24 * 60 * 60 * 1000),
                coingecko_id: cryptoId  // Add CoinGecko ID for URL construction
            });
            
            // delay to avoid rate limiting
            await new Promise(resolve => setTimeout(resolve, 200));
        }
    }
    
    // insert recs
    for (const rec of recommendations) {
        try {
            await db.execute(
                `INSERT INTO recommendations (recommendation_id, user_id, asset_type, asset_symbol, asset_name, 
                 current_price, recommendation_type, confidence_score, reasoning, news_summary, expires_at) 
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
                [rec.recommendation_id, rec.user_id, rec.asset_type, rec.asset_symbol, rec.asset_name,
                 rec.current_price, rec.recommendation_type, rec.confidence_score, rec.reasoning, 
                 rec.news_summary, rec.expires_at]
            );
        } catch (error) {
            console.error('Error inserting recommendation:', error);
        }
    }
    
    console.log(`Generated ${recommendations.length} recommendations for user ${userId}`);
    return recommendations;
}

// refresh
app.post('/api/recommendations/refresh', authenticateToken, async (req, res) => {
    try {
        // get prefs
        const [prefs] = await db.execute(
            'SELECT preferred_asset_type, investment_type, industries, cryptocurrencies FROM user_preferences WHERE user_id = ?',
            [req.user.user_id]
        );
        
        if (prefs.length === 0) {
            return res.status(400).json({ message: 'Please set your preferences first.' });
        }
        
        // delete old
        await db.execute(
            'DELETE FROM recommendations WHERE user_id = ?',
            [req.user.user_id]
        );
        
        // generate new
        await generateRecommendations(req.user.user_id, prefs[0]);
        
        res.json({ message: 'Recommendations refreshed successfully!' });
    } catch (error) {
        console.error('Error refreshing recommendations:', error);
        res.status(500).json({ message: 'Internal server error' });
    }
});

// test price
app.get('/api/stocks/price/:symbol', async (req, res) => {
    try {
        const { symbol } = req.params;
        const data = await fetchStockPrice(symbol.toUpperCase());
        
        if (data) {
            res.json({
                symbol: symbol.toUpperCase(),
                price: data.price,
                change: data.change,
                changePercent: data.changePercent,
                cached: priceCache[symbol.toUpperCase()] ? true : false
            });
        } else {
            res.status(404).json({ message: 'Stock data not found' });
        }
    } catch (error) {
        console.error('Error fetching stock price:', error);
        res.status(500).json({ message: 'Internal server error' });
    }
});

// get crypto price
app.get('/api/crypto/price/:id', async (req, res) => {
    try {
        const { id } = req.params;
        const data = await fetchCryptoPrice(id.toLowerCase());
        
        if (data) {
            res.json({
                id: id.toLowerCase(),
                price: data.price,
                change: data.change,
                changePercent: data.changePercent,
                cached: priceCache[`crypto_${id.toLowerCase()}`] ? true : false
            });
        } else {
            res.status(404).json({ message: 'Crypto data not found' });
        }
    } catch (error) {
        console.error('Error fetching crypto price:', error);
        res.status(500).json({ message: 'Internal server error' });
    }
});

// fetch crypto news
async function fetchCryptoNews() {
    const apiKey = process.env.MARKETAUX_API_KEY || process.env.API_KEY;
    
    console.log('crypto api key:', apiKey ? 'yes' : 'no');
    
    if (!apiKey) {
        console.log('no key for crypto');
        return [
            {
                title: "Bitcoin Market Update",
                summary: "Latest developments in cryptocurrency markets.",
                url: "https://www.coindesk.com",
                source: "CoinDesk",
                date: new Date().toISOString()
            }
        ];
    }
    
    try {
        console.log('calling crypto api');
        const requestUrl = 'https://api.marketaux.com/v1/news/all';
        const params = {
            api_token: apiKey,
            symbols: 'BTC,ETH,BNB,XRP,ADA,DOGE,SOL,MATIC,DOT,AVAX',
            filter_entities: true,
            language: 'en',
            limit: 10
        };
        
        const response = await axios.get(requestUrl, { params });
        
        console.log('crypto api status:', response.status);
        console.log('crypto data count:', response.data?.data?.length);
        
        if (response.data && response.data.data) {
            const news = response.data.data.map(item => ({
                title: item.title,
                summary: item.description || item.snippet || item.title,
                url: item.url,
                source: item.source || 'Crypto News',
                date: item.published_at
            }));
            console.log(`crypto processed: ${news.length}`);
            return news;
        }
        
        console.log('no crypto data');
        return [];
    } catch (error) {
        console.error('crypto api error:', error.message);
        
        return [
            {
                title: "Cryptocurrency Market Update",
                summary: "Latest developments in crypto markets.",
                url: "https://www.coindesk.com",
                source: "CoinDesk",
                date: new Date().toISOString()
            }
        ];
    }
}

// fetch stock news
async function fetchStockNews() {
    const apiKey = process.env.MARKETAUX_API_KEY || process.env.API_KEY;
    
    console.log('api key:', apiKey ? 'yes' : 'no');
    
    if (!apiKey) {
        console.log('no key');
        return [
            {
                title: "Tech Giants Report Record Quarterly Earnings",
                summary: "Major technology companies have announced better-than-expected earnings.",
                url: "https://www.cnbc.com",
                source: "CNBC",
                date: new Date().toISOString()
            }
        ];
    }
    
    try {
        console.log('calling api');
        const requestUrl = 'https://api.marketaux.com/v1/news/all';
        const params = {
            api_token: apiKey,
            symbols: 'AAPL,MSFT,GOOGL,TSLA,AMZN',
            filter_entities: true,
            language: 'en',
            limit: 10
        };
        
        const response = await axios.get(requestUrl, { params });
        
        console.log('api status:', response.status);
        console.log('data count:', response.data?.data?.length);
        
        if (response.data && response.data.data) {
            const news = response.data.data.map(item => ({
                title: item.title,
                summary: item.description || item.snippet || item.title,
                url: item.url,
                source: item.source || 'Stock News',
                date: item.published_at
            }));
            console.log(`processed: ${news.length}`);
            return news;
        }
        
        console.log('no data');
        return [];
    } catch (error) {
        console.error('api error:', error.message);
        
        return [
            {
                title: "Stock Market Update",
                summary: "Latest developments in the stock markets.",
                url: "https://www.cnbc.com",
                source: "CNBC",
                date: new Date().toISOString()
            }
        ];
    }
}


app.get('/api/hello', (req, res) => {
    res.json({
        message: 'Hello from the API!',
        data: {
            project: 'CryptoAdvisor',
            course: 'CS440',
            backend: 'Node.js with Express'
        }
    });
});

app.get('/api/status', (req, res) => {
    res.json({
        status: 'Server is running',
        uptime: process.uptime(),
        port: PORT,
        environment: process.env.NODE_ENV || 'development',
        database: {
            type: dbType || 'Not configured',
            configured: db !== null
        }
    });
});


// db info
app.get('/api/db/info', (req, res) => {
    res.json({
        database: {
            type: dbType || 'Not configured',
            configured: db !== null,
            config: {
                host: process.env.DB_HOST || 'Not set',
                port: process.env.DB_PORT || 'Not set',
                database: process.env.DB_NAME || 'Not set',
                user: process.env.DB_USER || 'Not set',
                password_set: !!process.env.DB_PASSWORD
            }
        }
    });
});

// register
app.post('/api/auth/register', async (req, res) => {
    try {
        const { user_name, user_email, user_password } = req.body;

        // validate fields
        if (!user_name || !user_email || !user_password) {
            return res.status(400).json({ 
                message: 'Username, email, and password are required' 
            });
        }

        // check existing
        const [existingUsers] = await db.execute(
            'SELECT user_id FROM users WHERE user_email = ? OR user_name = ?',
            [user_email, user_name]
        );

        if (existingUsers.length > 0) {
            return res.status(409).json({ 
                message: 'User with this email or username already exists' 
            });
        }

        // generate id
        const user_id = uuidv4();

        // insert user
        await db.execute(
            'INSERT INTO users (user_id, user_name, user_email, user_password) VALUES (?, ?, ?, ?)',
            [user_id, user_name, user_email, user_password]
        );

        // generate token
        const token = jwt.sign(
            { user_id, user_name, user_email },
            process.env.JWT_SECRET,
            { expiresIn: '24h' }
        );

        res.status(201).json({
            message: 'User registered successfully',
            token,
            user: {
                user_id,
                user_name,
                user_email
            }
        });

    } catch (error) {
        console.error('Registration error:', error);
        res.status(500).json({ message: 'Internal server error' });
    }
});

// login
app.post('/api/auth/login', async (req, res) => {
    try {
        const { user_email, user_password } = req.body;

        console.log('login:', user_email);

        // validate
        if (!user_email || !user_password) {
            console.log('missing creds');
            return res.status(400).json({ 
                message: 'Email and password are required' 
            });
        }

        // find user
        const [users] = await db.execute(
            'SELECT user_id, user_name, user_email, user_password FROM users WHERE user_email = ?',
            [user_email]
        );

        if (users.length === 0) {
            console.log('user not found');
            return res.status(401).json({ 
                message: 'Invalid email or password' 
            });
        }

        const user = users[0];

        // check password
        if (user.user_password !== user_password) {
            console.log('invalid password');
            return res.status(401).json({ 
                message: 'Invalid email or password' 
            });
        }

        console.log('login success:', user.user_name, user.user_id);

        // generate token
        const token = jwt.sign(
            { user_id: user.user_id, user_name: user.user_name, user_email: user.user_email },
            process.env.JWT_SECRET,
            { expiresIn: '24h' }
        );

        res.json({
            message: 'Login successful',
            token,
            user: {
                user_id: user.user_id,
                user_name: user.user_name,
                user_email: user.user_email
            }
        });

    } catch (error) {
        console.error('Login error:', error);
        res.status(500).json({ message: 'Internal server error' });
    }
});

// update user
app.put('/api/auth/update', authenticateToken, async (req, res) => {
    try {
        const { user_name, user_email, user_password } = req.body;
        const user_id = req.user.user_id;

        // validate field
        if (!user_name && !user_email && !user_password) {
            return res.status(400).json({ 
                message: 'At least one field (username, email, or password) must be provided' 
            });
        }

        // build update
        const updates = [];
        const values = [];

        if (user_name) {
            updates.push('user_name = ?');
            values.push(user_name);
        }
        if (user_email) {
            updates.push('user_email = ?');
            values.push(user_email);
        }
        if (user_password) {
            updates.push('user_password = ?');
            values.push(user_password);
        }

        values.push(user_id);

        await db.execute(
            `UPDATE users SET ${updates.join(', ')} WHERE user_id = ?`,
            values
        );

        // get updated
        const [updatedUsers] = await db.execute(
            'SELECT user_id, user_name, user_email FROM users WHERE user_id = ?',
            [user_id]
        );

        const updatedUser = updatedUsers[0];

        // new token
        const token = jwt.sign(
            { 
                user_id: updatedUser.user_id, 
                user_name: updatedUser.user_name, 
                user_email: updatedUser.user_email 
            },
            process.env.JWT_SECRET,
            { expiresIn: '24h' }
        );

        res.json({
            message: 'User updated successfully',
            token,
            user: {
                user_id: updatedUser.user_id,
                user_name: updatedUser.user_name,
                user_email: updatedUser.user_email
            }
        });

    } catch (error) {
        console.error('Update error:', error);
        res.status(500).json({ message: 'Internal server error' });
    }
});

// delete user
app.delete('/api/auth/delete', authenticateToken, async (req, res) => {
    try {
        const user_id = req.user.user_id;

        // delete user
        const [result] = await db.execute(
            'DELETE FROM users WHERE user_id = ?',
            [user_id]
        );

        if (result.affectedRows === 0) {
            return res.status(404).json({ 
                message: 'User not found' 
            });
        }

        res.json({
            message: 'User deleted successfully'
        });

    } catch (error) {
        console.error('Delete error:', error);
        res.status(500).json({ message: 'Internal server error' });
    }
});

// get profile
app.get('/api/auth/profile', authenticateToken, async (req, res) => {
    try {
        const user_id = req.user.user_id;

        const [users] = await db.execute(
            'SELECT user_id, user_name, user_email FROM users WHERE user_id = ?',
            [user_id]
        );

        if (users.length === 0) {
            return res.status(404).json({ 
                message: 'User not found' 
            });
        }

        res.json({
            user: users[0]
        });

    } catch (error) {
        console.error('Profile error:', error);
        res.status(500).json({ message: 'Internal server error' });
    }
});

// not found
app.use('*', (req, res) => {
    res.status(404).json({
        message: 'Route not found',
        availableRoutes: [
            'GET /',
            'GET /api/hello',
            'GET /api/status',
            'GET /api/db/info',
            'POST /api/auth/register',
            'POST /api/auth/login',
            'PUT /api/auth/update (Protected)',
            'DELETE /api/auth/delete (Protected)',
            'GET /api/auth/profile (Protected)'
        ]
    });
});

// shutdown handler
process.on('SIGINT', async () => {
    console.log('\n🛑 Shutting down server...');
    if (db) {
        try {
            await db.end();
            console.log('📦 MySQL connection closed');
        } catch (error) {
            console.error('Error closing database connection:', error);
        }
    }
    process.exit(0);
});

// start
app.listen(PORT, () => {
    console.log(`🚀 CryptoAdvisor Backend Server running on port ${PORT}`);
    console.log(`📍 Server URL: http://localhost:${PORT}`);
    console.log(`🗄️  Database: ${dbType || 'Not configured'}`);
    if (db) {
        console.log(`🔗 Database connection: ${process.env.DB_HOST}:${process.env.DB_PORT}/${process.env.DB_NAME}`);
    }
    console.log(`📋 Available routes:`);
    console.log(`   GET /                    - Hello World`);
    console.log(`   GET /api/hello           - API Hello`);
    console.log(`   GET /api/status          - Server Status`);
    console.log(`   GET /api/db/info         - Database Info`);
    console.log(`   POST /api/auth/register  - User Registration`);
    console.log(`   POST /api/auth/login     - User Login`);
    console.log(`   PUT /api/auth/update     - Update User (Protected)`);
    console.log(`   DELETE /api/auth/delete  - Delete User (Protected)`);
    console.log(`   GET /api/auth/profile    - Get User Profile (Protected)`);
});

