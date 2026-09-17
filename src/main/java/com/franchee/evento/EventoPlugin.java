package com.franchee.evento;

import com.franchee.evento.listeners.CombatListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class EventoPlugin extends JavaPlugin {

    private EventoManager eventoManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.eventoManager = new EventoManager(this);

        getServer().getPluginManager().registerEvents(new CombatListener(), this);

        getLogger().info("EventoPlugin habilitado.");
    }

    @Override
    public void onDisable() {
        getLogger().info("EventoPlugin deshabilitado.");
    }

    public EventoManager getEventoManager() {
        return eventoManager;
    }

    // ---------------- Skin de El Aparecido ----------------

    public String getSkinValue() {
        return getConfig().getString("skinAparecido.value");
    }

    public String getSkinSignature() {
        return getConfig().getString("skinAparecido.signature");
    }

    // ---------------- Persistencia de ubicaciones ----------------

    public Location getPuntoEncuentro() {
        return leerUbicacion("puntoEncuentro");
    }

    public Location getArena() {
        return leerUbicacion("arena");
    }

    private Location leerUbicacion(String clave) {
        FileConfiguration config = getConfig();
        if (!config.isConfigurationSection(clave)) return null;

        String mundoNombre = config.getString(clave + ".mundo");
        World mundo = mundoNombre != null ? Bukkit.getWorld(mundoNombre) : null;
        if (mundo == null) return null;

        double x = config.getDouble(clave + ".x");
        double y = config.getDouble(clave + ".y");
        double z = config.getDouble(clave + ".z");
        float yaw = (float) config.getDouble(clave + ".yaw");
        float pitch = (float) config.getDouble(clave + ".pitch");
        return new Location(mundo, x, y, z, yaw, pitch);
    }

    private void guardarUbicacion(String clave, Location loc) {
        FileConfiguration config = getConfig();
        config.set(clave + ".mundo", loc.getWorld().getName());
        config.set(clave + ".x", loc.getX());
        config.set(clave + ".y", loc.getY());
        config.set(clave + ".z", loc.getZ());
        config.set(clave + ".yaw", (double) loc.getYaw());
        config.set(clave + ".pitch", (double) loc.getPitch());
        saveConfig();
    }

    // ---------------- Comandos ----------------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("evento")) return false;

        if (args.length < 1) {
            sender.sendMessage("Uso: /evento <setpuntoencuentro|setarena|iniciar [segundos]|cancelar>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "setpuntoencuentro" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Solo un jugador puede usar esto (para marcar donde está parado).");
                    return true;
                }
                guardarUbicacion("puntoEncuentro", player.getLocation());
                player.sendMessage("Punto de encuentro guardado en tu ubicación actual.");
                return true;
            }
            case "setarena" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Solo un jugador puede usar esto (para marcar donde está parado).");
                    return true;
                }
                guardarUbicacion("arena", player.getLocation());
                player.sendMessage("Arena guardada en tu ubicación actual.");
                return true;
            }
            case "iniciar" -> {
                if (eventoManager.hayEventoActivo()) {
                    sender.sendMessage("Ya hay un evento en curso.");
                    return true;
                }
                int segundos = 300;
                if (args.length >= 2) {
                    try {
                        segundos = Integer.parseInt(args[1]);
                    } catch (NumberFormatException ex) {
                        sender.sendMessage("Ese no es un número válido de segundos.");
                        return true;
                    }
                }
                eventoManager.iniciar(segundos);
                return true;
            }
            case "cancelar" -> {
                if (!eventoManager.hayEventoActivo()) {
                    sender.sendMessage("No hay ningún evento en curso.");
                    return true;
                }
                eventoManager.cancelar();
                return true;
            }
            default -> {
                sender.sendMessage("Uso: /evento <setpuntoencuentro|setarena|iniciar [segundos]|cancelar>");
                return true;
            }
        }
    }
}
