package com.rcutanf.teamhunter.client.guide_sys;

import com.rcutanf.teamhunter.client.guide_sys.impl.trigger.*;
import java.util.function.Supplier;

public enum TriggerType {
    inventory(InventoryTrigger::new),//物品栏变化触发器,在玩家身上任意物品变化时触发
    biome(BiomeTrigger::new),//生物群系触发器,在玩家所在的生物群系变化时触发
    structure(StructureTrigger::new),//结构触发器,在玩家所在的结构变化时触发（包括结构为null的情况）
    surroundingBlock(SurroundingBlockTrigger::new),//周围方块触发器,在玩家周围方块变化时触发，使用周期轮询方式检查
    entity(EntityTrigger::new),//实体触发器,在玩家附近实体变化时触发，使用周期轮询方式检查
    dimension(DimensionTrigger::new),//,维度触发器,在玩家所在维度变化时触发
    weather(WeatherTrigger::new),//天气触发器,在玩家所在维度天气变化时触发
    height(HeightTrigger::new),//高度触发器,在玩家高度变化时触发，使用周期轮询方式检查
    //playerMove//玩家移动触发器,在玩家移动时触发，使用周期轮询方式检查
    advancementProcess(AdvancementProgressTrigger::new);//进度更新触发器,在进度产生更新时触发

    private final Supplier<AbstractGuideSysTrigger> factory;

    TriggerType(Supplier<AbstractGuideSysTrigger> factory) {
        this.factory = factory;
    }

    public AbstractGuideSysTrigger createTrigger() {
        return factory.get();
    }
}
//由于历史遗留问题，早期的使用了小写字母命名枚举值，现阶段为了兼容性仍然保留小写字母命名方式