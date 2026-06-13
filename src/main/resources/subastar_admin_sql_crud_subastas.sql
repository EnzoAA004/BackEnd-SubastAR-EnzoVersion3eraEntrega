-- ============================================================
-- SubastAR - Script administrativo SQL Server
-- CRUD manual de subastas, catalogos e items
--
-- PROPOSITO:
-- - Permitir a un administrador operar subastas desde SQL Server.
-- - Servir como apoyo local/test para crear, consultar, actualizar,
--   abrir, cerrar y verificar subastas.
--
-- IMPORTANTE:
-- - Este archivo NO es una migracion Flyway.
-- - Este archivo NO cambia el modelo de datos.
-- - No crea tablas, no agrega columnas y no modifica constraints.
-- - Ejecutar primero en entorno local/test.
-- - Si se usa en una base compartida, hacer backup antes.
-- - Revisar cada bloque y ejecutar solo el bloque necesario.
--
-- NOTA SOBRE ESTADOS:
-- - El esquema actual de subastas usa estados compatibles con el CHECK
--   existente: 'abierta' y 'cerrada' (grafia heredada del schema).
-- - No se usa 'finalizada' ni 'cancelada' porque eso requeriria cambiar
--   el modelo/constraint de base de datos.
-- ============================================================

USE subastar_db;
GO

SET NOCOUNT ON;
GO

-- ============================================================
-- 00 - Consultas de apoyo
-- ============================================================
-- Objetivo:
-- - Ver IDs disponibles antes de ejecutar bloques de alta/update.
--
-- Cuando usarlo:
-- - Antes de completar variables como REEMPLAZAR_ID_SUBASTADOR,
--   REEMPLAZAR_ID_RESPONSABLE, REEMPLAZAR_ID_PRODUCTO o REEMPLAZAR_ID_SUBASTA.
--
-- Resultado esperado:
-- - Listados de subastadores, responsables, productos, clientes/duenios
--   y subastas existentes.
-- ============================================================

SELECT
    s.identificador AS subastador_id,
    p.nombre,
    p.documento,
    s.matricula,
    s.region
FROM dbo.subastadores s
LEFT JOIN dbo.personas p ON p.identificador = s.identificador
ORDER BY s.identificador;

SELECT
    e.identificador AS responsable_id,
    p.nombre,
    p.documento,
    e.cargo,
    e.sector
FROM dbo.empleados e
LEFT JOIN dbo.personas p ON p.identificador = e.identificador
ORDER BY e.identificador;

SELECT
    pr.identificador AS producto_id,
    pr.descripcionCatalogo,
    pr.descripcionCompleta,
    pr.disponible,
    pr.duenio,
    pd.precio_base_sugerido
FROM dbo.productos pr
LEFT JOIN dbo.productos_detalle pd ON pd.producto_id = pr.identificador
ORDER BY pr.identificador DESC;

SELECT
    c.identificador AS cliente_id,
    p.nombre,
    p.documento,
    c.admitido,
    c.categoria
FROM dbo.clientes c
LEFT JOIN dbo.personas p ON p.identificador = c.identificador
ORDER BY c.identificador;

SELECT
    d.identificador AS duenio_id,
    p.nombre,
    p.documento,
    d.verificacionFinanciera,
    d.verificacionJudicial,
    d.calificacionRiesgo
FROM dbo.duenios d
LEFT JOIN dbo.personas p ON p.identificador = d.identificador
ORDER BY d.identificador;

SELECT
    su.identificador AS subasta_id,
    se.nombre,
    su.fecha,
    su.hora,
    su.estado,
    su.ubicacion,
    su.categoria,
    se.moneda,
    se.url_streaming,
    se.item_actual_id,
    COUNT(ic.identificador) AS cantidad_items
FROM dbo.subastas su
LEFT JOIN dbo.subastas_extra se ON se.subasta_id = su.identificador
LEFT JOIN dbo.catalogos ca ON ca.subasta = su.identificador
LEFT JOIN dbo.itemsCatalogo ic ON ic.catalogo = ca.identificador
GROUP BY
    su.identificador, se.nombre, su.fecha, su.hora, su.estado,
    su.ubicacion, su.categoria, se.moneda, se.url_streaming, se.item_actual_id
ORDER BY su.identificador DESC;
GO

