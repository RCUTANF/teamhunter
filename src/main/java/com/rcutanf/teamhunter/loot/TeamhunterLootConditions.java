package com.rcutanf.teamhunter.loot;

import com.rcutanf.teamhunter.Teamhunter;
import net.minecraft.loot.condition.LootConditionType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class TeamhunterLootConditions {
    public static final LootConditionType HAS_COMMAND_TAG = new LootConditionType(new HasCommandTagCondition.Serializer());

    public static void register() {
        Registry.register(Registries.LOOT_CONDITION_TYPE, HasCommandTagCondition.ID, HAS_COMMAND_TAG);
    }
}