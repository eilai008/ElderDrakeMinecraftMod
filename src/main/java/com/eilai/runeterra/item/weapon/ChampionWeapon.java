package com.eilai.runeterra.item.weapon;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Base class for all champion weapons.
 *
 * Tooltip removed entirely — the attribute modifiers set via
 * DataComponents.ATTRIBUTE_MODIFIERS are displayed automatically
 * by vanilla (damage, speed), which is sufficient for now.
 * Add tooltip support back once the correct TooltipContext
 * signature for your exact mappings version is confirmed.
 */
public abstract class ChampionWeapon extends Item {

    private final String championId;
    private final String weaponDisplayName;
    private final double attackDamage;
    private final double attackSpeed;
    private final double attackRange;

    protected ChampionWeapon(String championId, String weaponDisplayName,
                             double attackDamage, double attackSpeed, double attackRange,
                             Item.Properties properties) {
        super(properties.stacksTo(1));
        this.championId        = championId;
        this.weaponDisplayName = weaponDisplayName;
        this.attackDamage      = attackDamage;
        this.attackSpeed       = attackSpeed;
        this.attackRange       = attackRange;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getChampionId()        { return championId; }
    public String getWeaponDisplayName() { return weaponDisplayName; }
    public double getAttackDamage()      { return attackDamage; }
    public double getAttackSpeed()       { return attackSpeed; }
    public double getAttackRange()       { return attackRange; }

    // ── Attribute modifiers ───────────────────────────────────────────────────

    protected ItemAttributeModifiers buildModifiers() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(
                                Identifier.fromNamespaceAndPath("runeterra", championId + "_damage"),
                                attackDamage,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(
                                Identifier.fromNamespaceAndPath("runeterra", championId + "_speed"),
                                attackSpeed - 4.0,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    // ── Prevent dropping ──────────────────────────────────────────────────────

    @Override
    public boolean onDroppedByPlayer(ItemStack stack, Player player) {
        return false;
    }

    // ── Create a fresh locked stack ───────────────────────────────────────────

    public ItemStack createStack() {
        ItemStack stack = new ItemStack(this);
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, buildModifiers());
        return stack;
    }
}