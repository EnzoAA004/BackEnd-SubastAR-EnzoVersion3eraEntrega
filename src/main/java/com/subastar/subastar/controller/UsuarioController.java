package com.subastar.subastar.controller;

import com.subastar.subastar.dto.cuentaCobro.CuentaCobroRequest;
import com.subastar.subastar.dto.cuentaCobro.CuentaCobroResponse;
import com.subastar.subastar.dto.medioPago.*;
import com.subastar.subastar.dto.usuario.*;
import com.subastar.subastar.service.CuentaCobroService;
import com.subastar.subastar.service.MedioPagoService;
import com.subastar.subastar.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final MedioPagoService medioPagoService;
    private final CuentaCobroService cuentaCobroService;

    @GetMapping("/me")
    public ResponseEntity<UsuarioDetalle> getMe(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(usuarioService.getMe(user.getUsername()));
    }

    @PatchMapping("/me")
    public ResponseEntity<UsuarioDetalle> actualizarMe(@AuthenticationPrincipal UserDetails user,
            @RequestBody ActualizarUsuarioRequest req) {
        return ResponseEntity.ok(usuarioService.actualizarMe(user.getUsername(), req));
    }

    @GetMapping("/me/estado-cuenta")
    public ResponseEntity<EstadoCuentaResponse> getEstadoCuenta(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(usuarioService.getEstadoCuenta(user.getUsername()));
    }

    @GetMapping("/me/metricas")
    public ResponseEntity<MetricasResponse> getMetricas(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(usuarioService.getMetricas(user.getUsername()));
    }

    // ── Cuentas de cobro ──

    @GetMapping("/me/cuentas-cobro")
    public ResponseEntity<List<CuentaCobroResponse>> listarCuentasCobro(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(cuentaCobroService.listar(user.getUsername()));
    }

    @PostMapping("/me/cuentas-cobro")
    public ResponseEntity<CuentaCobroResponse> crearCuentaCobro(
            @Valid @RequestBody CuentaCobroRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cuentaCobroService.crear(user.getUsername(), req));
    }

    // ── Medios de pago ──

    @GetMapping("/me/medios-pago")
    public ResponseEntity<List<MedioPagoResumen>> listarMediosPago(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(medioPagoService.listar(user.getUsername()));
    }

    @PostMapping(value = "/me/medios-pago", consumes = "multipart/form-data")
    public ResponseEntity<MedioPagoResumen> agregarMedioPago(
            @AuthenticationPrincipal UserDetails user,
            @RequestPart("tipo") String tipo,
            @RequestPart(value = "nombre_banco", required = false) String nombreBanco,
            @RequestPart(value = "pais_banco", required = false) String paisBanco,
            @RequestPart(value = "cbu_iban", required = false) String cbuIban,
            @RequestPart(value = "fondos_reservados", required = false) String fondosReservados,
            @RequestPart(value = "numero_tarjeta", required = false) String numeroTarjeta,
            @RequestPart(value = "titular", required = false) String titular,
            @RequestPart(value = "vencimiento", required = false) String vencimiento,
            @RequestPart(value = "codigo_seguridad", required = false) String codigoSeguridad,
            @RequestPart(value = "dni_titular", required = false) String dniTitular,
            @RequestPart(value = "es_internacional", required = false) String esInternacional,
            @RequestPart(value = "banco_emisor", required = false) String bancoEmisor,
            @RequestPart(value = "monto_certificado", required = false) String montoCertificado,
            @RequestPart(value = "numero_cheque", required = false) String numeroCheque,
            @RequestPart(value = "foto_cheque", required = false) MultipartFile fotoCheque) {

        MedioPagoResumen result = switch (tipo.trim()) {
            case "cuenta_bancaria" -> {
                CuentaBancariaRequest req = new CuentaBancariaRequest();
                req.setNombreBanco(nombreBanco);
                req.setPaisBanco(paisBanco);
                req.setCbuIban(cbuIban);
                req.setFondosReservados(fondosReservados != null ? new java.math.BigDecimal(fondosReservados)
                        : java.math.BigDecimal.ZERO);
                yield medioPagoService.agregarCuenta(user.getUsername(), req);
            }
            case "tarjeta_credito" -> {
                TarjetaCreditoRequest req = new TarjetaCreditoRequest();
                req.setNumeroTarjeta(numeroTarjeta);
                req.setTitular(titular);
                req.setVencimiento(vencimiento);
                req.setCodigoSeguridad(codigoSeguridad);
                req.setDniTitular(dniTitular);
                req.setEsInternacional("true".equalsIgnoreCase(esInternacional));
                yield medioPagoService.agregarTarjeta(user.getUsername(), req);
            }
            case "cheque_certificado" -> {
                ChequeCertificadoRequest req = new ChequeCertificadoRequest();
                req.setBancoEmisor(bancoEmisor);
                req.setMontoCertificado(montoCertificado != null ? new java.math.BigDecimal(montoCertificado)
                        : java.math.BigDecimal.ZERO);
                req.setNumeroCheque(numeroCheque);
                yield medioPagoService.agregarCheque(user.getUsername(), req, fotoCheque);
            }
            default -> throw new com.subastar.subastar.exception.BadRequestException(
                    "Tipo de medio de pago inválido: " + tipo);
        };

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @DeleteMapping("/me/medios-pago/{id}")
    public ResponseEntity<Map<String, String>> eliminarMedioPago(@AuthenticationPrincipal UserDetails user,
                                                                 @PathVariable Integer id) {
        medioPagoService.eliminar(user.getUsername(), id);
        return ResponseEntity.ok(Map.of("message", "Medio de pago eliminado correctamente."));
    }
}
