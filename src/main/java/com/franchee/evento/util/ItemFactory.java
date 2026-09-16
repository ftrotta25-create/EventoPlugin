package com.franchee.evento.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Recompensas de El Aparecido:
 * - Sudario del Aparecido: para todos los que hicieron dano.
 * - Corazon de Matias: extra, solo para quien hizo mas dano (MVP).
 */
public class ItemFactory {

    public ItemStack crearSudario() {
        ItemStack item = new ItemStack(Material.PHANTOM_MEMBRANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Sudario del Aparecido", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Quedo de quien ya no esta del todo.", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Recompensa por enfrentar a El Aparecido.", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack crearCorazonDeMatias() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Corazón de Matías", NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Lo que quedaba de el, todavia late.", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Recompensa al que mas dano hizo a El Aparecido.", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }
}
