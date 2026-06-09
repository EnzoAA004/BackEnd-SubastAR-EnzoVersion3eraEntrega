package com.subastar.subastar.service;

import com.subastar.subastar.dto.subasta.*;
import com.subastar.subastar.dto.puja.PujaResumen;
import com.subastar.subastar.exception.ResourceNotFoundException;
import com.subastar.subastar.model.*;
import com.subastar.subastar.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class SubastaService {

    private final SubastaRepository subastaRepository;
    private final SubastaExtraRepository subastaExtraRepository;
    private final CatalogoRepository catalogoRepository;
    private final ItemCatalogoRepository itemCatalogoRepository;
    private final PujoRepository pujoRepository;
    private final PujoExtraRepository pujoExtraRepository;
    private final RegistroDeSubastaRepository registroDeSubastaRepository;
    private final ProductoDetalleRepository productoDetalleRepository;
    private final FotoRepository fotoRepository;
    private final CredencialRepository credencialRepository;

    public List<SubastaResumen> listar(String estado, String categoria, String moneda, String busqueda) {
        return subastaRepository.findAll().stream()
                .filter(s -> estado == null || matchEstadoApi(s, estado))
                .filter(s -> categoria == null || categoria.equals(s.getCategoria()))
                .filter(s -> {
                    if (moneda == null) return true;
                    SubastaExtra extra = subastaExtraRepository.findBySubastaId(s.getIdentificador()).orElse(null);
                    return extra != null && moneda.equals(extra.getMoneda());
                })
                .filter(s -> {
                    if (busqueda == null) return true;
                    SubastaExtra extra = subastaExtraRepository.findBySubastaId(s.getIdentificador()).orElse(null);
                    String nombre = extra != null ? extra.getNombre() : "";
                    return nombre != null && nombre.toLowerCase().contains(busqueda.toLowerCase());
                })
                .map(this::toResumen)
                .collect(Collectors.toList());
    }

    public SubastaDetalle getDetalle(Integer id) {
        Subasta s = subastaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subasta no encontrada"));
        SubastaExtra extra = subastaExtraRepository.findBySubastaId(id).orElse(null);

        SubastaDetalle d = new SubastaDetalle();
        fillResumen(d, s, extra);
        d.setHorario(s.getHora() != null ? s.getHora().toString() + " hs" : null);
        d.setUrlStreaming(extra != null ? extra.getUrlStreaming() : null);
        return d;
    }

    public List<ItemCatalogoResponse> getCatalogo(Integer subastaId, boolean autenticado) {
        subastaRepository.findById(subastaId)
                .orElseThrow(() -> new ResourceNotFoundException("Subasta no encontrada"));
        List<Catalogo> catalogos = catalogoRepository.findAllBySubastaIdentificador(subastaId);
        return catalogos.stream()
                .flatMap(c -> itemCatalogoRepository.findByCatalogoIdentificador(c.getIdentificador()).stream())
                .map(item -> toItemResponse(item, autenticado))
                .collect(Collectors.toList());
    }

    public ItemCatalogoResponse getItem(Integer subastaId, Integer itemId, boolean autenticado) {
        subastaRepository.findById(subastaId)
                .orElseThrow(() -> new ResourceNotFoundException("Subasta no encontrada"));
        ItemCatalogo item = itemCatalogoRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Subasta o ítem no encontrado"));
        return toItemResponse(item, autenticado);
    }

    public EstadoEnVivoResponse getEnVivo(Integer subastaId) {
        Subasta s = subastaRepository.findById(subastaId)
                .orElseThrow(() -> new ResourceNotFoundException("Subasta no encontrada"));
        SubastaExtra extra = subastaExtraRepository.findBySubastaId(subastaId).orElse(null);

        EstadoEnVivoResponse resp = new EstadoEnVivoResponse();

        if (extra != null && extra.getItemActualId() != null) {
            ItemCatalogo itemActual = itemCatalogoRepository.findById(extra.getItemActualId()).orElse(null);
            if (itemActual != null) {
                resp.setItemActual(toItemResponse(itemActual, true));

                Pujo mejorPujo = pujoRepository.findTopByItemIdentificadorOrderByImporteDesc(itemActual.getIdentificador()).orElse(null);
                BigDecimal mejorOferta = mejorPujo != null ? mejorPujo.getImporte() : BigDecimal.ZERO;
                resp.setMejorOferta(mejorOferta);

                if (mejorPujo != null) {
                    Persona p = mejorPujo.getAsistente().getCliente().getPersona();
                    resp.setMejorPostor(p != null ? p.getNombre() : "Postor " + mejorPujo.getAsistente().getNumeroPostor());
                }

                BigDecimal precioBase = itemActual.getPrecioBase();
                resp.setPujaMinima(calcularPujaMinima(precioBase, mejorOferta));
                resp.setPujaMaxima(calcularPujaMaxima(precioBase, mejorOferta));

                List<PujaResumen> historial = pujoRepository
                        .findByItemIdentificadorOrderByIdentificadorDesc(itemActual.getIdentificador())
                        .stream().map(this::toPujaResumen).collect(Collectors.toList());
                resp.setHistorialPujas(historial);
            }
        }
        return resp;
    }

    public ResultadoPujaResponse getResultado(Integer subastaId, Integer itemId, String email) {
        subastaRepository.findById(subastaId)
                .orElseThrow(() -> new ResourceNotFoundException("Subasta no encontrada"));
        ItemCatalogo item = itemCatalogoRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item no encontrado"));
        if (!item.getCatalogo().getSubasta().getIdentificador().equals(subastaId)) {
            throw new ResourceNotFoundException("Item no encontrado en la subasta");
        }
        ResultadoPujaResponse response = new ResultadoPujaResponse();
        response.setItemId(itemId);
        response.setNombreItem(item.getProducto().getDescripcionCatalogo());
        if (!"si".equals(item.getSubastado())) {
            response.setEstado("en_curso");
            return response;
        }
        Integer clienteId = credencialRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"))
                .getPersonaId();
        Pujo ganador = pujoRepository.findGanadorByItemId(itemId).orElse(null);
        if (ganador == null) {
            response.setEstado("sin_pujas");
            return response;
        }
        response.setEstado("finalizada");
        response.setMontoFinal(ganador.getImporte());
        response.setFueGanador(ganador.getAsistente().getCliente().getIdentificador().equals(clienteId));
        return response;
    }

    private SubastaResumen toResumen(Subasta s) {
        SubastaResumen r = new SubastaResumen();
        fillResumen(r, s, subastaExtraRepository.findBySubastaId(s.getIdentificador()).orElse(null));
        return r;
    }

    private void fillResumen(SubastaResumen r, Subasta s, SubastaExtra extra) {
        r.setId(s.getIdentificador());
        r.setNombre(extra != null && extra.getNombre() != null ? extra.getNombre() : "Subasta #" + s.getIdentificador());
        r.setDireccion(s.getUbicacion());
        if (s.getFecha() != null && s.getHora() != null) {
            r.setFechaInicio(LocalDateTime.of(s.getFecha(), s.getHora()));
        }
        r.setCategoria(s.getCategoria());
        r.setMoneda(extra != null ? extra.getMoneda() : "ARS");
        r.setEstado(calcEstadoApi(s));

        long totalArticulos = catalogoRepository.findAllBySubastaIdentificador(s.getIdentificador())
                .stream().flatMap(c -> itemCatalogoRepository.findByCatalogoIdentificador(c.getIdentificador()).stream())
                .count();
        r.setTotalArticulos((int) totalArticulos);

        if (s.getSubastador() != null && s.getSubastador().getPersona() != null) {
            r.setRematador(s.getSubastador().getPersona().getNombre());
        }
    }

    private String calcEstadoApi(Subasta s) {
        if (s.getFecha() == null) return "proxima";
        LocalDateTime inicio = LocalDateTime.of(s.getFecha(), s.getHora() != null ? s.getHora() : java.time.LocalTime.MIDNIGHT);
        LocalDateTime ahora = LocalDateTime.now();
        if (ahora.isBefore(inicio)) return "proxima";
        if ("abierta".equals(s.getEstado())) return "en_vivo";
        return "finalizada";
    }

    private boolean matchEstadoApi(Subasta s, String estadoApi) {
        return estadoApi.equals("todas") || estadoApi.equals(calcEstadoApi(s));
    }

    ItemCatalogoResponse toItemResponse(ItemCatalogo item, boolean autenticado) {
        ItemCatalogoResponse r = new ItemCatalogoResponse();
        r.setId(item.getIdentificador());
        r.setNumeroPieza(String.valueOf(item.getIdentificador()));

        Producto prod = item.getProducto();
        ProductoDetalle det = productoDetalleRepository.findById(prod.getIdentificador()).orElse(null);

        r.setNombre(det != null && det.getNombre() != null ? det.getNombre() : prod.getDescripcionCatalogo());
        r.setDescripcion(prod.getDescripcionCatalogo());
        if (autenticado) r.setPrecioBase(item.getPrecioBase());

        r.setEstado(calcEstadoItem(item));

        List<String> imgs = fotoRepository.findByProductoIdentificador(prod.getIdentificador())
                .stream().map(f -> "/api/v1/fotos/" + f.getIdentificador()).collect(Collectors.toList());
        r.setImagenes(imgs);

        if (prod.getDuenio() != null && prod.getDuenio().getPersona() != null) {
            r.setDuenoActual(prod.getDuenio().getPersona().getNombre());
        }
        if (det != null) {
            r.setArtista(det.getArtistaDisenador());
            r.setFechaCreacion(det.getFechaCreacionObra() != null ? det.getFechaCreacionObra().toString() : null);
            r.setHistoria(det.getDatosHistoricos());
        }
        return r;
    }

    private String calcEstadoItem(ItemCatalogo item) {
        boolean vendido = registroDeSubastaRepository.findAll().stream()
                .anyMatch(r -> r.getProducto().getIdentificador().equals(item.getProducto().getIdentificador()));
        if (vendido) return "vendido";
        if ("si".equals(item.getSubastado())) return "sin_puja";
        return "disponible";
    }

    private PujaResumen toPujaResumen(Pujo p) {
        PujaResumen r = new PujaResumen();
        r.setId(p.getIdentificador());
        r.setUsuarioId(p.getAsistente().getCliente().getIdentificador());
        Persona persona = p.getAsistente().getCliente().getPersona();
        r.setNombreUsuario(persona != null ? persona.getNombre() : "Usuario " + r.getUsuarioId());
        r.setMonto(p.getImporte());
        pujoExtraRepository.findByPujoId(p.getIdentificador())
                .ifPresent(pe -> r.setTimestamp(pe.getTimestampPuja()));
        r.setEsGanadora("si".equals(p.getGanador()));
        return r;
    }

    private BigDecimal calcularPujaMinima(BigDecimal precioBase, BigDecimal mejorOferta) {
        if (precioBase == null) return BigDecimal.ZERO;
        if (mejorOferta == null || mejorOferta.compareTo(BigDecimal.ZERO) <= 0) return precioBase;
        return mejorOferta.add(precioBase.multiply(BigDecimal.valueOf(0.01)));
    }

    private BigDecimal calcularPujaMaxima(BigDecimal precioBase, BigDecimal mejorOferta) {
        if (precioBase == null) return BigDecimal.ZERO;
        BigDecimal incrementoMaximo = precioBase.multiply(BigDecimal.valueOf(0.20));
        if (mejorOferta == null || mejorOferta.compareTo(BigDecimal.ZERO) <= 0) {
            return precioBase.add(incrementoMaximo);
        }
        return mejorOferta.add(incrementoMaximo);
    }
}
