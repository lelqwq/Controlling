package com.blamejared.controlling.api;

import java.util.Collections;
import java.util.List;

import net.minecraft.client.settings.KeyBinding;

import com.blamejared.controlling.keybinding.BindingContextRegistry;
import com.blamejared.controlling.keybinding.ComboKeyBinding;
import com.blamejared.controlling.keybinding.KeyModifier;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;

/**
 * Public API surface for interacting with Controlling combo keybindings.
 */
public final class ControllingApi {

    private ControllingApi() {}

    /**
     * @return true when the keybinding supports combo modifiers.
     */
    public static boolean supportsComboKeyBinding(KeyBinding keyBinding) {
        return keyBinding instanceof ComboKeyBinding;
    }

    /**
     * @return the active combo modifier, or {@link ComboModifier#NONE} when combos are unavailable.
     */
    public static ComboModifier getComboModifier(KeyBinding keyBinding) {
        if (keyBinding instanceof ComboKeyBinding comboKeyBinding) {
            return ComboModifier.fromInternal(comboKeyBinding.controlling$getKeyModifier());
        }
        return ComboModifier.NONE;
    }

    /**
     * @return the default combo modifier, or {@link ComboModifier#NONE} when combos are unavailable.
     */
    public static ComboModifier getDefaultComboModifier(KeyBinding keyBinding) {
        if (keyBinding instanceof ComboKeyBinding comboKeyBinding) {
            return ComboModifier.fromInternal(comboKeyBinding.controlling$getDefaultKeyModifier());
        }
        return ComboModifier.NONE;
    }

    /**
     * Sets the current combo binding value.
     *
     * @return false when the keybinding does not support combos.
     */
    public static boolean setComboKeyBinding(KeyBinding keyBinding, ComboModifier comboModifier, int keyCode) {
        if (!(keyBinding instanceof ComboKeyBinding comboKeyBinding)) {
            return false;
        }
        final KeyModifier keyModifier = comboModifier == null ? KeyModifier.NONE : comboModifier.toInternal();
        comboKeyBinding.controlling$setKeyModifierAndCode(keyModifier, keyCode);
        KeyBinding.resetKeyBindingArrayAndHash();
        return true;
    }

    /**
     * Sets the default combo modifier for a keybinding. The default key code still comes from
     * {@link KeyBinding#getKeyCodeDefault()}.
     *
     * @return false when the keybinding does not support combos.
     */
    public static boolean setDefaultComboKeyBinding(KeyBinding keyBinding, ComboModifier comboModifier) {
        if (!(keyBinding instanceof ComboKeyBinding comboKeyBinding)) {
            return false;
        }
        final boolean wasDefault = comboKeyBinding.controlling$isSetToDefaultValue();
        final KeyModifier keyModifier = comboModifier == null ? KeyModifier.NONE : comboModifier.toInternal();
        comboKeyBinding.controlling$setDefaultKeyModifier(keyModifier);
        if (wasDefault) {
            comboKeyBinding.controlling$setKeyModifier(keyModifier);
        }
        return true;
    }

    /**
     * Declares the scenarios in which the keybinding is used. Bindings that share a key and modifier only conflict when
     * at least one of their contexts intersects.
     * <p>
     * The declaration replaces any previous API declaration for the binding. Passing no contexts removes the
     * declaration. This method is client-side only and is a no-op on a dedicated server.
     */
    public static void setBindingContexts(KeyBinding keyBinding, KeyContext... contexts) {
        if (FMLCommonHandler.instance().getSide() != Side.CLIENT) {
            return;
        }
        BindingContextRegistry.setApiContexts(keyBinding, contexts);
    }

    /**
     * @return the effective contexts of the keybinding, or an empty list when the binding is null.
     */
    public static List<KeyContext> getBindingContexts(KeyBinding keyBinding) {
        if (keyBinding == null) {
            return Collections.emptyList();
        }
        return BindingContextRegistry.getContexts(keyBinding);
    }
}
