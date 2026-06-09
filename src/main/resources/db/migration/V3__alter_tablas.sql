-- ============================================================
-- V3 - Alteraciones de tablas existentes
-- ============================================================

IF COL_LENGTH('dbo.productos_detalle', 'precio_base_sugerido') IS NULL
BEGIN
    ALTER TABLE dbo.productos_detalle ADD precio_base_sugerido DECIMAL(18,2) NULL;
END;

IF COL_LENGTH('dbo.productos_detalle', 'divisa_precio_base_sugerido') IS NULL
BEGIN
    ALTER TABLE dbo.productos_detalle ADD divisa_precio_base_sugerido VARCHAR(10) NULL;
END;

IF COL_LENGTH('dbo.tarjetas_credito', 'es_internacional') IS NULL
BEGIN
    ALTER TABLE dbo.tarjetas_credito ADD es_internacional BIT NOT NULL CONSTRAINT df_tarjetas_credito_es_internacional DEFAULT 0;
END;
