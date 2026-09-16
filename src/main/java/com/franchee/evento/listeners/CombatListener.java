package com.franchee.evento.listeners;

import com.franchee.evento.boss.ElAparecido;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

public class CombatListener implements Listener {

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Zombie zombie) || !ElAparecido.esElAparecido(zombie)) return;

        ElAparecido jefe = ElAparecido.get(zombie.getUniqueId());
        if (jefe == null) return;

        Player atacante = resolverJugadorAtacante(e.getDamager());
        if (atacante != null) {
            jefe.registrarDanio(atacante, e.getFinalDamage());
        }
    }

    private Player resolverJugadorAtacante(org.bukkit.entity.Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile proyectil && proyectil.getShooter() instanceof Player p) return p;
        return null;
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        if (!(e.getEntity() instanceof Zombie zombie) || !ElAparecido.esElAparecido(zombie)) return;

        ElAparecido jefe = ElAparecido.get(zombie.getUniqueId());
        if (jefe != null) {
            e.getDrops().clear();
            jefe.alMorir();
        }
    }
}
