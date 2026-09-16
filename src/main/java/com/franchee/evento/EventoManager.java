package com.franchee.evento;

import com.franchee.evento.boss.ElAparecido;
import com.franchee.evento.util.ItemFactory;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;

/**
 * Maneja el estado del evento de principio a fin:
 * IDLE -> CUENTA_REGRESIVA -> PORTAL_ABIERTO -> EN_COMBATE -> IDLE
 */
public class EventoManager {

    private enum Estado { IDLE, CUENTA_REGRESIVA, PORTAL_ABIERTO, EN_COMBATE }

    private static final long DURACION_PORTAL_TICKS = 20L * 60; // 60s con el portal abierto
    private static final double RADIO_PORTAL = 2.5;

    private final EventoPlugin plugin;
    private final ItemFactory itemFactory = new ItemFactory();

    private Estado estado = Estado.IDLE;
    private BukkitTask tareaActual;
    private BossBar bossBarCountdown;
    private final Set<java.util.UUID> yaTeletransportados = new HashSet<>();

    public EventoManager(EventoPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean hayEventoActivo() {
        return estado != Estado.IDLE;
    }

    public void iniciar(int segundosCountdown) {
        if (hayEventoActivo()) return;
        if (plugin.getPuntoEncuentro() == null) {
            plugin.getServer().broadcast(Component.text(
                    "No se configuró el punto de encuentro (/evento setpuntoencuentro).", NamedTextColor.RED));
            return;
        }
        if (plugin.getArena() == null) {
            plugin.getServer().broadcast(Component.text(
                    "No se configuró la arena (/evento setarena).", NamedTextColor.RED));
            return;
        }

        estado = Estado.CUENTA_REGRESIVA;
        iniciarCountdown(segundosCountdown);
    }

    public void cancelar() {
        if (tareaActual != null) tareaActual.cancel();
        if (bossBarCountdown != null) {
            for (Player p : plugin.getServer().getOnlinePlayers()) bossBarCountdown.removeViewer(p);
        }
        yaTeletransportados.clear();
        estado = Estado.IDLE;
        plugin.getServer().broadcast(Component.text("El evento fue cancelado.", NamedTextColor.GRAY));
    }

    private void iniciarCountdown(int segundosTotales) {
        plugin.getServer().broadcast(Component.text(
                "¡Algo se acerca! El evento empieza en " + segundosTotales + " segundos. Punto de encuentro marcado.",
                NamedTextColor.DARK_PURPLE));

        bossBarCountdown = BossBar.bossBar(
                Component.text("El Aparecido llega en...", NamedTextColor.DARK_PURPLE),
                1.0f, BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS
        );
        for (Player p : plugin.getServer().getOnlinePlayers()) bossBarCountdown.addViewer(p);

        final int[] restante = {segundosTotales};
        tareaActual = new BukkitRunnable() {
            @Override
            public void run() {
                if (restante[0] <= 0) {
                    for (Player p : plugin.getServer().getOnlinePlayers()) bossBarCountdown.removeViewer(p);
                    abrirPortal();
                    cancel();
                    return;
                }
                bossBarCountdown.progress((float) restante[0] / segundosTotales);
                bossBarCountdown.name(Component.text(
                        "El Aparecido llega en... " + restante[0] + "s", NamedTextColor.DARK_PURPLE));

                if (restante[0] <= 10 || restante[0] % 30 == 0) {
                    for (Player p : plugin.getServer().getOnlinePlayers()) {
                        p.sendActionBar(Component.text(restante[0] + "s hasta que se abra el portal...",
                                NamedTextColor.LIGHT_PURPLE));
                    }
                }
                restante[0]--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void abrirPortal() {
        estado = Estado.PORTAL_ABIERTO;
        yaTeletransportados.clear();
        Location punto = plugin.getPuntoEncuentro();

        plugin.getServer().broadcast(Component.text(
                "¡El portal se abrió en el punto de encuentro! Entrá para ir a la arena.",
                NamedTextColor.DARK_PURPLE));

        final long[] ticksRestantes = {DURACION_PORTAL_TICKS};
        tareaActual = new BukkitRunnable() {
            @Override
            public void run() {
                if (ticksRestantes[0] <= 0) {
                    cerrarPortalYEmpezarCombate();
                    cancel();
                    return;
                }

                dibujarAnilloDePortal(punto);

                for (Player jugador : punto.getWorld().getPlayers()) {
                    if (yaTeletransportados.contains(jugador.getUniqueId())) continue;
                    if (jugador.getLocation().distance(punto) <= RADIO_PORTAL) {
                        teletransportarAArena(jugador);
                    }
                }

                ticksRestantes[0] -= 5;
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private void dibujarAnilloDePortal(Location centro) {
        double angulo = (System.currentTimeMillis() % 3600) * (Math.PI / 180.0);
        for (int i = 0; i < 16; i++) {
            double a = angulo + (2 * Math.PI * i / 16);
            double x = Math.cos(a) * RADIO_PORTAL;
            double z = Math.sin(a) * RADIO_PORTAL;
            centro.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                    centro.clone().add(x, 1.0, z), 1, 0, 0, 0, 0);
        }
    }

    private void teletransportarAArena(Player jugador) {
        yaTeletransportados.add(jugador.getUniqueId());
        jugador.teleport(plugin.getArena());
        jugador.playSound(jugador.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.7f);
        jugador.sendMessage(Component.text("Cruzaste el portal...", NamedTextColor.DARK_PURPLE));
    }

    private void cerrarPortalYEmpezarCombate() {
        if (yaTeletransportados.isEmpty()) {
            plugin.getServer().broadcast(Component.text(
                    "Nadie cruzó el portal a tiempo. Se canceló el evento.", NamedTextColor.GRAY));
            estado = Estado.IDLE;
            return;
        }

        estado = Estado.EN_COMBATE;
        plugin.getServer().broadcast(Component.text(
                "El portal se cerró. El Aparecido se manifiesta...", NamedTextColor.DARK_PURPLE));

        Location arena = plugin.getArena();
        arena.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, arena, 80, 1, 1, 1, 0.05);
        arena.getWorld().playSound(arena, Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.8f);

        new ElAparecido(plugin, itemFactory, arena);
    }

    /** Llamado por ElAparecido cuando muere o se limpia (finalizar()). */
    public void alTerminarCombate() {
        estado = Estado.IDLE;
        yaTeletransportados.clear();
    }
}
