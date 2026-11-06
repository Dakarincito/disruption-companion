# AI Usage & Decision Note

**Herramientas:** Claude (Anthropic) Y ChatGPT. Lo usé para analizar el enunciado para ver si coincidiamos el mismo analisis y observacion con los mock
o me daba una percepción diferente,
discutir ambigüedades, definir ambiguedades coherentes, confirmar arquitectura, generar código (Los DTOs del mock) y tests, redactar borradores del README y apoyarme con
el despliegue a Linode y creación de SubDominio con Candy Y duckDNS. Lo traté como un par técnico al que le pedía justificar cada propuesta, confirmar analisis y si coincidia en mi propuesta, no como autor de la solución.
Tambien que me generará otra reserva simulada para poder hacer pruebas.

## Decisiones que tomé personalmente

- **Track Backend con Java 17 y Spring Boot:** es mi stack, java 17 ya nos ofrece varias ventajas, nos ahorramos lombok y el mayor riesgo del caso está en la decisión.
- **Arquitectura hexagonal:** rechacé la primera estructura de la IA porque acoplaba la lógica al formato de los JSON y use la tradicional domain, application e infraestructura ya que me propuso una en español con repository, pero ni siquiera usamos base de datos.
- **Dos vistas, pasajero y ejecutivo:** son audiencias con necesidades y permisos distintos que yo defini para poder explicar y presentar mejor.
- **Alojamiento al pernoctar en la escala:** detecté el riesgo, pero lo dejé documentado y no en la lógica de la v1.
- **Escenarios propios** adulto solo; 250 min con R2 sin determinar y otro escenario para ver qué decide con varias reglas a la vez.

## Outputs que acepté, rechacé o modifiqué

| Output de la IA | Qué hice |
|---|---|
| Primera estructura de paquetes | Rechazada; usé mi estructura hexagonal |
| Creacion de nuevos valores y supuestos de ambiguedades | Modificado; acepté algunos, otros los modifique |
| Propuesta de salida JSON para ejecutivo | Modificado: Establecí un JSON que a mi juicio era mas demostrativo para pasajero o ejecutivo |
| Instalar git y hacer commit | Rechazado: Personalmente controlo el flujo del git |
| Generacion de readme.md | Aceptados tras revisarlos y modificar algunos textos a unos mas coherentes |
| Lógica de decisión, validaciones y tests | Aceptados tras revisarlos y probarlos |

## Cómo validé

Ejecuté los tests y probé los endpoints en Postman (caso exito, 400 y 404). Contrasté cada resultado con mi
análisis previo del caso (conexión perdida por 90 min, R3 y R5, O1 candidata, si requiere humano), probé
escenarios fuera del mock y forcé ids de tramo repetidos para entender una limitación. Pedirle a la IA el porqué
de cada decisión reveló errores, como las resuelve y que me muestre su analisis para ver si llego a la misma conclusion que yo, 
cuando la usé para desplegar a Linode y con HTTPS, valide llamados con el GET y si me generaba el mismo JSON de respuesta.

## Información que no compartiría con un modelo

Datos reales de pasajeros (RUT, email, teléfono), credenciales o tokens, y datos internos de la compañía. Aquí
solo usé los datos ficticios del enunciado pero jamás le entregaria el proyecto zip, un archivo .java o de alguna clase sin verificar que tenga dato sensible o el metodo de negocio de un proyecto,
En el producto, ningún modelo recibe datos personales y tanto los segmentos de vuelos o politicas son ficticios para simular si llegamos a misma conclusion.
Tampoco le entregue mi IP ni información de servidor de Linode cuando intente desplegarla, ni token ni mi clave de acceso parq configuracion de DNS y rules.
