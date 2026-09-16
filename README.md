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
| Niebla | 100-75% | Invisible, solo ataca a distancia con bolas de fuego con trayectoria errática |
| Sombra | 75-50% | Se vuelve visible, pelea cuerpo a cuerpo, invoca 2-3 Vex ("Sombras Menores") |
| Carne | 50-25% | +40% velocidad y daño, grito de área cada 8s (Lentitud + Fatiga a quien esté cerca) |
| Colapso | 25-0% | -50% daño propio, pero tira ráfagas de daño en puntos al azar de la arena cada 3s |

## Recompensas

- **Sudario del Aparecido**: para todos los que le hicieron daño al jefe.
- **Corazón de Matías**: extra, solo para quien más daño acumuló (MVP,
  anunciado por server al morir el jefe).

## Ajustar a gusto

Todo el balance (vida, daño, rangos, cooldowns, duración del portal) son
constantes al principio de `ElAparecido.java` y `EventoManager.java` —
fácil de tunear sin tocar la lógica.

## Cosas para tener en cuenta

- El jefe usa `setAI(false)` y todo el movimiento/combate lo maneja el
  plugin a mano (igual que el Jefe Cabra) — así el comportamiento es
  100% predecible y no pelea contra la IA vanilla del Zombie.
- No hay recetas de crafteo ni altar de invocación acá — el jefe se
  invoca solo automáticamente al cerrarse el portal. Si más adelante
  querés que la gente lo invoque con un ítem en vez de por countdown,
  avisame y lo adaptamos.
- La textura del jefe (para que no sea un Zombie vanilla) queda
  pendiente de un `CustomModelData` — decime si querés que se lo
  agregue ya mismo con un placeholder, como hicimos con la Pava y el
  Mate.