-- ============================================================
-- 01 - CREATE de subasta completa con catalogo e items existentes
-- ============================================================
-- Objetivo:
-- - Crear una subasta nueva.
-- - Crear su fila complementaria en subastas_extra.
-- - Crear un catalogo asociado a la subasta.
-- - Agregar automaticamente bienes/productos existentes al catalogo.
--
-- Importante:
-- - Los bienes subastables se toman desde dbo.productos.
-- - Los items del catalogo se crean en dbo.itemsCatalogo.
-- - Si queres usar IDs especificos, completar las variables marcadas.
-- ============================================================

DECLARE @crear_nombre VARCHAR(255) = 'Subasta de bienes existentes';
DECLARE @crear_fecha DATE = '2026-12-20';
DECLARE @crear_hora TIME = '15:00:00';
DECLARE @crear_estado VARCHAR(255) = 'cerrada'; -- usar 'cerrada' o 'abierta'
DECLARE @crear_subastador_id INT = NULL; -- opcional: poner un ID de dbo.subastadores.identificador
DECLARE @crear_ubicacion VARCHAR(255) = 'Buenos Aires, Argentina';
DECLARE @crear_capacidad INT = 100;
DECLARE @crear_tiene_deposito VARCHAR(255) = 'si'; -- si | no
DECLARE @crear_seguridad_propia VARCHAR(255) = 'si'; -- si | no
DECLARE @crear_categoria VARCHAR(255) = 'oro'; -- comun | especial | plata | oro | platino
DECLARE @crear_moneda VARCHAR(10) = 'ARS'; -- ARS | USD
DECLARE @crear_url_streaming VARCHAR(500) = NULL;
DECLARE @crear_responsable_id INT = NULL; -- opcional: poner un ID de dbo.empleados.identificador
DECLARE @crear_catalogo_descripcion VARCHAR(255) = 'Catalogo - Subasta de bienes existentes';

-- Cantidad de bienes/productos existentes que se quieren agregar al catalogo.
DECLARE @cantidad_productos_catalogo INT = 3;

-- Valores por defecto si el producto no tiene precio sugerido.
DECLARE @precio_base_default DECIMAL(18,2) = 1000.00;
DECLARE @comision_default DECIMAL(18,2) = 100.00;

