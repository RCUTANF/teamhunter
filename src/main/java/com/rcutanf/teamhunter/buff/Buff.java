package com.rcutanf.teamhunter.buff;

    import net.minecraft.server.MinecraftServer;

    /**
     * 抽象Buff类，代表一个可以应用于目标的状态效果
     */
    public abstract class Buff {
        // 基本属性
        protected abstract int getId();
        protected abstract String getName();
        protected abstract String getDescription();
        protected abstract int getDuration();  // 持续时间，-1表示永久
        protected abstract boolean isPositive();  // 是否为正面效果

        // 服务器实例，用于访问游戏资源
        protected MinecraftServer server;

        // 当前状态
        protected int remainingDuration = 0;
        protected boolean isActive = false;

        /**
         * 构造函数
         */
        public Buff(MinecraftServer server) {
            this.server = server;
        }

        /**
         * 初始化Buff
         */
        public void initialize() {
            initialize(getDuration());
        }

        /**
         * 使用指定持续时间初始化Buff
         */
        public void initialize(int duration) {
            this.remainingDuration = duration;
            this.isActive = true;
        }

        /**
         * 应用Buff效果
         */
        public abstract void applyEffect();

        /**
         * 移除Buff效果
         */
        public abstract void removeEffect();

        /**
         * 更新Buff状态
         * @return 如果Buff仍然有效返回true，否则返回false
         */
        public boolean update() {
            if (!isActive) return false;

            // 永久Buff不减少持续时间
            if (remainingDuration == -1) return true;

            remainingDuration--;
            if (remainingDuration <= 0) {
                isActive = false;
                return false;
            }
            return true;
        }

        /**
         * 强制移除Buff
         */
        public void dispel() {
            isActive = false;
            remainingDuration = 0;
        }

        /**
         * 获取Buff是否激活
         */
        public boolean isActive() {
            return isActive;
        }

        /**
         * 获取剩余持续时间
         */
        public int getRemainingDuration() {
            return remainingDuration;
        }
    }