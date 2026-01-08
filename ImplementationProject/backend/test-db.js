require('dotenv').config();
const mysql = require('mysql2/promise');

async function setupDatabase() {
  console.log('🔧 Database Setup Check');
  console.log('=======================');
  
  // Check environment variables
  console.log('\n📋 Environment Variables:');
  console.log('DB_HOST:', process.env.DB_HOST || '❌ NOT SET');
  console.log('DB_PORT:', process.env.DB_PORT || '❌ NOT SET');
  console.log('DB_USER:', process.env.DB_USER || '❌ NOT SET');
  console.log('DB_NAME:', process.env.DB_NAME || '❌ NOT SET');
  console.log('DB_PASSWORD:', process.env.DB_PASSWORD ? '✅ SET' : '❌ NOT SET');
  
  // Check if all required vars are set
  const requiredVars = ['DB_HOST', 'DB_USER', 'DB_PASSWORD', 'DB_NAME'];
  const missingVars = requiredVars.filter(varName => !process.env[varName]);
  
  if (missingVars.length > 0) {
    console.log('\n❌ Missing required environment variables:', missingVars);
    console.log('\n📝 Please create a .env file with the following:');
    console.log('DB_HOST=your-rds-endpoint.amazonaws.com');
    console.log('DB_PORT=3306');
    console.log('DB_USER=admin');
    console.log('DB_PASSWORD=your_password');
    console.log('DB_NAME=your_database_name');
    return;
  }
  
  // Test database connection
  console.log('\n🔌 Testing database connection...');
  let connection = null;
  
  try {
    connection = await mysql.createConnection({
      host: process.env.DB_HOST,
      port: parseInt(process.env.DB_PORT) || 3306,
      user: process.env.DB_USER,
      password: process.env.DB_PASSWORD,
      database: process.env.DB_NAME,
      connectTimeout: 10000
    });
    
    console.log('✅ Database connection successful!');
    
  } catch (error) {
    console.log('❌ Database connection failed:', error.message);
    return;
  } finally {
    if (connection) {
      await connection.end();
    }
  }
}

// Run the test
setupDatabase().catch(console.error);