-- Los triggers usaban GETDATE() (UTC en el servidor) mientras que
-- el código Java usa LocalDateTime.now() en timezone ART (UTC-3).
-- Esto generaba timestamps 3 horas adelantados en notificaciones de BD.
-- Se reemplazan todos los GETDATE() de triggers de chat_mensajes
-- por la hora local de Argentina (UTC-3, sin DST).

CREATE OR ALTER TRIGGER dbo.trg_bien_estado_notificacion
ON dbo.productos_detalle
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    INSERT INTO dbo.chat_mensajes (cliente_id, tipo, emisor, contenido, timestamp_msg, leido)
    SELECT
        i.cliente_id,
        'bien',
        'bot',
        CONCAT(N'Tu bien "', i.nombre, N'" fue aceptado. Pronto te informaremos la fecha de subasta, precio base y comisiones.'),
        CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '-03:00') AS datetime2),
        0
    FROM inserted i
    JOIN deleted d ON i.producto_id = d.producto_id
    WHERE i.estado_solicitud = 'aceptado'
      AND d.estado_solicitud != 'aceptado';
END;
GO

CREATE OR ALTER TRIGGER dbo.trg_notificar_inspeccion_pendiente
ON dbo.productos_detalle
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    INSERT INTO dbo.chat_mensajes (cliente_id, tipo, emisor, contenido, timestamp_msg, leido)
    SELECT
        i.cliente_id,
        'bien',
        'sistema',
        LEFT(
            N'Estamos interesados en tu bien "' + ISNULL(i.nombre, '') + N'". '
                + N'Para continuar, traelo a nuestro deposito: Corrientes 2300, CABA (lun-vie 9-17 hs). '
                + N'Importante: si tras la inspeccion presencial no es aceptado, el costo de devolucion corre por tu cuenta.',
            1000
        ),
        CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '-03:00') AS datetime2),
        0
    FROM INSERTED i
    INNER JOIN DELETED d ON i.producto_id = d.producto_id
    WHERE i.estado_solicitud = 'pendiente_inspeccion'
      AND ISNULL(d.estado_solicitud, '') <> 'pendiente_inspeccion';
END;
GO

CREATE OR ALTER TRIGGER dbo.trg_notificar_rechazo_bien
ON dbo.productos_detalle
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    INSERT INTO dbo.chat_mensajes (cliente_id, tipo, emisor, contenido, timestamp_msg, leido)
    SELECT
        i.cliente_id,
        'bien',
        'sistema',
        LEFT(
            'Tu bien "' + ISNULL(i.nombre, 'sin nombre') + '" no fue aceptado para subasta.' + CHAR(10) +
            'Motivo: '  + ISNULL(i.motivo_rechazo, 'Sin especificar') + CHAR(10) +
            'Podes contactar con la empresa para mas informacion.',
            1000
        ),
        CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '-03:00') AS datetime2),
        0
    FROM INSERTED i
    JOIN DELETED d ON i.producto_id = d.producto_id
    WHERE i.estado_solicitud = 'rechazado'
      AND ISNULL(d.estado_solicitud, '') <> 'rechazado'
      AND ISNULL(i.motivo_rechazo, '') <> 'El usuario rechazo las condiciones propuestas';
END;
GO
