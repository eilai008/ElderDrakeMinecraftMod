package com.eilai.runeterra.init;

import com.eilai.runeterra.Runeterra;
import com.eilai.runeterra.item.ChampionCrystalItem;
import com.eilai.runeterra.item.ElderbladeItem;
import com.eilai.runeterra.item.SpellbookItem;
import com.eilai.runeterra.item.weapon.VayneWeaponItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(Runeterra.MODID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Runeterra.MODID);

    // ── Spawn eggs ────────────────────────────────────────────────────────────

    public static final DeferredItem<SpawnEggItem> ELDER_DRAKE_SPAWN_EGG = ITEMS.registerItem(
            "elder_drake_spawn_egg",
            properties -> new SpawnEggItem(properties.spawnEgg(ModEntities.ELDER_DRAKE.get())));

    public static final DeferredItem<SpawnEggItem> SCUTTLE_CRAB_SPAWN_EGG = ITEMS.registerItem(
            "scuttle_crab_spawn_egg",
            properties -> new SpawnEggItem(properties.spawnEgg(ModEntities.SCUTTLE_CRAB.get())));

    // ── Existing items ────────────────────────────────────────────────────────

    public static final DeferredItem<ElderbladeItem> DRAKEBLADE = ITEMS.registerItem(
            "elderblade",
            properties -> new ElderbladeItem(
                    properties.stacksTo(1).attributes(
                            ItemAttributeModifiers.builder()
                                    .add(Attributes.ATTACK_DAMAGE,
                                            new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 14.0,
                                                    AttributeModifier.Operation.ADD_VALUE),
                                            net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                                    .add(Attributes.ATTACK_SPEED,
                                            new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.2,
                                                    AttributeModifier.Operation.ADD_VALUE),
                                            net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                                    .build())));

    // ── Champion system items ─────────────────────────────────────────────────

    public static final DeferredItem<ChampionCrystalItem> CHAMPION_CRYSTAL =
            ITEMS.registerItem("champion_crystal", ChampionCrystalItem::new);

    public static final DeferredItem<SpellbookItem> SPELLBOOK =
            ITEMS.registerItem("spellbook", SpellbookItem::new);

    // ── Champion weapons ──────────────────────────────────────────────────────

    public static final DeferredItem<VayneWeaponItem> VAYNE_WEAPON =
            ITEMS.registerItem("vayne_weapon", VayneWeaponItem::new);
    // Add more weapons here as you implement champions:
    // public static final DeferredItem<GarenSwordItem> GAREN_WEAPON =
    //         ITEMS.registerItem("garen_weapon", properties -> new GarenSwordItem());

    // ── Creative tab ──────────────────────────────────────────────────────────

    public static final Supplier<CreativeModeTab> RUNETERRA_TAB = CREATIVE_TABS.register(
            "runeterra_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.runeterra"))
                    .icon(() -> CHAMPION_CRYSTAL.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(CHAMPION_CRYSTAL.get());
                        output.accept(SPELLBOOK.get());
                        output.accept(ELDER_DRAKE_SPAWN_EGG.get());
                        output.accept(SCUTTLE_CRAB_SPAWN_EGG.get());
                        output.accept(DRAKEBLADE.get());
                        output.accept(VAYNE_WEAPON.get());
                    })
                    .build());

    // ── Registration ──────────────────────────────────────────────────────────

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);

        // Register champion weapons with WeaponRegistry
        WeaponRegistry.register("vayne", VAYNE_WEAPON::get);
        // WeaponRegistry.register("garen", GAREN_WEAPON::get);
    }
}