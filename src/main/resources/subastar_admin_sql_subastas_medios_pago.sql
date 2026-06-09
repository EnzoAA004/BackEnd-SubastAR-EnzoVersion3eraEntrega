-- ============================================================
-- SubastAR - Rutinas SQL Server administrativas
-- Flujos de subasta en vivo + habilitación de medios de pago
--
-- IMPORTANTE:
-- - NO crea usuarios finales.
-- - NO crea medios de pago.
-- - NO crea pujas manuales.
-- - NO reemplaza endpoints de usuario.
--
-- Uso esperado:
-- 1) Usuario se registra/loguea por endpoints.
-- 2) Usuario carga medio de pago por endpoint.
-- 3) Empresa verifica medio de pago por SQL.
-- 4) Empresa abre subasta / setea ítem actual por SQL.
-- 5) Usuario puja por endpoint.
-- 6) Empresa cierra ítem por SQL.
-- 7) Usuario consulta compras/chat/factura por endpoints.
-- ============================================================

USE subastar_db;
GO

SET NOCOUNT ON;
GO

-- ============================================================
-- SCRIPT 01 - Verificar medio de pago de un usuario por email
-- ============================================================
-- Usar después de: POST /usuarios/me/medios-pago
-- Si @medio_pago_id_verificar es NULL, verifica todos los medios activos del usuario.
-- ============================================================

DECLARE @email_usuario_verificar VARCHAR(255) = 'REEMPLAZAR_EMAIL_DEL_USUARIO';
DECLARE @medio_pago_id_verificar INT = NULL; -- poner ID específico o dejar NULL

IF NOT EXISTS (
    SELECT 1
    FROM credenciales c
    INNER JOIN clientes cl ON cl.identificador = c.persona_id
    WHERE c.email = @email_usuario_verificar
)
BEGIN
    THROW 51001, 'No existe cliente con ese email.', 1;
END;

IF @medio_pago_id_verificar IS NOT NULL
   AND NOT EXISTS (
        SELECT 1
        FROM medios_pago mp
        INNER JOIN credenciales c ON c.persona_id = mp.cliente_id
        WHERE c.email = @email_usuario_verificar
          AND mp.id = @medio_pago_id_verificar
          AND mp.eliminado = 0
   )
BEGIN
    THROW 51002, 'El medio de pago indicado no existe, no pertenece al usuario o está eliminado.', 1;
END;

UPDATE mp
SET verificado = 1
FROM medios_pago mp
INNER JOIN credenciales c ON c.persona_id = mp.cliente_id
WHERE c.email = @email_usuario_verificar
  AND mp.eliminado = 0
  AND (@medio_pago_id_verificar IS NULL OR mp.id = @medio_pago_id_verificar);

SELECT
    mp.id,
    c.email,
    mp.tipo,
    mp.descripcion,
    mp.verificado,
    mp.eliminado,
    mp.creado_en
FROM medios_pago mp
INNER JOIN credenciales c ON c.persona_id = mp.cliente_id
WHERE c.email = @email_usuario_verificar
ORDER BY mp.id;
GO

-- ============================================================
-- SCRIPT 02 - Deshabilitar verificación de medio de pago
-- ============================================================
-- Sirve para probar error: "El medio de pago no está verificado por la empresa".
-- ============================================================

DECLARE @email_usuario_desverificar VARCHAR(255) = 'REEMPLAZAR_EMAIL_DEL_USUARIO';
DECLARE @medio_pago_id_desverificar INT = NULL; -- poner ID específico o dejar NULL

UPDATE mp
SET verificado = 0
FROM medios_pago mp
INNER JOIN credenciales c ON c.persona_id = mp.cliente_id
WHERE c.email = @email_usuario_desverificar
  AND mp.eliminado = 0
  AND (@medio_pago_id_desverificar IS NULL OR mp.id = @medio_pago_id_desverificar);

SELECT
    mp.id,
    c.email,
    mp.tipo,
    mp.verificado,
    mp.eliminado
