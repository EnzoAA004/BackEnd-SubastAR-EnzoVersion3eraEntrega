IF COL_LENGTH('dbo.tarjetas_credito', 'es_internacional') IS NULL
BEGIN
    ALTER TABLE dbo.tarjetas_credito
    ADD es_internacional BIT NOT NULL
        CONSTRAINT df_tarjetas_credito_es_internacional DEFAULT 0;
END;
GO
