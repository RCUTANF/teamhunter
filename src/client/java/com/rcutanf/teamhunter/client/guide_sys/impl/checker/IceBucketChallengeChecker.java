package com.rcutanf.teamhunter.client.guide_sys.impl.checker;

import com.rcutanf.teamhunter.client.guide_sys.AbstractGuideSysChecker;
import com.rcutanf.teamhunter.client.guide_sys.TriggerType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 * Hot Stuff 成就检查器
 * 检查玩家是否获得铁锭/铁桶或附近有熔岩
 */
public class IceBucketChallengeChecker extends AbstractGuideSysChecker {


    private boolean hasLavaNearby = false; // 附近是否有熔岩

    public IceBucketChallengeChecker() {
        // 设置成就ID和使用物品栏+方块检测触发器
        super(Identifier.of("minecraft", "story/form_obsidian"), "ice_bucket_challenge", Arrays.asList(TriggerType.inventory, TriggerType.surroundingBlock));
        conditions.add(new Condition("diamond_pickaxe", "具备钻石镐", 50));
        conditions.add(new Condition("lava", "附近可获得岩浆", 50));
    }

    @Override
    public void onEvent(Object eventData) {
        // 如果成就已完成，不再处理
        if (isCompleted()) {
            return;
        }

        // 根据事件数据类型处理不同触发器
        if (eventData instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) eventData;

            // 通过数据内容判断是哪种触发器
            if (data.containsKey("itemStack") && data.containsKey("isAdded")) {
                // 物品栏事件
                handleInventoryEvent(data);
            } else if (data.containsKey("blockTypes")) {
                // 方块检测事件
                handleBlockEvent(data);
            }
        }
    }

    private void handleInventoryEvent(Map<String, Object> data) {
        ItemStack itemStack = (ItemStack) data.get("itemStack");
        boolean isAdded = (boolean) data.get("isAdded");

        String itemId = itemStack.getItem().toString();

        if (isAdded) {
            if(itemId.equals("minecraft:diamond_pickaxe")){
                if(!isActive()){setActive(true);}
                Condition diamond_pickaxe = getConditionByName("diamond_pickaxe");
                if (diamond_pickaxe != null) {
                    diamond_pickaxe.finish();
                }
                updateTotalProgress();
            }
        }
    }

    private void handleBlockEvent(Map<String, Object> data) {
        @SuppressWarnings("unchecked")
        Set<String> blockTypes = (Set<String>) data.get("blockTypes");

        // 检查附近是否有熔岩
        hasLavaNearby = blockTypes.contains("block.minecraft.lava");
        Condition lavaCondition = getConditionByName("lava");
        if (hasLavaNearby) {
            if (lavaCondition != null) {
                if(!isActive()){setActive(true);}
                lavaCondition.finish();
                updateTotalProgress();
            }
        }
        else  {
            if (lavaCondition != null) {
                lavaCondition.setUnfinished();
                updateTotalProgress();
            }
        }
    }
}