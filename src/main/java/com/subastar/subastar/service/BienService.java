package com.subastar.subastar.service;

import com.subastar.subastar.dto.bien.*;
import com.subastar.subastar.exception.BadRequestException;
import com.subastar.subastar.exception.ForbiddenException;
import com.subastar.subastar.exception.ResourceNotFoundException;
import com.subastar.subastar.model.*;
import com.subastar.subastar.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class BienService {

    private final BienSolicitudRepository bienSolicitudRepository;
    private final BienSolicitudArchivoRepository bienSolicitudArchivoRepository;
    private final ProductoDetalleRepository productoDetalleRepository;
    private final ProductoRepository productoRepository;
    private final CredencialRepository credencialRepository;
    private final ClienteRepository clienteRepository;
    private final ItemCatalogoRepository itemCatalogoRepository;
    private final CatalogoRepository catalogoRepository;
    private final NotificacionService notificacionService;
    private final EmpleadoRepository empleadoRepository;
    private final DuenioRepository duenioRepository;
    private final CuentaCobroRepository cuentaCobroRepository;

    private static final int MIN_FOTOS = 6;

    private static final Logger log = LoggerFactory.getLogger(BienService.class);

    private final CloudinaryService cloudinaryService;

    @Value("${cloudinary.cloud-name:}")
    private String cloudinaryCloudName;

    public BienSolicitudResponse iniciarSolicitud(String email, CrearBienSolicitudRequest req) {
        validarTipo(req.getTipo());
        Integer clienteId = getClienteId(email);
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));

        BienSolicitud sol = new BienSolicitud();
        sol.setCodigoSolicitud(generarCodigo());
        sol.setCliente(cliente);
        sol.setTipo(req.getTipo());
        sol.setEstado("iniciada");
        sol.setPasoActual("datos");
        bienSolicitudRepository.save(sol);
        return toResponse(sol);
    }

    @Transactional
    public BienSolicitudResponse cargarDatos(String email, String codigoSolicitud, BienDatosRequest req) {
        BienSolicitud sol = getSolicitudDelCliente(email, codigoSolicitud);

        if ("objeto_disenador".equals(req.getTipo())) {
            if (req.getArtistaDisenador() == null || req.getArtistaDisenador().isBlank()) {
                throw new BadRequestException("artista_disenador es requerido para tipo objeto_disenador");
            }
            if (req.getFechaCreacion() == null) {
                throw new BadRequestException("fecha_creacion es requerida para tipo objeto_disenador");
            }
        }
        if ("otro".equals(req.getTipo()) && (req.getInformacionAdicional() == null || req.getInformacionAdicional().isBlank())) {
            throw new BadRequestException("informacion_adicional es requerida para tipo otro");
        }

        sol.setNombre(req.getNombre());
        sol.setDescripcionTecnica(req.getDescripcionTecnica());
        sol.setCantidadElementos(req.getCantidadElementos());
        sol.setEpocaOrigen(req.getEpocaOrigen());
        sol.setArtistaDisenador(req.getArtistaDisenador());
        sol.setFechaCreacionObra(req.getFechaCreacion());
        sol.setDatosHistoricos(req.getDatosHistoricos());
        sol.setInformacionAdicional(req.getInformacionAdicional());
        sol.setCuentaCobroBanco(req.getCuentaCobroBanco());
        sol.setCuentaCobroPais(req.getCuentaCobroPais());
        sol.setCuentaCobroCbuIban(req.getCuentaCobroCbuIban());
        sol.setPrecioBaseSugerido(req.getPrecioBaseSugerido());
        sol.setDivisaPrecioBaseSugerido(req.getDivisaPrecioBaseSugerido());
        sol.setEstado("datos_cargados");
        sol.setPasoActual("fotos");
        bienSolicitudRepository.save(sol);

        return toResponse(sol);
    }

    @Transactional
    public BienSolicitudResponse cargarFotos(String email, String codigoSolicitud, List<MultipartFile> fotos) {
        BienSolicitud sol = getSolicitudDelCliente(email, codigoSolicitud);

        if (fotos == null || fotos.isEmpty()) throw new BadRequestException("Se debe enviar al menos una foto");

        log.info("Cargando {} fotos para solicitud {} por usuario {}", fotos.size(), codigoSolicitud, email);

        for (MultipartFile f : fotos) {
            BienSolicitudArchivo arch = new BienSolicitudArchivo();
            arch.setCodigoArchivo("FOTO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            arch.setSolicitud(sol);
            arch.setNombreArchivo(f.getOriginalFilename() != null ? f.getOriginalFilename() : "foto.jpg");
            arch.setTipoArchivo("foto");
            try {
                byte[] data = f.getBytes();
                // intentar subir a Cloudinary; si falla o no está configurado, se guardan los bytes
                String url = cloudinaryService.upload(data, arch.getCodigoArchivo());
                if (url != null) {
                    arch.setUrl(url);
                    // opcional: no almacenar datos para reducir tamaño en DB
                    arch.setDatos(null);
                } else {
                    arch.setDatos(data);
                }
            } catch (IOException e) {
                log.error("Error al procesar la foto: {}", f.getOriginalFilename(), e);
                throw new BadRequestException("Error al procesar la foto: " + f.getOriginalFilename());
            }
            bienSolicitudArchivoRepository.save(arch);
            log.debug("Foto guardada (codigo {}) para solicitud {}", arch.getCodigoArchivo(), codigoSolicitud);
        }

        int totalFotos = bienSolicitudArchivoRepository.countBySolicitudIdAndTipoArchivo(sol.getId(), "foto");
        if (totalFotos >= MIN_FOTOS) {
            sol.setEstado("fotos_cargadas");
            sol.setPasoActual("documentos");
            bienSolicitudRepository.save(sol);
        }
        return toResponse(sol);
    }

    @Transactional
    public BienSolicitudResponse cargarFotosCloudinary(String email, String codigoSolicitud, CloudinaryFotoRequest req) {
        BienSolicitud sol = getSolicitudDelCliente(email, codigoSolicitud);

        if (req == null || req.getFotos() == null || req.getFotos().isEmpty()) {
            throw new BadRequestException("Se debe enviar al menos una foto");
        }

        for (CloudinaryFotoRequest.CloudinaryFotoItem foto : req.getFotos()) {
            if (foto == null || foto.getPublicId() == null || foto.getPublicId().isBlank()) {
                throw new BadRequestException("public_id es requerido para cada foto");
            }

            BienSolicitudArchivo arch = new BienSolicitudArchivo();
            arch.setCodigoArchivo("FOTO-" + UUID.randomUUID());
            arch.setSolicitud(sol);
            arch.setNombreArchivo(foto.getOriginalFilename() != null && !foto.getOriginalFilename().isBlank()
                    ? foto.getOriginalFilename() : foto.getPublicId());
            arch.setTipoArchivo("foto");
            arch.setDatos(null);
            arch.setUrl(foto.getPublicId());
            bienSolicitudArchivoRepository.save(arch);
        }

        int totalFotos = bienSolicitudArchivoRepository.countBySolicitudIdAndTipoArchivo(sol.getId(), "foto");
        if (totalFotos >= MIN_FOTOS) {
            sol.setEstado("fotos_cargadas");
            sol.setPasoActual("documentos");
            bienSolicitudRepository.save(sol);
        }
        return toResponse(sol);
    }

    @Transactional
    public void eliminarFoto(String email, String codigoSolicitud, String codigoFoto) {
        BienSolicitud sol = getSolicitudDelCliente(email, codigoSolicitud);
        BienSolicitudArchivo arch = bienSolicitudArchivoRepository.findByCodigoArchivo(codigoFoto)
                .orElseThrow(() -> new ResourceNotFoundException("Foto no encontrada"));
        if (!arch.getSolicitud().getId().equals(sol.getId())) throw new ForbiddenException("La foto no pertenece a esta solicitud");
        bienSolicitudArchivoRepository.delete(arch);
    }

    @Transactional
    public BienSolicitudResponse cargarDocumentos(String email, String codigoSolicitud,
                                                   CargarDocumentosBienRequest req, List<MultipartFile> docs) {
        BienSolicitud sol = getSolicitudDelCliente(email, codigoSolicitud);

        if (Boolean.TRUE.equals(req.getDeclaraPropiedad())) {
            sol.setDeclaraPropiedad(true);
        }

        if (docs != null) {
            for (MultipartFile f : docs) {
                if (f == null || f.isEmpty()) continue;
                BienSolicitudArchivo arch = new BienSolicitudArchivo();
                arch.setCodigoArchivo("DOC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                arch.setSolicitud(sol);
                arch.setNombreArchivo(f.getOriginalFilename() != null ? f.getOriginalFilename() : "doc.pdf");
                arch.setTipoArchivo("documento");
                try {
                    byte[] data = f.getBytes();
                    String url = cloudinaryService.upload(data, arch.getCodigoArchivo());
                    if (url != null) {
                        arch.setUrl(url);
                        arch.setDatos(null);
                    } else {
                        arch.setDatos(data);
                    }
                } catch (IOException e) {
                    throw new BadRequestException("Error al procesar el documento: " + f.getOriginalFilename());
                }
                bienSolicitudArchivoRepository.save(arch);
            }
        }

        sol.setEstado("documentos_cargados");
        sol.setPasoActual("confirmar");
        bienSolicitudRepository.save(sol);

        return toResponse(sol);
    }

    @Transactional
    public void eliminarDocumento(String email, String codigoSolicitud, String codigoDoc) {
        BienSolicitud sol = getSolicitudDelCliente(email, codigoSolicitud);
        BienSolicitudArchivo arch = bienSolicitudArchivoRepository.findByCodigoArchivo(codigoDoc)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado"));
        if (!arch.getSolicitud().getId().equals(sol.getId())) throw new ForbiddenException("El documento no pertenece a esta solicitud");
        bienSolicitudArchivoRepository.delete(arch);
    }

    @Transactional
    public BienSolicitudEnviadaResponse confirmar(String email, String codigoSolicitud) {
        BienSolicitud sol = getSolicitudDelCliente(email, codigoSolicitud);

        int fotos = bienSolicitudArchivoRepository.countBySolicitudIdAndTipoArchivo(sol.getId(), "foto");
        if (fotos < MIN_FOTOS) throw new BadRequestException("Se requieren al menos " + MIN_FOTOS + " fotos");
        if (!sol.isDeclaraPropiedad()) throw new BadRequestException("Debe aceptar la declaración de propiedad");
        if (sol.getNombre() == null) throw new BadRequestException("Los datos del bien son incompletos");

        Empleado revisor = empleadoRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new BadRequestException("No hay empleados disponibles para procesar la solicitud. Contacte al administrador."));

        Integer clienteId = sol.getCliente().getIdentificador();
        Duenio duenio = duenioRepository.findById(clienteId).orElseGet(() -> {
            Duenio d = new Duenio();
            d.setIdentificador(clienteId);
            d.setVerificadorId(revisor.getIdentificador());
            if (sol.getCliente().getPais() != null) {
                d.setNumeroPaisId(sol.getCliente().getPais().getNumero());
            }
            return duenioRepository.save(d);
        });

        Producto producto = new Producto();
        producto.setFecha(LocalDate.now());
        producto.setDisponible("no");
        producto.setDescripcionCatalogo(sol.getNombre());
        producto.setDescripcionCompleta(sol.getDescripcionTecnica() != null ? sol.getDescripcionTecnica() : sol.getNombre());
        producto.setRevisor(revisor);
        producto.setDuenio(duenio);
        producto = productoRepository.save(producto);

        ProductoDetalle det = new ProductoDetalle();
        det.setProductoId(producto.getIdentificador());
        det.setClienteId(clienteId);
        det.setNombre(sol.getNombre());
        det.setTipo(sol.getTipo());
        det.setCantidadElementos(sol.getCantidadElementos());
        det.setEpocaOrigen(sol.getEpocaOrigen());
        det.setArtistaDisenador(sol.getArtistaDisenador());
        det.setFechaCreacionObra(sol.getFechaCreacionObra());
        det.setDatosHistoricos(sol.getDatosHistoricos());
        det.setInformacionAdicional(sol.getInformacionAdicional());
        det.setPrecioBaseSugerido(sol.getPrecioBaseSugerido());
        det.setDivisaPrecioBaseSugerido(sol.getDivisaPrecioBaseSugerido());
        det.setEstadoSolicitud("en_revision");
        productoDetalleRepository.save(det);

        sol.setEstado("enviado_a_revision");
        sol.setPasoActual("finalizado");
        sol.setProductoId(producto.getIdentificador());
        bienSolicitudRepository.save(sol);

        notificacionService.notificarBienConfirmado(sol.getCliente(), sol.getNombre());

        BienSolicitudEnviadaResponse resp = new BienSolicitudEnviadaResponse();
        resp.setCodigoSolicitud(sol.getCodigoSolicitud());
        resp.setCodigoBien("BIEN-" + producto.getIdentificador());
        resp.setEstado("en_revision");
        resp.setMessage("Tu bien fue enviado para revisión. Te notificaremos cuando la empresa complete la inspección y te informaremos la fecha, valor base y comisiones.");
        return resp;
    }

    public List<BienResumen> getMisBienes(String email, String estadoFiltro) {
        Integer clienteId = getClienteId(email);
        List<ProductoDetalle> detalles = estadoFiltro != null
                ? productoDetalleRepository.findByClienteIdAndEstadoSolicitud(clienteId, estadoFiltro)
                : productoDetalleRepository.findByClienteId(clienteId);
        return detalles.stream().map(this::toBienResumen).collect(Collectors.toList());
    }

    public BienDetalle getMiBien(String email, Integer productoId) {
        Integer clienteId = getClienteId(email);
        ProductoDetalle det = productoDetalleRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Bien no encontrado"));
        if (!clienteId.equals(det.getClienteId())) throw new ForbiddenException("El bien no pertenece al usuario");
        return toBienDetalle(det);
    }

    @Transactional
    public BienDetalle actualizarMiBien(String email, Integer productoId, ActualizarBienRequest req) {
        Integer clienteId = getClienteId(email);
        ProductoDetalle det = productoDetalleRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Bien no encontrado"));
        if (!clienteId.equals(det.getClienteId())) {
            throw new ForbiddenException("El bien no pertenece al usuario");
        }

        boolean yaSubastado = itemCatalogoRepository.findAll().stream()
                .anyMatch(ic -> ic.getProducto().getIdentificador().equals(productoId)
                        && "si".equalsIgnoreCase(ic.getSubastado()));
        if (yaSubastado) {
            throw new BadRequestException("No se puede modificar un bien ya subastado");
        }

        if (req.getInformacionAdicional() != null) {
            det.setInformacionAdicional(req.getInformacionAdicional());
        }
        if (req.getPrecioBaseSugerido() != null) {
            det.setPrecioBaseSugerido(req.getPrecioBaseSugerido());
        }
        if (req.getDivisaPrecioBaseSugerido() != null) {
            String divisa = req.getDivisaPrecioBaseSugerido().trim().toUpperCase();
            if (!divisa.equals("ARS") && !divisa.equals("USD")) {
                throw new BadRequestException("La divisa del precio base sugerido debe ser ARS o USD");
            }
            det.setDivisaPrecioBaseSugerido(divisa);
        }
        if (req.getPrecioBaseSugerido() != null && det.getDivisaPrecioBaseSugerido() == null) {
            det.setDivisaPrecioBaseSugerido("ARS");
        }

        productoDetalleRepository.save(det);
        return toBienDetalle(det);
    }

    @Transactional
    public void rechazarBien(Integer productoId, String motivo) {
        ProductoDetalle det = productoDetalleRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Bien no encontrado"));
        det.setEstadoSolicitud("rechazado");
        det.setMotivoRechazo(motivo);
        productoDetalleRepository.save(det);
        // la notificación la genera el trigger de BD al detectar el cambio de estado
    }

    @Transactional
    public void aceptarCondiciones(String email, Integer productoId, AceptarCondicionesRequest req) {
        Integer clienteId = getClienteId(email);
        ProductoDetalle det = productoDetalleRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Bien no encontrado"));
        if (!clienteId.equals(det.getClienteId())) throw new ForbiddenException("El bien no pertenece al usuario");
        det.setAceptoCondiciones(req.isAcepta());
        if (!req.isAcepta()) {
            det.setEstadoSolicitud("rechazado");
            det.setMotivoRechazo("El usuario rechazó las condiciones propuestas");

            Cliente cliente = clienteRepository.findById(clienteId)
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));

            java.math.BigDecimal base = det.getPrecioBase() != null ? det.getPrecioBase() : det.getPrecioBaseSugerido();
            java.math.BigDecimal costoDev = base != null
                    ? base.multiply(java.math.BigDecimal.valueOf(0.10))
                    : java.math.BigDecimal.ZERO;

            notificacionService.notificarDevolucion(cliente, det.getNombre(), costoDev);
        } else if (req.getCuentaCobroId() != null) {
            cuentaCobroRepository.findById(req.getCuentaCobroId())
                    .filter(cc -> cc.getClienteId().equals(clienteId))
                    .orElseThrow(() -> new ResourceNotFoundException("Cuenta de cobro no encontrada"));
            det.setCuentaCobroId(req.getCuentaCobroId());
        }
        productoDetalleRepository.save(det);
    }

    private BienResumen toBienResumen(ProductoDetalle det) {
        BienResumen r = new BienResumen();
        r.setId(det.getProductoId());
        r.setNombre(det.getNombre());
        r.setEstado(det.getEstadoSolicitud());
        r.setMotivoRechazo(det.getMotivoRechazo());
        r.setPrecioBase(det.getPrecioBase());
        r.setComision(det.getComision());
        r.setSubastaAsignada(det.getSubastaAsignada());
        r.setPolizaId(det.getPolizaId());
        r.setCostoEnvio(det.getCostoEnvio());
        r.setAceptoCondiciones(det.getAceptoCondiciones());
        r.setUbicacionDeposito(det.getUbicacionDeposito());

        if (r.getPrecioBase() == null || r.getComision() == null || r.getSubastaAsignada() == null) {
            itemCatalogoRepository.findAll().stream()
                    .filter(ic -> ic.getProducto().getIdentificador().equals(det.getProductoId()))
                    .findFirst()
                    .ifPresent(ic -> {
                        if (r.getPrecioBase() == null) {
                            r.setPrecioBase(ic.getPrecioBase());
                        }
                        if (r.getComision() == null) {
                            r.setComision(ic.getComision());
                        }
                        if (r.getSubastaAsignada() == null && ic.getCatalogo().getSubasta() != null) {
                            r.setSubastaAsignada("Subasta #" + ic.getCatalogo().getSubasta().getIdentificador());
                        }
                    });
        }

        if (r.getPolizaId() == null && det.getProducto() != null && det.getProducto().getSeguroNroPoliza() != null) {
            r.setPolizaId(det.getProducto().getSeguroNroPoliza());
        }
        return r;
    }

    private BienDetalle toBienDetalle(ProductoDetalle det) {
        BienDetalle d = new BienDetalle();
        BienResumen base = toBienResumen(det);
        d.setId(base.getId()); d.setNombre(base.getNombre()); d.setEstado(base.getEstado());
        d.setSubastaAsignada(base.getSubastaAsignada()); d.setPrecioBase(base.getPrecioBase());
        d.setComision(base.getComision()); d.setMotivoRechazo(base.getMotivoRechazo());
        d.setCostoEnvio(base.getCostoEnvio());
        d.setUbicacionDeposito(base.getUbicacionDeposito()); d.setPolizaId(base.getPolizaId());
        d.setDescripcionTecnica(det.getProducto() != null ? det.getProducto().getDescripcionCompleta() : null);
        d.setCantidadElementos(det.getCantidadElementos());
        d.setInformacionAdicional(det.getInformacionAdicional());
        d.setPrecioBaseSugerido(det.getPrecioBaseSugerido());
        d.setDivisaPrecioBaseSugerido(det.getDivisaPrecioBaseSugerido() != null
                ? det.getDivisaPrecioBaseSugerido()
                : det.getPrecioBaseSugerido() != null ? "ARS" : null);
        d.setAceptoCondiciones(det.getAceptoCondiciones());
        int fotos = 0;
        boolean documentacionAdjunta = false;
        List<BienFotoResponse> fotosResponse = new ArrayList<>();
        List<BienDocumentoResponse> documentosResponse = new ArrayList<>();
        var solicitudOpt = bienSolicitudRepository.findByProductoId(det.getProductoId());
        if (solicitudOpt.isPresent()) {
            BienSolicitud solicitud = solicitudOpt.get();
            fotos = bienSolicitudArchivoRepository.countBySolicitudIdAndTipoArchivo(solicitud.getId(), "foto");
            int documentos = bienSolicitudArchivoRepository.countBySolicitudIdAndTipoArchivo(solicitud.getId(), "documento");
            documentacionAdjunta = documentos > 0;
            fotosResponse = bienSolicitudArchivoRepository
                    .findBySolicitudIdAndTipoArchivo(solicitud.getId(), "foto")
                    .stream()
                    .map(archivo -> {
                        BienFotoResponse foto = new BienFotoResponse();
                        foto.setCodigoFoto(archivo.getCodigoArchivo());
                        foto.setNombreArchivo(archivo.getNombreArchivo());
                        foto.setPublicId(archivo.getUrl());
                        foto.setUrl(buildCloudinaryImageUrl(archivo.getUrl()));
                        foto.setTipo(archivo.getTipoArchivo());
                        return foto;
                    })
                    .toList();
            documentosResponse = bienSolicitudArchivoRepository
                    .findBySolicitudIdAndTipoArchivo(solicitud.getId(), "documento")
                    .stream()
                    .map(archivo -> {
                        BienDocumentoResponse documento = new BienDocumentoResponse();
                        documento.setCodigoDocumento(archivo.getCodigoArchivo());
                        documento.setNombreArchivo(archivo.getNombreArchivo());
                        documento.setUrl(archivo.getUrl());
                        documento.setTipo(archivo.getTipoArchivo());
                        documento.setContentType(resolveContentType(archivo.getNombreArchivo()));
                        return documento;
                    })
                    .toList();
        }
        d.setFotosCargadas(fotos);
        d.setDocumentacionAdjunta(documentacionAdjunta);
        d.setFotos(fotosResponse);
        d.setDocumentos(documentosResponse);
        return d;
    }

    private String buildCloudinaryImageUrl(String storedValue) {
        if (storedValue == null || storedValue.isBlank()) {
            return null;
        }
        if (storedValue.startsWith("http://") || storedValue.startsWith("https://")) {
            return storedValue;
        }
        if (cloudinaryCloudName == null || cloudinaryCloudName.isBlank()) {
            return null;
        }
        return "https://res.cloudinary.com/"
                + cloudinaryCloudName
                + "/image/upload/f_auto,q_auto/"
                + storedValue;
    }

    private String resolveContentType(String filename) {
        if (filename == null) return "application/octet-stream";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        return "application/octet-stream";
    }

    private BienSolicitudResponse toResponse(BienSolicitud sol) {
        BienSolicitudResponse r = new BienSolicitudResponse();
        r.setCodigoSolicitud(sol.getCodigoSolicitud());
        r.setTipo(sol.getTipo());
        r.setEstado(sol.getEstado());
        r.setPasoActual(sol.getPasoActual());
        r.setDatosCompletos(sol.getNombre() != null);
        int fotosCount = bienSolicitudArchivoRepository.countBySolicitudIdAndTipoArchivo(sol.getId(), "foto");
        r.setFotosCargadas(fotosCount);
        r.setMinimoFotosRequeridas(MIN_FOTOS);
        r.setMaximoFotosPermitidas(null);
        r.setDeclaracionPropiedadAceptada(sol.isDeclaraPropiedad());
        int docsCount = bienSolicitudArchivoRepository.countBySolicitudIdAndTipoArchivo(sol.getId(), "documento");
        r.setDocumentacionAdjunta(docsCount > 0);
        r.setPuedeConfirmar(r.isDatosCompletos() && fotosCount >= MIN_FOTOS && sol.isDeclaraPropiedad());

        if (sol.getNombre() != null) {
            BienSolicitudResponse.BienDatosResumen bien = new BienSolicitudResponse.BienDatosResumen();
            bien.setNombre(sol.getNombre());
            bien.setDescripcionTecnica(sol.getDescripcionTecnica());
            bien.setCantidadElementos(sol.getCantidadElementos());
            bien.setEpocaOrigen(sol.getEpocaOrigen());
            bien.setArtistaDisenador(sol.getArtistaDisenador());
            bien.setFechaCreacion(sol.getFechaCreacionObra() != null ? sol.getFechaCreacionObra().toString() : null);
            bien.setDatosHistoricos(sol.getDatosHistoricos());
            bien.setInformacionAdicional(sol.getInformacionAdicional());
            r.setBien(bien);
        }

        List<BienArchivoResumen> fotos = bienSolicitudArchivoRepository
                .findBySolicitudIdAndTipoArchivo(sol.getId(), "foto")
                .stream().map(this::toArchivoResumen).collect(Collectors.toList());
        r.setFotos(fotos);

        List<BienArchivoResumen> docs = bienSolicitudArchivoRepository
                .findBySolicitudIdAndTipoArchivo(sol.getId(), "documento")
                .stream().map(this::toArchivoResumen).collect(Collectors.toList());
        r.setDocumentos(docs);
        return r;
    }

    private BienArchivoResumen toArchivoResumen(BienSolicitudArchivo a) {
        BienArchivoResumen r = new BienArchivoResumen();
        r.setCodigoArchivo(a.getCodigoArchivo());
        r.setNombreArchivo(a.getNombreArchivo());
        r.setTipoArchivo(a.getTipoArchivo());
        r.setUrlTemporal(a.getUrl());
        return r;
    }

    private BienSolicitud getSolicitudDelCliente(String email, String codigo) {
        Integer clienteId = getClienteId(email);
        BienSolicitud sol = bienSolicitudRepository.findByCodigoSolicitud(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada"));
        if (!sol.getCliente().getIdentificador().equals(clienteId)) {
            throw new ForbiddenException("La solicitud no pertenece al usuario");
        }
        return sol;
    }

    private Integer getClienteId(String email) {
        return credencialRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"))
                .getPersonaId();
    }

    private void validarTipo(String tipo) {
        if (!List.of("obra_arte", "objeto_disenador", "otro").contains(tipo)) {
            throw new BadRequestException("Tipo inválido: " + tipo);
        }
    }

    private String generarCodigo() {
        return "SOL-BIEN-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
