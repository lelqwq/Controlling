package com.blamejared.controlling.client.gui;

import java.util.function.Predicate;

import net.minecraft.client.Minecraft;

import com.blamejared.controlling.keybinding.KeyConflictResolver;

public enum DisplayMode {

    ALL,
    CONFLICTING,
    UNBOUND;

    public Predicate<GuiNewKeyBindingList.KeyEntry> getPredicate() {
        switch (this) {
            case ALL:
                return keyEntry -> true;
            case CONFLICTING:
                return keyEntry -> KeyConflictResolver
                        .hasConflict(keyEntry.getKeybinding(), Minecraft.getMinecraft().gameSettings.keyBindings);
            case UNBOUND:
                return keyEntry -> keyEntry.getKeybinding().getKeyCode() == 0;
        }
        throw new IllegalStateException();
    }

}
