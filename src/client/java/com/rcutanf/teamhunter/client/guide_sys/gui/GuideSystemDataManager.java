package com.rcutanf.teamhunter.client.guide_sys.gui;


import com.rcutanf.teamhunter.Teamhunter;
import com.rcutanf.teamhunter.client.guide_sys.advancementListener.AdvancementEventManager;
import com.rcutanf.teamhunter.client.guide_sys.advancementListener.AdvancementRecord;
import com.rcutanf.teamhunter.client.guide_sys.def.AchievementChecker;
import com.rcutanf.teamhunter.client.guide_sys.gui.data.*;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;


public class GuideSystemDataManager {
    private static GuideSystemDataManager instance;
    private final List<GuideData> incompleteGuides = new ArrayList<>();
    private final List<CompletedGuideData> completedGuides = new ArrayList<>();

    private GuideSystemDataManager() {}

    public static GuideSystemDataManager getInstance() {
        if (instance == null) {
            instance = new GuideSystemDataManager();
        }
        return instance;
    }

    // 获取未完成指南数据
    public List<GuideData> getIncompleteGuides() {
        return new ArrayList<>(incompleteGuides);
    }

    // 获取已完成指南数据
    public List<CompletedGuideData> getCompletedGuides() {
        refreshCompletedGuides(); // 每次获取时刷新
        return new ArrayList<>(completedGuides);
    }

    // 添加未完成指南
    public void addIncompleteGuide(AchievementChecker checker, int progress) {
        try {
            GuideData guideData = GuideData.fromAchievementChecker(checker, progress);
            if (guideData != null) {
                // 检查是否已存在，如果存在则更新
                incompleteGuides.removeIf(guide -> guide.getId().equals(checker.advancementId));
                incompleteGuides.add(0, guideData);
            }
        } catch (Exception e) {
            Teamhunter.LOGGER.info("添加指南时出错: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    // 更新指南进度
    public void updateGuideProgress(AchievementChecker checker, int progress) {
        for (int i = 0; i < incompleteGuides.size(); i++) {
            GuideData guide = incompleteGuides.get(i);
            if (guide.getId().equals(checker.advancementId)) {
                // 更新现有指南
                GuideData updatedGuide = GuideData.fromAchievementChecker(checker, progress);
                if (updatedGuide != null) {
                    incompleteGuides.set(i, updatedGuide);
                    // 移动到顶部
                    incompleteGuides.add(0, incompleteGuides.remove(i));
                }
                return;
            }
        }
        // 如果不存在且进度大于0，则添加
        if (progress > 0) {
            addIncompleteGuide(checker, progress);
        }
    }

    // 标记指南完成
    public void markGuideComplete(Identifier advancementId) {
        incompleteGuides.removeIf(guide -> {
            if (guide.getId().equals(advancementId)) {
                // 可以在这里添加完成动画逻辑
                return true;
            }
            return false;
        });
    }

    // 移除指南
    public void removeGuide(Identifier advancementId) {
        incompleteGuides.removeIf(guide -> guide.getId().equals(advancementId));
    }

    // 清空未完成指南
    public void clearIncompleteGuides() {
        incompleteGuides.clear();
    }

    // 刷新已完成指南列表
    private void refreshCompletedGuides() {
        completedGuides.clear();
        List<AdvancementRecord> records = AdvancementEventManager.getInstance().getAdvancementRecords();

        for (AdvancementRecord record : records) {
            CompletedGuideData completedData = CompletedGuideData.fromAdvancementRecord(record);
            if (completedData != null) {
                completedGuides.add(completedData);
            }
        }

        // 按完成时间排序
        completedGuides.sort((a, b) -> Long.compare(a.getCompletionTime(), b.getCompletionTime()));
    }
}