BEGIN TRY
    BEGIN TRAN;

    -- Si no se completo subastador, toma el primero existente.
    IF @crear_subastador_id IS NULL
        BEGIN
            SELECT TOP 1
                @crear_subastador_id = identificador
            FROM dbo.subastadores
            ORDER BY identificador;
        END;

    IF @crear_subastador_id IS NULL
        THROW 52001, 'No hay subastadores cargados. Crear un subastador o completar @crear_subastador_id.', 1;

    IF NOT EXISTS (SELECT 1 FROM dbo.subastadores WHERE identificador = @crear_subastador_id)
        THROW 52002, 'El subastador indicado no existe.', 1;

    -- Si no se completo responsable, toma el primer empleado existente.
    IF @crear_responsable_id IS NULL
        BEGIN
            SELECT TOP 1
                @crear_responsable_id = identificador
            FROM dbo.empleados
            ORDER BY identificador;
        END;

    IF @crear_responsable_id IS NULL
        THROW 52003, 'No hay empleados cargados para responsable de catalogo. Crear un empleado o completar @crear_responsable_id.', 1;

    IF NOT EXISTS (SELECT 1 FROM dbo.empleados WHERE identificador = @crear_responsable_id)
        THROW 52004, 'El responsable indicado no existe.', 1;

    IF @cantidad_productos_catalogo IS NULL OR @cantidad_productos_catalogo <= 0
        THROW 52005, 'Completar @cantidad_productos_catalogo con un valor mayor a 0.', 1;

    IF NOT EXISTS (
        SELECT 1
        FROM dbo.productos pr
        WHERE NOT EXISTS (
            SELECT 1
            FROM dbo.itemsCatalogo ic
            WHERE ic.producto = pr.identificador
        )
    )
        THROW 52006, 'No hay productos disponibles para agregar al catalogo. Cargar bienes/productos primero o revisar si ya estan todos asignados.', 1;

    INSERT INTO dbo.subastas (
        fecha,
        hora,
        estado,
        subastador,
        ubicacion,
        capacidadAsistentes,
        tieneDeposito,
        seguridadPropia,
        categoria
    )
    VALUES (
               @crear_fecha,
               @crear_hora,
               @crear_estado,
               @crear_subastador_id,
               @crear_ubicacion,
               @crear_capacidad,
               @crear_tiene_deposito,
               @crear_seguridad_propia,
               @crear_categoria
           );

    DECLARE @nueva_subasta_id INT = CONVERT(INT, SCOPE_IDENTITY());

    INSERT INTO dbo.subastas_extra (
        subasta_id,
        nombre,
        moneda,
        url_streaming,
        item_actual_id
    )
    VALUES (
               @nueva_subasta_id,
               @crear_nombre,
               @crear_moneda,
               @crear_url_streaming,
               NULL
           );

    INSERT INTO dbo.catalogos (
        descripcion,
        subasta,
        responsable
    )
    VALUES (
               @crear_catalogo_descripcion,
               @nueva_subasta_id,
               @crear_responsable_id
           );

    DECLARE @nuevo_catalogo_id INT = CONVERT(INT, SCOPE_IDENTITY());

    INSERT INTO dbo.itemsCatalogo (
        catalogo,
        producto,
        precioBase,
        comision,
        subastado
    )
    SELECT TOP (@cantidad_productos_catalogo)
        @nuevo_catalogo_id AS catalogo,
        pr.identificador AS producto,
        COALESCE(pd.precio_base_sugerido, @precio_base_default) AS precioBase,
        @comision_default AS comision,
        'no' AS subastado
    FROM dbo.productos pr
             LEFT JOIN dbo.productos_detalle pd ON pd.producto_id = pr.identificador
    WHERE NOT EXISTS (
        SELECT 1
        FROM dbo.itemsCatalogo ic
        WHERE ic.producto = pr.identificador
    )
    ORDER BY pr.identificador DESC;

    IF NOT EXISTS (
        SELECT 1
        FROM dbo.itemsCatalogo
        WHERE catalogo = @nuevo_catalogo_id
    )
        THROW 52007, 'No se pudo agregar ningun producto al catalogo.', 1;

    DECLARE @primer_item_id INT;

    SELECT TOP 1
        @primer_item_id = identificador
    FROM dbo.itemsCatalogo
    WHERE catalogo = @nuevo_catalogo_id
    ORDER BY identificador;

    UPDATE dbo.subastas_extra
    SET item_actual_id = @primer_item_id
    WHERE subasta_id = @nueva_subasta_id;

    COMMIT;

    SELECT
        su.identificador AS subasta_id,
        se.nombre,
        su.fecha,
        su.hora,
        su.estado,
        su.ubicacion,
        su.capacidadAsistentes,
        su.categoria,
        su.subastador AS subastador_id,
        se.moneda,
        se.url_streaming,
        se.item_actual_id,
        ca.identificador AS catalogo_id,
        ca.descripcion AS catalogo_descripcion,
        ca.responsable AS responsable_id
    FROM dbo.subastas su
             INNER JOIN dbo.subastas_extra se ON se.subasta_id = su.identificador
             INNER JOIN dbo.catalogos ca ON ca.subasta = su.identificador
    WHERE su.identificador = @nueva_subasta_id;

    SELECT
        ic.identificador AS item_id,
        ic.catalogo AS catalogo_id,
        ic.producto AS producto_id,
        pr.descripcionCatalogo,
        pr.descripcionCompleta,
        ic.precioBase,
        ic.comision,
        ic.subastado
    FROM dbo.itemsCatalogo ic
             INNER JOIN dbo.productos pr ON pr.identificador = ic.producto
    WHERE ic.catalogo = @nuevo_catalogo_id
    ORDER BY ic.identificador;

END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
GO

-- ============================================================
-- 02 - READ/LIST de subastas
-- ============================================================
-- Objetivo:
-- - Consultar subastas por listado general, estado o ID.
--
-- Cuando usarlo:
-- - Para verificar datos antes/despues de operaciones administrativas.
--
-- Variables:
-- - @read_estado: dejar NULL para no filtrar por estado.
-- - @read_subasta_id: dejar NULL para listar todas.
-- ============================================================

DECLARE @read_estado VARCHAR(255) = NULL; -- 'abierta', 'cerrada' o NULL
DECLARE @read_subasta_id INT = NULL; -- REEMPLAZAR_ID_SUBASTA o NULL

SELECT
    su.identificador AS subasta_id,
    se.nombre,
    su.fecha,
    su.hora,
    su.estado,
    su.ubicacion,
    su.categoria,
    se.moneda,
    sp.identificador AS subastador_id,
    ps.nombre AS subastador_nombre,
    se.url_streaming,
    se.item_actual_id,
    COUNT(ic.identificador) AS cantidad_items
