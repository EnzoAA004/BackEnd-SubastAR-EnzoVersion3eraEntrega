-- ============================================================
-- V1 - Tablas originales del profesor (schema SubastAR)
-- ============================================================

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'empleados' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.empleados (
        identificador INT NOT NULL,
        cargo         VARCHAR(255),
        sector        INT,
        CONSTRAINT pk_empleados PRIMARY KEY (identificador)
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'paises' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.paises (
        numero       INT NOT NULL,
        nombre       VARCHAR(255),
        nombreCorto  VARCHAR(255),
        capital      VARCHAR(255),
        nacionalidad VARCHAR(255),
        idiomas      VARCHAR(255),
        CONSTRAINT pk_paises PRIMARY KEY (numero)
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'personas' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.personas (
        identificador INT IDENTITY NOT NULL,
        documento     VARCHAR(255),
        nombre        VARCHAR(255),
        direccion     VARCHAR(255),
        estado        VARCHAR(255) CONSTRAINT chkEstado CHECK ([estado] = 'incativo' OR [estado] = 'activo'),
        foto          VARBINARY(MAX),
        CONSTRAINT pk_personas PRIMARY KEY (identificador)
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'sectores' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.sectores (
        identificador     INT IDENTITY NOT NULL,
        nombreSector      VARCHAR(255),
        codigoSector      VARCHAR(255),
        responsableSector INT CONSTRAINT fk_sectores_empleados REFERENCES dbo.empleados,
        CONSTRAINT pk_sectores PRIMARY KEY (identificador)
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'clientes' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.clientes (
        identificador INT NOT NULL,
        numeroPais    INT,
        admitido      VARCHAR(255) CONSTRAINT chkAdmitido CHECK ([admitido] = 'no' OR [admitido] = 'si'),
        categoria     VARCHAR(255) CONSTRAINT chkCategoria CHECK ([categoria] = 'platino' OR [categoria] = 'oro' OR [categoria] = 'plata' OR [categoria] = 'especial' OR [categoria] = 'comun'),
        verificador   INT NOT NULL,
        CONSTRAINT pk_clientes PRIMARY KEY (identificador),
        CONSTRAINT fk_clientes_personas FOREIGN KEY (identificador) REFERENCES dbo.personas,
        CONSTRAINT fk_clientes_paises FOREIGN KEY (numeroPais) REFERENCES dbo.paises (numero),
        CONSTRAINT fk_clientes_empleados FOREIGN KEY (verificador) REFERENCES dbo.empleados (identificador)
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'duenios' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.duenios (
        identificador          INT NOT NULL,
        numeroPais             INT,
        verificacionFinanciera VARCHAR(255) CONSTRAINT chkVF CHECK ([verificacionFinanciera] = 'no' OR [verificacionFinanciera] = 'si'),
        verificacionJudicial   VARCHAR(255) CONSTRAINT chkVJ CHECK ([verificacionJudicial] = 'no' OR [verificacionJudicial] = 'si'),
        calificacionRiesgo     INT CONSTRAINT chkCR CHECK ([calificacionRiesgo] = 6 OR [calificacionRiesgo] = 5 OR [calificacionRiesgo] = 4 OR [calificacionRiesgo] = 3 OR [calificacionRiesgo] = 2 OR [calificacionRiesgo] = 1),
        verificador            INT NOT NULL,
        CONSTRAINT pk_duenios PRIMARY KEY (identificador),
        CONSTRAINT fk_duenios_personas FOREIGN KEY (identificador) REFERENCES dbo.personas,
        CONSTRAINT fk_duenios_empleados FOREIGN KEY (verificador) REFERENCES dbo.empleados (identificador)
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'subastadores' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.subastadores (
        identificador INT NOT NULL,
        matricula     VARCHAR(255),
        region        VARCHAR(255),
        CONSTRAINT pk_subastadores PRIMARY KEY (identificador),
        CONSTRAINT fk_subastadores_personas FOREIGN KEY (identificador) REFERENCES dbo.personas
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'seguros' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.seguros (
        nroPoliza       VARCHAR(30) NOT NULL,
        compania        VARCHAR(255),
        polizaCombinada VARCHAR(255) CONSTRAINT chkpolizaCombinada CHECK ([polizaCombinada] = 'no' OR [polizaCombinada] = 'si'),
        importe         DECIMAL(18, 2) NOT NULL CONSTRAINT chkImporte CHECK ([importe] > 0),
        CONSTRAINT pk_seguro PRIMARY KEY (nroPoliza)
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'productos' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.productos (
        identificador       INT IDENTITY NOT NULL,
        fecha               DATE,
        disponible          VARCHAR(255) CONSTRAINT chkD CHECK ([disponible] = 'no' OR [disponible] = 'si'),
        descripcionCatalogo VARCHAR(255) DEFAULT 'No Posee',
        descripcionCompleta VARCHAR(255),
        revisor             INT NOT NULL,
        duenio              INT NOT NULL,
        seguro              VARCHAR(255),
        CONSTRAINT pk_productos PRIMARY KEY (identificador),
        CONSTRAINT fk_productos_empleados FOREIGN KEY (revisor) REFERENCES dbo.empleados (identificador),
        CONSTRAINT fk_productos_duenios FOREIGN KEY (duenio) REFERENCES dbo.duenios (identificador)
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'fotos' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.fotos (
        identificador INT IDENTITY NOT NULL,
        producto      INT NOT NULL,
        foto          VARBINARY(MAX),
        CONSTRAINT pk_fotos PRIMARY KEY (identificador),
        CONSTRAINT fk_fotos_productos FOREIGN KEY (producto) REFERENCES dbo.productos
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'subastas' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.subastas (
        identificador       INT IDENTITY NOT NULL,
        fecha               DATE CONSTRAINT chkFecha CHECK ([fecha] > DATEADD(day, 10, GETDATE())),
        hora                TIME NOT NULL,
        estado              VARCHAR(255) CONSTRAINT chkES CHECK ([estado] = 'carrada' OR [estado] = 'abierta'),
        subastador          INT,
        ubicacion           VARCHAR(255),
        capacidadAsistentes INT,
        tieneDeposito       VARCHAR(255) CONSTRAINT chkTD CHECK ([tieneDeposito] = 'no' OR [tieneDeposito] = 'si'),
        seguridadPropia     VARCHAR(255) CONSTRAINT chkSP CHECK ([seguridadPropia] = 'no' OR [seguridadPropia] = 'si'),
        categoria           VARCHAR(255) CONSTRAINT chkCS CHECK ([categoria] = 'platino' OR [categoria] = 'oro' OR [categoria] = 'plata' OR [categoria] = 'especial' OR [categoria] = 'comun'),
        CONSTRAINT pk_subastas PRIMARY KEY (identificador),
        CONSTRAINT fk_subastas_subastadores FOREIGN KEY (subastador) REFERENCES dbo.subastadores (identificador)
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'catalogos' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.catalogos (
        identificador INT IDENTITY NOT NULL,
        descripcion   VARCHAR(255),
        subasta       INT,
        responsable   INT NOT NULL,
        CONSTRAINT pk_catalogos PRIMARY KEY (identificador),
        CONSTRAINT fk_catalogos_subastas FOREIGN KEY (subasta) REFERENCES dbo.subastas,
        CONSTRAINT fk_catalogos_empleados FOREIGN KEY (responsable) REFERENCES dbo.empleados
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'itemsCatalogo' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.itemsCatalogo (
        identificador INT IDENTITY NOT NULL,
        catalogo      INT NOT NULL,
        producto      INT NOT NULL,
        precioBase    DECIMAL(18, 2) NOT NULL CONSTRAINT chkPB CHECK ([precioBase] > 0.01),
        comision      DECIMAL(18, 2) NOT NULL CONSTRAINT chkC CHECK ([comision] > 0.01),
        subastado     VARCHAR(255) CONSTRAINT chkS CHECK ([subastado] = 'no' OR [subastado] = 'si'),
        CONSTRAINT pk_itemsCatalogo PRIMARY KEY (identificador),
        CONSTRAINT fk_itemsCatalogo_catalogos FOREIGN KEY (catalogo) REFERENCES dbo.catalogos,
        CONSTRAINT fk_itemsCatalogo_productos FOREIGN KEY (producto) REFERENCES dbo.productos
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'asistentes' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.asistentes (
        identificador INT IDENTITY NOT NULL,
        numeroPostor  INT NOT NULL,
        cliente       INT NOT NULL,
        subasta       INT NOT NULL,
        CONSTRAINT pk_asistentes PRIMARY KEY (identificador),
        CONSTRAINT fk_asistentes_clientes FOREIGN KEY (cliente) REFERENCES dbo.clientes,
        CONSTRAINT fk_asistentes_subasta FOREIGN KEY (subasta) REFERENCES dbo.subastas
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'pujos' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.pujos (
        identificador INT IDENTITY NOT NULL,
        asistente     INT NOT NULL,
        item          INT NOT NULL,
        importe       DECIMAL(18, 2) NOT NULL CONSTRAINT chkI CHECK ([importe] > 0.01),
        ganador       VARCHAR(255) DEFAULT 'no' CONSTRAINT chkG CHECK ([ganador] = 'no' OR [ganador] = 'si'),
        CONSTRAINT pk_pujos PRIMARY KEY (identificador),
        CONSTRAINT fk_pujos_asistentes FOREIGN KEY (asistente) REFERENCES dbo.asistentes,
        CONSTRAINT fk_pujos_itemsCatalogo FOREIGN KEY (item) REFERENCES dbo.itemsCatalogo
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'registroDeSubasta' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.registroDeSubasta (
        identificador INT IDENTITY NOT NULL,
        subasta       INT NOT NULL,
        duenio        INT NOT NULL,
        producto      INT NOT NULL,
        cliente       INT NOT NULL,
        importe       DECIMAL(18, 2) NOT NULL CONSTRAINT chkImportePagado CHECK ([importe] > 0.01),
        comision      DECIMAL(18, 2) NOT NULL CONSTRAINT chkComisionPagada CHECK ([comision] > 0.01),
        CONSTRAINT pk_registroDeSubasta PRIMARY KEY (identificador),
        CONSTRAINT fk_registroDeSubasta_subastas FOREIGN KEY (subasta) REFERENCES dbo.subastas,
        CONSTRAINT fk_registroDeSubasta_duenios FOREIGN KEY (duenio) REFERENCES dbo.duenios,
        CONSTRAINT fk_registroDeSubasta_producto FOREIGN KEY (producto) REFERENCES dbo.productos,
        CONSTRAINT fk_registroDeSubasta_cliente FOREIGN KEY (cliente) REFERENCES dbo.clientes
    );
END;