FROM medios_pago mp
INNER JOIN credenciales c ON c.persona_id = mp.cliente_id
WHERE c.email = @email_usuario_desverificar
ORDER BY mp.id;
GO

-- ============================================================
-- SCRIPT 03 - Cambiar categoría y admisión de un cliente
-- ============================================================
-- Sirve para probar:
-- - categoría insuficiente;
-- - usuario no admitido;
-- - usuario oro/platino sin máximo de 20%.
-- Categorías: comun | especial | plata | oro | platino
-- Admitido: si | no
-- ============================================================

DECLARE @email_categoria VARCHAR(255) = 'REEMPLAZAR_EMAIL_DEL_USUARIO';
DECLARE @nueva_categoria VARCHAR(20) = 'comun';
DECLARE @nuevo_admitido VARCHAR(5) = 'si';

IF @nueva_categoria NOT IN ('comun', 'especial', 'plata', 'oro', 'platino')
BEGIN
    THROW 51003, 'Categoría inválida. Usar comun, especial, plata, oro o platino.', 1;
END;

IF @nuevo_admitido NOT IN ('si', 'no')
BEGIN
    THROW 51004, 'Valor admitido inválido. Usar si o no.', 1;
END;

UPDATE cl
SET categoria = @nueva_categoria,
    admitido = @nuevo_admitido
FROM clientes cl
INNER JOIN credenciales c ON c.persona_id = cl.identificador
WHERE c.email = @email_categoria;

IF @@ROWCOUNT = 0
BEGIN
    THROW 51005, 'No se encontró cliente para actualizar categoría/admisión.', 1;
END;

SELECT
    c.email,
    cl.identificador AS cliente_id,
    cl.categoria,
    cl.admitido
FROM clientes cl
INNER JOIN credenciales c ON c.persona_id = cl.identificador
WHERE c.email = @email_categoria;
GO

-- ============================================================
-- SCRIPT 04 - Abrir/habilitar una subasta en vivo
-- ============================================================
-- Deja fecha/hora en el pasado para que el backend calcule estado en_vivo.
-- Después probar: GET /subastas/{id}/en-vivo
-- ============================================================

DECLARE @subasta_id_abrir INT = 1; -- REEMPLAZAR

IF NOT EXISTS (SELECT 1 FROM subastas WHERE identificador = @subasta_id_abrir)
BEGIN
    THROW 51101, 'Subasta no encontrada.', 1;
END;

UPDATE subastas
SET estado = 'abierta',
    fecha = CAST(GETDATE() AS DATE),
    hora = CAST(DATEADD(HOUR, -1, GETDATE()) AS TIME)
WHERE identificador = @subasta_id_abrir;

SELECT
    identificador,
    estado,
    fecha,
    hora,
    categoria,
    ubicacion
FROM subastas
WHERE identificador = @subasta_id_abrir;
GO

-- ============================================================
-- SCRIPT 05 - Setear ítem actual de una subasta en vivo
-- ============================================================
-- Actualiza subastas_extra.item_actual_id.
-- Validaciones: ítem existe, pertenece al catálogo de la subasta y no está subastado.
-- ============================================================

DECLARE @subasta_id_item_actual INT = 1; -- REEMPLAZAR
DECLARE @item_actual_id INT = 1;         -- REEMPLAZAR

IF NOT EXISTS (SELECT 1 FROM subastas WHERE identificador = @subasta_id_item_actual)
BEGIN
    THROW 51201, 'Subasta no encontrada.', 1;
END;

IF NOT EXISTS (SELECT 1 FROM itemsCatalogo WHERE identificador = @item_actual_id)
BEGIN
    THROW 51202, 'Ítem no encontrado.', 1;
END;

IF NOT EXISTS (
    SELECT 1
    FROM itemsCatalogo ic
    INNER JOIN catalogos c ON c.identificador = ic.catalogo
    WHERE ic.identificador = @item_actual_id
      AND c.subasta = @subasta_id_item_actual
)
BEGIN
    THROW 51203, 'El ítem no pertenece al catálogo de esa subasta.', 1;