FROM dbo.subastas su
LEFT JOIN dbo.subastas_extra se ON se.subasta_id = su.identificador
LEFT JOIN dbo.subastadores sp ON sp.identificador = su.subastador
LEFT JOIN dbo.personas ps ON ps.identificador = sp.identificador
LEFT JOIN dbo.catalogos ca ON ca.subasta = su.identificador
LEFT JOIN dbo.itemsCatalogo ic ON ic.catalogo = ca.identificador
WHERE (@read_estado IS NULL OR su.estado = @read_estado)
  AND (@read_subasta_id IS NULL OR su.identificador = @read_subasta_id)
GROUP BY
    su.identificador, se.nombre, su.fecha, su.hora, su.estado,
    su.ubicacion, su.categoria, se.moneda, sp.identificador,
    ps.nombre, se.url_streaming, se.item_actual_id
ORDER BY su.identificador DESC;
GO

-- ============================================================
-- 03 - UPDATE de datos basicos
-- ============================================================
-- Objetivo:
-- - Actualizar campos administrativos de subasta y subastas_extra.
--
-- Cuando usarlo:
-- - Para corregir fecha, hora, estado, nombre, moneda, streaming o datos basicos.
--
-- Variables:
-- - Completar @upd_subasta_id.
-- - Dejar NULL en cualquier campo que NO se quiera modificar.
--
-- Validaciones:
-- - La subasta debe existir.
--
-- Resultado esperado:
-- - SELECT final con la subasta actualizada.
-- ============================================================

DECLARE @upd_subasta_id INT = 4; -- REEMPLAZAR_ID_SUBASTA
DECLARE @upd_nombre VARCHAR(255) = NULL;
DECLARE @upd_fecha DATE = '2026-06-11';
DECLARE @upd_hora TIME = '01:12:00';
DECLARE @upd_estado VARCHAR(255) = 'abierta'; -- 'abierta' | 'cerrada' | NULL
DECLARE @upd_ubicacion VARCHAR(255) = NULL;
DECLARE @upd_capacidad INT = NULL;
DECLARE @upd_categoria VARCHAR(255) = NULL;
DECLARE @upd_moneda VARCHAR(10) = NULL;
DECLARE @upd_url_streaming VARCHAR(500) = NULL;

BEGIN TRY
    BEGIN TRAN;

    IF @upd_subasta_id IS NULL
        THROW 52101, 'Completar @upd_subasta_id.', 1;

    IF NOT EXISTS (SELECT 1 FROM dbo.subastas WHERE identificador = @upd_subasta_id)
        THROW 52102, 'La subasta indicada no existe.', 1;

    UPDATE dbo.subastas
    SET
        fecha = COALESCE(@upd_fecha, fecha),
        hora = COALESCE(@upd_hora, hora),
        estado = COALESCE(@upd_estado, estado),
        ubicacion = COALESCE(@upd_ubicacion, ubicacion),
        capacidadAsistentes = COALESCE(@upd_capacidad, capacidadAsistentes),
        categoria = COALESCE(@upd_categoria, categoria)
    WHERE identificador = @upd_subasta_id;

    UPDATE dbo.subastas_extra
    SET
        nombre = COALESCE(@upd_nombre, nombre),
        moneda = COALESCE(@upd_moneda, moneda),
        url_streaming = COALESCE(@upd_url_streaming, url_streaming)
    WHERE subasta_id = @upd_subasta_id;

    COMMIT;

    SELECT
        su.identificador AS subasta_id,
        se.nombre,
        su.fecha,
        su.hora,
        su.estado,
        su.ubicacion,
        su.capacidadAsistentes,
        su.categoria,
        se.moneda,
        se.url_streaming
    FROM dbo.subastas su
    LEFT JOIN dbo.subastas_extra se ON se.subasta_id = su.identificador
    WHERE su.identificador = @upd_subasta_id;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
GO

-- ============================================================
-- 04 - Agregar producto al catalogo de una subasta
-- ============================================================
-- Objetivo:
-- - Insertar un item de catalogo asociado a una subasta existente.
--
-- Cuando usarlo:
-- - Despues de crear una subasta/catalogo y antes de abrirla.
--
-- Variables:
-- - @item_subasta_id: REEMPLAZAR_ID_SUBASTA
-- - @item_producto_id: REEMPLAZAR_ID_PRODUCTO
-- - @item_precio_base / @item_comision: valores positivos.
--
-- Validaciones:
-- - Subasta existente.
-- - Producto existente.
-- - Catalogo existente para la subasta.
-- - Evita duplicar el mismo producto en el mismo catalogo.
--
-- Resultado esperado:
-- - Nuevo item con subastado = 'no'.
-- ============================================================

