package com.rcutanf.teamhunter.client.guide_sys;

public enum TriggerType {
    inventory,//物品栏变化触发器,在玩家身上任意物品变化时触发
    biome,//生物群系触发器,在玩家所在的生物群系变化时触发
    structure,//结构触发器,在玩家所在的结构变化时触发（包括结构为null的情况）
    surroundingBlock,//周围方块触发器,在玩家周围方块变化时触发，使用周期轮询方式检查
    entity,//实体触发器,在玩家附近实体变化时触发，使用周期轮询方式检查
    dimension,//,维度触发器,在玩家所在维度变化时触发
    weather,//天气触发器,在玩家所在维度天气变化时触发
    height//高度触发器,在玩家高度变化时触发，使用周期轮询方式检查
}
//由于历史遗留问题，早期的使用了小写字母命名枚举值，现阶段为了兼容性仍然保留小写字母命名方式
