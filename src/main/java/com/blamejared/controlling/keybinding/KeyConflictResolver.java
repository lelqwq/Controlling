package com.blamejared.controlling.keybinding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

/**
 * Central conflict resolution for combo key bindings.
 * <p>
 * Two bindings conflict when they share the same key code, the same combo modifier and at least one intersecting
 * {@link com.blamejared.controlling.api.KeyContext}.
 */
public final class KeyConflictResolver {

    private KeyConflictResolver() {}

    public static KeyModifier modifierOf(KeyBinding keyBinding) {
        if (keyBinding instanceof ComboKeyBinding comboKeyBinding) {
            return comboKeyBinding.controlling$getKeyModifier();
        }
        return KeyModifier.NONE;
    }

    /**
     * @return true when both bindings are assigned to the same key code and modifier, regardless of their context.
     */
    public static boolean sharesKey(KeyBinding first, KeyBinding second) {
        if (first == null || second == null || first == second) {
            return false;
        }
        if (first.getKeyCode() == Keyboard.KEY_NONE || first.getKeyCode() != second.getKeyCode()) {
            return false;
        }
        return modifierOf(first) == modifierOf(second);
    }

    /**
     * @return true when both bindings share a key and their contexts intersect.
     */
    public static boolean conflicts(KeyBinding first, KeyBinding second) {
        return sharesKey(first, second) && BindingContextRegistry.intersects(first, second)
                && !IgnoredKeyConflicts.isIgnored(first, second);
    }

    public static boolean hasConflict(KeyBinding keyBinding, KeyBinding[] allBindings) {
        if (keyBinding == null || keyBinding.getKeyCode() == Keyboard.KEY_NONE) {
            return false;
        }
        for (KeyBinding other : allBindings) {
            if (conflicts(keyBinding, other)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasConflict(Collection<KeyBinding> bindings) {
        if (bindings == null || bindings.size() < 2) {
            return false;
        }
        final List<KeyBinding> list = new ArrayList<>(bindings);
        for (int i = 0; i < list.size(); i++) {
            for (int j = i + 1; j < list.size(); j++) {
                if (conflicts(list.get(i), list.get(j))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @return all visible bindings that share the key and modifier with the given binding, including the binding
     *         itself. An empty list is returned when no other binding shares the key.
     */
    public static List<KeyBinding> sharingBindings(KeyBinding keyBinding, KeyBinding[] allBindings) {
        final List<KeyBinding> sharing = new ArrayList<>();
        if (keyBinding == null || keyBinding.getKeyCode() == Keyboard.KEY_NONE) {
            return sharing;
        }
        sharing.add(keyBinding);
        for (KeyBinding other : allBindings) {
            if (other != keyBinding && sharesKey(keyBinding, other) && !other.getKeyCategory().endsWith(".hidden")) {
                sharing.add(other);
            }
        }
        if (sharing.size() == 1) {
            sharing.clear();
        }
        return sharing;
    }
}
