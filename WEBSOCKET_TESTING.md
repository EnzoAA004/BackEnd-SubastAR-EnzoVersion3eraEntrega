# WebSocket/STOMP local

El backend expone STOMP en:

```text
ws://localhost:8080/api/v1/ws
```

La API REST sigue ejecutando las acciones criticas. WebSocket solo publica avisos en tiempo real despues de que la transaccion confirma en base.

## Conexion desde frontend

Enviar el JWT en el frame `CONNECT`:

```ts
client.configure({
  brokerURL: "ws://localhost:8080/api/v1/ws",
  connectHeaders: {
    Authorization: `Bearer ${token}`,
  },
});
```

## Suscripciones

Subasta publica:

```text
/topic/subastas/{subastaId}
```

Notificaciones privadas del usuario autenticado:

```text
/user/queue/notificaciones
```

## Payload BID_PLACED

```json
{
  "type": "BID_PLACED",
  "subastaId": 1,
  "itemId": 10,
  "pujaId": 55,
  "monto": 15000,
  "nombreUsuario": "Enzo",
  "timestamp": "2026-06-09T20:15:30"
}
```

## Payload NOTIFICATION_CREATED

```json
{
  "type": "NOTIFICATION_CREATED",
  "notificationId": 101,
  "tipo": "bot",
  "titulo": "Aviso",
  "contenido": "Tu puja de $15.000 por el item fue registrada exitosamente.",
  "timestamp": "2026-06-09T20:15:30",
  "leido": false,
  "url": null,
  "target": null
}
```

## Probar puja en vivo

1. Levantar el backend con `./mvnw spring-boot:run` o `mvn spring-boot:run`.
2. Iniciar sesion por REST y guardar el JWT.
3. Conectar STOMP a `ws://localhost:8080/api/v1/ws` con `Authorization: Bearer <token>`.
4. Suscribirse a `/topic/subastas/{subastaId}`.
5. Ejecutar la puja existente por REST:

```http
POST /api/v1/subastas/{subastaId}/pujas
Authorization: Bearer <token>
Content-Type: application/json

{
  "monto": 15000,
  "medio_pago_id": 3
}
```

Si la puja se valida y se guarda sobre el lote activo, llega un `BID_PLACED` al topic de la subasta.

## Probar notificacion interna

1. Conectar STOMP con el JWT del usuario destino.
2. Suscribirse a `/user/queue/notificaciones`.
3. Ejecutar una accion REST que cree una notificacion interna, por ejemplo una puja valida.
4. El usuario recibe `NOTIFICATION_CREATED` en su cola privada.

## Notas

- No se implementaron push notifications reales.
- `LOT_CHANGED` y `AUCTION_FINISHED` quedan reservados hasta que exista una logica clara de cambio de lote o cierre.
- Si se prueba desde otro origen web, configurar `CORS_ALLOWED_ORIGINS`.
