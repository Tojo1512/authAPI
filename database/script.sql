-- Creation des tables pour le fournisseur d'identité

-- Table des utilisateurs
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    is_email_verified BOOLEAN DEFAULT FALSE,
    email_verification_token VARCHAR(255),
    email_verification_expiry TIMESTAMP,
    failed_login_attempts INTEGER DEFAULT 0,
    last_failed_login TIMESTAMP,
    account_locked BOOLEAN DEFAULT FALSE,
    account_locked_until TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    phone_number VARCHAR(20),
    date_of_birth DATE,
    profile_picture_url VARCHAR(255),
    address VARCHAR(255),
    gender VARCHAR(50),
    status VARCHAR(100) DEFAULT 'déconnecté'
);

CREATE TABLE users2 (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,  -- Stockage du mot de passe sans hachage
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Trigger pour mettre à jour updated_at automatiquement
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Table pour la gestion des sessions
CREATE TABLE sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Table pour l'authentification multifacteur
CREATE TABLE mfa_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(6) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Table des rôles
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- Table de liaison users_roles (relation many-to-many)
CREATE TABLE users_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- Table des permissions
CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table de liaison roles_permissions (relation many-to-many)
CREATE TABLE roles_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);

-- Index pour améliorer les performances
CREATE INDEX idx_sessions_user_id ON sessions(user_id);
CREATE INDEX idx_sessions_token ON sessions(token);
CREATE INDEX idx_mfa_tokens_user_id ON mfa_tokens(user_id);
CREATE INDEX idx_users_email ON users(email);

-- Fonction pour hasher le mot de passe
CREATE EXTENSION IF NOT EXISTS pgcrypto;


-- Supprimer l'ancienne fonction
DROP FUNCTION IF EXISTS verify_login;

-- Créer une nouvelle version simplifiée
CREATE OR REPLACE FUNCTION verify_login(
    p_email VARCHAR,
    p_password VARCHAR
) RETURNS SETOF users AS $$
DECLARE
    v_user users;
BEGIN
    -- Vérifier les identifiants
    SELECT * INTO v_user
    FROM users u
    WHERE u.email = p_email 
    AND u.password_hash = crypt(p_password, u.password_hash);

    IF FOUND THEN
        -- Mettre à jour le statut si connexion réussie
        UPDATE users 
        SET status = 'connecté',
            failed_login_attempts = 0,
            last_failed_login = NULL,
            account_locked = FALSE,
            account_locked_until = NULL
        WHERE users.id = v_user.id;
        
        RETURN NEXT v_user;
    ELSE
        -- Incrémenter les tentatives échouées
        UPDATE users 
        SET failed_login_attempts = COALESCE(failed_login_attempts, 0) + 1,
            last_failed_login = CURRENT_TIMESTAMP,
            account_locked = CASE 
                WHEN COALESCE(failed_login_attempts, 0) >= 2 THEN TRUE 
                ELSE account_locked 
            END,
            account_locked_until = CASE 
                WHEN COALESCE(failed_login_attempts, 0) >= 2 THEN CURRENT_TIMESTAMP + INTERVAL '1 hour'
                ELSE account_locked_until 
            END
        WHERE users.email = p_email;
    END IF;
    
    RETURN;
END;
$$ LANGUAGE plpgsql;