-- ============================================================
-- SubastAR - Setup minimo para probar SUBASTA EN VIVO
-- SQL Server
--
-- Este script NO crea usuarios finales, NO crea medios de pago,
-- NO crea pujas y NO crea compras.
--
-- Solo crea contexto administrativo/base que el usuario de la app
-- no puede crear por endpoints:
-- - pais minimo
-- - empleado interno
-- - subastador/rematador
-- - duenio demo de piezas existentes
-- - seguro base opcional
-- - productos iniciales
-- - subasta abierta/en vivo
-- - catalogo
-- - items de catalogo
-- - subastas_extra con moneda, streaming e item_actual_id
--
-- Ejecutar despues de:
-- 1) tablas originales
-- 2) src/main/resources/nuevas_tablas.sql
-- ============================================================

USE subastar_db;
GO

SET NOCOUNT ON;
GO

-- ============================================================
-- 0) Validaciones minimas de tablas requeridas
-- ============================================================

IF OBJECT_ID('dbo.personas', 'U') IS NULL THROW 50001, 'Falta tabla personas. Ejecuta primero el schema base.', 1;
IF OBJECT_ID('dbo.paises', 'U') IS NULL THROW 50002, 'Falta tabla paises. Ejecuta primero el schema base.', 1;
IF OBJECT_ID('dbo.empleados', 'U') IS NULL THROW 50003, 'Falta tabla empleados. Ejecuta primero el schema base.', 1;
IF OBJECT_ID('dbo.subastadores', 'U') IS NULL THROW 50004, 'Falta tabla subastadores. Ejecuta primero el schema base.', 1;
IF OBJECT_ID('dbo.duenios', 'U') IS NULL THROW 50005, 'Falta tabla duenios. Ejecuta primero el schema base.', 1;
IF OBJECT_ID('dbo.productos', 'U') IS NULL THROW 50006, 'Falta tabla productos. Ejecuta primero el schema base.', 1;
IF OBJECT_ID('dbo.subastas', 'U') IS NULL THROW 50007, 'Falta tabla subastas. Ejecuta primero el schema base.', 1;
IF OBJECT_ID('dbo.catalogos', 'U') IS NULL THROW 50008, 'Falta tabla catalogos. Ejecuta primero el schema base.', 1;
IF OBJECT_ID('dbo.itemsCatalogo', 'U') IS NULL THROW 50009, 'Falta tabla itemsCatalogo. Ejecuta primero el schema base.', 1;
IF OBJECT_ID('dbo.subastas_extra', 'U') IS NULL THROW 50010, 'Falta tabla subastas_extra. Ejecuta nuevas_tablas.sql.', 1;
GO

-- ============================================================
-- 1) Pais minimo
-- ============================================================

MERGE paises AS target
USING (
    VALUES
        (32, 'Argentina', 'AR', 'Buenos Aires', 'Argentina', 'Espanol')
) AS source (numero, nombre, nombreCorto, capital, nacionalidad, idiomas)
ON target.numero = source.numero
WHEN MATCHED THEN
    UPDATE SET
        nombre = source.nombre,
        nombreCorto = source.nombreCorto,
        capital = source.capital,
        nacionalidad = source.nacionalidad,
        idiomas = source.idiomas
WHEN NOT MATCHED THEN
    INSERT (numero, nombre, nombreCorto, capital, nacionalidad, idiomas)
    VALUES (source.numero, source.nombre, source.nombreCorto, source.capital, source.nacionalidad, source.idiomas);
GO

-- ============================================================
-- 2) Personas internas: empleado, subastador y duenio demo
-- ============================================================

DECLARE @empleado_id INT;
DECLARE @subastador_id INT;
DECLARE @duenio_id INT;

-- Empleado responsable/verificador
IF NOT EXISTS (SELECT 1 FROM personas WHERE documento = 'EMP-LIVE-001')
BEGIN
    INSERT INTO personas (documento, nombre, direccion, estado, foto)
    VALUES ('EMP-LIVE-001', 'Empleado Verificador Demo', 'Oficina Central SubastAR', 'activo', NULL);
END;

SELECT @empleado_id = identificador
FROM personas
WHERE documento = 'EMP-LIVE-001';

IF NOT EXISTS (SELECT 1 FROM empleados WHERE identificador = @empleado_id)
BEGIN
    INSERT INTO empleados (identificador, cargo, sector)
    VALUES (@empleado_id, 'Verificador', 1);
