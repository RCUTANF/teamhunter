package com.rcutanf.teamhunter.client.ui;

import com.rcutanf.teamhunter.client.config.GuideSysConfig;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
import net.minecraft.text.Text;

public class GuideSysConfigScreen extends Screen {
    private final Screen parent;
    private GuideSysConfig config;
    private final ThreePartsLayoutWidget layoutWidget = new ThreePartsLayoutWidget(this);

    public GuideSysConfigScreen(Screen parent) {
        super(Text.translatable("teamhunter.guidesys.config.title"));
        this.parent = parent;
        this.config = GuideSysConfig.getInstance();
    }

    @Override
    protected void init() {
        // 添加标题
        this.layoutWidget.addHeader(this.title, this.textRenderer);

        // 创建主体网格布局
        GridWidget gridWidget = this.layoutWidget.addBody(new GridWidget());
        gridWidget.getMainPositioner().margin(4, 4, 4, 0);
        GridWidget.Adder adder = gridWidget.createAdder(1);

        // 添加所有配置控件
        addConfigWidgets(adder);

        // 添加底部保存按钮
        this.layoutWidget.addFooter(ButtonWidget.builder(
                Text.translatable("teamhunter.guidesys.config.save"),
                button -> {
                    GuideSysConfig.saveConfig();
                    this.close();
                }
        ).width(200).build());

        this.layoutWidget.forEachChild(this::addDrawableChild);
        this.refreshWidgetPositions();
    }

    private void addConfigWidgets(GridWidget.Adder adder) {
        // 启用/禁用按钮
        adder.add(ButtonWidget.builder(
                Text.translatable(config.isEnabled() ? "teamhunter.guidesys.config.enabled" : "teamhunter.guidesys.config.disabled"),
                button -> {
                    config.setEnabled(!config.isEnabled());
                    button.setMessage(Text.translatable(config.isEnabled() ? "teamhunter.guidesys.config.enabled" : "teamhunter.guidesys.config.disabled"));
                }
        ).width(300).build());

        // X坐标滑块
        adder.add(new SliderWidget(0, 0, 300, 20,
                Text.translatable("teamhunter.guidesys.config.x", config.getPosX()),
                config.getPosX() / 500.0) {
            @Override
            protected void updateMessage() {
                setMessage(Text.translatable("teamhunter.guidesys.config.x", getValue()));
            }
            @Override
            protected void applyValue() {
                config.setPosX(getValue());
            }
            private int getValue() {
                return (int) (value * 500);
            }
        });

        // Y坐标滑块
        adder.add(new SliderWidget(0, 0, 300, 20,
                Text.translatable("teamhunter.guidesys.config.y", config.getPosY()),
                config.getPosY() / 400.0) {
            @Override
            protected void updateMessage() {
                setMessage(Text.translatable("teamhunter.guidesys.config.y", getValue()));
            }
            @Override
            protected void applyValue() {
                config.setPosY(getValue());
            }
            private int getValue() {
                return (int) (value * 400);
            }
        });

        // 宽度滑块 (140-500)
        adder.add(new SliderWidget(0, 0, 300, 20,
                Text.translatable("teamhunter.guidesys.config.width", config.getWidth()),
                (config.getWidth() - 140) / 360.0) {
            @Override
            protected void updateMessage() {
                setMessage(Text.translatable("teamhunter.guidesys.config.width", getValue()));
            }
            @Override
            protected void applyValue() {
                config.setWidth(getValue());
            }
            private int getValue() {
                return (int) (140 + value * 360);
            }
        });

        // 最大显示项目数滑块 (3-100)
        adder.add(new SliderWidget(0, 0, 300, 20,
                Text.translatable("teamhunter.guidesys.config.max_items", config.getMaxItems()),
                (config.getMaxItems() - 3) / 97.0) {
            @Override
            protected void updateMessage() {
                setMessage(Text.translatable("teamhunter.guidesys.config.max_items", getValue()));
            }
            @Override
            protected void applyValue() {
                config.setMaxItems(getValue());
            }
            private int getValue() {
                return (int) (3 + value * 97);
            }
        });

        // 文字缩放滑块
        adder.add(new SliderWidget(0, 0, 300, 20,
                Text.translatable("teamhunter.guidesys.config.text_scale", String.format("%.2f", config.getTextScale())),
                (config.getTextScale() - 0.5f) / 1.5f) {
            @Override
            protected void updateMessage() {
                setMessage(Text.translatable("teamhunter.guidesys.config.text_scale", String.format("%.2f", getValue())));
            }
            @Override
            protected void applyValue() {
                config.setTextScale(getValue());
            }
            private float getValue() {
                return (float) (0.5 + value * 1.5);
            }
        });

        // 显示背景按钮
        adder.add(ButtonWidget.builder(
                Text.translatable(config.isShowBackground() ? "teamhunter.guidesys.config.background_on" : "teamhunter.guidesys.config.background_off"),
                button -> {
                    config.setShowBackground(!config.isShowBackground());
                    button.setMessage(Text.translatable(config.isShowBackground() ? "teamhunter.guidesys.config.background_on" : "teamhunter.guidesys.config.background_off"));
                }
        ).width(300).build());

        // 提示文本轮播开关
        adder.add(ButtonWidget.builder(
                Text.translatable(config.isEnableHintCarousel() ? "teamhunter.guidesys.config.hint_carousel_on" : "teamhunter.guidesys.config.hint_carousel_off"),
                button -> {
                    config.setEnableHintCarousel(!config.isEnableHintCarousel());
                    button.setMessage(Text.translatable(config.isEnableHintCarousel() ? "teamhunter.guidesys.config.hint_carousel_on" : "teamhunter.guidesys.config.hint_carousel_off"));
                }
        ).width(300).build());

        // 提示文本切换间隔滑块
        adder.add(new SliderWidget(0, 0, 300, 20,
                Text.translatable("teamhunter.guidesys.config.hint_interval", config.getHintSwitchInterval() / 1000.0f),
                (config.getHintSwitchInterval() - 1000) / 9000.0) {
            @Override
            protected void updateMessage() {
                setMessage(Text.translatable("teamhunter.guidesys.config.hint_interval", getValue() / 1000.0f));
            }
            @Override
            protected void applyValue() {
                config.setHintSwitchInterval(getValue());
            }
            private int getValue() {
                return (int) (1000 + value * 9000);
            }
        });
    }

    @Override
    protected void refreshWidgetPositions() {
        this.layoutWidget.refreshPositions();
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }
}