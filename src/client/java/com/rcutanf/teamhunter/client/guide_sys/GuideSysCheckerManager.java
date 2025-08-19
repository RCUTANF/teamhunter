package com.rcutanf.teamhunter.client.guide_sys;

import com.rcutanf.teamhunter.client.guide_sys.impl.checker.AcquireHardwareChecker;
import com.rcutanf.teamhunter.client.guide_sys.impl.checker.GettingAnUpgradeChecker;
import com.rcutanf.teamhunter.client.guide_sys.impl.checker.HotStuffChecker;
import com.rcutanf.teamhunter.client.guide_sys.impl.checker.IsntItIronPickChecker;

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
        // 先取消所有检查器的注册
        checkers.values().forEach(AbstractGuideSysChecker::unregister);
        // 然后清空集合
        checkers.clear();
    }

    /**
     * 初始化所有预定义的检查器
     * 应在系统启动时调用此方法
     */
    public void initializeAllCheckers() {
        // 清除任何可能存在的旧检查器
        clearAllCheckers();

        // 实例化所有检查器
        // 注意：每个检查器的构造函数中会自动调用registerChecker
        new GettingAnUpgradeChecker();
        new AcquireHardwareChecker();
        new IsntItIronPickChecker();
        new HotStuffChecker();

        System.out.println("已初始化 " + checkers.size() + " 个成就检查器");
    }

    /**
     * 重新加载所有检查器
     * 可用于配置更改后重新初始化
     */
    public void reloadCheckers() {
        initializeAllCheckers();
    }
}