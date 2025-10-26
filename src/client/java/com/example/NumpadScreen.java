package com.example;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class NumpadScreen extends Screen {

    public NumpadScreen() {
        super(Text.literal("Numpad"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xFF202020); // background
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }
}
