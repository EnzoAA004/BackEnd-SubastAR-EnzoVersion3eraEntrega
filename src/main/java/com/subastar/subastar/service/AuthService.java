package com.subastar.subastar.service;

import com.subastar.subastar.dto.auth.*;
import com.subastar.subastar.dto.usuario.UsuarioResumen;
import com.subastar.subastar.dto.auth.RecuperarPasswordRequest;
import com.subastar.subastar.dto.auth.ConfirmarPasswordRequest;
import com.subastar.subastar.exception.BadRequestException;
import com.subastar.subastar.exception.ConflictException;
import com.subastar.subastar.exception.GoneException;
import com.subastar.subastar.exception.ResourceNotFoundException;
import com.subastar.subastar.exception.UnauthorizedException;
import com.subastar.subastar.model.*;
import com.subastar.subastar.repository.*;
import com.subastar.subastar.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private final RegistroPendienteRepository registroPendienteRepository;
    private final CredencialRepository credencialRepository;
    private final PersonaRepository personaRepository;
    private final ClienteRepository clienteRepository;
    private final PaisRepository paisRepository;
    private final EmpleadoRepository empleadoRepository;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional
    public void registroStep1(RegistroStep1Request req, MultipartFile dniFrente, MultipartFile dniDorso) {
        if (credencialRepository.existsByEmail(req.getEmail())) {
            throw new ConflictException("El email ya está registrado");
        }
        if (registroPendienteRepository.existsByEmail(req.getEmail())) {
            throw new ConflictException("El email ya está registrado");
        }

        // Buscar número de país por nombre
        Pais pais = paisRepository.findAll().stream()
                .filter(p -> p.getNombre() != null && p.getNombre().equalsIgnoreCase(req.getPaisOrigen())
                          || p.getNombreCorto() != null && p.getNombreCorto().equalsIgnoreCase(req.getPaisOrigen()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("País no encontrado: " + req.getPaisOrigen()));

        RegistroPendiente registro = new RegistroPendiente();
        registro.setEmail(req.getEmail());
        registro.setNombre(req.getNombre());
        registro.setApellido(req.getApellido());
        registro.setDomicilio(req.getDomicilio());
        registro.setPaisNumero(pais.getNumero());
        registro.setEstado("pendiente_revision");

        try {
            if (dniFrente != null && !dniFrente.isEmpty()) registro.setFotoDniFrente(dniFrente.getBytes());
            if (dniDorso != null && !dniDorso.isEmpty()) registro.setFotoDniDorso(dniDorso.getBytes());
        } catch (IOException e) {
            throw new BadRequestException("Error al procesar las imágenes del DNI");
        }

        registroPendienteRepository.save(registro);
    }

    @Transactional
    public String verificarCodigo(VerificarCodigoRequest req) {
        RegistroPendiente registro = registroPendienteRepository
                .findByEmail(req.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Email no encontrado en el sistema"));

        if ("pendiente_revision".equals(registro.getEstado())) {
            throw new com.subastar.subastar.exception.ForbiddenException(
                    "Tu cuenta aún no fue aprobada. Recibirás un email cuando esté lista.");
        }

        if (registro.getCodigoExpiresAt() != null && LocalDateTime.now().isAfter(registro.getCodigoExpiresAt())) {
            throw new GoneException("El código expiró");
        }
        if (!req.getCodigo().equals(registro.getCodigoVerificacion())) {
            throw new BadRequestException("Código inválido o malformado");
        }

        String token = UUID.randomUUID().toString();
        registro.setTokenVerificacion(token);
        registro.setTokenExpiresAt(LocalDateTime.now().plusHours(2));
        registroPendienteRepository.save(registro);
        return token;
    }

    @Transactional
    public LoginResponse completarRegistro(RegistroStep2Request req) {
        RegistroPendiente registro = registroPendienteRepository
                .findByTokenVerificacion(req.getTokenVerificacion())
                .orElseThrow(() -> new ResourceNotFoundException("Token de verificación no encontrado"));

        if (registro.getTokenExpiresAt() != null && LocalDateTime.now().isAfter(registro.getTokenExpiresAt())) {
            throw new BadRequestException("Token de verificación expirado");
        }
        if (!req.getPassword().equals(req.getPasswordConfirmacion())) {
            throw new BadRequestException("Las contraseñas no coinciden o no cumplen requisitos mínimos");
        }

        // Crear Persona
        Persona persona = new Persona();
        persona.setNombre(registro.getNombre() + " " + registro.getApellido());
        persona.setDocumento("PENDIENTE");
        persona.setDireccion(registro.getDomicilio());
        persona.setEstado("activo");
        persona.setFoto(registro.getFotoDniFrente());
        persona = personaRepository.save(persona);

        // Necesitamos un empleado verificador. Usar el primero disponible.
        Empleado verificador = empleadoRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new BadRequestException("No hay empleados verificadores disponibles. Contacte al administrador."));

        // Crear Cliente
        Cliente cliente = new Cliente();
        cliente.setIdentificador(persona.getIdentificador());
        cliente.setAdmitido("si");
        cliente.setCategoria("comun");
        cliente.setVerificadorId(verificador.getIdentificador());
        Pais pais = paisRepository.findById(registro.getPaisNumero()).orElse(null);
        if (pais != null) cliente.setPais(pais);
        clienteRepository.save(cliente);

        // Crear Credencial
        Credencial credencial = new Credencial();
        credencial.setPersonaId(persona.getIdentificador());
        credencial.setEmail(registro.getEmail());
        credencial.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        credencialRepository.save(credencial);

        // Eliminar el registro pendiente
        registroPendienteRepository.delete(registro);

        String token = jwtUtil.generateToken(registro.getEmail(), cliente.getIdentificador(), "comun");
        UsuarioResumen resumen = buildResumen(cliente, registro.getEmail(), persona);
        return new LoginResponse(token, "Bearer", resumen);
    }

    public LoginResponse login(LoginRequest req) {
        registroPendienteRepository.findByEmail(req.getEmail()).ifPresent(registro -> {
            if ("pendiente_revision".equals(registro.getEstado())) {
                throw new ConflictException("pendiente_revision");
            }
            if ("aprobado".equals(registro.getEstado())) {
                throw new ConflictException("pendiente_codigo");
            }
        });

        Credencial credencial = credencialRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Credenciales incorrectas"));

        if (!passwordEncoder.matches(req.getPassword(), credencial.getPasswordHash())) {
            throw new UnauthorizedException("Credenciales incorrectas");
        }

        Cliente cliente = clienteRepository.findById(credencial.getPersonaId())
                .orElseThrow(() -> new UnauthorizedException("Credenciales incorrectas"));

        if ("bloqueado".equals(estadoCliente(cliente))) {
            throw new com.subastar.subastar.exception.ForbiddenException("Cuenta bloqueada (usuario derivado a la justicia)");
        }

        Persona persona = credencial.getPersona();
        String token = jwtUtil.generateToken(req.getEmail(), cliente.getIdentificador(), cliente.getCategoria());
        UsuarioResumen resumen = buildResumen(cliente, req.getEmail(), persona);
        return new LoginResponse(token, "Bearer", resumen);
    }

    @Transactional
    public void logout(String token) {
        if (token != null && token.startsWith("Bearer ")) token = token.substring(7);
        if (token != null && jwtUtil.isTokenValid(token)) {
            TokenBlacklist blacklist = new TokenBlacklist();
            blacklist.setToken(token);
            blacklist.setExpiresAt(jwtUtil.extractExpiration(token));
            tokenBlacklistRepository.save(blacklist);
        }
    }

    @Transactional
    public void aprobarRegistro(Integer id) {
        RegistroPendiente registro = registroPendienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro pendiente no encontrado"));

        if ("aprobado".equals(registro.getEstado())) {
            throw new BadRequestException("Este registro ya fue aprobado");
        }

        String codigo = String.format("%04d", new Random().nextInt(10000));
        registro.setEstado("aprobado");
        registro.setCodigoVerificacion(codigo);
        registro.setCodigoExpiresAt(LocalDateTime.now().plusHours(24));
        registroPendienteRepository.save(registro);

        emailService.enviarNotificacionAprobacion(registro.getEmail(), codigo);
    }

    @Transactional
    public void reenviarCodigo(ReenviarCodigoRequest req) {
        RegistroPendiente registro = registroPendienteRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("No hay ningún registro pendiente para ese email"));

        if ("pendiente_revision".equals(registro.getEstado())) {
            throw new BadRequestException("Tu registro aún está siendo revisado. Te notificaremos por email cuando sea aprobado.");
        }

        if (registro.getCodigoExpiresAt() != null && LocalDateTime.now().isBefore(registro.getCodigoExpiresAt())) {
            throw new BadRequestException("El código anterior todavía es válido");
        }

        String codigo = String.format("%04d", new Random().nextInt(10000));
        registro.setCodigoVerificacion(codigo);
        registro.setCodigoExpiresAt(LocalDateTime.now().plusHours(24));
        registroPendienteRepository.save(registro);

        emailService.enviarNotificacionAprobacion(req.getEmail(), codigo);
    }

    @Transactional
    public void cancelarRegistro(String email) {
        registroPendienteRepository.findByEmail(email)
                .ifPresent(registroPendienteRepository::delete);
    }

    @Transactional
    public void solicitarRecuperacion(RecuperarPasswordRequest req) {
        Credencial credencial = credencialRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        String token = UUID.randomUUID().toString();
        credencial.setResetToken(token);
        credencial.setResetTokenExpiresAt(LocalDateTime.now().plusHours(1));
        credencialRepository.save(credencial);

        emailService.enviarTokenRecuperacion(req.getEmail(), token);
    }

    @Transactional
    public void confirmarRecuperacion(ConfirmarPasswordRequest req) {
        Credencial credencial = credencialRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new BadRequestException("Email o token inválido"));

        if (credencial.getResetToken() == null || !credencial.getResetToken().equals(req.getToken())) {
            throw new BadRequestException("Email o token inválido");
        }
        if (credencial.getResetTokenExpiresAt() == null
                || LocalDateTime.now().isAfter(credencial.getResetTokenExpiresAt())) {
            throw new GoneException("El token de recuperación expiró. Solicitá uno nuevo.");
        }

        credencial.setPasswordHash(passwordEncoder.encode(req.getNuevaPassword()));
        credencial.setResetToken(null);
        credencial.setResetTokenExpiresAt(null);
        credencialRepository.save(credencial);
    }

    private String estadoCliente(Cliente cliente) {
        // Estado simplificado: activo para todos excepto casos específicos
        if (!"si".equals(cliente.getAdmitido())) return "bloqueado";
        return "activo";
    }

    private UsuarioResumen buildResumen(Cliente cliente, String email, Persona persona) {
        UsuarioResumen r = new UsuarioResumen();
        r.setId(cliente.getIdentificador());
        if (persona != null && persona.getNombre() != null) {
            String[] parts = persona.getNombre().split(" ", 2);
            r.setNombre(parts[0]);
            r.setApellido(parts.length > 1 ? parts[1] : "");
        }
        r.setEmail(email);
        r.setCategoria(cliente.getCategoria() != null ? cliente.getCategoria() : "comun");
        r.setEstado(estadoCliente(cliente));
        return r;
    }
}
