package com.rcutanf.teamhunter.client.ui;

import com.rcutanf.teamhunter.client.config.RadarConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class RadarConfigScreen extends Screen {
    private final Screen parent;
    private RadarConfig config;

    public RadarConfigScreen(Screen parent) {
        super(Text.translatable("teamhunter.config.title"));
        this.parent = parent;
        this.config = RadarConfig.getInstance();
    }

    @Override
    protected void init() {
        // 雷达尺寸滑块 (30-200)
        this.addDrawableChild(new SliderWidget(this.width / 2 - 100, 50, 200, 20,
                Text.translatable("teamhunter.config.size", config.getRadarSize()),
                (config.getRadarSize() - 30) / 170.0) {
            @Override
            protected void updateMessage() {
                setMessage(Text.translatable("teamhunter.config.size", getValue()));
            }

            @Override
            protected void applyValue() {
                int newSize = getValue();
                config.setRadarSize(newSize);
                // 确保值被立即应用到静态变量
                PlayerRadarHud.setRadarSize(newSize);
            }

            private int getValue() {
                return (int) (30 + value * 170);
            }
        });

        // X坐标滑块 (0-300)
        this.addDrawableChild(new SliderWidget(this.width / 2 - 100, 80, 200, 20,
                Text.translatable("teamhunter.config.x", config.getRadarX()),
                Math.min(1.0, config.getRadarX() / 300.0)) {
            @Override
            protected void updateMessage() {
                setMessage(Text.translatable("teamhunter.config.x", getValue()));
            }

            @Override
            protected void applyValue() {
                config.setRadarX(getValue());
            }

            private int getValue() {
                return (int) (value * 300);
            }
        });

        // Y坐标滑块 (0-300)
        this.addDrawableChild(new SliderWidget(this.width / 2 - 100, 110, 200, 20,
                Text.translatable("teamhunter.config.y", config.getRadarY()),
                Math.min(1.0, config.getRadarY() / 300.0)) {
            @Override
            protected void updateMessage() {
                setMessage(Text.translatable("teamhunter.config.y", getValue()));
            }

            @Override
            protected void applyValue() {
                config.setRadarY(getValue());
            }

            private int getValue() {
                return (int) (value * 300);
            }
        });

        // 玩家名称缩放滑块 (0.2-1.0)
        this.addDrawableChild(new SliderWidget(this.width / 2 - 100, 170, 200, 20,
                Text.translatable("teamhunter.config.name_scale", String.format("%.2f", config.getNameScale())),
                (config.getNameScale() - 0.2f) / 0.8f) {
            @Override
            protected void updateMessage() {
                setMessage(Text.translatable("teamhunter.config.name_scale", String.format("%.2f", getValue())));
            }

            @Override
            protected void applyValue() {
                config.setNameScale(getValue());
            }

            private float getValue() {
                return (float) (0.2 + value * 0.8);
            }
        });

        // 启用/禁用按钮
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable(config.isEnabled() ? "teamhunter.config.enabled" : "teamhunter.config.disabled"),
                button -> {
                    config.setEnabled(!config.isEnabled());
                    button.setMessage(Text.translatable(config.isEnabled() ? "teamhunter.config.enabled" : "teamhunter.config.disabled"));
                }
        ).dimensions(this.width / 2 - 100, 140, 200, 20).build());

        // 保存按钮
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("teamhunter.config.save"),
                button -> {
                    RadarConfig.saveConfig();
                    this.close();
                }
        ).dimensions(this.width / 2 - 100, this.height - 50, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }
}