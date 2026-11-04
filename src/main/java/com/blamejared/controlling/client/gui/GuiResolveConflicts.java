package com.blamejared.controlling.client.gui;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

import com.blamejared.controlling.keybinding.BindingContextRegistry;
import com.blamejared.controlling.keybinding.IgnoredKeyConflicts;
import com.blamejared.controlling.keybinding.KeyConflictResolver;

public class GuiResolveConflicts extends GuiScreen {

    private final GuiNewControls parent;
    private final KeyBinding binding;
    private final List<KeyBinding> candidates = new ArrayList<>();
    private final Map<KeyBinding, Boolean> changes = new IdentityHashMap<>();
    private int page;
    private int pageSize;
    private String error;

    public GuiResolveConflicts(GuiNewControls parent, KeyBinding binding) {
        this.parent = parent;
        this.binding = binding;
        for (KeyBinding other : net.minecraft.client.Minecraft.getMinecraft().gameSettings.keyBindings) {
            if (!other.getKeyCategory().endsWith(".hidden") && ((KeyConflictResolver.sharesKey(binding, other)
                    && BindingContextRegistry.intersects(binding, other))
                    || IgnoredKeyConflicts.isIgnored(binding, other)))
                candidates.add(other);
        }
    }

    @Override
    public void initGui() {
        int x = width / 2 - 150;
        pageSize = Math.max(1, (height - 125) / 36);
        page = Math.min(page, Math.max(0, (candidates.size() - 1) / pageSize));
        for (int i = 0; i < pageSize; i++) buttonList.add(new GuiButton(100 + i, x, 35 + i * 36, 300, 20, ""));
        buttonList.add(new GuiButton(6, x, height - 80, 75, 20, I18n.format("controlling.context.previous")));
        buttonList.add(new GuiButton(7, x + 225, height - 80, 75, 20, I18n.format("controlling.context.next")));
        buttonList.add(new GuiButton(3, x, height - 52, 145, 20, I18n.format("controlling.context.save")));
        buttonList.add(new GuiButton(4, x + 155, height - 52, 145, 20, I18n.format("gui.cancel")));
        updateButtons();
    }

    private boolean ignored(KeyBinding other) {
        Boolean changed = changes.get(other);
        return changed == null ? IgnoredKeyConflicts.isIgnored(binding, other) : changed;
    }

    private void updateButtons() {
        for (GuiButton button : buttonList) {
            if (button.id >= 100) {
                int index = page * pageSize + button.id - 100;
                button.visible = index < candidates.size();
                if (button.visible) {
                    KeyBinding other = candidates.get(index);
                    button.displayString = fontRendererObj.trimStringToWidth(
                            (ignored(other) ? "[x] " : "[ ] ") + I18n.format(other.getKeyDescription()),
                            286);
                }
            } else if (button.id == 6) button.enabled = page > 0;
            else if (button.id == 7) button.enabled = (page + 1) * pageSize < candidates.size();
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        error = null;
        if (button.id >= 100) {
            KeyBinding other = candidates.get(page * pageSize + button.id - 100);
            boolean value = !ignored(other);
            if (value == IgnoredKeyConflicts.isIgnored(binding, other)) changes.remove(other);
            else changes.put(other, value);
        } else if (button.id == 6) page--;
        else if (button.id == 7) page++;
        else if (button.id == 4) {
            mc.displayGuiScreen(parent);
            return;
        } else if (button.id == 3) {
            if (changes.isEmpty() || IgnoredKeyConflicts.save(binding, changes)) {
                mc.displayGuiScreen(parent);
                return;
            }
            error = I18n.format("controlling.conflicts.saveError");
        }
        updateButtons();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(
                fontRendererObj,
                I18n.format("controlling.conflicts.title", I18n.format(binding.getKeyDescription())),
                width / 2,
                12,
                0xffffff);
        if (candidates.isEmpty())
            drawCenteredString(fontRendererObj, I18n.format("controlling.conflicts.none"), width / 2, 55, 0xaaaaaa);
        for (int i = 0; i < pageSize && page * pageSize + i < candidates.size(); i++) {
            KeyBinding other = candidates.get(page * pageSize + i);
            String category = I18n.format(other.getKeyCategory());
            String info = I18n.format("controlling.conflicts.category", category) + " - "
                    + BindingContextRegistry.describeContexts(other);
            drawString(
                    fontRendererObj,
                    fontRendererObj.trimStringToWidth(info, 300),
                    width / 2 - 150,
                    57 + i * 36,
                    0xaaaaaa);
        }
        if (error != null) drawCenteredString(fontRendererObj, error, width / 2, height - 94, 0xff5555);
        else drawCenteredString(
                fontRendererObj,
                I18n.format("controlling.conflicts.hint"),
                width / 2,
                height - 94,
                0xaaaaaa);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void keyTyped(char character, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE) mc.displayGuiScreen(parent);
    }
}
