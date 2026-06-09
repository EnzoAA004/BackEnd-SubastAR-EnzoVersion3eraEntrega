package com.subastar.subastar.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public void enviarCodigoVerificacion(String email, String codigo) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Código de verificación - Subastar");
        message.setText("Tu código de verificación es: " + codigo);
        mailSender.send(message);
    }

    public void enviarNotificacionAprobacion(String email, String codigo) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Registro aprobado - Subastar");
        message.setText("Tu registro fue aprobado. Tu código es: " + codigo);
        mailSender.send(message);
    }

    public void enviarTokenRecuperacion(String email, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Recuperación de contraseña - Subastar");
        message.setText(
            "Recibimos una solicitud para restablecer tu contraseña.\n\n" +
            "Tu token de recuperación es: " + token + "\n\n" +
            "Este token es válido por 1 hora. Si no solicitaste este cambio, ignorá este mensaje."
        );
        mailSender.send(message);
    }
}