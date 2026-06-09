package com.subastar.subastar.service;

import com.subastar.subastar.model.MedioPago;
import com.subastar.subastar.repository.MedioPagoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MedioPagoVerificacionScheduler {

    private final MedioPagoRepository medioPagoRepository;
    private final NotificacionService notificacionService;

    @Scheduled(fixedDelayString = "${app.scheduler.verificacion-medios-delay:10000}")
    public void verificarMediosPagoAprobados() {
        for (MedioPago medio : medioPagoRepository.findByVerificadoTrueAndEliminadoFalseAndNotificadoVerificacionFalse()) {
            notificacionService.notificarMedioPagoVerificado(medio.getCliente(), medio.getDescripcion());
            medio.setNotificadoVerificacion(true);
            medioPagoRepository.save(medio);
        }
    }

    @Scheduled(fixedDelayString = "${app.scheduler.verificacion-medios-delay:10000}")
    public void notificarMediosPagoRechazados() {
        for (MedioPago medio : medioPagoRepository.findByRechazadoTrueAndNotificadoRechazoFalse()) {
            notificacionService.notificarMedioPagoRechazado(medio.getCliente(), medio.getDescripcion());
            medio.setNotificadoRechazo(true);
            medioPagoRepository.save(medio);
        }
    }
}
