package com.rcutanf.teamhunter.buff;

    /**
     * Buff接口，定义Buff的基本属性和行为。
     */
    public interface Buff {
        String getId();
        BuffType getType();
        String getName();
        String getDescription();

        void apply();

        void remove();

        //TODO：抽象的target，以及buff时间


    }