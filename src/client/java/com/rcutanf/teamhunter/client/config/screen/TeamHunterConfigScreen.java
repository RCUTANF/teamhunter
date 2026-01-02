package com.rcutanf.teamhunter.client.config.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class TeamHunterConfigScreen extends Screen {
    private final Screen parent;

    public TeamHunterConfigScreen(Screen parent) {
        super(Text.translatable("teamhunter.config.main.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = 60;
        int spacing = 30;
        int currentY = startY;
        int buttonWidth = 200;
        int buttonHeight = 20;

        // 雷达配置按钮
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("teamhunter.config.radar"),
                button -> this.client.setScreen(new RadarConfigScreen(this))
        ).dimensions(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight).build());
        currentY += spacing;

        // 引导系统配置按钮
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("teamhunter.config.guidesys"),
                button -> this.client.setScreen(new GuideSysConfigScreen(this))
        ).dimensions(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight).build());
        currentY += spacing;

        // 预留未来功能配置按钮
        // 示例：队伍管理配置
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("teamhunter.config.team").append(Text.translatable("teamhunter.config.coming_soon")),
                button -> {}
        ).dimensions(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight).build());
        currentY += spacing;

        // 示例：通用设置配置
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("teamhunter.config.general").append(Text.translatable("teamhunter.config.coming_soon")),
                button -> {}
        ).dimensions(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight).build());
        currentY += spacing;

        // 返回/完成按钮
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.done"),
                button -> this.close()
        ).dimensions(centerX - 100, this.height - 40, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {

        // 标题
        context.drawCenteredTextWithShadow(this.textRenderer, this.title,
            this.width / 2, 20, 0xFFFFFF);

        // 描述文本
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("teamhunter.config.main.description"),
                this.width / 2, 35, 0xAAAAAA);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }
}