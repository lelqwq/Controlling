package com.blamejared.controlling.client.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

import com.blamejared.controlling.api.KeyContext;
import com.blamejared.controlling.keybinding.BindingContextRegistry;

public class GuiEditContexts extends GuiScreen {

    private static final String CONTAINER = "net.minecraft.client.gui.inventory.GuiContainer";
    private final GuiNewControls parent;
    private final KeyBinding binding;
    private final List<KeyContext> choices;
    private final List<KeyContext> selected;
    private final List<KeyContext> custom = new ArrayList<>();
    private final List<KeyContext> removed = new ArrayList<>();
    private KeyContext confirmingDelete;
    private int page;
    private int pageSize;
    private boolean automatic;
    private String error;

    public GuiEditContexts(GuiNewControls parent, KeyBinding binding) {
        this.parent = parent;
        this.binding = binding;
        this.selected = new ArrayList<>(BindingContextRegistry.getContexts(binding));
        this.choices = BindingContextRegistry.getAvailableContexts();
        for (KeyContext context : selected) {
            if (!choices.contains(context)) choices.add(context);
        }
    }

    @Override
    public void initGui() {
        int x = width / 2 - 150;
        pageSize = Math.max(1, (height - 125) / 36);
        page = Math.min(page, (choices.size() - 1) / pageSize);
        for (int i = 0; i < pageSize; i++) {
            buttonList.add(new GuiButton(100 + i, x, 35 + i * 36, 300, 20, ""));
            buttonList.add(new GuiButton(1000 + i, x + 245, 35 + i * 36, 55, 20, ""));
        }
        buttonList.add(new GuiButton(6, x, height - 80, 75, 20, I18n.format("controlling.context.previous")));
        buttonList.add(new GuiButton(7, x + 225, height - 80, 75, 20, I18n.format("controlling.context.next")));
        buttonList.add(new GuiButton(8, x + 91, height - 80, 118, 20, I18n.format("controlling.context.add")));
        buttonList.add(new GuiButton(3, x, height - 52, 96, 20, I18n.format("controlling.context.save")));
        buttonList.add(new GuiButton(4, x + 102, height - 52, 96, 20, I18n.format("gui.cancel")));
        buttonList.add(new GuiButton(5, x + 204, height - 52, 96, 20, ""));
        updateButtons();
    }

    private String name(KeyContext context) {
        return CONTAINER.equals(context.getScreenClassName()) ? I18n.format("controlling.context.container")
                : context.getDisplayName();
    }

    private String description(KeyContext context) {
        if (context.getCustomName() != null) return I18n.format("controlling.context.customDescription");
        if (context.equals(KeyContext.ANY)) return I18n.format("controlling.context.anyDescription");
        if (context.equals(KeyContext.WORLD)) return I18n.format("controlling.context.worldDescription");
        if (context.equals(KeyContext.GUI)) return I18n.format("controlling.context.guiDescription");
        if (CONTAINER.equals(context.getScreenClassName()))
            return I18n.format("controlling.context.containerDescription");
        return I18n.format("controlling.context.screenDescription");
    }

