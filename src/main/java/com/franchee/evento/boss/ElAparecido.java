package com.franchee.evento.boss;

import com.franchee.evento.EventoPlugin;
import com.franchee.evento.util.ItemFactory;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.PlayerDisguise;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Vex;
import org.bukkit.entity.Zombie;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * El Aparecido: jefe de evento grupal, pensado para 6-15 jugadores.
 *
 * Fases (por % de vida):
 *  100-75  NIEBLA:   visible pero envuelto en niebla, ataca a distancia con bolas de fuego erraticas, se mueve mas lento
 *  75-50   SOMBRA:   se vuelve visible y cuerpo a cuerpo, invoca Vex ("sombras menores")
 *  50-25   CARNE:    maximo poder, grito de area (Lentitud + Fatiga = "aturdimiento")
 *  25-0    COLAPSO:  menos dano propio, pero tira rafagas de dano al azar en la arena
 */
public class ElAparecido {

    public static final String METADATA_KEY = "esElAparecido";

    // --- Balance para grupo de 6-15. Ajustar segun testeo. ---
    private static final double VIDA_MAXIMA = 700.0;
    private static final double DANIO_BASE = 10.0;
    private static final double VELOCIDAD_BASE = 0.28;
    private static final double RANGO_PERSECUCION = 25.0;
    private static final double RANGO_ATAQUE = 2.5;
    private static final long COOLDOWN_ATAQUE_MS = 1000L;
    private static final long COOLDOWN_DISPARO_NIEBLA_MS = 2500L;
    private static final double RANGO_GRITO = 8.0;
    private static final long COOLDOWN_GRITO_MS = 8000L;
    private static final long COOLDOWN_RAFAGA_MS = 3000L;
    private static final double RADIO_RAFAGA_ARENA = 10.0;
    private static final double RANGO_EMBATE = 4.5;
    private static final double DANIO_EMBATE = 9.0;
    private static final double EMPUJE_EMBATE = 0.6;
    private static final long COOLDOWN_EMBATE_MS = 6000L;

    private final EventoPlugin plugin;
    private final ItemFactory itemFactory;
    private final Zombie entidad;
    private final BossBar bossBar;

    private enum Fase { NIEBLA, SOMBRA, CARNE, COLAPSO }
    private Fase faseActual = Fase.NIEBLA;

    private final Map<UUID, Double> danioPorJugador = new HashMap<>();
    private long proximoAtaquePermitido = 0L;
    private long proximoDisparoPermitido = 0L;
    private long proximoGritoPermitido = 0L;
    private long proximaRafagaPermitida = 0L;
    private long proximoEmbatePermitido = 0L;
    private BukkitRunnable tickTask;

    private static final Map<UUID, ElAparecido> ACTIVOS = new HashMap<>();

    public ElAparecido(EventoPlugin plugin, ItemFactory itemFactory, Location location) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.entidad = location.getWorld().spawn(location, Zombie.class);
        this.bossBar = BossBar.bossBar(
                Component.text("El Aparecido", NamedTextColor.DARK_GRAY),
                1.0f, BossBar.Color.PURPLE, BossBar.Overlay.NOTCHED_20
        );

