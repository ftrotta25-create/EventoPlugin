package com.franchee.evento.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.UUID;

/**
 * Recompensas de El Aparecido:
 * - Sudario del Aparecido: para todos los que hicieron dano. Da un pequeno
 *   bonus de velocidad mientras se tiene en la mano.
 * - Corazon de Matias: extra, solo para quien hizo mas dano (MVP). Da vida
 *   maxima y dano de ataque extra mientras se tiene en la mano.
 */
public class ItemFactory {

    // --- Balance de atributos. Ajustar a gusto. ---
    private static final double SUDARIO_BONUS_VELOCIDAD = 0.15; // +15%
    private static final double CORAZON_BONUS_VIDA = 4.0;       // +4 = 2 corazones
    private static final double CORAZON_BONUS_DANIO = 2.0;      // +2 de dano de ataque

    public ItemStack crearSudario() {
        ItemStack item = new ItemStack(Material.PHANTOM_MEMBRANE);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Sudario del Aparecido", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Quedo de quien ya no esta del todo.", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Recompensa por enfrentar a El Aparecido.", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("En mano principal:", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("+15% Velocidad de movimiento", NamedTextColor.AQUA)
                        .decoration(TextDecoration.ITALIC, false)
        ));

        // Un AttributeModifier solo se activa cuando el item esta en el slot
        // que le indicamos, asi que para que funcione en cualquiera de las
        // dos manos hace falta un modifier por cada slot (con UUID distinto).
        meta.addAttributeModifier(Attribute.GENERIC_MOVEMENT_SPEED, new AttributeModifier(
                UUID.randomUUID(),
                "sudario_velocidad_mano_principal",
                SUDARIO_BONUS_VELOCIDAD,
                AttributeModifier.Operation.MULTIPLY_SCALAR_1,
                EquipmentSlot.HAND
        ));
        meta.addAttributeModifier(Attribute.GENERIC_MOVEMENT_SPEED, new AttributeModifier(
                UUID.randomUUID(),
                "sudario_velocidad_segunda_mano",
                SUDARIO_BONUS_VELOCIDAD,
                AttributeModifier.Operation.MULTIPLY_SCALAR_1,
                EquipmentSlot.OFF_HAND
        ));
        // Ocultamos la lista de atributos vanilla de Minecraft porque ya la
        // mostramos nosotros mismos en el lore de arriba, con mejor formato.
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        item.setItemMeta(meta);
        return item;
    }

    public ItemStack crearCorazonDeMatias() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Corazón de Matías, el Insaciable", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Lo que quedaba de el, todavia late.", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Cada latido es una promesa que no piensa cumplir.", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Recompensa al que mas dano hizo a El Aparecido.", NamedTextColor.LIGHT_PURPLE)
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("En mano principal:", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("+4 Corazones de vida maxima", NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("+2 Dano de ataque", NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false)
        ));

        meta.addAttributeModifier(Attribute.GENERIC_MAX_HEALTH, new AttributeModifier(
                UUID.randomUUID(),
                "corazon_vida_mano_principal",
                CORAZON_BONUS_VIDA,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.HAND
        ));
        meta.addAttributeModifier(Attribute.GENERIC_MAX_HEALTH, new AttributeModifier(
                UUID.randomUUID(),
                "corazon_vida_segunda_mano",
                CORAZON_BONUS_VIDA,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.OFF_HAND
        ));
        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier(
                UUID.randomUUID(),
                "corazon_danio_mano_principal",
                CORAZON_BONUS_DANIO,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.HAND
        ));
        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier(
                UUID.randomUUID(),
                "corazon_danio_segunda_mano",
                CORAZON_BONUS_DANIO,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.OFF_HAND
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
        meta.setUnbreakable(true);

        item.setItemMeta(meta);
        return item;
    }
}