END;

-- Subastador/rematador
IF NOT EXISTS (SELECT 1 FROM personas WHERE documento = 'SUB-LIVE-001')
BEGIN
    INSERT INTO personas (documento, nombre, direccion, estado, foto)
    VALUES ('SUB-LIVE-001', 'Dr. Carlos Rios', 'Sala de Remates SubastAR', 'activo', NULL);
END;

SELECT @subastador_id = identificador
FROM personas
WHERE documento = 'SUB-LIVE-001';

IF NOT EXISTS (SELECT 1 FROM empleados WHERE identificador = @subastador_id)
BEGIN
    INSERT INTO empleados (identificador, cargo, sector)
    VALUES (@subastador_id, 'Subastador', 2);
END;

IF NOT EXISTS (SELECT 1 FROM subastadores WHERE identificador = @subastador_id)
BEGIN
    INSERT INTO subastadores (identificador, matricula, region)
    VALUES (@subastador_id, 'MAT-LIVE-001', 'Buenos Aires');
END;

-- Duenio demo de piezas ya existentes para catalogo
IF NOT EXISTS (SELECT 1 FROM personas WHERE documento = 'DUE-LIVE-001')
BEGIN
    INSERT INTO personas (documento, nombre, direccion, estado, foto)
    VALUES ('DUE-LIVE-001', 'Coleccion Martinez', 'Av. Coleccionistas 123', 'activo', NULL);
END;

SELECT @duenio_id = identificador
FROM personas
WHERE documento = 'DUE-LIVE-001';

IF NOT EXISTS (SELECT 1 FROM duenios WHERE identificador = @duenio_id)
BEGIN
    INSERT INTO duenios (
        identificador,
        numeroPais,
        verificacionFinanciera,
        verificacionJudicial,
        verificador
    )
    VALUES (
        @duenio_id,
        32,
        'si',
        'si',
        @empleado_id
    );
END;

SELECT
    @empleado_id AS empleado_id,
    @subastador_id AS subastador_id,
    @duenio_id AS duenio_id;
GO

-- ============================================================
-- 3) Seguro base opcional
-- ============================================================

IF OBJECT_ID('dbo.seguros', 'U') IS NOT NULL
BEGIN
    IF NOT EXISTS (SELECT 1 FROM seguros WHERE nroPoliza = 'POL-LIVE-001')
    BEGIN
        INSERT INTO seguros (nroPoliza, compania, polizaCombinada, importe)
        VALUES ('POL-LIVE-001', 'Aseguradora Demo', 'no', 250000.00);
    END;
END;
GO

-- ============================================================
-- 4) Subasta abierta/en vivo (sin productos ni catalogo)
-- ============================================================

DECLARE @subastador_id INT;
DECLARE @subasta_id INT;

SELECT @subastador_id = identificador FROM personas WHERE documento = 'SUB-LIVE-001';

-- Crear subasta si no existe una con este nombre en subastas_extra.
SELECT @subasta_id = se.subasta_id
FROM subastas_extra se
WHERE se.nombre = 'Coleccion Martinez - Arte moderno';

IF @subasta_id IS NULL
    BEGIN
        INSERT INTO subastas (
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
                   CAST(GETDATE() AS DATE),
                   CAST(DATEADD(HOUR, -1, GETDATE()) AS TIME),
                   'abierta',
                   @subastador_id,
                   'Av. Corrientes 1234, CABA',
                   200,
                   'si',
                   'si',
                   'comun'
               );

        SET @subasta_id = SCOPE_IDENTITY();
    END
ELSE
    BEGIN
        UPDATE subastas
        SET
            fecha = CAST(GETDATE() AS DATE),
            hora = CAST(DATEADD(HOUR, -1, GETDATE()) AS TIME),
            estado = 'abierta',
            categoria = 'comun',
            ubicacion = 'Av. Corrientes 1234, CABA',
            subastador = @subastador_id
        WHERE identificador = @subasta_id;
    END;

-- Extra de subasta (sin item actual)
MERGE subastas_extra AS target
USING (
    SELECT
        @subasta_id AS subasta_id,
        'Coleccion Martinez - Arte moderno' AS nombre,
        'ARS' AS moneda,
        'https://streaming.demo/subastar/coleccion-martinez' AS url_streaming,
        NULL AS item_actual_id
) AS source
ON target.subasta_id = source.subasta_id
WHEN MATCHED THEN
    UPDATE SET
               nombre = source.nombre,
               moneda = source.moneda,
               url_streaming = source.url_streaming,
               item_actual_id = source.item_actual_id
