# AuthAPI - Système d'Authentification Sécurisé

AuthAPI est une API d'authentification robuste construite avec Spring Boot et PostgreSQL. Elle fournit des mécanismes d'authentification sécurisés et efficaces pour vos applications, incluant l'authentification à deux facteurs et la gestion des sessions.

## Table des matières

- [Fonctionnalités](#fonctionnalités)
- [Prérequis](#prérequis)
- [Installation](#installation)
- [Configuration](#configuration)
- [Utilisation](#utilisation)
- [Documentation API](#documentation-api)
- [Sécurité](#sécurité)
- [Contribution](#contribution)
- [Licence](#licence)

## Fonctionnalités

### Authentification
- Inscription avec vérification d'email
- Connexion sécurisée avec 2FA
- Gestion des sessions avec expiration automatique
- Verrouillage de compte après tentatives échouées

### Sécurité
- Hachage des mots de passe avec BCrypt
- Protection contre les attaques par force brute
- Tokens de session sécurisés
- Validation des données entrantes

### Gestion des utilisateurs
- CRUD complet des utilisateurs
- Gestion des rôles et permissions
- Récupération de mot de passe
- Historique des connexions

## Prérequis

- Java 17 ou supérieur
- PostgreSQL 12 ou supérieur
- Maven 3.6 ou supérieur
- Serveur SMTP pour l'envoi d'emails

## Installation

1. **Cloner le dépôt**
```bash
git clone https://github.com/votre-username/authAPI.git
cd authAPI
```

2. **Configurer la base de données**
```sql
-- Exécuter le script SQL
psql -U postgres -f database/script.sql
```

3. **Configurer l'application**
Créer un fichier `application.properties` :
```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/authAPI
spring.datasource.username=votre_username
spring.datasource.password=votre_password

# Email
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=votre_email@gmail.com
spring.mail.password=votre_mot_de_passe

# Application
app.session.timeout-minutes=30
app.verification.token.expiry-hours=24
```

4. **Compiler et lancer**
```bash
mvn clean install
mvn spring-boot:run
```

## Configuration

### Configuration de la sécurité

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    // Voir SecurityConfig.java pour plus de détails
}
```

### Paramètres personnalisables

- `app.session.timeout-minutes`: Durée de validité des sessions
- `app.verification.token.expiry-hours`: Durée de validité des tokens de vérification
- `app.2fa.code.length`: Longueur des codes 2FA
- `app.login.max-attempts`: Nombre maximum de tentatives de connexion

## Utilisation

### Inscription d'un utilisateur

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "utilisateur@example.com",
    "password": "MotDePasse123!"
  }'
```

### Connexion

1. **Initier la connexion**
```bash
curl -X POST http://localhost:8080/api/auth/login/initiate \
  -H "Content-Type: application/json" \
  -d '{
    "email": "utilisateur@example.com",
    "password": "MotDePasse123!"
  }'
```

2. **Valider le code 2FA**
```bash
curl -X POST http://localhost:8080/api/auth/login/verify \
  -H "Content-Type: application/json" \
  -d '{
    "email": "utilisateur@example.com",
    "pin": "123456"
  }'
```

## Documentation API

### Endpoints principaux

#### Authentification
- `POST /api/auth/register` - Inscription
- `POST /api/auth/login/initiate` - Initiation de connexion
- `POST /api/auth/login/verify` - Vérification 2FA
- `GET /api/auth/verify-email` - Vérification d'email

#### Sessions
- `POST /api/sessions/validate` - Validation de session
- `POST /api/sessions/logout` - Déconnexion

#### Utilisateurs
- `GET /api/users` - Liste des utilisateurs
- `PUT /api/users/{id}` - Mise à jour utilisateur
- `DELETE /api/users/{id}` - Suppression utilisateur

## Sécurité

### Bonnes pratiques implémentées

- Hachage des mots de passe avec BCrypt
- Validation des données entrantes
- Protection CSRF désactivée pour API REST
- Rate limiting sur les endpoints sensibles
- Nettoyage automatique des sessions expirées

### Recommandations

1. Utilisez HTTPS en production
2. Configurez des en-têtes de sécurité appropriés
3. Mettez en place une politique de mots de passe forts
4. Activez la journalisation des événements de sécurité

## Contribution

1. Fork le projet
2. Créez votre branche (`git checkout -b feature/AmazingFeature`)
3. Committez vos changements (`git commit -m 'Add some AmazingFeature'`)
4. Push vers la branche (`git push origin feature/AmazingFeature`)
5. Ouvrez une Pull Request

## Licence

Ce projet est sous licence MIT. Voir le fichier `LICENSE` pour plus de détails.