DECLARE @item_subasta_id INT = NULL; -- REEMPLAZAR_ID_SUBASTA
DECLARE @item_producto_id INT = NULL; -- REEMPLAZAR_ID_PRODUCTO
DECLARE @item_precio_base DECIMAL(18,2) = 1000.00;
DECLARE @item_comision DECIMAL(18,2) = 100.00;

BEGIN TRY
    BEGIN TRAN;

    IF @item_subasta_id IS NULL
        THROW 52201, 'Completar @item_subasta_id.', 1;

    IF @item_producto_id IS NULL
        THROW 52202, 'Completar @item_producto_id.', 1;

    IF NOT EXISTS (SELECT 1 FROM dbo.subastas WHERE identificador = @item_subasta_id)
        THROW 52203, 'La subasta indicada no existe.', 1;

    IF NOT EXISTS (SELECT 1 FROM dbo.productos WHERE identificador = @item_producto_id)
        THROW 52204, 'El producto indicado no existe.', 1;

    DECLARE @item_catalogo_id INT;

    SELECT TOP 1 @item_catalogo_id = identificador
    FROM dbo.catalogos
    WHERE subasta = @item_subasta_id
    ORDER BY identificador;

    IF @item_catalogo_id IS NULL
        THROW 52205, 'La subasta no tiene catalogo. Crear catalogo con la seccion 01 o insertar uno manualmente.', 1;

    IF EXISTS (
        SELECT 1
        FROM dbo.itemsCatalogo
        WHERE catalogo = @item_catalogo_id
          AND producto = @item_producto_id
    )
        THROW 52206, 'Ese producto ya existe en el catalogo de la subasta.', 1;

    INSERT INTO dbo.itemsCatalogo (
        catalogo,
        producto,
        precioBase,
        comision,
        subastado
    )
    VALUES (
        @item_catalogo_id,
        @item_producto_id,
        @item_precio_base,
        @item_comision,
        'no'
    );

    DECLARE @nuevo_item_id INT = CONVERT(INT, SCOPE_IDENTITY());

    COMMIT;

    SELECT
        ic.identificador AS item_id,
        ic.catalogo,
        ic.producto,
        pr.descripcionCatalogo,
        ic.precioBase,
        ic.comision,
        ic.subastado
    FROM dbo.itemsCatalogo ic
    INNER JOIN dbo.productos pr ON pr.identificador = ic.producto
    WHERE ic.identificador = @nuevo_item_id;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
GO

-- ============================================================
-- 05 - Setear item actual de subasta en vivo
-- ============================================================
-- Objetivo:
-- - Actualizar subastas_extra.item_actual_id.
--
-- Cuando usarlo:
-- - Antes o durante una subasta abierta para indicar el lote actual.
--
-- Validaciones:
-- - Subasta existente.
-- - Item existente.
-- - Item pertenece al catalogo de la subasta.
-- - Estado compatible: 'abierta'.
--
-- Resultado esperado:
-- - item_actual_id actualizado.
-- ============================================================

DECLARE @actual_subasta_id INT = 4; -- REEMPLAZAR_ID_SUBASTA
DECLARE @actual_item_id INT = 6; -- REEMPLAZAR_ID_ITEM_CATALOGO

