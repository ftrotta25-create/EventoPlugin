# EventoPlugin

Plugin standalone para Paper **1.20.1**. Evento de jefe grupal: **El Aparecido**,
pensado para 6-15 jugadores, con countdown, portal a una arena (mundo aparte
via Multiverse) y un jefe de 4 fases.

## Configuración inicial (una sola vez)

1. Pará donde quieras que la gente se junte y corré:
   ```
   /evento setpuntoencuentro
   ```
2. Andá a tu mundo/arena de Multiverse, parate donde querés que aparezca
   la gente al cruzar el portal, y corré:
   ```
   /evento setarena
   ```
   Estas dos ubicaciones quedan guardadas en `config.yml` — no hace falta
   volver a setearlas salvo que quieras cambiarlas.

## Cómo correr el evento

```
/evento iniciar [segundos]
```
Si no ponés segundos, arranca con 300 (5 minutos) de cuenta regresiva.
Durante la cuenta regresiva se muestra una bossbar + actionbar a todo el
server. Al terminar, se abre un portal (partículas de fuego de alma) en
el punto de encuentro por 60 segundos — cualquiera que camine dentro del
anillo se teletransporta a la arena. Pasado ese tiempo el portal se
cierra y aparece El Aparecido.

```
/evento cancelar
```
Corta cualquier evento en curso (cuenta regresiva, portal abierto, o
incluso en combate — aunque en combate no despawnea al jefe, solo
resetea el estado del manager).

## El Aparecido — las 4 fases

| Fase | Vida | Comportamiento |
|---|---|---|
| Niebla | 100-75% | Visible, se mueve un 30% más lento, ataca a distancia con bolas de fuego con trayectoria errática |
| Sombra | 75-50% | Velocidad normal, pelea cuerpo a cuerpo, invoca 2-3 Vex ("Sombras Menores") |
| Carne | 50-25% | +40% velocidad y daño, grito de área cada 8s (Lentitud + Fatiga a quien esté cerca) |
| Colapso | 25-0% | -50% daño propio, pero tira ráfagas de daño en puntos al azar de la arena cada 3s |

## Recompensas

- **Sudario del Aparecido**: para todos los que le hicieron daño al jefe. +15% de velocidad de movimiento mientras lo tengas en cualquiera de las dos manos.
- **Vestigio del Aparecido**: extra, solo para quien más daño acumuló (MVP, anunciado por server al morir el jefe). +4 corazones de vida máxima y +2 de daño de ataque mientras lo tengas en cualquiera de las dos manos.

## Skin de El Aparecido (LibsDisguises)

El jefe es un Zombie por dentro, pero si tenés **LibsDisguises** instalado
se disfraza automáticamente de jugador con un skin custom al aparecer.

1. Instalá LibsDisguises si no lo tenés (requiere también **PacketEvents**
   como dependencia — chequeá la página del plugin).
2. Andá a **https://mineskin.org**, subí tu archivo `.png` y generalo.
3. Copiá los dos campos `value` y `signature` que te da (son strings
   largos en base64) y pegalos en `plugins/EventoPlugin/config.yml`:
   ```yaml
   skinAparecido:
     value: 'ACA_VA_EL_VALUE_LARGO'
     signature: 'ACA_VA_LA_SIGNATURE_LARGA'
   ```
4. Reiniciá el server (o `/reload`, aunque no es lo ideal). Listo, no
   hace falta ningún comando extra — se aplica solo al spawnear el jefe.

**Por qué no usamos directamente un archivo .png:** `PlayerDisguise`
no carga imágenes locales por código — ese truco de "`setSkin
archivo.png`" solo funciona escribiéndolo a mano en el comando
`/disguise` in-game, no llamando al método de Java desde un plugin.
La forma soportada oficialmente para plugins es este gameprofile
firmado por Mojang (`value`+`signature`), que armamos a partir de lo
que generás en mineskin.org.

Si LibsDisguises no está instalado, o dejaste `value`/`signature`
vacíos, el jefe sigue funcionando 100% igual (fases, daño, todo) —
simplemente se ve como un Zombie vanilla.

## Ajustar a gusto

Todo el balance (vida, daño, rangos, cooldowns, duración del portal) son
constantes al principio de `ElAparecido.java` y `EventoManager.java` —
fácil de tunear sin tocar la lógica.

## Cosas para tener en cuenta

- El jefe mantiene la IA prendida pero le sacamos todos los goals
  vanilla (`getMobGoals().removeAllGoals()`) y movemos con el
  Pathfinder real de Paper (`entidad.getPathfinder().moveTo(...)`) —
  esto SÍ traduce en movimiento real (sube bloques, esquiva
  obstáculos), a diferencia de `setVelocity()` con `setAI(false)` que
  deja al mob congelado.
- Desde la fase Sombra en adelante, además del golpe cuerpo a cuerpo
  normal, el jefe tiene un ataque de área nuevo ("Embate"): se prepara
  con sonido + partículas por 0.75s (tiempo para esquivar alejándose)
  y después suelta una onda que daña y empuja a todos los jugadores
  dentro de su radio. Cooldown de 6s.
- No hay recetas de crafteo ni altar de invocación acá — el jefe se
  invoca solo automáticamente al cerrarse el portal. Si más adelante
  querés que la gente lo invoque con un ítem en vez de por countdown,
  avisame y lo adaptamos.
