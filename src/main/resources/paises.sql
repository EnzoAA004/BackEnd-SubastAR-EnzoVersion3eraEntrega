USE subastar_db;
GO

IF NOT EXISTS (SELECT 1 FROM paises)
BEGIN
    INSERT INTO paises (
        numero,
        nombre,
        nombreCorto,
        capital,
        nacionalidad,
        idiomas
    )
    VALUES
        (32,  N'Argentina',       N'AR', N'Buenos Aires',        N'Argentina',        N'Español'),
        (76,  N'Brasil',          N'BR', N'Brasilia',            N'Brasileña',        N'Portugués'),
        (152, N'Chile',           N'CL', N'Santiago',            N'Chilena',          N'Español'),
        (858, N'Uruguay',         N'UY', N'Montevideo',          N'Uruguaya',         N'Español'),
        (600, N'Paraguay',        N'PY', N'Asunción',            N'Paraguaya',        N'Español, Guaraní'),
        (68,  N'Bolivia',         N'BO', N'Sucre',               N'Boliviana',        N'Español'),
        (604, N'Peru',            N'PE', N'Lima',                N'Peruana',          N'Español'),
        (170, N'Colombia',        N'CO', N'Bogotá',              N'Colombiana',       N'Español'),
        (862, N'Venezuela',       N'VE', N'Caracas',             N'Venezolana',       N'Español'),
        (218, N'Ecuador',         N'EC', N'Quito',               N'Ecuatoriana',      N'Español'),
        (484, N'Mexico',          N'MX', N'Ciudad de México',    N'Mexicana',         N'Español'),
        (840, N'Estados Unidos',  N'US', N'Washington D.C.',     N'Estadounidense',   N'Inglés'),
        (124, N'Canada',          N'CA', N'Ottawa',              N'Canadiense',       N'Inglés, Francés'),
        (724, N'España',          N'ES', N'Madrid',              N'Española',         N'Español'),
        (380, N'Italia',          N'IT', N'Roma',                N'Italiana',         N'Italiano'),
        (250, N'Francia',         N'FR', N'París',               N'Francesa',         N'Francés'),
        (276, N'Alemania',        N'DE', N'Berlín',              N'Alemana',          N'Alemán'),
        (826, N'Reino Unido',     N'GB', N'Londres',             N'Británica',        N'Inglés'),
        (620, N'Portugal',        N'PT', N'Lisboa',              N'Portuguesa',       N'Portugués'),
        (392, N'Japon',           N'JP', N'Tokio',               N'Japonesa',         N'Japonés');
END
GO