BEGIN TRY
    BEGIN TRAN;

    IF @actual_subasta_id IS NULL
        THROW 52301, 'Completar @actual_subasta_id.', 1;

    IF @actual_item_id IS NULL
        THROW 52302, 'Completar @actual_item_id.', 1;

    IF NOT EXISTS (SELECT 1 FROM dbo.subastas WHERE identificador = @actual_subasta_id)
        THROW 52303, 'La subasta indicada no existe.', 1;

    IF NOT EXISTS (SELECT 1 FROM dbo.itemsCatalogo WHERE identificador = @actual_item_id)
        THROW 52304, 'El item indicado no existe.', 1;

    IF NOT EXISTS (
        SELECT 1
        FROM dbo.itemsCatalogo ic
        INNER JOIN dbo.catalogos ca ON ca.identificador = ic.catalogo
        WHERE ic.identificador = @actual_item_id
          AND ca.subasta = @actual_subasta_id
    )
        THROW 52305, 'El item no pertenece al catalogo de la subasta indicada.', 1;

    IF NOT EXISTS (
        SELECT 1
        FROM dbo.subastas
        WHERE identificador = @actual_subasta_id
          AND estado = 'abierta'
    )
        THROW 52306, 'Para setear item actual, la subasta debe estar en estado abierta.', 1;

    UPDATE dbo.subastas_extra
    SET item_actual_id = @actual_item_id
    WHERE subasta_id = @actual_subasta_id;

    COMMIT;

    SELECT
        su.identificador AS subasta_id,
        se.nombre,
        su.estado,
        se.item_actual_id,
        ic.producto,
        pr.descripcionCatalogo
    FROM dbo.subastas su
    INNER JOIN dbo.subastas_extra se ON se.subasta_id = su.identificador
    LEFT JOIN dbo.itemsCatalogo ic ON ic.identificador = se.item_actual_id
    LEFT JOIN dbo.productos pr ON pr.identificador = ic.producto
    WHERE su.identificador = @actual_subasta_id;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
GO

-- ============================================================
-- 06 - Abrir subasta
-- ============================================================
-- Objetivo:
-- - Cambiar estado a 'abierta'.
--
-- Cuando usarlo:
-- - Cuando la subasta ya tiene catalogo, items y item actual.
--
-- Validaciones:
-- - Subasta existente.
-- - Tiene catalogo.
-- - Tiene al menos un item.
-- - Tiene item_actual_id seteado.
--
-- Resultado esperado:
-- - La API REST de pujas queda habilitada para esa subasta.
-- ============================================================

DECLARE @abrir_subasta_id INT = 4; -- REEMPLAZAR_ID_SUBASTA

BEGIN TRY
    BEGIN TRAN;

    IF @abrir_subasta_id IS NULL
        THROW 52401, 'Completar @abrir_subasta_id.', 1;

    IF NOT EXISTS (SELECT 1 FROM dbo.subastas WHERE identificador = @abrir_subasta_id)
        THROW 52402, 'La subasta indicada no existe.', 1;

    IF NOT EXISTS (SELECT 1 FROM dbo.catalogos WHERE subasta = @abrir_subasta_id)
        THROW 52403, 'La subasta no tiene catalogo.', 1;

    IF NOT EXISTS (
        SELECT 1
        FROM dbo.itemsCatalogo ic
        INNER JOIN dbo.catalogos ca ON ca.identificador = ic.catalogo
        WHERE ca.subasta = @abrir_subasta_id
    )
        THROW 52404, 'La subasta no tiene items de catalogo.', 1;

    IF NOT EXISTS (
        SELECT 1
        FROM dbo.subastas_extra
        WHERE subasta_id = @abrir_subasta_id
          AND item_actual_id IS NOT NULL
    )
        THROW 52405, 'La subasta no tiene item_actual_id. Usar seccion 05 antes de abrir.', 1;

    UPDATE dbo.subastas
    SET estado = 'abierta'
    WHERE identificador = @abrir_subasta_id;

    COMMIT;

    SELECT
        su.identificador AS subasta_id,
        se.nombre,
        su.estado,
        se.item_actual_id
    FROM dbo.subastas su
    LEFT JOIN dbo.subastas_extra se ON se.subasta_id = su.identificador
    WHERE su.identificador = @abrir_subasta_id;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
GO

-- ============================================================
-- 07 - Cerrar/finalizar subasta
-- ============================================================
-- Objetivo:
-- - Cambiar estado a 'cerrada' para cerrar administrativamente.
--
-- Cuando usarlo:
-- - Al finalizar una subasta.
--
-- Validaciones:
-- - Subasta existente.
-- - Evita cerrar dos veces.
--
-- Nota:
-- - No se limpia item_actual_id para no romper flujos existentes.
-- ============================================================

DECLARE @cerrar_subasta_id INT = 4; -- REEMPLAZAR_ID_SUBASTA

