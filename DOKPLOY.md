# Deploying to Dokploy

## 1. Create the app

In Dokploy, create a new **Application** and point it to this repository.

- **Build type:** Dockerfile
- **Dockerfile path:** `Dockerfile`
- **Build context:** `.`

---

## 2. Add a PostgreSQL database

Create a new **Database → PostgreSQL** service in Dokploy.
Once created, Dokploy gives you the internal connection string — use it for `DB_URL` below.

---

## 3. Environment variables

Set these in **Application → Environment**:

```env
SERVER_PORT=8080

# Use the internal Dokploy connection string for the DB
DB_URL=jdbc:postgresql://<dokploy-db-host>:5432/pelisplus_db
DB_NAME=pelisplus_db
DB_USERNAME=pelisplus_user
DB_PASSWORD=your_secure_password

# Comma-separated list of allowed frontend origins
CORS_ALLOWED_ORIGINS=https://pelisplus.nexoralabs.com

# Generate a secure Base64 secret (at least 256 bits)
# Run: openssl rand -base64 32
JWT_SECRET=your_base64_secret_here
JWT_EXPIRATION_MINUTES=216000
JWT_ISSUER=pelis-plus

SWAGGER_ENABLED=false
SWAGGER_UI_PATH=/swagger-ui.html

# Confirmation emails (Brevo SMTP)
MAIL_ENABLED=true
MAIL_SMTP_HOST=smtp-relay.brevo.com
MAIL_SMTP_PORT=587
MAIL_SMTP_USERNAME=your_brevo_smtp_login
MAIL_SMTP_KEY=your_brevo_smtp_key
# A verified sender configured in Brevo
MAIL_FROM_ADDRESS=entradas@your-domain.com
# Required. All confirmation emails, including production, are sent here.
MAIL_TEST_RECIPIENT=your-test-recipient@example.com
```

> **Tip:** Keep `SWAGGER_ENABLED=false` in production.
> The backend logs each confirmation-email stage with the order ID. It never logs SMTP credentials or the purchaser's email address.

---

## 4. Port

Set the exposed port to **8080** in **Application → General → Port**.

---

## 5. Deploy

Click **Deploy**. Dokploy will build the image and start the container.
