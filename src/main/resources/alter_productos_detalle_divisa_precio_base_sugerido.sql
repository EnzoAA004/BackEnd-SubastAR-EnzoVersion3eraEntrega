IF COL_LENGTH('dbo.productos_detalle', 'divisa_precio_base_sugerido') IS NULL
BEGIN
    ALTER TABLE dbo.productos_detalle
    ADD divisa_precio_base_sugerido VARCHAR(10) NULL;
END;
GO

UPDATE dbo.productos_detalle
SET divisa_precio_base_sugerido = 'ARS'
WHERE precio_base_sugerido IS NOT NULL
  AND divisa_precio_base_sugerido IS NULL;
GO