BEGIN TRY
    BEGIN TRAN;

    IF @cerrar_subasta_id IS NULL
        THROW 52501, 'Completar @cerrar_subasta_id.', 1;

    IF NOT EXISTS (SELECT 1 FROM dbo.subastas WHERE identificador = @cerrar_subasta_id)
        THROW 52502, 'La subasta indicada no existe.', 1;

    IF EXISTS (
        SELECT 1
        FROM dbo.subastas
        WHERE identificador = @cerrar_subasta_id
          AND estado = 'cerrada'
    )
        THROW 52503, 'La subasta ya esta cerrada.', 1;

    UPDATE dbo.subastas
    SET estado = 'cerrada'
    WHERE identificador = @cerrar_subasta_id;

    COMMIT;

    SELECT
        su.identificador AS subasta_id,
        se.nombre,
        su.estado,
        se.item_actual_id
    FROM dbo.subastas su
    LEFT JOIN dbo.subastas_extra se ON se.subasta_id = su.identificador
    WHERE su.identificador = @cerrar_subasta_id;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
GO

-- ============================================================
-- 08 - Soft delete / cancelacion administrativa
-- ============================================================
-- Objetivo:
-- - Evitar DELETE fisico y dejar la subasta fuera de operacion.
--
-- Cuando usarlo:
-- - Para cancelar administrativamente una subasta sin borrar datos.
--
-- Importante:
-- - El schema actual no acepta estado 'cancelada'.
-- - Se usa 'cerrada' como estado administrativo equivalente.
-- - No borrar subastas con pujas, asistentes o registros relacionados.
-- ============================================================

DECLARE @cancelar_subasta_id INT = NULL; -- REEMPLAZAR_ID_SUBASTA

BEGIN TRY
    BEGIN TRAN;

    IF @cancelar_subasta_id IS NULL
        THROW 52601, 'Completar @cancelar_subasta_id.', 1;

    IF NOT EXISTS (SELECT 1 FROM dbo.subastas WHERE identificador = @cancelar_subasta_id)
        THROW 52602, 'La subasta indicada no existe.', 1;

    IF EXISTS (
        SELECT 1
        FROM dbo.pujos pu
        INNER JOIN dbo.itemsCatalogo ic ON ic.identificador = pu.item
        INNER JOIN dbo.catalogos ca ON ca.identificador = ic.catalogo
        WHERE ca.subasta = @cancelar_subasta_id
    )
        THROW 52603, 'No cancelar manualmente: la subasta tiene pujas. Revisar flujo funcional.', 1;

    IF EXISTS (SELECT 1 FROM dbo.asistentes WHERE subasta = @cancelar_subasta_id)
        THROW 52604, 'No cancelar manualmente: la subasta tiene asistentes.', 1;

    IF EXISTS (SELECT 1 FROM dbo.registroDeSubasta WHERE subasta = @cancelar_subasta_id)
        THROW 52605, 'No cancelar manualmente: la subasta tiene registros de compra/resultado.', 1;

    UPDATE dbo.subastas
    SET estado = 'cerrada'
    WHERE identificador = @cancelar_subasta_id;

    COMMIT;

    SELECT
        su.identificador AS subasta_id,
        se.nombre,
        su.estado
    FROM dbo.subastas su
    LEFT JOIN dbo.subastas_extra se ON se.subasta_id = su.identificador
    WHERE su.identificador = @cancelar_subasta_id;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
GO

-- ============================================================
-- 09 - DELETE fisico solo para datos de prueba
-- ============================================================
-- Objetivo:
-- - Borrar una subasta de prueba de forma muy protegida.
--
-- Recomendacion:
-- - NO usar en bases compartidas.
-- - Preferir seccion 08.
--
-- Protecciones:
-- - Requiere @confirmar_delete_fisico exacto.
-- - Valida que no haya pujas, asistentes ni registros criticos.
-- - Valida que el nombre contenga 'PRUEBA' o 'TEST'.
--
-- Si hay dudas con relaciones, no ejecutar este bloque.
-- ============================================================

DECLARE @delete_subasta_id INT = NULL; -- REEMPLAZAR_ID_SUBASTA
DECLARE @confirmar_delete_fisico VARCHAR(50) = 'NO';