END;

IF EXISTS (
    SELECT 1
    FROM itemsCatalogo
    WHERE identificador = @item_actual_id
      AND subastado = 'si'
)
BEGIN
    THROW 51204, 'El ítem ya figura como subastado.', 1;
END;

IF NOT EXISTS (SELECT 1 FROM subastas_extra WHERE subasta_id = @subasta_id_item_actual)
BEGIN
    INSERT INTO subastas_extra (subasta_id, nombre, moneda, url_streaming, item_actual_id)
    VALUES (@subasta_id_item_actual, 'Subasta #' + CAST(@subasta_id_item_actual AS VARCHAR(20)), 'ARS', NULL, @item_actual_id);
END
ELSE
BEGIN
    UPDATE subastas_extra
    SET item_actual_id = @item_actual_id
    WHERE subasta_id = @subasta_id_item_actual;
END;

SELECT
    s.identificador AS subasta_id,
    s.estado,
    s.fecha,
    s.hora,
    s.categoria,
    se.nombre,
    se.moneda,
    se.url_streaming,
    se.item_actual_id
FROM subastas s
INNER JOIN subastas_extra se ON se.subasta_id = s.identificador
WHERE s.identificador = @subasta_id_item_actual;
GO

-- ============================================================
-- SCRIPT 06 - Cambiar moneda/categoría/nombre/streaming de una subasta
-- ============================================================
-- Sirve para probar filtro por moneda, subastas USD y categoría insuficiente.
-- ============================================================

DECLARE @subasta_id_config INT = 1; -- REEMPLAZAR
DECLARE @nombre_subasta VARCHAR(255) = 'Subasta demo en vivo';
DECLARE @moneda_subasta VARCHAR(10) = 'ARS'; -- ARS | USD
DECLARE @categoria_subasta VARCHAR(20) = 'comun'; -- comun | especial | plata | oro | platino
DECLARE @url_streaming VARCHAR(500) = 'https://streaming.demo/subastar';

IF @moneda_subasta NOT IN ('ARS', 'USD')
BEGIN
    THROW 51301, 'Moneda inválida. Usar ARS o USD.', 1;
END;

IF @categoria_subasta NOT IN ('comun', 'especial', 'plata', 'oro', 'platino')
BEGIN
    THROW 51302, 'Categoría inválida.', 1;
END;

IF NOT EXISTS (SELECT 1 FROM subastas WHERE identificador = @subasta_id_config)
BEGIN
    THROW 51303, 'Subasta no encontrada.', 1;
END;

UPDATE subastas
SET categoria = @categoria_subasta
WHERE identificador = @subasta_id_config;

IF NOT EXISTS (SELECT 1 FROM subastas_extra WHERE subasta_id = @subasta_id_config)
BEGIN
    INSERT INTO subastas_extra (subasta_id, nombre, moneda, url_streaming, item_actual_id)
    VALUES (@subasta_id_config, @nombre_subasta, @moneda_subasta, @url_streaming, NULL);
END
ELSE
BEGIN
    UPDATE subastas_extra
    SET nombre = @nombre_subasta,
        moneda = @moneda_subasta,
        url_streaming = @url_streaming
    WHERE subasta_id = @subasta_id_config;
END;

SELECT
    s.identificador,
    s.categoria,
    se.nombre,
    se.moneda,
    se.url_streaming,
    se.item_actual_id
FROM subastas s
INNER JOIN subastas_extra se ON se.subasta_id = s.identificador
WHERE s.identificador = @subasta_id_config;
GO

-- ============================================================
-- SCRIPT 07 - Cerrar ítem actual y generar compra
-- ============================================================
-- Hace lo que antes hacía el endpoint admin eliminado:
-- - busca mejor puja;
-- - marca esa puja como ganadora;
-- - crea registroDeSubasta;
-- - crea compras_extra;
-- - marca ítem como subastado;
-- - limpia item_actual_id;
-- - inserta mensaje privado al ganador.
--
-- Después validar con:
-- GET /compras
-- GET /compras/{id}
-- GET /chat/conversaciones
-- GET /chat/conversaciones/compra
-- ============================================================

