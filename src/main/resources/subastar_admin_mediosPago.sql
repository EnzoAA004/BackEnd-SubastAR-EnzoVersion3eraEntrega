USE subastar_db;
GO

/* ============================================================
   HABILITAR / VERIFICAR MEDIOS DE PAGO DE UN USUARIO
   ============================================================

   Uso:
   1. Cambiar @email_usuario por el email del usuario logueado.
   2. Elegir el tipo:
        - NULL               = habilita todos los medios activos
        - 'cuenta_bancaria'  = solo cuentas bancarias
        - 'tarjeta_credito'  = solo tarjetas
        - 'cheque_certificado' = solo cheques
   3. Opcional: indicar @medio_pago_id si querés habilitar uno específico.

   Importante:
   - El medio de pago debe haber sido creado previamente por endpoint.
   - Esto representa la aprobación/verificación de la empresa.
*/

DECLARE @email_usuario VARCHAR(255) = 'enzoandreaasplanatti@gmail.com';

DECLARE @tipo_medio VARCHAR(50) = NULL;
-- Valores posibles:
-- NULL
-- 'cuenta_bancaria'
-- 'tarjeta_credito'
-- 'cheque_certificado'

DECLARE @medio_pago_id INT = NULL;
-- Si querés verificar uno específico, poner el ID.
-- Si querés verificar todos los medios del tipo elegido, dejar NULL.


/* ============================================================
   VALIDACIONES
   ============================================================ */

IF NOT EXISTS (
    SELECT 1
    FROM credenciales c
    INNER JOIN clientes cl ON cl.identificador = c.persona_id
    WHERE c.email = @email_usuario
)
BEGIN
    THROW 51001, 'No existe un cliente registrado con ese email.', 1;
END;

IF @tipo_medio IS NOT NULL
   AND @tipo_medio NOT IN ('cuenta_bancaria', 'tarjeta_credito', 'cheque_certificado')
BEGIN
    THROW 51002, 'Tipo de medio inválido. Usar cuenta_bancaria, tarjeta_credito, cheque_certificado o NULL.', 1;
END;

IF @medio_pago_id IS NOT NULL
   AND NOT EXISTS (
        SELECT 1
        FROM medios_pago mp
        INNER JOIN credenciales c ON c.persona_id = mp.cliente_id
        WHERE c.email = @email_usuario
          AND mp.id = @medio_pago_id
          AND mp.eliminado = 0
   )
BEGIN
    THROW 51003, 'El medio de pago indicado no existe, no pertenece al usuario o está eliminado.', 1;
END;


/* ============================================================
   VERIFICAR / HABILITAR MEDIOS DE PAGO
   ============================================================ */

UPDATE mp
SET verificado = 1
    FROM medios_pago mp
INNER JOIN credenciales c ON c.persona_id = mp.cliente_id
WHERE c.email = @email_usuario
  AND mp.eliminado = 0
  AND (@tipo_medio IS NULL OR mp.tipo = @tipo_medio)
  AND (@medio_pago_id IS NULL OR mp.id = @medio_pago_id);


/* ============================================================
   RESULTADO GENERAL
   ============================================================ */

SELECT
    mp.id AS medio_pago_id,
    c.email,
    mp.tipo,
    mp.descripcion,
    mp.verificado,
    mp.eliminado,
    mp.creado_en
FROM medios_pago mp
         INNER JOIN credenciales c ON c.persona_id = mp.cliente_id
WHERE c.email = @email_usuario
ORDER BY mp.id;
GO