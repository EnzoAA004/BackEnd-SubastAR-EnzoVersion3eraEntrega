IF COL_LENGTH('dbo.productos_detalle', 'precio_base_sugerido') IS NULL
BEGIN
    ALTER TABLE dbo.productos_detalle
    ADD precio_base_sugerido DECIMAL(18,2) NULL;
END;
GO