DECLARE @subasta_id_cerrar_item INT = 1; -- REEMPLAZAR
DECLARE @costo_envio DECIMAL(18,2) = 0.00;
DECLARE @direccion_entrega VARCHAR(255) = NULL;

DECLARE @item_id_cierre INT;
DECLARE @pujo_ganador_id INT;
DECLARE @cliente_ganador_id INT;
DECLARE @producto_id INT;
DECLARE @duenio_id INT;
DECLARE @importe_ganador DECIMAL(18,2);
DECLARE @comision_item DECIMAL(18,2);
DECLARE @medio_pago_id INT;
DECLARE @registro_id INT;
DECLARE @nombre_item NVARCHAR(255);
DECLARE @nro_poliza VARCHAR(30);

BEGIN TRY
    BEGIN TRANSACTION;

    SELECT @item_id_cierre = item_actual_id
    FROM subastas_extra WITH (UPDLOCK, HOLDLOCK)
    WHERE subasta_id = @subasta_id_cerrar_item;

    IF @item_id_cierre IS NULL
    BEGIN
        THROW 51401, 'La subasta no tiene ítem actual.', 1;
    END;

    IF NOT EXISTS (
        SELECT 1
        FROM itemsCatalogo ic
        INNER JOIN catalogos c ON c.identificador = ic.catalogo
        WHERE ic.identificador = @item_id_cierre
          AND c.subasta = @subasta_id_cerrar_item
    )
    BEGIN
        THROW 51402, 'El ítem actual no pertenece a la subasta indicada.', 1;
    END;

    SELECT
        @producto_id = ic.producto,
        @comision_item = ISNULL(ic.comision, 0),
        @duenio_id = p.duenio,
        @nro_poliza = p.seguro,
        @nombre_item = COALESCE(pd.nombre, p.descripcionCatalogo, 'Ítem #' + CAST(ic.identificador AS VARCHAR(20)))
    FROM itemsCatalogo ic
    INNER JOIN productos p ON p.identificador = ic.producto
    LEFT JOIN productos_detalle pd ON pd.producto_id = p.identificador
    WHERE ic.identificador = @item_id_cierre;

    SELECT TOP 1
        @pujo_ganador_id = p.identificador,
        @cliente_ganador_id = a.cliente,
        @importe_ganador = p.importe,
        @medio_pago_id = pe.medio_pago_id
    FROM pujos p
    INNER JOIN asistentes a ON a.identificador = p.asistente
    LEFT JOIN pujos_extra pe ON pe.pujo_id = p.identificador
    WHERE p.item = @item_id_cierre
    ORDER BY p.importe DESC, p.identificador ASC;

    -- Caso sin pujas: marcar subastado y limpiar vivo, sin compra de usuario.
    IF @pujo_ganador_id IS NULL
    BEGIN
        UPDATE itemsCatalogo
        SET subastado = 'si'
        WHERE identificador = @item_id_cierre;

        UPDATE subastas_extra
        SET item_actual_id = NULL
        WHERE subasta_id = @subasta_id_cerrar_item;

        COMMIT TRANSACTION;

        SELECT 'SIN_PUJAS' AS resultado, @subasta_id_cerrar_item AS subasta_id, @item_id_cierre AS item_cerrado;
        RETURN;
    END;

    UPDATE pujos
    SET ganador = CASE WHEN identificador = @pujo_ganador_id THEN 'si' ELSE 'no' END
    WHERE item = @item_id_cierre;

    SELECT TOP 1 @registro_id = identificador
    FROM registroDeSubasta
    WHERE subasta = @subasta_id_cerrar_item
      AND producto = @producto_id
      AND cliente = @cliente_ganador_id;

    IF @registro_id IS NULL
    BEGIN
        INSERT INTO registroDeSubasta (subasta, duenio, producto, cliente, importe, comision)
        VALUES (@subasta_id_cerrar_item, @duenio_id, @producto_id, @cliente_ganador_id, @importe_ganador, @comision_item);

        SET @registro_id = SCOPE_IDENTITY();
    END;

    IF NOT EXISTS (SELECT 1 FROM compras_extra WHERE registro_id = @registro_id)
    BEGIN
        INSERT INTO compras_extra (
            registro_id,
            fecha_compra,
            estado_pago,
            estado_entrega,
            medio_pago_id,
            costo_envio,
            direccion_entrega,
            factura_path
        )
        VALUES (
            @registro_id,
            GETDATE(),
            'pendiente',
            'coordinando',
            @medio_pago_id,
            @costo_envio,
            @direccion_entrega,
            NULL
        );
    END;

    IF @nro_poliza IS NOT NULL
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM seguros WHERE nroPoliza = @nro_poliza)
        BEGIN
            THROW 51403, 'La poliza asociada al producto no existe.', 1;
        END;

        IF EXISTS (
            SELECT 1
            FROM seguros_extra
            WHERE poliza_id = @nro_poliza
              AND beneficiario_id IS NOT NULL
              AND beneficiario_id <> @cliente_ganador_id
        )
        BEGIN
            THROW 51404, 'La poliza asociada al producto ya pertenece a otro beneficiario.', 1;
        END;

        IF EXISTS (SELECT 1 FROM seguros_extra WHERE poliza_id = @nro_poliza)
        BEGIN
            UPDATE seguros_extra
            SET beneficiario_id = @cliente_ganador_id
            WHERE poliza_id = @nro_poliza
              AND beneficiario_id IS NULL;
        END
        ELSE
        BEGIN
            INSERT INTO seguros_extra (
                poliza_id,
                beneficiario_id,
                vigencia_desde,
                vigencia_hasta,
                cobertura
            )
            VALUES (
                @nro_poliza,
                @cliente_ganador_id,
                CAST(GETDATE() AS DATE),
                DATEADD(YEAR, 1, CAST(GETDATE() AS DATE)),
                N'Cobertura asociada a compra ganada en subasta'
            );
        END;
    END;

    UPDATE itemsCatalogo
    SET subastado = 'si'
    WHERE identificador = @item_id_cierre;

    UPDATE subastas_extra
    SET item_actual_id = NULL
    WHERE subasta_id = @subasta_id_cerrar_item;

    INSERT INTO chat_mensajes (cliente_id, tipo, emisor, contenido, timestamp_msg, leido)
    VALUES (
        @cliente_ganador_id,
        'compra',
        'sistema',
        N'¡Ganaste el ítem "' + @nombre_item + N'"!' + CHAR(10)
            + N'Importe ofertado: $' + CAST(@importe_ganador AS NVARCHAR(30)) + CHAR(10)
            + N'Comisión: $' + CAST(@comision_item AS NVARCHAR(30)) + CHAR(10)
            + N'Total a pagar: $' + CAST((@importe_ganador + @comision_item + ISNULL(@costo_envio, 0)) AS NVARCHAR(30)) + CHAR(10)
            + N'Podés regularizar tu pago desde Mis compras.',
        GETDATE(),
        0
    );

    COMMIT TRANSACTION;

    SELECT
        'OK' AS resultado,
        @subasta_id_cerrar_item AS subasta_id,
        @item_id_cierre AS item_cerrado,
        @pujo_ganador_id AS puja_ganadora_id,
        @cliente_ganador_id AS cliente_ganador_id,
        @registro_id AS compra_registro_id,
        @importe_ganador AS importe,
        @comision_item AS comision,
        @medio_pago_id AS medio_pago_id;

