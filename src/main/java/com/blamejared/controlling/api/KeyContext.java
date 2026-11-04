package com.blamejared.controlling.api;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Describes the scenario in which a key binding is used.
 * <p>
 * A binding can belong to more than one context. Two bindings only conflict when they share the same key code, the same
 * combo modifier and at least one intersecting context.
 */
public final class KeyContext {

    /**
     * The binding is active everywhere. This is the default for bindings that did not declare a context.
     */
    public static final KeyContext ANY = new KeyContext(Type.ANY, null, null);

    /**
     * The binding is active in-world while no GUI screen is open.
     */
    public static final KeyContext WORLD = new KeyContext(Type.WORLD, null, null);

    /**
     * The binding is active while any GUI screen is open.
     */
    public static final KeyContext GUI = new KeyContext(Type.GUI, null, null);

    private static final Logger LOGGER = LogManager.getLogger("controlling");
    private static final Map<String, Class<?>> RESOLVED_SCREENS = new HashMap<>();
    private static final Set<String> WARNED_SCREENS = new HashSet<>();

    private final Type type;
    private final String screenClassName;
    private final Class<?> screenClass;

    private KeyContext(Type type, String screenClassName, Class<?> screenClass) {
        this.type = type;
        this.screenClassName = screenClassName;
        this.screenClass = screenClass;
    }

    /**
     * Creates a context that is only active while the given screen (or one of its subclasses) is open.
     */
    public static KeyContext screen(Class<? extends GuiScreen> screenClass) {
        Objects.requireNonNull(screenClass, "screenClass");
        return new KeyContext(Type.SCREEN, screenClass.getName(), screenClass);
    }

    /**
     * Creates a context from a screen class name. Resolving the class is deferred until contexts are compared, which
     * allows mods to declare contexts without a compile time dependency on the target screen.
     */
    public static KeyContext screen(String className) {
        Objects.requireNonNull(className, "className");
        final String trimmed = className.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Screen class name must not be empty");
        }
        return new KeyContext(Type.SCREEN, trimmed, null);
    }

    /** Creates a named context used only when reporting conflicts. */
    public static KeyContext custom(String name) {
        Objects.requireNonNull(name, "name");
        String trimmed = name.trim();
        if (trimmed.isEmpty()) throw new IllegalArgumentException("Context name must not be empty");
        return new KeyContext(Type.CUSTOM, trimmed, null);
    }

    /** @return the configured screen class name, or null for a general context. */
    public String getScreenClassName() {
        return this.type == Type.SCREEN ? this.screenClassName : null;
    }

    public String getCustomName() {
        return this.type == Type.CUSTOM ? this.screenClassName : null;
    }

    /** @return true when both contexts can be active at the same time. */
    public boolean intersects(KeyContext other) {
        if (other == null) {
            return false;
        }
        if (this == other || this.type == Type.ANY || other.type == Type.ANY) {
            return true;
        }
        switch (this.type) {
            case WORLD:
                return other.type == Type.WORLD;
            case GUI:
                return other.type == Type.GUI || other.type == Type.SCREEN;
            case SCREEN:
                if (other.type == Type.GUI) {
                    return true;
                }
                return other.type == Type.SCREEN && this.screensIntersect(other);
            case CUSTOM:
                return other.type == Type.CUSTOM && this.screenClassName.equals(other.screenClassName);
            default:
                return false;
        }
    }

    private boolean screensIntersect(KeyContext other) {
        if (this.screenClassName.equals(other.screenClassName)) {
            return true;
        }
        final Class<?> thisScreen = this.resolveScreenClass();
        final Class<?> otherScreen = other.resolveScreenClass();
        if (thisScreen == null || otherScreen == null) {
            return false;
        }
        return thisScreen.isAssignableFrom(otherScreen) || otherScreen.isAssignableFrom(thisScreen);
    }

    private Class<?> resolveScreenClass() {
        if (this.screenClass != null) {
            return this.screenClass;
        }
        if (RESOLVED_SCREENS.containsKey(this.screenClassName)) {
            return RESOLVED_SCREENS.get(this.screenClassName);
        }
        Class<?> resolved = null;
        try {
            final Class<?> candidate = Class.forName(this.screenClassName, false, KeyContext.class.getClassLoader());
            if (GuiScreen.class.isAssignableFrom(candidate)) {
                resolved = candidate;
            } else if (WARNED_SCREENS.add(this.screenClassName)) {
                LOGGER.warn(
                        "Key context screen class '{}' is not a GuiScreen, ignoring the screen hierarchy",
                        this.screenClassName);
            }
        } catch (final ClassNotFoundException | LinkageError e) {
            if (WARNED_SCREENS.add(this.screenClassName)) {
                LOGGER.warn(
                        "Unable to resolve key context screen class '{}', only exact class names will match",
                        this.screenClassName);
            }
        }
        RESOLVED_SCREENS.put(this.screenClassName, resolved);
        return resolved;
    }

    /**
     * @return a localized, human readable description of this context.
     */
    public String getDisplayName() {
        switch (this.type) {
            case ANY:
                return I18n.format("controlling.context.any");
            case WORLD:
                return I18n.format("controlling.context.world");
            case GUI:
                return I18n.format("controlling.context.gui");
            case SCREEN:
                return I18n.format("controlling.context.screen", this.getScreenSimpleName());
            case CUSTOM:
                return this.screenClassName;
            default:
                throw new IllegalStateException("Unknown key context type: " + this.type);
        }
    }

    private String getScreenSimpleName() {
        if (this.screenClass != null) {
            return this.screenClass.getSimpleName();
        }
        final int lastDot = this.screenClassName.lastIndexOf('.');
        return lastDot < 0 ? this.screenClassName : this.screenClassName.substring(lastDot + 1);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof KeyContext other)) {
            return false;
        }
        return this.type == other.type && Objects.equals(this.screenClassName, other.screenClassName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.type, this.screenClassName);
    }

    @Override
    public String toString() {
        return this.type == Type.SCREEN || this.type == Type.CUSTOM
                ? this.type.name() + "(" + this.screenClassName + ")"
                : this.type.name();
    }

    private enum Type {
        ANY,
        WORLD,
        GUI,
        SCREEN,
        CUSTOM
    }
}