    private void updateButtons() {
        for (GuiButton button : this.buttonList) {
            if (button.id >= 1000) {
                int index = page * pageSize + button.id - 1000;
                button.visible = index < choices.size() && !BindingContextRegistry.isPresetContext(choices.get(index));
                if (button.visible) button.displayString = I18n.format(
                        choices.get(index).equals(confirmingDelete) ? "controlling.context.deleteConfirm"
                                : "controlling.context.delete");
            } else if (button.id >= 100) {
                int index = page * pageSize + button.id - 100;
                button.visible = index < choices.size();
                if (button.visible) {
                    KeyContext context = choices.get(index);
                    button.width = BindingContextRegistry.isPresetContext(context) ? 300 : 241;
                    button.displayString = fontRendererObj.trimStringToWidth(
                            (!automatic && selected.contains(context) ? "[x] " : "[ ] ") + name(context),
                            button.width - 14);
                }
            } else if (button.id == 5) {
                button.displayString = (automatic ? "[x] " : "") + I18n.format("controlling.context.auto");
            } else if (button.id == 6) {
                button.enabled = page > 0;
            } else if (button.id == 7) {
                button.enabled = (page + 1) * pageSize < choices.size();
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        error = null;
        if (button.id >= 1000) {
            KeyContext context = choices.get(page * pageSize + button.id - 1000);
            if (BindingContextRegistry.isPresetContext(context)) return;
            if (!context.equals(confirmingDelete)) {
                confirmingDelete = context;
            } else {
                choices.remove(context);
                if (selected.remove(context) && selected.isEmpty()) automatic = true;
                custom.remove(context);
                if (!removed.contains(context)) removed.add(context);
                confirmingDelete = null;
                page = Math.min(page, (choices.size() - 1) / pageSize);
            }
        } else if (button.id >= 100) {
            confirmingDelete = null;
            KeyContext context = choices.get(page * pageSize + button.id - 100);
            automatic = false;
            if (selected.contains(context)) selected.remove(context);
            else {
                if (context.equals(KeyContext.ANY)) selected.clear();
                else selected.remove(KeyContext.ANY);
                selected.add(context);
            }
        } else if (button.id == 4) {
            mc.displayGuiScreen(parent);
            return;
        } else if (button.id == 5) {
            automatic = true;
        } else if (button.id == 6) {
            page--;
        } else if (button.id == 7) {
            page++;
        } else if (button.id == 8) {
            mc.displayGuiScreen(new GuiAddContext(this));
            return;
        } else if (button.id == 3) {
            if (!automatic && selected.isEmpty()) {
                error = I18n.format("controlling.context.empty");
            } else if (BindingContextRegistry.saveUserContexts(binding, automatic ? null : selected, custom, removed)) {
                mc.displayGuiScreen(parent);
                return;
            } else {
                error = I18n.format("controlling.context.saveError");
            }
        }
        updateButtons();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(
                fontRendererObj,
                I18n.format("controlling.context.title", I18n.format(binding.getKeyDescription())),
                width / 2,
                12,
                0xffffff);
        String tooltip = null;
        for (int i = 0; i < pageSize && page * pageSize + i < choices.size(); i++) {
            KeyContext context = choices.get(page * pageSize + i);
            int y = 35 + i * 36;
            drawString(
                    fontRendererObj,
                    fontRendererObj.trimStringToWidth(
                            context.equals(confirmingDelete) ? I18n.format("controlling.context.deleteWarning")
                                    : description(context),
                            300),
                    width / 2 - 150,
                    y + 22,
                    0xaaaaaa);
            if (context.getScreenClassName() != null && mouseX >= width / 2 - 150
                    && mouseX < width / 2 + 150
                    && mouseY >= y
                    && mouseY < y + 32)
                tooltip = context.getScreenClassName();
            else if (context.getCustomName() != null && mouseX >= width / 2 - 150
                    && mouseX < width / 2 + 150
                    && mouseY >= y
                    && mouseY < y + 32)
                tooltip = context.getCustomName();
        }
        if (error == null) drawCenteredString(
                fontRendererObj,
                (page + 1) + " / " + ((choices.size() + pageSize - 1) / pageSize),
                width / 2,
                height - 94,
                0xffffff);
        if (error != null) drawCenteredString(fontRendererObj, error, width / 2, height - 92, 0xff5555);
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (tooltip != null) func_146283_a(Collections.singletonList(tooltip), mouseX, mouseY);
    }

    @Override
    protected void keyTyped(char character, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE) mc.displayGuiScreen(parent);
    }

    void addCustomContext(KeyContext context) {
        if (!choices.contains(context)) choices.add(context);
        if (!BindingContextRegistry.isPresetContext(context) && !custom.contains(context)) custom.add(context);
        removed.remove(context);
        selected.remove(KeyContext.ANY);
        if (!selected.contains(context)) selected.add(context);
        automatic = false;
        page = choices.indexOf(context) / pageSize;
    }
}