END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    DECLARE @err NVARCHAR(4000) = ERROR_MESSAGE();
    THROW 51499, @err, 1;
END CATCH;
GO

-- ============================================================
-- SCRIPT 08 - Setear siguiente ítem disponible como ítem actual
-- ============================================================
-- Usar después de cerrar un ítem para seguir con la subasta.
-- ============================================================

DECLARE @subasta_id_siguiente INT = 1; -- REEMPLAZAR
DECLARE @siguiente_item_id INT;

SELECT TOP 1 @siguiente_item_id = ic.identificador
FROM itemsCatalogo ic
INNER JOIN catalogos c ON c.identificador = ic.catalogo
WHERE c.subasta = @subasta_id_siguiente
  AND ISNULL(ic.subastado, 'no') <> 'si'
ORDER BY ic.identificador ASC;

IF @siguiente_item_id IS NULL
BEGIN
    THROW 51501, 'No quedan ítems disponibles para esta subasta.', 1;
END;

UPDATE subastas_extra
SET item_actual_id = @siguiente_item_id
WHERE subasta_id = @subasta_id_siguiente;

SELECT
    se.subasta_id,
    se.item_actual_id,
    ic.precioBase,
    ic.comision,
    p.descripcionCatalogo
FROM subastas_extra se
INNER JOIN itemsCatalogo ic ON ic.identificador = se.item_actual_id
INNER JOIN productos p ON p.identificador = ic.producto
WHERE se.subasta_id = @subasta_id_siguiente;
GO

