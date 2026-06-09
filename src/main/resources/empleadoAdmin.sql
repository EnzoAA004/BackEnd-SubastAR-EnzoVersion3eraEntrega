USE subastar_db;
GO

DECLARE @empleado_id INT;

-- Crear persona interna de empresa si no existe
IF NOT EXISTS (
    SELECT 1
    FROM personas
    WHERE documento = 'EMP-VERIFICADOR-001'
)
BEGIN
INSERT INTO personas (
    documento,
    nombre,
    direccion,
    estado,
    foto
)
VALUES (
           'EMP-VERIFICADOR-001',
           'Empleado Verificador',
           'Oficina Central SubastAR',
           'activo',
           NULL
       );

SET @empleado_id = SCOPE_IDENTITY();
END
ELSE
BEGIN
SELECT @empleado_id = identificador
FROM personas
WHERE documento = 'EMP-VERIFICADOR-001';
END;

-- Crear empleado asociado a esa persona
IF NOT EXISTS (
    SELECT 1
    FROM empleados
    WHERE identificador = @empleado_id
)
BEGIN
INSERT INTO empleados (
    identificador,
    cargo,
    sector
)
VALUES (
           @empleado_id,
           'Verificador',
           1
       );
END;
GO

SELECT
    p.identificador,
    p.documento,
    p.nombre,
    e.cargo,
    e.sector
FROM empleados e
         INNER JOIN personas p ON p.identificador = e.identificador;
GO