        configurarEntidad();
        ACTIVOS.put(entidad.getUniqueId(), this);
        iniciarTick();
    }

    private void configurarEntidad() {
        entidad.customName(Component.text("El Aparecido", NamedTextColor.DARK_GRAY));
        entidad.setCustomNameVisible(true);
        entidad.setMetadata(METADATA_KEY, new FixedMetadataValue(plugin, true));
        entidad.setRemoveWhenFarAway(false);
        entidad.setPersistent(true);
        entidad.setShouldBurnInDay(false);
        // IMPORTANTE: con setAI(false) el mob nunca traduce velocidad en
        // movimiento real (queda "congelado" a nivel vanilla, mas alla de
        // cualquier setVelocity()). La forma correcta de tener control total
        // pero que el jefe SI se mueva es dejar la IA prendida y sacarle los
        // goals vanilla (ataque, mirar random, etc.) con la Mob Goal API de
        // Paper. El movimiento real lo hacemos con getPathfinder().
        entidad.setAI(true);
        plugin.getServer().getMobGoals().removeAllGoals(entidad);

        AttributeInstance vida = entidad.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (vida != null) vida.setBaseValue(VIDA_MAXIMA);
        entidad.setHealth(VIDA_MAXIMA);

        AttributeInstance danio = entidad.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (danio != null) danio.setBaseValue(DANIO_BASE);

        AttributeInstance velocidad = entidad.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (velocidad != null) velocidad.setBaseValue(VELOCIDAD_BASE);

        entrarFaseNiebla();
        aplicarDisfraz();
    }

    /**
     * Disfraza al Zombie de jugador con el skin custom, usando LibsDisguises
     * si esta instalado. El skin se define en config.yml (skinAparecido.value
     * y skinAparecido.signature), generados en https://mineskin.org a partir
     * de tu .png.
     *
     * IMPORTANTE: PlayerDisguise#setSkin(String) NO carga archivos .png
     * directamente por Java (ese truco solo existe en el parser de
     * comandos in-game). Lo que SI soporta oficialmente es un gameprofile
     * serializado como JSON cuando el string supera los 50 caracteres
     * (ver disguises.yml de LibsDisguises) - por eso lo armamos a mano acá.
     *
     * Si LibsDisguises no esta instalado, o el value/signature no estan
     * configurados, el jefe sigue funcionando normal, solo se ve como
     * Zombie vanilla.
     */
    private void aplicarDisfraz() {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("LibsDisguises")) {
            plugin.getLogger().warning("LibsDisguises no esta instalado - El Aparecido se ve como Zombie vanilla.");
            return;
        }

        String value = plugin.getSkinValue();
        String signature = plugin.getSkinSignature();
        if (value == null || value.isBlank() || signature == null || signature.isBlank()) {
            plugin.getLogger().warning("No hay skin configurado (skinAparecido.value/signature en config.yml) - "
                    + "El Aparecido se ve como Zombie vanilla.");
            return;
        }

        try {
            String perfilJson = "{\"id\":\"" + UUID.randomUUID().toString().replace("-", "")
                    + "\",\"name\":\"ElAparecido\",\"properties\":[{\"name\":\"textures\",\"value\":\""
                    + value + "\",\"signature\":\"" + signature + "\"}]}";

            PlayerDisguise disfraz = new PlayerDisguise("ElAparecido");
            disfraz.setSkin(perfilJson);
            disfraz.setHearSelfDisguise(false);
            DisguiseAPI.disguiseToAll(entidad, disfraz);
        } catch (Exception ex) {
            plugin.getLogger().warning("No se pudo aplicar el disfraz de El Aparecido: " + ex.getMessage());
        }
    }

    private void iniciarTick() {
        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!entidad.isValid() || entidad.isDead()) {
                    finalizar();
                    return;
                }
                actualizarBossBar();
                revisarFase();
                comportamientoDeFase();
            }
        };
        tickTask.runTaskTimer(plugin, 0L, 5L); // cada 0.25s, hace falta granularidad para la niebla erratica
    }

    private void actualizarBossBar() {
        double vidaActual = entidad.getHealth();
        double vidaMax = entidad.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
        float progreso = (float) Math.max(0.0, Math.min(1.0, vidaActual / vidaMax));
        bossBar.progress(progreso);

        for (Player jugador : entidad.getWorld().getPlayers()) {
            bossBar.addViewer(jugador);
        }
    }

    private double porcentajeVida() {
        double vidaMax = entidad.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
        return entidad.getHealth() / vidaMax;
    }

    private void revisarFase() {
        double p = porcentajeVida();
        Fase nuevaFase;
        if (p > 0.75) nuevaFase = Fase.NIEBLA;
        else if (p > 0.50) nuevaFase = Fase.SOMBRA;
        else if (p > 0.25) nuevaFase = Fase.CARNE;
        else nuevaFase = Fase.COLAPSO;

        if (nuevaFase != faseActual) {
            faseActual = nuevaFase;
            switch (faseActual) {
                case SOMBRA -> entrarFaseSombra();
                case CARNE -> entrarFaseCarne();
                case COLAPSO -> entrarFaseColapso();
                default -> {}
            }
        }
    }

    // ---------------- FASE 1: NIEBLA ----------------
    private void entrarFaseNiebla() {
        bossBar.name(Component.text("El Aparecido (niebla)", NamedTextColor.GRAY));
        entidad.getWorld().playSound(entidad.getLocation(), Sound.AMBIENT_CAVE, 1.0f, 0.6f);
    }

    private void comportamientoNiebla() {
        // Nube de niebla como identidad visual de la fase, sin ocultarlo (se puede golpear).
        entidad.getWorld().spawnParticle(Particle.SOUL, entidad.getLocation().add(0, 1, 0), 3, 0.5, 0.5, 0.5, 0.01);
        entidad.getWorld().spawnParticle(Particle.CLOUD, entidad.getLocation().add(0, 0.3, 0), 4, 0.6, 0.2, 0.6, 0.01);

        moverHaciaObjetivo(0.7); // mas lento que las fases siguientes, mantiene distancia como tirador

        Player objetivo = jugadorMasCercano(RANGO_PERSECUCION);
        if (objetivo == null) return;

        long ahora = System.currentTimeMillis();
        if (ahora >= proximoDisparoPermitido) {
            dispararBolaErratica(objetivo);
            proximoDisparoPermitido = ahora + COOLDOWN_DISPARO_NIEBLA_MS;
        }
    }

    private void dispararBolaErratica(Player objetivo) {
        Vector direccion = objetivo.getEyeLocation().toVector()
                .subtract(entidad.getEyeLocation().toVector())
                .normalize();
        // le metemos error random a la direccion para que sea "erratica"
        direccion.add(new Vector(
                (Math.random() - 0.5) * 0.6,
                (Math.random() - 0.5) * 0.3,
                (Math.random() - 0.5) * 0.6
        )).normalize();

        SmallFireball bola = entidad.launchProjectile(SmallFireball.class, direccion);
        bola.setYield(0f);
        bola.setIsIncendiary(false);
    }

    // ---------------- FASE 2: SOMBRA ----------------
    private void entrarFaseSombra() {
        bossBar.name(Component.text("El Aparecido (sombra)", NamedTextColor.DARK_PURPLE));
        entidad.getWorld().playSound(entidad.getLocation(), Sound.ENTITY_VEX_AMBIENT, 1.5f, 0.7f);

        Location base = entidad.getLocation();
        int cantidad = 2 + (int) (Math.random() * 2); // 2 o 3
        for (int i = 0; i < cantidad; i++) {
            Location spawnLoc = base.clone().add((Math.random() - 0.5) * 6, 1, (Math.random() - 0.5) * 6);
            Vex sombra = base.getWorld().spawn(spawnLoc, Vex.class);
            sombra.customName(Component.text("Sombra Menor", NamedTextColor.DARK_GRAY));
            sombra.setCustomNameVisible(true);
        }
    }

    /**
     * Mueve al jefe hacia el jugador mas cercano usando el Pathfinder real de
     * Paper (com.destroystokyo.paper.entity.Pathfinder). A diferencia de
     * setVelocity(), esto SI mueve al mob (sube bloques, esquiva obstaculos,
     * gira solo hacia donde va) porque usa el mismo sistema de navegacion
     * que cualquier mob vanilla — simplemente le sacamos los goals de ATAQUE
     * y de MIRAR ALEATORIO en configurarEntidad() para que no interfieran.
     *
     * multiplicadorVelocidad es un multiplicador sobre la velocidad base del
     * mob (1.0 = velocidad normal, 0.7 = 30% mas lento, etc.).
     */
    private void moverHaciaObjetivo(double multiplicadorVelocidad) {
        Player objetivo = jugadorMasCercano(RANGO_PERSECUCION);
        if (objetivo == null) return;

        entidad.getPathfinder().moveTo(objetivo, multiplicadorVelocidad);
    }

    private void comportamientoSombraOSuperior() {
        moverHaciaObjetivo(1.0);
        comportamientoEmbate();

        Player objetivo = jugadorMasCercano(RANGO_ATAQUE);
        if (objetivo == null) return;

        long ahora = System.currentTimeMillis();
        if (ahora >= proximoAtaquePermitido) {
            double valorDanio = entidad.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue();
            objetivo.damage(valorDanio, entidad);
            proximoAtaquePermitido = ahora + COOLDOWN_ATAQUE_MS;
        }
    }

    /**
     * Ataque nuevo, de area: el jefe se prepara (aviso de sonido + particulas
     * durante 0.75s, tiempo suficiente para alejarse) y despues suelta una
     * onda expansiva que dana y empuja a todos los jugadores dentro de
     * RANGO_EMBATE, sin importar si estan pegados o un poco mas lejos que el
     * golpe cuerpo a cuerpo normal. Disponible desde la fase Sombra en
     * adelante (se llama junto con el ataque normal).
     */
    private void comportamientoEmbate() {
        long ahora = System.currentTimeMillis();
        if (ahora < proximoEmbatePermitido) return;

        Player objetivoCercano = jugadorMasCercano(RANGO_EMBATE);
        if (objetivoCercano == null) return;

        proximoEmbatePermitido = ahora + COOLDOWN_EMBATE_MS;

        // Aviso: se puede esquivar alejandose apenas se escucha/ve esto.
        entidad.getWorld().playSound(entidad.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 1.0f, 0.7f);
        entidad.getWorld().spawnParticle(Particle.CRIT, entidad.getLocation().add(0, 0.2, 0),
                30, 1.5, 0.2, 1.5, 0.15);

        Location centro = entidad.getLocation();
        new BukkitRunnable() {
            @Override
            public void run() {
                centro.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, centro.clone().add(0, 0.2, 0), 1);
                centro.getWorld().playSound(centro, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.1f);

                // Anillo de particulas para marcar visualmente el radio afectado.
                for (int i = 0; i < 24; i++) {
                    double angulo = 2 * Math.PI * i / 24;
                    double x = Math.cos(angulo) * RANGO_EMBATE;
                    double z = Math.sin(angulo) * RANGO_EMBATE;
                    centro.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                            centro.clone().add(x, 0.2, z), 1, 0, 0, 0, 0);
                }

                for (Player jugador : centro.getWorld().getPlayers()) {
                    double distancia = jugador.getLocation().distance(centro);
                    if (distancia > RANGO_EMBATE) continue;

                    jugador.damage(DANIO_EMBATE, entidad);
                    if (distancia > 0.1) {
                        Vector empuje = jugador.getLocation().toVector().subtract(centro.toVector())
                                .normalize().multiply(EMPUJE_EMBATE);
                        jugador.setVelocity(jugador.getVelocity().add(new Vector(empuje.getX(), 0.35, empuje.getZ())));
                    }
                }
            }
        }.runTaskLater(plugin, 15L); // 0.75s de aviso antes de que explote de verdad
    }

    // ---------------- FASE 3: CARNE ----------------
    private void entrarFaseCarne() {
        bossBar.name(Component.text("El Aparecido (¡CARNE!)", NamedTextColor.RED));
        AttributeInstance velocidad = entidad.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (velocidad != null) velocidad.setBaseValue(velocidad.getBaseValue() * 1.4);
        AttributeInstance danio = entidad.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (danio != null) danio.setBaseValue(danio.getBaseValue() * 1.4);
        entidad.getWorld().playSound(entidad.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.3f);
    }

    private void comportamientoGrito() {
        long ahora = System.currentTimeMillis();
        if (ahora < proximoGritoPermitido) return;
        proximoGritoPermitido = ahora + COOLDOWN_GRITO_MS;

        entidad.getWorld().spawnParticle(Particle.SONIC_BOOM, entidad.getLocation().add(0, 1, 0), 1);
        entidad.getWorld().playSound(entidad.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 1.5f);

        for (Player jugador : entidad.getWorld().getPlayers()) {
            if (jugador.getLocation().distance(entidad.getLocation()) <= RANGO_GRITO) {
                jugador.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 3));
                jugador.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 60, 2));
            }
        }
    }

    // ---------------- FASE 4: COLAPSO ----------------
    private void entrarFaseColapso() {
        bossBar.name(Component.text("El Aparecido (colapso)", NamedTextColor.LIGHT_PURPLE));
        AttributeInstance danio = entidad.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (danio != null) danio.setBaseValue(danio.getBaseValue() * 0.5);
        entidad.getWorld().playSound(entidad.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 1.0f, 1.6f);
    }

    private void comportamientoRafagas() {
        long ahora = System.currentTimeMillis();
        if (ahora < proximaRafagaPermitida) return;
        proximaRafagaPermitida = ahora + COOLDOWN_RAFAGA_MS;

        Location centro = entidad.getLocation();
        Location punto = centro.clone().add(
                (Math.random() - 0.5) * 2 * RADIO_RAFAGA_ARENA,
                0,
                (Math.random() - 0.5) * 2 * RADIO_RAFAGA_ARENA
        );
        // aviso visual antes de que explote
        centro.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, punto.add(0, 0.2, 0), 15, 0.4, 0.1, 0.4);
        centro.getWorld().playSound(punto, Sound.BLOCK_SOUL_SAND_BREAK, 1.0f, 0.7f);

        new BukkitRunnable() {
            @Override
            public void run() {
                punto.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, punto, 1);
                punto.getWorld().playSound(punto, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.2f);
                for (Player jugador : punto.getWorld().getPlayers()) {
                    if (jugador.getLocation().distance(punto) <= 2.0) {
                        jugador.damage(6.0);
                    }
                }
            }
        }.runTaskLater(plugin, 20L); // 1 segundo de aviso
    }

    private void comportamientoDeFase() {
        switch (faseActual) {
            case NIEBLA -> comportamientoNiebla();
            case SOMBRA -> comportamientoSombraOSuperior();
            case CARNE -> {
                comportamientoSombraOSuperior();
                comportamientoGrito();
            }
            case COLAPSO -> {
                comportamientoSombraOSuperior();
                comportamientoGrito();
                comportamientoRafagas();
            }
        }
    }

    private Player jugadorMasCercano(double rangoMax) {
        return entidad.getWorld().getPlayers().stream()
                .filter(p -> p.getGameMode() == org.bukkit.GameMode.SURVIVAL || p.getGameMode() == org.bukkit.GameMode.ADVENTURE)
                .filter(p -> p.getLocation().distanceSquared(entidad.getLocation()) <= rangoMax * rangoMax)
                .min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(entidad.getLocation())))
                .orElse(null);
    }

    // ---------------- DANO Y MUERTE ----------------
    public void registrarDanio(Player jugador, double cantidad) {
        danioPorJugador.merge(jugador.getUniqueId(), cantidad, Double::sum);
    }

    public void alMorir() {
        entregarRecompensas();
        finalizar();
    }

    private void entregarRecompensas() {
        UUID mvpId = null;
        double maxDanio = -1;
        for (Map.Entry<UUID, Double> entry : danioPorJugador.entrySet()) {
            if (entry.getValue() > maxDanio) {
                maxDanio = entry.getValue();
                mvpId = entry.getKey();
            }
        }

        for (UUID uuid : danioPorJugador.keySet()) {
            Player jugador = entidad.getServer().getPlayer(uuid);
            if (jugador == null) continue;
            jugador.getInventory().addItem(itemFactory.crearSudario());
            if (uuid.equals(mvpId)) {
                jugador.getInventory().addItem(itemFactory.crearVestigioDelAparecido());
            }
        }

        if (mvpId != null) {
            Player mvp = entidad.getServer().getPlayer(mvpId);
            String nombreMvp = mvp != null ? mvp.getName() : "???";
            entidad.getServer().broadcast(Component.text(
                    "El Aparecido cayó. MVP: " + nombreMvp + " con " + Math.round(maxDanio) + " de daño.",
                    NamedTextColor.LIGHT_PURPLE));
        }
    }

    private void finalizar() {
        for (Player p : entidad.getWorld().getPlayers()) {
            bossBar.removeViewer(p);
        }
        if (tickTask != null) tickTask.cancel();
        ACTIVOS.remove(entidad.getUniqueId());
        plugin.getEventoManager().alTerminarCombate();
    }

    public static boolean esElAparecido(org.bukkit.entity.LivingEntity entidad) {
        return entidad.hasMetadata(METADATA_KEY);
    }

    public static ElAparecido get(UUID uuid) {
        return ACTIVOS.get(uuid);
    }
}