-- ============================================================
-- SCRIPT 09 - Cerrar subasta completa
-- ============================================================

DECLARE @subasta_id_cerrar INT = 1; -- REEMPLAZAR

IF NOT EXISTS (SELECT 1 FROM subastas WHERE identificador = @subasta_id_cerrar)
BEGIN
    THROW 51601, 'Subasta no encontrada.', 1;
END;

UPDATE subastas
SET estado = 'cerrada'
WHERE identificador = @subasta_id_cerrar;

IF EXISTS (SELECT 1 FROM subastas_extra WHERE subasta_id = @subasta_id_cerrar)
BEGIN
    UPDATE subastas_extra
    SET item_actual_id = NULL
    WHERE subasta_id = @subasta_id_cerrar;
END;

SELECT
    s.identificador,
    s.estado,
    se.item_actual_id
FROM subastas s
LEFT JOIN subastas_extra se ON se.subasta_id = s.identificador
WHERE s.identificador = @subasta_id_cerrar;
GO

-- ============================================================
-- SCRIPT 10 - Reabrir ítem para repetir prueba
-- ============================================================
-- Por defecto NO borra pujas ni compras; solo desmarca subastado y setea ítem actual.
-- Si @borrar_pujas_y_compra = 1, borra pujas/compra asociadas al ítem.
-- ============================================================

DECLARE @subasta_id_reabrir INT = 1; -- REEMPLAZAR
DECLARE @item_id_reabrir INT = 1;    -- REEMPLAZAR
DECLARE @borrar_pujas_y_compra BIT = 0;

BEGIN TRY
    BEGIN TRANSACTION;

    IF @borrar_pujas_y_compra = 1
    BEGIN
        DELETE ce
        FROM compras_extra ce
        INNER JOIN registroDeSubasta r ON r.identificador = ce.registro_id
        INNER JOIN itemsCatalogo ic ON ic.producto = r.producto
        WHERE ic.identificador = @item_id_reabrir
          AND r.subasta = @subasta_id_reabrir;

        DELETE r
        FROM registroDeSubasta r
        INNER JOIN itemsCatalogo ic ON ic.producto = r.producto
        WHERE ic.identificador = @item_id_reabrir
          AND r.subasta = @subasta_id_reabrir;

        DELETE pe
        FROM pujos_extra pe
        INNER JOIN pujos p ON p.identificador = pe.pujo_id
        WHERE p.item = @item_id_reabrir;

        DELETE FROM pujos
        WHERE item = @item_id_reabrir;
    END;

    UPDATE itemsCatalogo
    SET subastado = 'no'
    WHERE identificador = @item_id_reabrir;

    UPDATE subastas
    SET estado = 'abierta',
        fecha = CAST(GETDATE() AS DATE),
        hora = CAST(DATEADD(HOUR, -1, GETDATE()) AS TIME)
    WHERE identificador = @subasta_id_reabrir;

    UPDATE subastas_extra
    SET item_actual_id = @item_id_reabrir
    WHERE subasta_id = @subasta_id_reabrir;

    COMMIT TRANSACTION;

    SELECT
        s.identificador AS subasta_id,
        s.estado,
        se.item_actual_id,
        ic.subastado
    FROM subastas s
    INNER JOIN subastas_extra se ON se.subasta_id = s.identificador
    INNER JOIN itemsCatalogo ic ON ic.identificador = se.item_actual_id
    WHERE s.identificador = @subasta_id_reabrir;

