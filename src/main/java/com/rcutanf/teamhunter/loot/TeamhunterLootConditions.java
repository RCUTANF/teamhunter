package com.rcutanf.teamhunter.loot;

import net.minecraft.loot.condition.LootConditionType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class TeamhunterLootConditions {
    public static final LootConditionType HAS_COMMAND_TAG = new LootConditionType(
            HasCommandTagCondition.CODEC
    );

    public static void register() {
        System.out.println("[TeamHunter] 注册战利品条件: " + HasCommandTagCondition.ID);
        Registry.register(Registries.LOOT_CONDITION_TYPE, HasCommandTagCondition.ID, HAS_COMMAND_TAG);
    }
}
