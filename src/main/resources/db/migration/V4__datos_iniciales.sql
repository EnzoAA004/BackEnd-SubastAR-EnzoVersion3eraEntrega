-- ============================================================
-- V4 - Datos iniciales minimos para que el sistema funcione
-- ============================================================

-- Paises
IF NOT EXISTS (SELECT 1 FROM dbo.paises)
BEGIN
    INSERT INTO dbo.paises (numero, nombre, nombreCorto, capital, nacionalidad, idiomas)
    VALUES
        (32,  N'Argentina',       N'AR', N'Buenos Aires',        N'Argentina',        N'Español'),
        (76,  N'Brasil',          N'BR', N'Brasilia',            N'Brasileña',        N'Portugués'),
        (152, N'Chile',           N'CL', N'Santiago',            N'Chilena',          N'Español'),
        (858, N'Uruguay',         N'UY', N'Montevideo',          N'Uruguaya',         N'Español'),
        (600, N'Paraguay',        N'PY', N'Asuncion',            N'Paraguaya',        N'Español, Guaraní'),
        (68,  N'Bolivia',         N'BO', N'Sucre',               N'Boliviana',        N'Español'),
        (604, N'Peru',            N'PE', N'Lima',                N'Peruana',          N'Español'),
        (170, N'Colombia',        N'CO', N'Bogota',              N'Colombiana',       N'Español'),
        (862, N'Venezuela',       N'VE', N'Caracas',             N'Venezolana',       N'Español'),
        (218, N'Ecuador',         N'EC', N'Quito',               N'Ecuatoriana',      N'Español'),
        (484, N'Mexico',          N'MX', N'Ciudad de Mexico',    N'Mexicana',         N'Español'),
        (840, N'Estados Unidos',  N'US', N'Washington D.C.',     N'Estadounidense',   N'Inglés'),
        (124, N'Canada',          N'CA', N'Ottawa',              N'Canadiense',       N'Inglés, Francés'),
        (724, N'España',          N'ES', N'Madrid',              N'Española',         N'Español'),
        (380, N'Italia',          N'IT', N'Roma',                N'Italiana',         N'Italiano'),
        (250, N'Francia',         N'FR', N'Paris',               N'Francesa',         N'Francés'),
        (276, N'Alemania',        N'DE', N'Berlin',              N'Alemana',          N'Alemán'),
        (826, N'Reino Unido',     N'GB', N'Londres',             N'Britanica',        N'Inglés'),
        (620, N'Portugal',        N'PT', N'Lisboa',              N'Portuguesa',       N'Portugués'),
        (392, N'Japon',           N'JP', N'Tokio',               N'Japonesa',         N'Japonés');
END;

-- Empleado verificador (necesario para registrar clientes)
IF NOT EXISTS (SELECT 1 FROM dbo.personas WHERE documento = 'EMP-VERIFICADOR-001')
BEGIN
    INSERT INTO dbo.personas (documento, nombre, direccion, estado, foto)
    VALUES ('EMP-VERIFICADOR-001', 'Empleado Verificador', 'Oficina Central SubastAR', 'activo', NULL);
END;

DECLARE @empleadoId INT;
SELECT @empleadoId = identificador FROM dbo.personas WHERE documento = 'EMP-VERIFICADOR-001';

IF NOT EXISTS (SELECT 1 FROM dbo.empleados WHERE identificador = @empleadoId)
BEGIN
    INSERT INTO dbo.empleados (identificador, cargo, sector)
    VALUES (@empleadoId, 'Verificador', NULL);
END;