WHEN NOT MATCHED THEN
    INSERT (subasta_id, nombre, moneda, url_streaming, item_actual_id)
    VALUES (source.subasta_id, source.nombre, source.moneda, source.url_streaming, source.item_actual_id);

SELECT
    @subasta_id AS subasta_id_para_insomnia;
GO

-- ============================================================
-- 5) Subasta abierta/en vivo + catalogo + items
-- ============================================================

DECLARE @subastador_id INT;
DECLARE @empleado_id INT;
DECLARE @producto_reloj INT;
DECLARE @producto_vasija INT;
DECLARE @subasta_id INT;
DECLARE @catalogo_id INT;
DECLARE @item_reloj INT;
DECLARE @item_vasija INT;

SELECT @subastador_id = identificador FROM personas WHERE documento = 'SUB-LIVE-001';
SELECT @empleado_id = identificador FROM personas WHERE documento = 'EMP-LIVE-001';
SELECT @producto_reloj = identificador FROM productos WHERE descripcionCatalogo = 'Reloj Patek Philippe';
SELECT @producto_vasija = identificador FROM productos WHERE descripcionCatalogo = 'Vasija japonesa';

-- Crear subasta si no existe una con este nombre en subastas_extra.
SELECT @subasta_id = se.subasta_id
FROM subastas_extra se
WHERE se.nombre = 'Coleccion Martinez - Arte moderno';

IF @subasta_id IS NULL
BEGIN
    INSERT INTO subastas (
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
        CAST(GETDATE() AS DATE),
        CAST(DATEADD(HOUR, -1, GETDATE()) AS TIME),
        'abierta',
        @subastador_id,
        'Av. Corrientes 1234, CABA',
        200,
        'si',
        'si',
        'comun'
    );

    SET @subasta_id = SCOPE_IDENTITY();
END
ELSE
BEGIN
    UPDATE subastas
    SET
        fecha = CAST(GETDATE() AS DATE),
        hora = CAST(DATEADD(HOUR, -1, GETDATE()) AS TIME),
        estado = 'abierta',
        categoria = 'comun',
        ubicacion = 'Av. Corrientes 1234, CABA',
        subastador = @subastador_id
    WHERE identificador = @subasta_id;
END;

-- Catalogo
SELECT TOP 1 @catalogo_id = identificador
FROM catalogos
WHERE subasta = @subasta_id;

IF @catalogo_id IS NULL
BEGIN
    INSERT INTO catalogos (
        descripcion,
        subasta,
        responsable
    )
    VALUES (
        'Catalogo demo para subasta en vivo',
        @subasta_id,
        @empleado_id
    );

    SET @catalogo_id = SCOPE_IDENTITY();
END;

-- Item 1: reloj
SELECT @item_reloj = identificador
FROM itemsCatalogo
WHERE catalogo = @catalogo_id
  AND producto = @producto_reloj;

IF @item_reloj IS NULL
BEGIN
    INSERT INTO itemsCatalogo (
        catalogo,
        producto,
        precioBase,
        comision,
        subastado
    )
    VALUES (
        @catalogo_id,
        @producto_reloj,
        100000.00,
        10000.00,
        'no'
    );

    SET @item_reloj = SCOPE_IDENTITY();
END
ELSE
BEGIN
    UPDATE itemsCatalogo
    SET precioBase = 100000.00,
        comision = 10000.00,
        subastado = 'no'
    WHERE identificador = @item_reloj;
END;

-- Item 2: vasija
SELECT @item_vasija = identificador
FROM itemsCatalogo
WHERE catalogo = @catalogo_id
  AND producto = @producto_vasija;

IF @item_vasija IS NULL
BEGIN
    INSERT INTO itemsCatalogo (
        catalogo,
        producto,
        precioBase,
        comision,
        subastado
    )
    VALUES (
        @catalogo_id,
        @producto_vasija,
        80000.00,
        8000.00,
        'no'
    );

    SET @item_vasija = SCOPE_IDENTITY();