END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    DECLARE @err2 NVARCHAR(4000) = ERROR_MESSAGE();
    THROW 51799, @err2, 1;
END CATCH;
GO

-- ============================================================
-- SCRIPT 11 - Generar multa administrativa sobre una compra
-- ============================================================
-- Después validar:
-- GET /usuarios/me/estado-cuenta
-- POST /subastas/{id}/pujas
-- ============================================================

DECLARE @registro_id_multa INT = 1; -- REEMPLAZAR por registroDeSubasta.identificador
DECLARE @porcentaje_multa DECIMAL(5,2) = 10.00;

DECLARE @cliente_id_multa INT;
DECLARE @importe_compra DECIMAL(18,2);
DECLARE @monto_multa DECIMAL(18,2);

SELECT
    @cliente_id_multa = cliente,
    @importe_compra = importe
FROM registroDeSubasta
WHERE identificador = @registro_id_multa;

IF @cliente_id_multa IS NULL
BEGIN
    THROW 51801, 'Compra/registro no encontrado.', 1;
END;

SET @monto_multa = ROUND(@importe_compra * (@porcentaje_multa / 100.0), 2);

IF NOT EXISTS (
    SELECT 1
    FROM multas
    WHERE registro_id = @registro_id_multa
      AND estado = 'pendiente'
)
BEGIN
    INSERT INTO multas (cliente_id, registro_id, monto, estado, creado_en)
    VALUES (@cliente_id_multa, @registro_id_multa, @monto_multa, 'pendiente', GETDATE());

    INSERT INTO chat_mensajes (cliente_id, tipo, emisor, contenido, timestamp_msg, leido)
    VALUES (
        @cliente_id_multa,
        'multa',
        'sistema',
        N'Se te aplicó una multa de $' + CAST(@monto_multa AS NVARCHAR(30)) + N'.' + CHAR(10)
            + N'Motivo: incumplimiento de pago.' + CHAR(10)
            + N'Debés regularizarla antes de participar en otra subasta.',
        GETDATE(),
        0
    );
END;

SELECT *
FROM multas
WHERE cliente_id = @cliente_id_multa
ORDER BY id DESC;
GO

-- ============================================================
-- SCRIPT 12 - Marcar multas como pagadas por email
-- ============================================================
-- Sirve para volver a habilitar al usuario para pujar.
-- ============================================================

DECLARE @email_limpiar_multas VARCHAR(255) = 'REEMPLAZAR_EMAIL_DEL_USUARIO';

UPDATE m
SET estado = 'pagada'
FROM multas m
INNER JOIN credenciales c ON c.persona_id = m.cliente_id
WHERE c.email = @email_limpiar_multas
  AND m.estado = 'pendiente';

SELECT m.*
FROM multas m
INNER JOIN credenciales c ON c.persona_id = m.cliente_id
WHERE c.email = @email_limpiar_multas
ORDER BY m.id DESC;
GO

-- ============================================================
-- SCRIPT 13 - Cambiar estado de entrega/pago de una compra
-- ============================================================
-- estado_pago: pendiente | pagado
-- estado_entrega: coordinando | en_camino | entregado | listo_para_retirar
-- ============================================================

