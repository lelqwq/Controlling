package com.blamejared.controlling.keybinding;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.settings.KeyBinding;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.blamejared.controlling.api.KeyContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import cpw.mods.fml.common.Loader;

/**
 * Resolves the {@link KeyContext}s of a key binding.
 * <p>
 * Sources are consulted in the following order: user config, API declarations, built-in compatibility table and finally
 * the default (vanilla bindings are {@link KeyContext#WORLD}, everything else is {@link KeyContext#ANY}).
 */
public final class BindingContextRegistry {

    private static final Logger LOGGER = LogManager.getLogger("controlling");
    private static final String BUILTIN_RESOURCE = "/assets/controlling/key_contexts.json";
    private static final String USER_CONFIG = "controlling/key_contexts.json";

    private static final Set<String> VANILLA_KEY_DESCRIPTIONS = Collections.unmodifiableSet(
            new HashSet<>(
                    Arrays.asList(
                            "key.attack",
                            "key.use",
                            "key.forward",
                            "key.left",
                            "key.back",
                            "key.right",
                            "key.jump",
                            "key.sneak",
                            "key.sprint",
                            "key.inventory",
                            "key.drop",
                            "key.chat",
                            "key.playerlist",
                            "key.pickItem",
                            "key.command",
                            "key.screenshot",
                            "key.togglePerspective",
                            "key.fullscreen")));

    private static final Map<KeyBinding, List<KeyContext>> API_CONTEXTS = new IdentityHashMap<>();
    private static final Map<KeyBinding, List<KeyContext>> CACHE = new IdentityHashMap<>();
    private static final Map<String, JsonEntry> BUILTIN_ENTRIES = new LinkedHashMap<>();
    private static final Map<String, JsonEntry> USER_ENTRIES = new LinkedHashMap<>();
    private static boolean loaded = false;

    private BindingContextRegistry() {}

    /**
     * Loads the built-in and user context files. Safe to call more than once.
     */
    public static void init() {
        ensureLoaded();
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        loadBuiltinEntries();
        loadUserEntries();
    }

    /**
     * Registers API-declared contexts. An empty array removes a previous declaration.
     */
    public static void setApiContexts(KeyBinding keyBinding, KeyContext... contexts) {
        if (keyBinding == null) {
            LOGGER.warn("ControllingApi.setBindingContexts called with a null key binding");
            return;
        }
        CACHE.remove(keyBinding);
        if (contexts == null || contexts.length == 0) {
            API_CONTEXTS.remove(keyBinding);
            return;
        }
        final Set<KeyContext> unique = new LinkedHashSet<>();
        for (KeyContext context : contexts) {
            if (context != null) {
                unique.add(context);
            }
        }
        if (unique.isEmpty()) {
            API_CONTEXTS.remove(keyBinding);
        } else {
            API_CONTEXTS.put(keyBinding, Collections.unmodifiableList(new ArrayList<>(unique)));
        }
    }

    /**
     * @return the effective, unmodifiable contexts of the binding.
     */
    public static List<KeyContext> getContexts(KeyBinding keyBinding) {
        if (keyBinding == null) {
            return Collections.singletonList(KeyContext.ANY);
        }
        final List<KeyContext> cached = CACHE.get(keyBinding);
        if (cached != null) {
            return cached;
        }
        ensureLoaded();
        final List<KeyContext> resolved = resolve(keyBinding);
        CACHE.put(keyBinding, resolved);
        return resolved;
    }

