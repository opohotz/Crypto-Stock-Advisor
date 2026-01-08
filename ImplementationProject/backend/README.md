to run cd into backend
npm install
npm start

curl -X POST http://localhost:3000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "user_name": "johndoe",
    "user_email": "john@example.com",
    "user_password": "password123"
  }'

  curl -X POST http://localhost:3000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "user_email": "john@example.com",
    "user_password": "password123"
  }'

  TOKEN="your-jwt-token-here"

curl -X PUT http://localhost:3000/api/auth/update \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "user_name": "newusername",
    "user_email": "newemail@example.com",
    "user_password": "newpassword123"
  }'

  TOKEN="your-jwt-token-here"

curl -X DELETE http://localhost:3000/api/auth/delete \
  -H "Authorization: Bearer $TOKEN"

  