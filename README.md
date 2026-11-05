# Disruption Companion — README de ingeniería

A partir de un `booking_id`, determina **qué le ocurrió al viaje, qué le corresponde al pasajero y si se resuelve
según las opciones recomendadas o requiere a una persona**.

## 1. Track, ejecución y alcance

**Track Backend/Integración:** Usé Java con Spring boot, dado que hay fuentes contradictorias, datos parciales,
opciones que vencen, se controla mejor con lógica determinística, testeable y trazable. La capa del pasajero
es un endpoint que entrega el mensaje final ya construido.

**Ejecutar** (Use Java 17+ y Version 4+ de Springboot): `./gradlew test` y `./gradlew bootRun`. El reloj se fija en el momento del escenario
(`app.clock.fixed-instant`).

Antes de comenzar, crée dos endpoints, pues considero 2 audiencias diferentes, aunque en el fronted se puede renderizar y mostrar según perfil 
pero yo quise aplicar 2 endpoints para mejor analisis y visualización.

| Endpoint | Para |
|---|---|
| `GET /api/v1/bookings/{id}/assessment` | Ejecutivo: decisión, motivos, reglas, opciones y evidencia |
| `GET /api/v1/bookings/{id}/passenger-view` | Pasajero: mensaje simple con información verificada |

Errores en formato ProblemDetail: 400 (id inválido), 404 (no existe), 502 (una tool falla o rompe el contrato).

Cree otros casos de prueba a modo personal para ir validando segun contexto propio.

| Reserva | Escenario | Decisión |
|---|---|---|
| `ABC123` | Enunciado: adulto + niño, 160 min, pierde la conexión | `REQUIRES_HUMAN` (R5: menor) |
| `XYZ789` | Mismo vuelo, adulto solo | `RECOMMEND_TO_PASSENGER` (R3, ofrece O1) |
| `QRS456` | Otro vuelo mismo caso, adulto + niño pero ahora con 250 min  de retraso| `REQUIRES_HUMAN` (R2 sin determinar y R5) |

Los JSON del enunciado no se modificaron; agregué dos reservas más y los eventos del vuelo de `QRS456`.

## 2. Problema, supuestos y ambigüedades

**Resultado esperado:** menos incertidumbre y contactos evitables, sin información no verificada. **Medición:** %
de casos sin agente, contactos por disrupción y reclamos por información incorrecta (≈ 0). **Alcance v1:**
retrasos y conexiones perdidas; cancelación, compensación y reembolso van a una persona.

**Supuestos configurables:** retraso relevante ≥ 15 min (estándar de puntualidad) mas que nada para asumir si esta retrasado o no , conexión mínima 60 min para dicha escala, es decir, en desembarcar, caminar, pasar controles, etc, asumo ese tiempo, 
la duracion de los vuelos no influye, evento
vigente 60 min. **Otros:** el retraso se traslada a la llegada; horas locales de cada aeropuerto (el contrato no trae
zona horaria); `delay_minutes` = mayor retraso; `completeness` desconocido = `PARTIAL`; tipo `CHILD`/`INFANT` o edad
< 18 se considera un menor que se asume también para nuestros analisis.

- **Relevante:** la conexión perdida o retraso ≥ 15 min; un cambio de puerta solo queda a mdo informativo.
- **Qué ve el pasajero:** estado, derecho con su política y siguiente paso; no ve datos personales ni info que no este verificada.
- **Fuentes contradictorias:** descarto las fuentes degradadas; si una confiable contradice un retraso posterior, lo derivo.

## 3. Decisiones y alternativas descartadas

| Decisión | Descartado | Por qué |
|---|---|---|
| Reglas en código, sin IA decidiendo | Preguntarle a un LLM qué corresponde | Mismo resultado siempre y explicable ante un reclamo; un LLM podría inventar derechos, y suma latencia y costo |
| Reconocer solo las 5 condiciones conocidas | Un intérprete que lea cualquier condición | Una regla nueva o modificada va a una persona en vez de aplicarse mal. Costo: cambiar una política requiere actualizar el código |
| La confidence solo restringe | Usarla como criterio | Su cálculo no está definido; HIGH con asientos insuficientes se descarta |
| Datos personales no declarados en los DTOs | Leerlos y filtrarlos | Según pdf, mandamos lo justo y necesario, lo que no se carga no se filtra |
| Dos endpoints con DTOs distintos | Un JSON filtrado por rol | La vista del pasajero no tiene campos internos |
| Hexagonal | Lógica acoplada a Spring | Cambiar el mock por la tool real y no toca el dominio |

## 4. Clasificación de pasos

| Paso | Clasificación | Riesgo | Control |
|---|---|---|---|
| Obtener viaje y eventos | Determinístico | Datos mal formados | Validación de contrato → 502 |
| Resolver fuentes y conexión | Determinístico | Fuente equivocada | Contradicción → persona |
| Políticas y opciones | Determinístico | Regla u opción inválida | Condición desconocida → persona; TTL, asientos, ruta |
| Decidir | Determinístico | Automatizar de más | Cualquier motivo → persona |
| Mensaje al pasajero | Determinístico / AI-assisted (futuro) | Prometer algo no validado | Solo datos validados y `policy_ref` |
| Revisar casos y aprobar reprogramaciones | Human | Demora; acción irreversible y tiempo para aprobar | SLA, evidencia, idempotency key |

## 5. Consulta compleja y tools de escritura

*"Mi vuelo se atrasó, ¿alcanzo mi conexión? Viajo con mi hijo, ¿me cambian gratis o me devuelven la plata?"*
Un modelo (opcional) clasifica intenciones usando solo el texto. El flujo determinístico responde conexión perdida
y cambio sin costo (R3). El reembolso usa una política no evaluada y hay un menor del que se asume (R5): **se detiene** y crea un
caso humano. La IA nunca decide ni afirma derechos.

las tool de escritura las diseñe pero no las implementé

**`create_human_case`** con la evaluación como evidencia y una huella para no duplicar;
**`create_rebooking_request`** solo tras confirmación, refrescando opciones vencidas, y ante **503** no se informa
éxito ni fracaso: se reintenta con la **misma** key y, si persiste, va a una persona; **`notify_passenger`** con
`policy_ref` cuando menciona un derecho.

## 6. Tests, validación, limitaciones y próximos pasos

**Tests:** `EventResolverTest` (fuentes, contradicción, frescura, evento desconocido), `ConnectionAnalyzerTest`
(1, 2 y 3 tramos), `DecisionFlowTest` (caso base → persona, adulto solo → automático, regla desconocida → persona)
y `contextLoads`. Endpoints y errores 400/404 validados en Postman con las tres reservas.

**Limitaciones y próximos pasos:** mocks con opciones fijas; tools de escritura y autenticación sin implementar;
sin test HTTP automatizado; no evaluo alojamiento al pernoctar en la escala; asumo que menor es < 18, no se determina un destino internacional,un adolescente con tipo `ADULT`
sin edad no se detecta como menor; los ids de tramo del mock no identifican un vuelo único (en producción, vuelo +
fecha); con zona horaria por aeropuerto para reprogramar desde el origen antes del embarque.
