package com.remix.authAPI.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendVerificationEmail(String to, String verificationLink) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            
            String htmlMsg = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { 
                            font-family: Arial, sans-serif; 
                            line-height: 1.6; 
                            color: #333; 
                        }
                        .container { 
                            max-width: 600px; 
                            margin: 0 auto; 
                            padding: 20px; 
                        }
                        .button { 
                            background-color: #4CAF50; 
                            color: white !important; 
                            padding: 12px 20px; 
                            text-decoration: none; 
                            border-radius: 4px; 
                            display: inline-block; 
                            margin: 20px 0;
                        }
                        .link {
                            word-break: break-all;
                            color: #4CAF50;
                        }
                    </style>
                </head>
                <body>
                    <div class='container'>
                        <h1>Vérification de votre compte</h1>
                        <p>Merci de vous être inscrit ! Pour activer votre compte, veuillez cliquer sur le bouton ci-dessous :</p>
                        <p><a class='button' href='%s'>Vérifier mon compte</a></p>
                        <p>Si le bouton ne fonctionne pas, vous pouvez copier et coller ce lien dans votre navigateur :</p>
                        <p class='link'>%s</p>
                    </div>
                </body>
                </html>
                """, verificationLink, verificationLink);

            helper.setTo(to);
            helper.setSubject("Vérification de votre compte");
            helper.setText(htmlMsg, true); // true indique que c'est du HTML
            
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            System.err.println("Erreur lors de l'envoi de l'email : " + e.getMessage());
            throw new RuntimeException("Erreur lors de l'envoi de l'email", e);
        }
    }

    public void send2FACode(String to, String code) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            
            String htmlMsg = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { 
                            font-family: Arial, sans-serif; 
                            line-height: 1.6; 
                            color: #333; 
                        }
                        .container { 
                            max-width: 600px; 
                            margin: 0 auto; 
                            padding: 20px; 
                        }
                        .code {
                            font-size: 24px;
                            font-weight: bold;
                            color: #4CAF50;
                            letter-spacing: 2px;
                            margin: 20px 0;
                        }
                    </style>
                </head>
                <body>
                    <div class='container'>
                        <h1>Code de vérification</h1>
                        <p>Voici votre code de vérification à 6 chiffres :</p>
                        <p class='code'>%s</p>
                        <p>Ce code expirera dans 5 minutes.</p>
                        <p>Si vous n'avez pas demandé ce code, ignorez cet email.</p>
                    </div>
                </body>
                </html>
                """, code);

            helper.setTo(to);
            helper.setSubject("Code de vérification pour votre connexion");
            helper.setText(htmlMsg, true);
            
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("Erreur lors de l'envoi du code 2FA", e);
        }
    }

    public void sendUnlockEmail(String to, String unlockLink) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            
            String htmlMsg = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { 
                            font-family: Arial, sans-serif; 
                            line-height: 1.6; 
                            color: #333; 
                        }
                        .container { 
                            max-width: 600px; 
                            margin: 0 auto; 
                            padding: 20px; 
                        }
                        .button {
                            background-color: #4CAF50;
                            color: white !important;
                            padding: 12px 20px;
                            text-decoration: none;
                            border-radius: 4px;
                            display: inline-block;
                            margin: 20px 0;
                        }
                    </style>
                </head>
                <body>
                    <div class='container'>
                        <h1>Déverrouillage de votre compte</h1>
                        <p>Votre compte a été temporairement verrouillé suite à plusieurs tentatives de connexion échouées.</p>
                        <p>Pour déverrouiller votre compte, cliquez sur le bouton ci-dessous :</p>
                        <p><a class='button' href='%s'>Déverrouiller mon compte</a></p>
                    </div>
                </body>
                </html>
                """, unlockLink);

            helper.setTo(to);
            helper.setSubject("Déverrouillage de votre compte");
            helper.setText(htmlMsg, true);
            
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("Erreur lors de l'envoi de l'email de déverrouillage", e);
        }
    }
} 