BEGIN TRY
    BEGIN TRAN;

    IF @confirmar_delete_fisico <> 'SI_BORRAR_DATOS_DE_PRUEBA'
        THROW 52701, 'DELETE fisico bloqueado. Usar confirmacion exacta solo para datos de prueba.', 1;

    IF @delete_subasta_id IS NULL
        THROW 52702, 'Completar @delete_subasta_id.', 1;

    IF NOT EXISTS (SELECT 1 FROM dbo.subastas WHERE identificador = @delete_subasta_id)
        THROW 52703, 'La subasta indicada no existe.', 1;

    IF EXISTS (
        SELECT 1
        FROM dbo.pujos pu
        INNER JOIN dbo.itemsCatalogo ic ON ic.identificador = pu.item
        INNER JOIN dbo.catalogos ca ON ca.identificador = ic.catalogo
        WHERE ca.subasta = @delete_subasta_id
    )
        THROW 52704, 'No se puede borrar: existen pujas relacionadas.', 1;

    IF EXISTS (SELECT 1 FROM dbo.asistentes WHERE subasta = @delete_subasta_id)
        THROW 52705, 'No se puede borrar: existen asistentes relacionados.', 1;

    IF EXISTS (SELECT 1 FROM dbo.registroDeSubasta WHERE subasta = @delete_subasta_id)
        THROW 52706, 'No se puede borrar: existen registros criticos relacionados.', 1;

    IF NOT EXISTS (
        SELECT 1
        FROM dbo.subastas_extra
        WHERE subasta_id = @delete_subasta_id
          AND (UPPER(nombre) LIKE '%PRUEBA%' OR UPPER(nombre) LIKE '%TEST%')
    )
        THROW 52707, 'No se puede borrar: el nombre no parece de prueba/test.', 1;

    UPDATE dbo.subastas_extra
    SET item_actual_id = NULL
    WHERE subasta_id = @delete_subasta_id;

    DELETE ic
    FROM dbo.itemsCatalogo ic
    INNER JOIN dbo.catalogos ca ON ca.identificador = ic.catalogo
    WHERE ca.subasta = @delete_subasta_id;

    DELETE FROM dbo.catalogos
    WHERE subasta = @delete_subasta_id;

    DELETE FROM dbo.subastas_extra
    WHERE subasta_id = @delete_subasta_id;

    DELETE FROM dbo.subastas
    WHERE identificador = @delete_subasta_id;

    COMMIT;

    SELECT @delete_subasta_id AS subasta_borrada_de_prueba;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
GO

-- ============================================================
-- 10 - Verificacion final
-- ============================================================
-- Objetivo:
-- - Ver estado completo de una subasta, catalogo, items, pujas y asistentes.
--
-- Cuando usarlo:
-- - Despues de cualquier bloque administrativo.
--
-- Variables:
-- - Completar @verif_subasta_id.
-- ============================================================

DECLARE @verif_subasta_id INT = NULL; -- REEMPLAZAR_ID_SUBASTA

IF @verif_subasta_id IS NULL
    THROW 52801, 'Completar @verif_subasta_id.', 1;

SELECT
    su.identificador AS subasta_id,
    se.nombre,
    su.fecha,
    su.hora,
    su.estado,
    su.ubicacion,
    su.capacidadAsistentes,
    su.tieneDeposito,
    su.seguridadPropia,
    su.categoria,
    se.moneda,
    se.url_streaming,
    se.item_actual_id
FROM dbo.subastas su
LEFT JOIN dbo.subastas_extra se ON se.subasta_id = su.identificador
WHERE su.identificador = @verif_subasta_id;

SELECT
    ca.identificador AS catalogo_id,
    ca.descripcion,
    ca.responsable,
    pr.nombre AS responsable_nombre
FROM dbo.catalogos ca
LEFT JOIN dbo.personas pr ON pr.identificador = ca.responsable
WHERE ca.subasta = @verif_subasta_id;

SELECT
    ic.identificador AS item_id,
    ic.catalogo,
    ic.producto,
    p.descripcionCatalogo,
    ic.precioBase,
    ic.comision,
    ic.subastado
FROM dbo.itemsCatalogo ic
INNER JOIN dbo.catalogos ca ON ca.identificador = ic.catalogo
INNER JOIN dbo.productos p ON p.identificador = ic.producto
WHERE ca.subasta = @verif_subasta_id
ORDER BY ic.identificador;

SELECT
    COUNT(DISTINCT pu.identificador) AS cantidad_pujas,
    COUNT(DISTINCT asi.identificador) AS cantidad_asistentes,
    COUNT(DISTINCT r.identificador) AS cantidad_registros_resultado
FROM dbo.subastas su
LEFT JOIN dbo.asistentes asi ON asi.subasta = su.identificador
LEFT JOIN dbo.catalogos ca ON ca.subasta = su.identificador
LEFT JOIN dbo.itemsCatalogo ic ON ic.catalogo = ca.identificador
LEFT JOIN dbo.pujos pu ON pu.item = ic.identificador
LEFT JOIN dbo.registroDeSubasta r ON r.subasta = su.identificador
WHERE su.identificador = @verif_subasta_id;
GO
