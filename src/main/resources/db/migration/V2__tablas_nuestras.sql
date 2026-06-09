-- ============================================================
-- V2 - Tablas nuevas SubastAR (extensiones al schema del profe)
-- ============================================================

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'credenciales' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.credenciales (
        id            INT IDENTITY(1,1) NOT NULL,
        persona_id    INT NOT NULL,
        email         VARCHAR(255) NOT NULL,
        password_hash VARCHAR(255) NOT NULL,
        CONSTRAINT pk_credenciales PRIMARY KEY (id),
        CONSTRAINT uq_credenciales_email UNIQUE (email),
        CONSTRAINT uq_credenciales_persona UNIQUE (persona_id),
        CONSTRAINT fk_credenciales_personas FOREIGN KEY (persona_id) REFERENCES dbo.personas (identificador)
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'registros_pendientes' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.registros_pendientes (
        id                   INT IDENTITY(1,1) NOT NULL,
        email                VARCHAR(255) NOT NULL,
        nombre               VARCHAR(150) NOT NULL,
        apellido             VARCHAR(150) NOT NULL,
        domicilio            VARCHAR(250) NOT NULL,
        pais_numero          INT NOT NULL,
        foto_dni_frente      VARBINARY(MAX),
        foto_dni_dorso       VARBINARY(MAX),
        estado               VARCHAR(30) NOT NULL DEFAULT 'pendiente_revision',
        codigo_verificacion  VARCHAR(10),
        codigo_expires_at    DATETIME2,
        token_verificacion   VARCHAR(500),
        token_expires_at     DATETIME2,
        creado_en            DATETIME2 NOT NULL DEFAULT GETDATE(),
        CONSTRAINT pk_registros_pendientes PRIMARY KEY (id),
        CONSTRAINT fk_registros_pais FOREIGN KEY (pais_numero) REFERENCES dbo.paises (numero)
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'subastas_extra' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.subastas_extra (
        subasta_id     INT NOT NULL,
        nombre         VARCHAR(255),
        moneda         VARCHAR(10) NOT NULL DEFAULT 'ARS',
        url_streaming  VARCHAR(500),
        item_actual_id INT,
        CONSTRAINT pk_subastas_extra PRIMARY KEY (subasta_id),
        CONSTRAINT fk_subastas_extra_subastas FOREIGN KEY (subasta_id) REFERENCES dbo.subastas (identificador),
        CONSTRAINT fk_subastas_extra_item FOREIGN KEY (item_actual_id) REFERENCES dbo.itemsCatalogo (identificador)
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'medios_pago' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.medios_pago (
        id          INT IDENTITY(1,1) NOT NULL,
        cliente_id  INT NOT NULL,
        tipo        VARCHAR(30) NOT NULL,
        descripcion VARCHAR(255),
        verificado  BIT NOT NULL DEFAULT 0,
        eliminado   BIT NOT NULL DEFAULT 0,
        creado_en   DATETIME2 NOT NULL DEFAULT GETDATE(),
        CONSTRAINT pk_medios_pago PRIMARY KEY (id),
        CONSTRAINT fk_medios_pago_clientes FOREIGN KEY (cliente_id) REFERENCES dbo.clientes (identificador)
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'cuentas_bancarias' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.cuentas_bancarias (
        id                INT IDENTITY(1,1) NOT NULL,
        medio_pago_id     INT NOT NULL,
        nombre_banco      VARCHAR(100) NOT NULL,
        pais_banco        VARCHAR(100) NOT NULL,
        cbu_iban          VARCHAR(50) NOT NULL,
        fondos_reservados DECIMAL(18,2) NOT NULL DEFAULT 0,
        CONSTRAINT pk_cuentas_bancarias PRIMARY KEY (id),
        CONSTRAINT uq_cuentas_medio UNIQUE (medio_pago_id),
        CONSTRAINT fk_cuentas_medios FOREIGN KEY (medio_pago_id) REFERENCES dbo.medios_pago (id)
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'tarjetas_credito' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.tarjetas_credito (
        id                    INT IDENTITY(1,1) NOT NULL,
        medio_pago_id         INT NOT NULL,
        numero_tarjeta_masked VARCHAR(20) NOT NULL,
        titular               VARCHAR(100) NOT NULL,
        vencimiento           VARCHAR(10) NOT NULL,
        dni_titular           VARCHAR(20) NOT NULL,
        es_internacional      BIT NOT NULL DEFAULT 0,
        CONSTRAINT pk_tarjetas_credito PRIMARY KEY (id),
        CONSTRAINT uq_tarjetas_medio UNIQUE (medio_pago_id),
        CONSTRAINT fk_tarjetas_medios FOREIGN KEY (medio_pago_id) REFERENCES dbo.medios_pago (id)
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'cheques_certificados' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.cheques_certificados (
        id                INT IDENTITY(1,1) NOT NULL,
        medio_pago_id     INT NOT NULL,
        banco_emisor      VARCHAR(100) NOT NULL,
        monto_certificado DECIMAL(18,2) NOT NULL,
        numero_cheque     VARCHAR(50) NOT NULL,
        foto_cheque       VARBINARY(MAX),
        CONSTRAINT pk_cheques_certificados PRIMARY KEY (id),
        CONSTRAINT uq_cheques_medio UNIQUE (medio_pago_id),
        CONSTRAINT fk_cheques_medios FOREIGN KEY (medio_pago_id) REFERENCES dbo.medios_pago (id)
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'productos_detalle' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.productos_detalle (
        producto_id                 INT NOT NULL,
        cliente_id                  INT,
        nombre                      VARCHAR(255),
        tipo                        VARCHAR(30),
        cantidad_elementos          INT DEFAULT 1,
        epoca_origen                NVARCHAR(500),
        artista_disenador           NVARCHAR(255),
        fecha_creacion_obra         DATE,
        datos_historicos            NVARCHAR(MAX),
        informacion_adicional       NVARCHAR(MAX),
        precio_base_sugerido        DECIMAL(18,2),
        divisa_precio_base_sugerido VARCHAR(10),
        estado_solicitud            VARCHAR(30) NOT NULL DEFAULT 'en_revision',
        motivo_rechazo              NVARCHAR(MAX),
        ubicacion_deposito          VARCHAR(255),
        CONSTRAINT pk_productos_detalle PRIMARY KEY (producto_id),
        CONSTRAINT fk_pd_productos FOREIGN KEY (producto_id) REFERENCES dbo.productos (identificador),
        CONSTRAINT fk_pd_clientes FOREIGN KEY (cliente_id) REFERENCES dbo.clientes (identificador)
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'bien_solicitudes' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.bien_solicitudes (
        id                    INT IDENTITY(1,1) NOT NULL,
        codigo_solicitud      VARCHAR(30) NOT NULL,
        cliente_id            INT NOT NULL,
        tipo                  VARCHAR(30) NOT NULL,
        estado                VARCHAR(50) NOT NULL DEFAULT 'iniciada',
        paso_actual           VARCHAR(30) NOT NULL DEFAULT 'datos',
        nombre                VARCHAR(255),
        descripcion_tecnica   NVARCHAR(MAX),
        cantidad_elementos    INT,
        epoca_origen          NVARCHAR(500),
        artista_disenador     NVARCHAR(255),
        fecha_creacion_obra   DATE,
        datos_historicos      NVARCHAR(MAX),
        informacion_adicional NVARCHAR(MAX),
        declara_propiedad     BIT NOT NULL DEFAULT 0,
        producto_id           INT,
        creado_en             DATETIME2 NOT NULL DEFAULT GETDATE(),
        cuenta_cobro_banco    VARCHAR(255),
        cuenta_cobro_cbu_iban VARCHAR(255),
        cuenta_cobro_pais     VARCHAR(255),
        CONSTRAINT pk_bien_solicitudes PRIMARY KEY (id),
        CONSTRAINT uq_bien_solicitudes_codigo UNIQUE (codigo_solicitud),
        CONSTRAINT fk_bs_clientes FOREIGN KEY (cliente_id) REFERENCES dbo.clientes (identificador),
        CONSTRAINT fk_bs_productos FOREIGN KEY (producto_id) REFERENCES dbo.productos (identificador)
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'bien_solicitud_archivos' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.bien_solicitud_archivos (
        id             INT IDENTITY(1,1) NOT NULL,
        codigo_archivo VARCHAR(30) NOT NULL,
        solicitud_id   INT NOT NULL,
        nombre_archivo VARCHAR(255) NOT NULL,
        tipo_archivo   VARCHAR(20) NOT NULL,
        datos          VARBINARY(MAX),
        creado_en      DATETIME2 NOT NULL DEFAULT GETDATE(),
        url            VARCHAR(2048),
        CONSTRAINT pk_bien_solicitud_archivos PRIMARY KEY (id),
        CONSTRAINT uq_bsa_codigo UNIQUE (codigo_archivo),
        CONSTRAINT fk_bsa_solicitudes FOREIGN KEY (solicitud_id) REFERENCES dbo.bien_solicitudes (id)
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'pujos_extra' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.pujos_extra (
        pujo_id        INT NOT NULL,
        timestamp_puja DATETIME2 NOT NULL DEFAULT GETDATE(),
        medio_pago_id  INT,
        CONSTRAINT pk_pujos_extra PRIMARY KEY (pujo_id),
        CONSTRAINT fk_pe_pujos FOREIGN KEY (pujo_id) REFERENCES dbo.pujos (identificador),
        CONSTRAINT fk_pe_medios FOREIGN KEY (medio_pago_id) REFERENCES dbo.medios_pago (id)
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'compras_extra' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.compras_extra (
        registro_id       INT NOT NULL,
        fecha_compra      DATETIME2 NOT NULL DEFAULT GETDATE(),
        estado_pago       VARCHAR(20) NOT NULL DEFAULT 'pendiente',
        estado_entrega    VARCHAR(30) NOT NULL DEFAULT 'coordinando',
        medio_pago_id     INT,
        costo_envio       DECIMAL(18,2),
        direccion_entrega VARCHAR(255),
        factura_path      VARCHAR(500),
        CONSTRAINT pk_compras_extra PRIMARY KEY (registro_id),
        CONSTRAINT fk_ce_registro FOREIGN KEY (registro_id) REFERENCES dbo.registroDeSubasta (identificador),
        CONSTRAINT fk_ce_medios FOREIGN KEY (medio_pago_id) REFERENCES dbo.medios_pago (id)
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'multas' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.multas (
        id          INT IDENTITY(1,1) NOT NULL,
        cliente_id  INT NOT NULL,
        registro_id INT,
        monto       DECIMAL(18,2) NOT NULL,
        estado      VARCHAR(20) NOT NULL DEFAULT 'pendiente',
        creado_en   DATETIME2 NOT NULL DEFAULT GETDATE(),
        CONSTRAINT pk_multas PRIMARY KEY (id),
        CONSTRAINT fk_multas_clientes FOREIGN KEY (cliente_id) REFERENCES dbo.clientes (identificador),
        CONSTRAINT fk_multas_registro FOREIGN KEY (registro_id) REFERENCES dbo.registroDeSubasta (identificador)
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'seguros_extra' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.seguros_extra (
        poliza_id         VARCHAR(30) NOT NULL,
        beneficiario_id   INT,
        vigencia_desde    DATE,
        vigencia_hasta    DATE,
        cobertura         NVARCHAR(MAX),
        contacto_telefono VARCHAR(50),
        contacto_email    VARCHAR(255),
        contacto_web      VARCHAR(255),
        CONSTRAINT pk_seguros_extra PRIMARY KEY (poliza_id),
        CONSTRAINT fk_se_seguros FOREIGN KEY (poliza_id) REFERENCES dbo.seguros (nroPoliza),
        CONSTRAINT fk_se_clientes FOREIGN KEY (beneficiario_id) REFERENCES dbo.clientes (identificador)
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'chat_mensajes' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.chat_mensajes (
        id            INT IDENTITY(1,1) NOT NULL,
        cliente_id    INT NOT NULL,
        tipo          VARCHAR(255),
        emisor        VARCHAR(255),
        contenido     VARCHAR(255),
        timestamp_msg DATETIME2 DEFAULT GETDATE() NOT NULL,
        leido         BIT DEFAULT 0 NOT NULL,
        CONSTRAINT pk_chat_mensajes PRIMARY KEY (id),
        CONSTRAINT fk_cm_clientes FOREIGN KEY (cliente_id) REFERENCES dbo.clientes (identificador)
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'token_blacklist' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.token_blacklist (
        id         INT IDENTITY NOT NULL,
        token      VARCHAR(255),
        expires_at DATETIME2 NOT NULL,
        creado_en  DATETIME2 DEFAULT GETDATE() NOT NULL,
        CONSTRAINT pk_token_blacklist PRIMARY KEY (id)
    );
END;
