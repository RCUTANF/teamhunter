package com.rcutanf.teamhunter.client.guide_sys.impl.checker;

import com.rcutanf.teamhunter.client.guide_sys.AbstractGuideSysChecker;
import com.rcutanf.teamhunter.client.guide_sys.TriggerType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.*;

/**
 * Hot Stuff 成就检查器
 * 检查玩家是否获得铁锭/铁桶或附近有熔岩
 */
public class HotStuffChecker extends AbstractGuideSysChecker {


    private int ironIngotCount = 0; // 当前铁锭数量
    private boolean hasLavaNearby = false; // 附近是否有熔岩

    public HotStuffChecker() {
        // 设置成就ID和使用物品栏+方块检测触发器
        super(Identifier.of("minecraft", "story/lava_bucket"), "hot_stuff", Arrays.asList(TriggerType.inventory, TriggerType.surroundingBlock));
        conditions.add(new Condition("bucket", "具备铁桶", 50));
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
            switch (itemId) {
                case "minecraft:iron_ingot":
                    if(!isActive()){setActive(true);}
                    Condition bucketCondition = getConditionByName("bucket");
                    if (bucketCondition != null) {
                        ironIngotCount = itemStack.getCount();
                        if (ironIngotCount >= 3) {
                            bucketCondition.setInsideProgress(49);
                        }
                        else{
                            bucketCondition.setInsideProgress(ironIngotCount * 50 / 3);//会舍去成差一点满，刚好能示意需要合成铁桶
                        }
                    }
                    updateTotalProgress();
                    break;

                case "minecraft:bucket":
                    if(!isActive()){setActive(true);}
                    Condition bucketCondition2 = getConditionByName("bucket");
                    if (bucketCondition2 != null) {
                        bucketCondition2.finish();
                    }
                    updateTotalProgress();
                    break;
                default:
                    // 其他物品不处理
                    break;
            }
        }


    }

    private void handleBlockEvent(Map<String, Object> data) {
        @SuppressWarnings("unchecked")
        Set<String> blockTypes = (Set<String>) data.get("blockTypes");

        // 检查附近是否有熔岩
        hasLavaNearby = blockTypes.contains("block.minecraft.lava");
        Condition lavaConditon = getConditionByName("lava");
        if (hasLavaNearby) {
            if (lavaConditon != null) {
                if(!isActive()){setActive(true);}
                lavaConditon.finish();
                updateTotalProgress();
            }
        }
        else  {
            if (lavaConditon != null) {
                lavaConditon.setUnfinished();
                updateTotalProgress();
            }
        }
    }
}