    /**
     * @return true when the two bindings can be active at the same time.
     */
    public static boolean intersects(KeyBinding first, KeyBinding second) {
        if (first == null || second == null) {
            return false;
        }
        for (KeyContext firstContext : getContexts(first)) {
            for (KeyContext secondContext : getContexts(second)) {
                if (firstContext.intersects(secondContext)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @return a localized, comma separated description of the binding's contexts.
     */
    public static String describeContexts(KeyBinding keyBinding) {
        final StringBuilder builder = new StringBuilder();
        for (KeyContext context : getContexts(keyBinding)) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(context.getDisplayName());
        }
        return builder.toString();
    }

    private static List<KeyContext> resolve(KeyBinding keyBinding) {
        final String description = keyBinding.getKeyDescription();

        final JsonEntry userEntry = USER_ENTRIES.get(description);
        if (userEntry != null && userEntry.matches(keyBinding)) {
            return entryContexts(userEntry, keyBinding);
        }

        final List<KeyContext> apiContexts = API_CONTEXTS.get(keyBinding);
        if (apiContexts != null) {
            return apiContexts;
        }

        final JsonEntry builtinEntry = BUILTIN_ENTRIES.get(description);
        if (builtinEntry != null && builtinEntry.matches(keyBinding)) {
            return entryContexts(builtinEntry, keyBinding);
        }

        return defaultContexts(keyBinding);
    }

    private static List<KeyContext> entryContexts(JsonEntry entry, KeyBinding keyBinding) {
        if (entry.contexts.isEmpty()) {
            LOGGER.warn(
                    "Invalid key context entry for '{}', falling back to the default context",
                    keyBinding.getKeyDescription());
            return defaultContexts(keyBinding);
        }
        return entry.contexts;
    }

    private static List<KeyContext> defaultContexts(KeyBinding keyBinding) {
        return VANILLA_KEY_DESCRIPTIONS.contains(keyBinding.getKeyDescription())
                ? Collections.singletonList(KeyContext.WORLD)
                : Collections.singletonList(KeyContext.ANY);
    }

    private static void loadBuiltinEntries() {
        try (InputStream stream = BindingContextRegistry.class.getResourceAsStream(BUILTIN_RESOURCE)) {
            if (stream == null) {
                LOGGER.warn("Missing built-in key context resource {}", BUILTIN_RESOURCE);
                return;
            }
            loadEntries(new InputStreamReader(stream, StandardCharsets.UTF_8), BUILTIN_ENTRIES, "built-in");
        } catch (final IOException e) {
            LOGGER.error("Failed to read built-in key context resource {}", BUILTIN_RESOURCE, e);
        }
    }

    private static void loadUserEntries() {
        final File configDir = Loader.instance() == null ? null : Loader.instance().getConfigDir();
        if (configDir == null) {
            LOGGER.warn("Unable to locate the config directory, user key contexts are disabled");
            return;
        }
        final File file = new File(configDir, USER_CONFIG);
        if (!file.isFile()) {
            LOGGER.info("No user key context config found at {}", file);
            return;
        }
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            loadEntries(reader, USER_ENTRIES, "user");
        } catch (final IOException | RuntimeException e) {
            LOGGER.error("Failed to read user key context config {}", file, e);
        }
    }

    private static void loadEntries(Reader reader, Map<String, JsonEntry> target, String source) {
        final JsonElement root = new JsonParser().parse(reader);
        if (!root.isJsonObject()) {
            LOGGER.error("Key context file ({}) must contain a JSON object", source);
            return;
        }

        final JsonObject rootObject = root.getAsJsonObject();
        final JsonElement contextsElement = rootObject.get("contexts");
        final JsonObject entries = contextsElement != null && contextsElement.isJsonObject()
                ? contextsElement.getAsJsonObject()
                : rootObject;

        for (Map.Entry<String, JsonElement> entry : entries.entrySet()) {
            final String description = entry.getKey();
            if ("version".equals(description) || "contexts".equals(description)) {
                continue;
            }
            final JsonEntry parsed = parseEntry(entry.getValue());
            if (parsed == null) {
                LOGGER.warn("Ignoring invalid key context entry '{}' in {}", description, source);
                continue;
            }
            target.put(description, parsed);
        }
        LOGGER.info("Loaded {} key context entries from the {} table", target.size(), source);
    }

    private static JsonEntry parseEntry(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }

        String category = null;
        JsonElement contextsElement = element;
        if (element.isJsonObject()) {
            final JsonObject object = element.getAsJsonObject();
            final JsonElement categoryElement = object.get("category");
            if (categoryElement != null && categoryElement.isJsonPrimitive()) {
                category = categoryElement.getAsString();
            }
            final JsonElement nestedContexts = object.get("contexts");
            if (nestedContexts != null) {
                contextsElement = nestedContexts;
            } else if (!object.has("screen")) {
                return null;
            }
        }

        final List<KeyContext> contexts = parseContexts(contextsElement);
        return new JsonEntry(category, contexts);
    }

    private static List<KeyContext> parseContexts(JsonElement element) {
        final List<KeyContext> contexts = new ArrayList<>();
        if (element == null || element.isJsonNull()) {
            return contexts;
        }
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                addContext(child, contexts);
            }
        } else {
            addContext(element, contexts);
        }
        return new ArrayList<>(new LinkedHashSet<>(contexts));
    }

    private static void addContext(JsonElement element, List<KeyContext> contexts) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            final String value = element.getAsString().trim();
            switch (value.toLowerCase(Locale.ROOT)) {
                case "any":
                    contexts.add(KeyContext.ANY);
                    return;
                case "world":
                    contexts.add(KeyContext.WORLD);
                    return;
                case "gui":
                    contexts.add(KeyContext.GUI);
                    return;
                default:
                    LOGGER.warn("Unknown key context '{}'", value);
                    return;
            }
        }
        if (element.isJsonObject()) {
            final JsonElement screenElement = element.getAsJsonObject().get("screen");
            if (screenElement != null && screenElement.isJsonPrimitive()) {
                final JsonPrimitive screen = screenElement.getAsJsonPrimitive();
                if (screen.isString() && !screen.getAsString().trim().isEmpty()) {
                    contexts.add(KeyContext.screen(screen.getAsString().trim()));
                    return;
                }
            }
            LOGGER.warn("Invalid key context object: {}", element);
            return;
        }
        LOGGER.warn("Invalid key context value: {}", element);
    }

    private static final class JsonEntry {

        private final String category;
        private final List<KeyContext> contexts;

        private JsonEntry(String category, List<KeyContext> contexts) {
            this.category = category;
            this.contexts = Collections.unmodifiableList(contexts);
        }

        private boolean matches(KeyBinding keyBinding) {
            return this.category == null || this.category.equals(keyBinding.getKeyCategory());
        }
    }
}