END
ELSE
BEGIN
    UPDATE itemsCatalogo
    SET precioBase = 80000.00,
        comision = 8000.00,
        subastado = 'no'
    WHERE identificador = @item_vasija;
END;

-- Extra de subasta: este es el dato clave del vivo.
MERGE subastas_extra AS target
USING (
    SELECT
        @subasta_id AS subasta_id,
        'Coleccion Martinez - Arte moderno' AS nombre,
        'ARS' AS moneda,
        'https://streaming.demo/subastar/coleccion-martinez' AS url_streaming,
        @item_reloj AS item_actual_id
) AS source
ON target.subasta_id = source.subasta_id
WHEN MATCHED THEN
    UPDATE SET
        nombre = source.nombre,
        moneda = source.moneda,
        url_streaming = source.url_streaming,
        item_actual_id = source.item_actual_id
WHEN NOT MATCHED THEN
    INSERT (subasta_id, nombre, moneda, url_streaming, item_actual_id)
    VALUES (source.subasta_id, source.nombre, source.moneda, source.url_streaming, source.item_actual_id);

SELECT
    @subasta_id AS subasta_id_para_insomnia,
    @catalogo_id AS catalogo_id,
    @item_reloj AS item_actual_id_para_insomnia,
    @item_vasija AS segundo_item_id_para_insomnia;
GO

-- ============================================================
-- 6) Script administrativo: verificar medio de pago creado por endpoint
-- ============================================================
-- Usar despues de:
-- POST /usuarios/me/medios-pago
--
-- Cambiar el email y, si queres, el id del medio.
-- Si @medio_pago_id queda NULL, verifica todos los medios activos del usuario.

DECLARE @email_usuario VARCHAR(255) = 'REEMPLAZAR_EMAIL_DEL_USUARIO';
DECLARE @medio_pago_id INT = NULL;

UPDATE mp
SET verificado = 1
FROM medios_pago mp
INNER JOIN credenciales c ON c.persona_id = mp.cliente_id
WHERE c.email = @email_usuario
  AND mp.eliminado = 0
  AND (@medio_pago_id IS NULL OR mp.id = @medio_pago_id);

SELECT
    mp.id,
    c.email,
    mp.tipo,
    mp.descripcion,
    mp.verificado,
    mp.eliminado
FROM medios_pago mp
INNER JOIN credenciales c ON c.persona_id = mp.cliente_id
WHERE c.email = @email_usuario;
GO

-- ============================================================
-- 7) Script administrativo opcional: cambiar categoria del usuario
-- ============================================================
-- Para probar restricciones:
-- - comun: puede pujar en subasta categoria comun.
-- - si cambias la subasta a oro y el cliente queda comun, deberia fallar.
-- - oro/platino no tienen limite maximo de 20% segun el service.

DECLARE @email_categoria VARCHAR(255) = 'REEMPLAZAR_EMAIL_DEL_USUARIO';
DECLARE @nueva_categoria VARCHAR(20) = 'comun'; -- comun | especial | plata | oro | platino

UPDATE cl
SET categoria = @nueva_categoria
FROM clientes cl
INNER JOIN credenciales c ON c.persona_id = cl.identificador
WHERE c.email = @email_categoria;

SELECT c.email, cl.identificador, cl.categoria, cl.admitido
FROM clientes cl
INNER JOIN credenciales c ON c.persona_id = cl.identificador
WHERE c.email = @email_categoria;
GO

-- ============================================================
-- 8) Script administrativo opcional: setear item actual manualmente
-- ============================================================
-- Usar cuando quieras pasar al segundo item sin cerrar compra.

DECLARE @subasta_id_set INT;
DECLARE @nuevo_item_actual_id INT;

SELECT @subasta_id_set = se.subasta_id
FROM subastas_extra se
WHERE se.nombre = 'Coleccion Martinez - Arte moderno';

SELECT TOP 1 @nuevo_item_actual_id = ic.identificador
FROM itemsCatalogo ic
INNER JOIN catalogos c ON c.identificador = ic.catalogo
INNER JOIN productos p ON p.identificador = ic.producto
WHERE c.subasta = @subasta_id_set
  AND p.descripcionCatalogo = 'Vasija japonesa';

UPDATE subastas_extra
SET item_actual_id = @nuevo_item_actual_id
WHERE subasta_id = @subasta_id_set;

SELECT *
FROM subastas_extra
WHERE subasta_id = @subasta_id_set;
GO
