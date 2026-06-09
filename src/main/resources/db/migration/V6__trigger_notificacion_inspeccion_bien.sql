-- Ampliar columna contenido para soportar mensajes mas largos
IF COL_LENGTH('dbo.chat_mensajes', 'contenido') IS NOT NULL
BEGIN
    ALTER TABLE dbo.chat_mensajes ALTER COLUMN contenido NVARCHAR(1000) NOT NULL;
END;
GO

-- Trigger: notifica al usuario cuando el admin marca el bien como pendiente de inspeccion fisica
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
        GETDATE(),
        0
    FROM INSERTED i
    INNER JOIN DELETED d ON i.producto_id = d.producto_id
    WHERE i.estado_solicitud = 'inspeccion_pendiente'
      AND ISNULL(d.estado_solicitud, '') <> 'inspeccion_pendiente';
END;
GO

-- Trigger: notifica al usuario cuando el admin rechaza un bien
CREATE OR ALTER TRIGGER dbo.trg_notificar_rechazo_bien
ON dbo.productos_detalle
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    INSERT INTO dbo.chat_mensajes (cliente_id, tipo, emisor, contenido, leido)
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
        0
    FROM INSERTED i
    JOIN DELETED d ON i.producto_id = d.producto_id
    WHERE i.estado_solicitud = 'rechazado'
      AND ISNULL(d.estado_solicitud, '') <> 'rechazado'
      AND ISNULL(i.motivo_rechazo, '') <> 'El usuario rechazo las condiciones propuestas';
END;
GO
