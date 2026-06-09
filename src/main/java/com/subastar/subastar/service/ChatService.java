package com.subastar.subastar.service;

import com.subastar.subastar.dto.chat.*;
import com.subastar.subastar.exception.BadRequestException;
import com.subastar.subastar.exception.ResourceNotFoundException;
import com.subastar.subastar.model.ChatMensaje;
import com.subastar.subastar.model.Cliente;
import com.subastar.subastar.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatService {

    private static final List<String> TIPOS_NOTIFICACION = List.of("bot", "compra", "multa", "bien", "poliza");
    private static final List<String> TIPOS_RESUMEN = List.of("bot", "compra", "multa", "bien", "poliza", "soporte");

    private final ChatMensajeRepository chatMensajeRepository;
    private final CredencialRepository credencialRepository;
    private final ClienteRepository clienteRepository;
    private final SeguroExtraRepository seguroExtraRepository;

    public List<ConversacionResumen> getConversaciones(String email) {
        Integer clienteId = getClienteId(email);
        List<ConversacionResumen> convs = new ArrayList<>();

        List<ChatMensaje> notificaciones = getNotificacionesEntrantes(clienteId);
        ConversacionResumen notificacionesResumen = new ConversacionResumen();
        notificacionesResumen.setTipo("notificaciones");
        notificacionesResumen.setTitulo("Notificaciones");
        notificacionesResumen.setSubtitulo(notificaciones.isEmpty()
                ? ""
                : resumir(notificaciones.get(0).getContenido()));
        notificacionesResumen.setMensajesNoLeidos(contarNoLeidos(notificaciones));
        convs.add(notificacionesResumen);

        for (String tipo : List.of("soporte", "bot", "poliza")) {
            ConversacionResumen c = new ConversacionResumen();
            c.setTipo(tipo);
            c.setTitulo(switch (tipo) {
                case "soporte" -> "Soporte";
                case "bot" -> "Bot";
                default -> "Póliza de seguro";
            });

            chatMensajeRepository
                    .findTopByClienteIdentificadorAndTipoOrderByIdDesc(clienteId, tipo)
                    .ifPresentOrElse(
                            m -> c.setSubtitulo(resumir(m.getContenido())),
                            () -> c.setSubtitulo("")
                    );

            int noLeidos = chatMensajeRepository
                    .countByClienteIdentificadorAndTipoAndLeidoFalse(clienteId, tipo);
            c.setMensajesNoLeidos(noLeidos);

            if ("poliza".equals(tipo)) {
                List<?> polizas = seguroExtraRepository.findByBeneficiarioId(clienteId);
                c.setPorcentajeCompletitud(polizas.isEmpty() ? 0 : 100);
            }
            convs.add(c);
        }
        return convs;
    }

    @Transactional
    public List<MensajeChatResponse> getMensajes(String email, String tipo) {
        Integer clienteId = getClienteId(email);
        List<ChatMensaje> mensajes = "notificaciones".equals(tipo)
                ? chatMensajeRepository.findByClienteIdentificadorAndTipoInOrderByIdAsc(clienteId, TIPOS_NOTIFICACION)
                    .stream()
                    .filter(this::esEntrante)
                    .toList()
                : chatMensajeRepository.findByClienteIdentificadorAndTipoOrderByIdAsc(clienteId, tipo);

        marcarComoLeidos(mensajes);

        if (mensajes.isEmpty() && "bot".equals(tipo)) {
            Cliente cliente = clienteRepository.findById(clienteId).orElse(null);
            String nombre = (cliente != null && cliente.getPersona() != null)
                    ? cliente.getPersona().getNombre() : "usuario";
            ChatMensaje bienvenida = new ChatMensaje();
            bienvenida.setCliente(cliente);
            bienvenida.setTipo("bot");
            bienvenida.setEmisor("bot");
            bienvenida.setContenido("¡Hola " + nombre + "! Bienvenido a SubastAR. Aquí recibirás notificaciones sobre tus subastas.");
            bienvenida.setLeido(true);
            chatMensajeRepository.save(bienvenida);
            mensajes = List.of(bienvenida);
        }

        return mensajes.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public ChatNotificacionesResumen getResumenNotificaciones(String email) {
        Integer clienteId = getClienteId(email);
        List<ChatMensaje> noLeidos = chatMensajeRepository
                .findByClienteIdentificadorAndTipoInAndLeidoFalse(clienteId, TIPOS_RESUMEN)
                .stream()
                .filter(this::esEntrante)
                .toList();

        Map<String, Integer> porTipo = new LinkedHashMap<>();
        TIPOS_RESUMEN.forEach(tipo -> porTipo.put(tipo, 0));
        noLeidos.forEach(m -> porTipo.computeIfPresent(m.getTipo(), (tipo, total) -> total + 1));

        ChatNotificacionesResumen resumen = new ChatNotificacionesResumen();
        resumen.setTotalNoLeidas(noLeidos.size());
        resumen.setHayNoLeidas(!noLeidos.isEmpty());
        resumen.setPorTipo(porTipo);
        return resumen;
    }

    public List<NotificacionResponse> getNotificaciones(String email) {
        return getNotificacionesEntrantes(getClienteId(email)).stream()
                .map(this::toNotificacionResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void marcarNotificacionesLeidas(String email, MarcarNotificacionesLeidasRequest req) {
        List<String> tipos = req == null || req.getTipos() == null || req.getTipos().isEmpty()
                ? TIPOS_NOTIFICACION
                : normalizarTiposNotificacion(req.getTipos());
        List<ChatMensaje> noLeidos = chatMensajeRepository
                .findByClienteIdentificadorAndTipoInAndLeidoFalse(getClienteId(email), tipos)
                .stream()
                .filter(this::esEntrante)
                .toList();
        marcarComoLeidos(noLeidos);
    }

    public MensajeChatResponse enviarMensaje(String email, String tipo, EnviarMensajeRequest req) {
        if ("notificaciones".equals(tipo)) {
            throw new BadRequestException("No se pueden enviar mensajes a la bandeja de notificaciones");
        }
        if (!List.of("soporte", "bot", "poliza").contains(tipo)) {
            throw new BadRequestException("Tipo de conversacion invalido");
        }
        Cliente cliente = clienteRepository.findById(getClienteId(email))
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
        ChatMensaje mensaje = new ChatMensaje();
        mensaje.setCliente(cliente);
        mensaje.setTipo(tipo);
        mensaje.setEmisor("cliente");
        mensaje.setContenido(req.getContenido().trim());
        mensaje.setLeido(true);
        return toResponse(chatMensajeRepository.save(mensaje));
    }

    private List<ChatMensaje> getNotificacionesEntrantes(Integer clienteId) {
        return chatMensajeRepository
                .findByClienteIdentificadorAndTipoInOrderByIdDesc(clienteId, TIPOS_NOTIFICACION)
                .stream()
                .filter(this::esEntrante)
                .toList();
    }

    private List<String> normalizarTiposNotificacion(List<String> tipos) {
        List<String> normalizados = tipos.stream()
                .filter(tipo -> tipo != null && !tipo.isBlank())
                .map(tipo -> tipo.trim().toLowerCase())
                .distinct()
                .toList();
        if (normalizados.isEmpty()) {
            return TIPOS_NOTIFICACION;
        }
        if (!TIPOS_NOTIFICACION.containsAll(normalizados)) {
            throw new BadRequestException("Los tipos de notificación permitidos son bot, compra, multa, bien y poliza");
        }
        return normalizados;
    }

    private void marcarComoLeidos(List<ChatMensaje> mensajes) {
        List<ChatMensaje> noLeidos = mensajes.stream()
                .filter(m -> !m.isLeido())
                .peek(m -> m.setLeido(true))
                .toList();
        if (!noLeidos.isEmpty()) {
            chatMensajeRepository.saveAll(noLeidos);
        }
    }

    private boolean esEntrante(ChatMensaje mensaje) {
        return !"cliente".equalsIgnoreCase(mensaje.getEmisor());
    }

    private int contarNoLeidos(List<ChatMensaje> mensajes) {
        return (int) mensajes.stream().filter(m -> !m.isLeido()).count();
    }

    private String resumir(String contenido) {
        return contenido.length() > 50 ? contenido.substring(0, 50) + "..." : contenido;
    }

    private NotificacionResponse toNotificacionResponse(ChatMensaje m) {
        NotificacionResponse r = new NotificacionResponse();
        r.setId(m.getId());
        r.setTipo(m.getTipo());
        r.setTitulo(switch (m.getTipo()) {
            case "compra" -> "Compra";
            case "multa" -> "Multa";
            case "bien" -> "Aviso";
            case "poliza" -> "Póliza";
            case "soporte" -> "Soporte";
            default -> "Aviso";
        });
        r.setContenido(m.getContenido());
        r.setTimestamp(m.getTimestampMsg());
        r.setLeido(m.isLeido());
        return r;
    }

    private MensajeChatResponse toResponse(ChatMensaje m) {
        MensajeChatResponse r = new MensajeChatResponse();
        r.setId(m.getId());
        // Normaliza 'sistema' como 'bot' para clientes que solo renderizan bot/cliente.
        String emisor = m.getEmisor();
        r.setEmisor("sistema".equalsIgnoreCase(emisor) ? "bot" : emisor);
        r.setContenido(m.getContenido());
        r.setTimestamp(m.getTimestampMsg());
        r.setLeido(m.isLeido());
        return r;
    }

    private Integer getClienteId(String email) {
        return credencialRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"))
                .getPersonaId();
    }
}
