IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'productos_detalle' AND COLUMN_NAME = 'acepto_condiciones')
    ALTER TABLE dbo.productos_detalle ADD acepto_condiciones BIT NULL;

IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'productos_detalle' AND COLUMN_NAME = 'comision')
    ALTER TABLE dbo.productos_detalle ADD comision DECIMAL(18,2) NULL;

IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'productos_detalle' AND COLUMN_NAME = 'costo_envio')
    ALTER TABLE dbo.productos_detalle ADD costo_envio DECIMAL(18,2) NULL;

IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'productos_detalle' AND COLUMN_NAME = 'poliza_id')
    ALTER TABLE dbo.productos_detalle ADD poliza_id NVARCHAR(255) NULL;

IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'productos_detalle' AND COLUMN_NAME = 'precio_base')
    ALTER TABLE dbo.productos_detalle ADD precio_base DECIMAL(18,2) NULL;

IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'productos_detalle' AND COLUMN_NAME = 'subasta_asignada')
    ALTER TABLE dbo.productos_detalle ADD subasta_asignada NVARCHAR(255) NULL;
