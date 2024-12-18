-- Mot de passe en clair : MotDePasseUltraSecurise2024!
INSERT INTO users (
    email, 
    password_hash, 
    is_email_verified, 
    email_verification_token, 
    email_verification_expiry, 
    failed_login_attempts, 
    last_failed_login, 
    account_locked, 
    account_locked_until, 
    created_at, 
    updated_at, 
    first_name, 
    last_name, 
    phone_number, 
    date_of_birth, 
    profile_picture_url, 
    address, 
    gender, 
    status
) 
VALUES (
    'hasinkasina2@gmail.com',
    '$2b$12$eO7.m9p9F1gfPbb95TIRrOOjF.SzNw8AjEK3GzT6Nm7QoU7Fb7bg2', -- Haché avec BCrypt
    TRUE, 
    'token12345',
    '2024-12-31 23:59:59',
    0, 
    NULL, 
    FALSE, 
    NULL, 
    CURRENT_TIMESTAMP, 
    CURRENT_TIMESTAMP, 
    'Jean', 
    'Dupont', 
    '+33612345678', 
    '1990-05-15', 
    'https://gmail.com/profile_pictures/user123.jpg', 
    '123 Rue des Fleurs, Paris, France', 
    'Homme', 
    'connecté'
);

--json
    {
        "email": "hasinkasina2@gmail.com",
        "password_hash": "MotDePasseUltraSecurise2024!", 
        "is_email_verified": true,
        "email_verification_token": "token12345",
        "email_verification_expiry": "2024-12-31T23:59:59",
        "failed_login_attempts": 0,
        "last_failed_login": null,
        "account_locked": false,
        "account_locked_until": null,
        "created_at": "2024-12-18T10:00:00",
        "updated_at": "2024-12-18T10:00:00",
        "first_name": "Jean",
        "last_name": "Dupont",
        "phone_number": "0346819543",
        "date_of_birth": "1990-05-15",
        "profile_picture_url": "https://example.com/profile_pictures/user1.jpg",
        "address": "123 Rue des Fleurs, Paris, France",
        "gender": "Homme",
        "status": "connecté"
    }

    {
        "email": "hasinkasina2@gmail.com",
        "password": "MotDePasseUltraSecurise2024!"
    }

