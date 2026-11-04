package com.blamejared.controlling.client.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;

import org.lwjgl.input.Keyboard;

import com.blamejared.controlling.api.KeyContext;

public class GuiAddContext extends GuiScreen {

    private final GuiEditContexts parent;
    private GuiTextField className;
    private String warning;

    public GuiAddContext(GuiEditContexts parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        int x = width / 2 - 150;
        className = new GuiTextField(fontRendererObj, x, 76, 300, 20);
        className.setMaxStringLength(512);
        className.setFocused(true);
        buttonList.add(new GuiButton(0, x, height - 52, 145, 20, I18n.format("controlling.context.add")));
        buttonList.add(new GuiButton(1, x + 155, height - 52, 145, 20, I18n.format("gui.cancel")));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 1) {
            mc.displayGuiScreen(parent);
            return;
        }
        String value = className.getText().trim();
        if (value.isEmpty()) {
            warning = I18n.format("controlling.context.classEmpty");
            return;
        }
        parent.addCustomContext(KeyContext.custom(value));
        mc.displayGuiScreen(parent);
    }

    @Override
    public void drawScreen(int x, int y, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, I18n.format("controlling.context.addTitle"), width / 2, 22, 0xffffff);
        drawString(fontRendererObj, I18n.format("controlling.context.classPrompt"), width / 2 - 150, 60, 0xffffff);
        className.drawTextBox();
        drawString(
                fontRendererObj,
                fontRendererObj.trimStringToWidth(I18n.format("controlling.context.classExample"), 300),
                width / 2 - 150,
                102,
                0xaaaaaa);
        if (warning != null) drawString(
                fontRendererObj,
                fontRendererObj.trimStringToWidth(warning, 300),
                width / 2 - 150,
                118,
                0xffff55);
        super.drawScreen(x, y, partialTicks);
    }

    @Override
    protected void mouseClicked(int x, int y, int button) {
        super.mouseClicked(x, y, button);
        className.mouseClicked(x, y, button);
    }

    @Override
    protected void keyTyped(char character, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parent);
            return;
        }
        if (className.textboxKeyTyped(character, keyCode)) {
            warning = null;
        }
    }

    @Override
    public void updateScreen() {
        className.updateCursorCounter();
    }
}
