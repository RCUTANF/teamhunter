package com.rcutanf.teamhunter.client.guide_sys.def;

import com.rcutanf.teamhunter.client.guide_sys.TriggerType;

import java.util.List;
import java.util.Map;


/**
 * 成就定义json的数据结构
 * 每个json的文件名为该成就英文名的驼峰命名法+Checker命名，如 "HotStuffChecker.json"。如遇到特殊字符（比如&）,使用对应的英文单词替代（比如 "HotAndStuffChecker.json"）
 */
public class AchievementDefinition {
    public String id; // 使用该成就的英文名称的下划线命名法命名，如 "hot_stuff"
    public String advancementId; // Minecraft 成就 ID，如 "minecraft:story/lava_bucket"
    public List<ConditionDefinition> conditions;//条件列表

    public static class ConditionDefinition {
        public String name; // 条件名称，英文下划线命名法命名，如 "collect_lava_bucket"
        public String description;//条件描述，显示在游戏中
        public int weight; // 条件权重，即占总进度的百分比。所有条件的权重之和应为100
        public boolean isHintOnly = false; // 默认为false，表示是否仅为提示不计算进度
        public List<RequirementDefinition> requirements;//该条件实际检测的实际需求

        public static class RequirementDefinition {
            public TriggerType triggerType; // 要注册的触发器类型，详见触发器类型enum
            public String matchKey; // 要匹配（检查）的id，比如"minecraft:bucket", "block.minecraft.lava"
            public Map<String, Object> components;// Minecraft 组件定义，默认为空。支持按照 Mojang 格式定义，用于更精确的物品/实体等匹配。部分参数支持模糊定义、宽泛定义，详见下方补充说明。
            public boolean partialMatch = false; // 默认为false，表示是否启用部分匹配。true时matchKey支持部分匹配，false时为精确匹配
            public int matchCount; // 需要的数量，通常为1。检查物品时可能需要大于1
            public int weight; // 该需求的权重。注意，所有需求总和为条件的权重。例如，如果本需求所属的条件权重为50，本需求只能占比低于50的权重，且所有应该计算的需求权重之和应等于该条件权重
            public boolean typeAnd; //定义条件的类型 true=与逻辑，false=或逻辑。详见下方定义逻辑补充
            public int OrGroupId; // 用于或逻辑分组，从0开始编号，相同编号的需求属于同一组
            public int priority; // 数值越小优先级越高，用于决定组合策略
        }
    }
}

/**
  * requirements 中 typeAnd 字段与分组/进度计算的逻辑补充说明：
  *
  * 总体思想：
  * - 将同一条件（Condition）内的需求（Requirement）按 `typeAnd` 字段分为两类处理。
  * - `typeAnd = true` 的需求：直接累加其 `insideProgress` 到条件总进度中。
  * - `typeAnd = false` 的需求：按 `OrGroupId` 分组，每组内按优先级选取第一个非零进度值。
  * - 条件的总体进度等于所有 `typeAnd = true` 需求的进度之和 + 所有 OR 组的进度之和。
  *
  * 字段语义与约束：
  * 1. `weight`
  *    - 表示该需求在所属条件中的最大贡献值（即该需求的满进度值）。
  *    - 某个条件内所有需求的 `weight` 总和应等于该条件的最大进度（或由设计保证不会超过条件的 max）。
  *    - 触发器触发时应把需求的 `insideProgress` 设置到 [0, weight] 范围内。
  *
  * 2. `typeAnd`
  *    - **true**：表示该需求使用"与（AND）"逻辑，其 `insideProgress` 会直接累加到条件的总进度中。
  *    - **false**：表示该需求使用"或（OR）"逻辑，需要与其他 `typeAnd = false` 的需求按 `OrGroupId` 分组处理。
  *
  * 3. `OrGroupId`
  *    - 仅对 `typeAnd = false` 的需求有效，用于将多个需求分为若干组。
  *    - 分组 ID 可以是任意整数，相同 ID 的需求属于同一组。
  *    - 在每个 OR 组内，程序会按 `priority` 升序排序，选取第一个 `insideProgress > 0` 的需求作为该组的进度贡献。
  *    - 如果组内所有需求的 `insideProgress` 都为 0，则该组的进度贡献为 0。
  *
  * 4. `priority`
  *    - 数值越小优先级越高，用于在 OR 组内决定需求的选择顺序。
  *    - 当同一 OR 组内有多个需求的 `insideProgress > 0` 时，优先选择 `priority` 最小的需求。
  *    - 对于 `typeAnd = true` 的需求，`priority` 字段不影响进度计算，但可用于其他逻辑处理。
  *
  * 进度计算流程：
  * 1. 遍历条件内的所有需求：
  *    - 如果 `typeAnd = true`，直接将其 `insideProgress` 累加到条件总进度。
  *    - 如果 `typeAnd = false`，将其收集到对应的 `OrGroupId` 组中。
  * 2. 处理每个 OR 组：
  *    - 按 `priority` 升序排序组内需求。
  *    - 选取第一个 `insideProgress > 0` 的需求，将其进度值累加到条件总进度。
  *    - 如果组内所有需求的进度都为 0，则该组不贡献进度。
  * 3. 条件的最终进度 = 所有 AND 需求的进度之和 + 所有 OR 组的进度之和。
  *
  * 设计约定与注意事项：
  * - 同一条件内，所有需求的 `weight` 总和应合理设计，避免超过条件的 `maxProgress`。
  * - OR 组内的需求通常具有相同的 `weight`，因为它们是互斥选择关系。
  * - `priority` 的设置应考虑游戏逻辑的合理性，优先级高的需求应该是更容易或更重要的选择。
  * - 单个需求的 `insideProgress` 由各触发器在事件发生时设置，范围不应超过该需求的 `weight`。
  * - 条件的 `insideProgress` 达到或超过其 `maxProgress` 时，会调用 `condition.finish()` 标记该条件完成。
  */

/**
 * components 字段的模糊匹配与宽泛定义说明：
 *
 * 1. minecraft:potion_contents 中的 potion 字段：
 *    - 原理解释：代码使用 String.contains() 进行部分匹配以支持模糊匹配
 *    - 可以只声明部分关键词，无需完整ID
 *    - 示例：
 *      > 声明 "healing" 可匹配所有治疗类药水（如 instant_healing、long_healing 等）
 *      > 声明 "long" 可匹配所有持续4分钟的药水
 *    - 注意：请勿带上命名空间前缀（如 minecraft:），会导致匹配失败
 *
 * 2. minecraft:enchantments 附魔匹配：
 *    - 可以不声明 level 字段
 *    - 只声明标准的附魔名称即可匹配所有等级
 *    - 示例：声明 "minecraft:sharpness" 可匹配锋利I到锋利V的所有等级
 */
