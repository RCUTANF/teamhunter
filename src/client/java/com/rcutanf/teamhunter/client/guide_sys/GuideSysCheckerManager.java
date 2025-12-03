package com.rcutanf.teamhunter.client.guide_sys;

import com.rcutanf.teamhunter.client.guide_sys.advancementListener.AdvancementEventManager;
import com.rcutanf.teamhunter.client.guide_sys.gui.GuideSystemDataManager;
import com.rcutanf.teamhunter.client.guide_sys.impl.AchievementLoader;
import com.rcutanf.teamhunter.client.guide_sys.impl.trigger.DimensionTrigger;
import com.rcutanf.teamhunter.client.guide_sys.impl.trigger.InventoryTrigger;
import com.rcutanf.teamhunter.client.guide_sys.impl.checker.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * GuideSysChecker的管理器类
 * 负责所有Checker的创建、查询和管理
 */
public class GuideSysCheckerManager {
    private static GuideSysCheckerManager instance;
    private final Map<String, AbstractGuideSysChecker> checkers;

    // 私有构造函数确保单例模式
    private GuideSysCheckerManager() {
        checkers = new HashMap<>();
    }

    // 单例获取方法
    public static synchronized GuideSysCheckerManager getInstance() {
        if (instance == null) {
            instance = new GuideSysCheckerManager();
        }
        return instance;
    }

    /**
     * 注册一个检查器
     * @param checker 要注册的检查器
     */
    public void registerChecker(AbstractGuideSysChecker checker) {
        if (checker != null) {
            checkers.put(checker.getCheckerID(), checker);
        }
    }

    /**
     * 根据ID移除检查器
     * @param checkerID 检查器ID
     */
    public void removeChecker(String checkerID) {
        AbstractGuideSysChecker checker = checkers.remove(checkerID);
        if (checker != null) {
            checker.unregister(); // 确保触发器取消注册
        }
    }

    /**
     * 根据ID获取检查器
     * @param checkerID 检查器ID
     * @return 对应的检查器，如果不存在则返回null
     */
    public AbstractGuideSysChecker getChecker(String checkerID) {
        return checkers.get(checkerID);
    }

    /**
     * 获取所有检查器
     * @return 所有检查器的列表
     */
    public List<AbstractGuideSysChecker> getAllCheckers() {
        return new ArrayList<>(checkers.values());
    }

    /**
     * 获取已完成的检查器
     * @return 已完成的检查器列表
     */
    public List<AbstractGuideSysChecker> getCompletedCheckers() {
        return checkers.values().stream()
                .filter(AbstractGuideSysChecker::isCompleted)
                .collect(Collectors.toList());
    }

    /**
     * 获取未完成的检查器
     * @return 未完成的检查器列表
     */
    public List<AbstractGuideSysChecker> getIncompleteCheckers() {
        return checkers.values().stream()
                .filter(checker -> !checker.isCompleted())
                .collect(Collectors.toList());
    }

    /**
     * 清除所有检查器
     */
    public void clearAllCheckers() {
        checkers.values().forEach(checker -> {
            // 1. 取消触发器注册
            checker.unregister();
            // 2. 取消成就事件监听器注册
            AdvancementEventManager.getInstance().unregisterListener(checker);
        });
        GuideSystemDataManager.getInstance().clearIncompleteGuides();
        checkers.clear();
        System.out.println("已清理所有成就检查器资源");
    }

    /**
     * 初始化所有预定义的检查器
     * 应在系统启动时调用此方法
     */
    public void initializeAllCheckers() {
        // 清除任何可能存在的旧检查器
        clearAllCheckers();

        AchievementLoader.convertDefinitionsToCheckers();

        intializeCustomCheckers();

        System.out.println("已初始化 " + checkers.size() + " 个成就检查器");

        // 初始化完成后，遍历玩家物品栏中的所有物品并触发事件
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            PlayerInventory inventory = client.player.getInventory();

            // 获取InventoryTrigger
            InventoryTrigger trigger = (InventoryTrigger) GuideSysTriggerManager.getInstance()
                .getTrigger(TriggerType.inventory);

            if (trigger != null) {
                // 遍历物品
                for (int i = 0; i < inventory.size(); i++) {
                    ItemStack stack = inventory.getStack(i);
                    if (!stack.isEmpty()) {
                        Map<String, Object> eventData = new HashMap<>();
                        eventData.put("itemStack", stack);
                        eventData.put("isAdded", true);
                        trigger.fire(eventData);
                    }
                }
            }
        }

        //发送当前维度更新包触发维度触发器
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        //维度触发器
        DimensionTrigger dimensionTrigger = (DimensionTrigger) GuideSysTriggerManager.getInstance()
                .getTrigger(TriggerType.dimension);
        if (dimensionTrigger != null && player != null) {
            Identifier currentDimension = player.getEntityWorld().getRegistryKey().getValue();
            dimensionTrigger.fire(dimensionTrigger.createDimensionChangeEventData(null, currentDimension));
        }
    }

    /**
     * 重新加载所有检查器
     * 可用于配置更改后重新初始化
     */
    public void reloadCheckers() {
        initializeAllCheckers();
    }

    private void intializeCustomCheckers() {
        // 在此处添加自定义检查器的初始化代码
        new SubspaceBubbleChecker();
    }
}