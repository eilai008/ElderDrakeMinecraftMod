package com.eilai.runeterra.item.weapon;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;
import java.util.UUID;

/**
 * Base class for all champion weapons.
 *
 * Each champion weapon subclass defines:
 *  - attackDamage  — flat bonus damage added on top of base 1.0
 *  - attackSpeed   — attacks per second (vanilla default is 4.0 for swords)
 *  - attackRange   — melee reach in blocks (vanilla default is 3.0)
 *  - championId    — the champion this weapon belongs to
 *  - displayName   — e.g. "Judgment" for Garen
 *
 * The weapon is locked in the champion weapon slot and cannot be
 * moved, dropped, or replaced by the player.
 */
public abstract class ChampionWeapon extends Item {

    // Stable UUIDs for attribute modifiers — one set per slot type
    protected static final UUID UUID_DAMAGE = UUID.fromString("CB3F55D3-645C-4F38-A000-000000000001");
    protected static final UUID UUID_SPEED  = UUID.fromString("CB3F55D3-645C-4F38-A000-000000000002");
    protected static final UUID UUID_RANGE  = UUID.fromString("CB3F55D3-645C-4F38-A000-000000000003");

    private final String championId;
    private final String weaponDisplayName;
    private final double attackDamage;
    private final double attackSpeed;
    private final double attackRange;

    protected ChampionWeapon(String championId, String weaponDisplayName,
                              double attackDamage, double attackSpeed, double attackRange,
                              Item.Properties properties) {
        super(properties
                .stacksTo(1)
                .setNoRepair());
        this.championId       = championId;
        this.weaponDisplayName = weaponDisplayName;
        this.attackDamage     = attackDamage;
        this.attackSpeed      = attackSpeed;
        this.attackRange      = attackRange;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getChampionId()        { return championId; }
    public String getWeaponDisplayName() { return weaponDisplayName; }
    public double getAttackDamage()      { return attackDamage; }
    public double getAttackSpeed()       { return attackSpeed; }
    public double getAttackRange()       { return attackRange; }

    // ── Attribute modifiers ───────────────────────────────────────────────────

    /**
     * Builds the attribute modifier component for this weapon.
     * Call this in your subclass constructor if you want modifiers
     * to apply automatically when held.
     */
    protected ItemAttributeModifiers buildModifiers() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(
                                UUID_DAMAGE,
                                "Champion weapon damage",
                                attackDamage,
                                AttributeModifier.Operation.ADD_VALUE),
                        net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(
                                UUID_SPEED,
                                "Champion weapon speed",
                                attackSpeed - 4.0, // offset from vanilla base of 4.0
                                AttributeModifier.Operation.ADD_VALUE),
                        net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .build();
    }

    // ── Prevent dropping ──────────────────────────────────────────────────────

    @Override
    public boolean onDroppedByPlayer(ItemStack stack, Player player) {
        // Prevent the player from dropping this weapon
        return false;
    }

    // ── Tooltip ───────────────────────────────────────────────────────────────

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§6" + weaponDisplayName));
        tooltip.add(Component.literal("§7Damage:  §f" + String.format("%.1f", attackDamage)));
        tooltip.add(Component.literal("§7Speed:   §f" + String.format("%.2f", attackSpeed)));
        tooltip.add(Component.literal("§7Range:   §f" + String.format("%.1f", attackRange)));
        tooltip.add(Component.literal("§8Champion weapon — cannot be removed."));
    }

    // ── Creates a fresh locked stack for this weapon ──────────────────────────

    public ItemStack createStack() {
        ItemStack stack = new ItemStack(this);
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, buildModifiers());
        return stack;
    }
}