DECLARE @registro_id_estado_compra INT = 1; -- REEMPLAZAR
DECLARE @estado_pago_nuevo VARCHAR(20) = 'pendiente';
DECLARE @estado_entrega_nuevo VARCHAR(30) = 'coordinando';

IF @estado_pago_nuevo NOT IN ('pendiente', 'pagado')
BEGIN
    THROW 51901, 'estado_pago inválido.', 1;
END;

IF @estado_entrega_nuevo NOT IN ('coordinando', 'en_camino', 'entregado', 'listo_para_retirar')
BEGIN
    THROW 51902, 'estado_entrega inválido.', 1;
END;

IF NOT EXISTS (SELECT 1 FROM compras_extra WHERE registro_id = @registro_id_estado_compra)
BEGIN
    THROW 51903, 'No existe compras_extra para ese registro.', 1;
END;

UPDATE compras_extra
SET estado_pago = @estado_pago_nuevo,
    estado_entrega = @estado_entrega_nuevo
WHERE registro_id = @registro_id_estado_compra;

SELECT *
FROM compras_extra
WHERE registro_id = @registro_id_estado_compra;
GO

-- ============================================================
-- SCRIPT 14 - Consultas útiles de diagnóstico
-- ============================================================

-- Subastas y vivo
SELECT
    s.identificador AS subasta_id,
    s.estado,
    s.fecha,
    s.hora,
    s.categoria,
    se.nombre,
    se.moneda,
    se.item_actual_id
FROM subastas s
LEFT JOIN subastas_extra se ON se.subasta_id = s.identificador
ORDER BY s.identificador DESC;

-- Ítems por subasta
SELECT
    s.identificador AS subasta_id,
    se.nombre AS subasta_nombre,
    c.identificador AS catalogo_id,
    ic.identificador AS item_id,
    p.identificador AS producto_id,
    p.descripcionCatalogo,
    ic.precioBase,
    ic.comision,
    ic.subastado
FROM subastas s
LEFT JOIN subastas_extra se ON se.subasta_id = s.identificador
INNER JOIN catalogos c ON c.subasta = s.identificador
INNER JOIN itemsCatalogo ic ON ic.catalogo = c.identificador
INNER JOIN productos p ON p.identificador = ic.producto
ORDER BY s.identificador DESC, ic.identificador ASC;

-- Medios de pago por usuario
SELECT
    c.email,
    cl.identificador AS cliente_id,
    cl.categoria,
    cl.admitido,
    mp.id AS medio_pago_id,
    mp.tipo,
    mp.verificado,
    mp.eliminado,
    mp.descripcion
FROM credenciales c
INNER JOIN clientes cl ON cl.identificador = c.persona_id
LEFT JOIN medios_pago mp ON mp.cliente_id = cl.identificador
ORDER BY c.email, mp.id;

-- Pujas por ítem
SELECT
    s.identificador AS subasta_id,
    ic.identificador AS item_id,
    p.identificador AS puja_id,
    per.nombre AS postor,
    p.importe,
    p.ganador,
    pe.timestamp_puja,
    pe.medio_pago_id
FROM pujos p
INNER JOIN asistentes a ON a.identificador = p.asistente
INNER JOIN clientes cl ON cl.identificador = a.cliente
INNER JOIN personas per ON per.identificador = cl.identificador
INNER JOIN itemsCatalogo ic ON ic.identificador = p.item
INNER JOIN catalogos c ON c.identificador = ic.catalogo
INNER JOIN subastas s ON s.identificador = c.subasta
LEFT JOIN pujos_extra pe ON pe.pujo_id = p.identificador
ORDER BY s.identificador DESC, ic.identificador, p.importe DESC;

-- Compras
SELECT
    r.identificador AS registro_id,
    r.subasta,
    r.producto,
    r.cliente,
    r.importe,
    r.comision,
    ce.estado_pago,
    ce.estado_entrega,
    ce.medio_pago_id,
    ce.costo_envio
FROM registroDeSubasta r
LEFT JOIN compras_extra ce ON ce.registro_id = r.identificador
ORDER BY r.identificador DESC;
GO
