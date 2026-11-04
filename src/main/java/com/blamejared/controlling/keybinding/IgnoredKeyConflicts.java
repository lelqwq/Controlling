package com.blamejared.controlling.keybinding;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import net.minecraft.client.settings.KeyBinding;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import cpw.mods.fml.common.Loader;

/** User-approved exceptions for specific pairs of key bindings. */
public final class IgnoredKeyConflicts {

    private static final Logger LOGGER = LogManager.getLogger("controlling");
    private static final Set<Pair> IGNORED = new HashSet<>();
    private static boolean loaded;

    private IgnoredKeyConflicts() {}

    private static Path configFile() {
        return Loader.instance().getConfigDir().toPath().resolve("controlling/ignored_conflicts.json");
    }

    public static boolean isIgnored(KeyBinding first, KeyBinding second) {
        if (first == null || second == null || first == second) return false;
        ensureLoaded();
        return IGNORED.contains(new Pair(first, second));
    }

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        try {
            reload(read(configFile()));
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Failed to load ignored key conflicts", e);
        }
    }

    public static boolean save(KeyBinding binding, Map<KeyBinding, Boolean> changes) {
        ensureLoaded();
        return save(configFile(), binding, changes);
    }

    static boolean save(Path file, KeyBinding binding, Map<KeyBinding, Boolean> changes) {
        Path temporary = null;
        try {
            JsonObject root = read(file);
            JsonArray original = root.getAsJsonArray("pairs");
            Set<Pair> modified = new HashSet<>();
            for (KeyBinding other : changes.keySet()) modified.add(new Pair(binding, other));
            JsonArray result = new JsonArray();
            for (JsonElement value : original) {
                Pair pair = Pair.parse(value);
                if (pair == null || !modified.contains(pair)) result.add(value);
            }
            for (Map.Entry<KeyBinding, Boolean> change : changes.entrySet()) {
                if (Boolean.TRUE.equals(change.getValue())) result.add(new Pair(binding, change.getKey()).toJson());
            }
            root.add("pairs", result);
            Files.createDirectories(file.getParent());
            temporary = Files.createTempFile(file.getParent(), "ignored_conflicts", ".tmp");
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(root, writer);
            }
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            reload(root);
            loaded = true;
            return true;
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Failed to save ignored key conflicts to {}", file, e);
            return false;
        } finally {
            if (temporary != null) try {
                Files.deleteIfExists(temporary);
            } catch (IOException ignored) {}
        }
    }

    private static JsonObject read(Path file) throws IOException {
        if (!Files.exists(file)) {
            JsonObject root = new JsonObject();
            root.addProperty("version", 1);
            root.add("pairs", new JsonArray());
            return root;
        }
        try (Reader source = Files.newBufferedReader(file, StandardCharsets.UTF_8);
                JsonReader reader = new JsonReader(source)) {
            reader.setLenient(false);
            JsonElement element = Streams.parse(reader);
            if (!element.isJsonObject() || reader.peek() != JsonToken.END_DOCUMENT)
                throw new IOException("Invalid JSON");
            JsonObject root = element.getAsJsonObject();
            if (!root.has("pairs") || !root.get("pairs").isJsonArray()) throw new IOException("Invalid pairs");
            return root;
        }
    }

    private static void reload(JsonObject root) {
        IGNORED.clear();
        for (JsonElement value : root.getAsJsonArray("pairs")) {
            Pair pair = Pair.parse(value);
            if (pair != null) IGNORED.add(pair);
        }
    }

    private static final class Pair {

        final String first;
        final String second;

        Pair(KeyBinding a, KeyBinding b) {
            this(id(a.getKeyDescription(), a.getKeyCategory()), id(b.getKeyDescription(), b.getKeyCategory()));
        }

        Pair(String a, String b) {
            first = a.compareTo(b) <= 0 ? a : b;
            second = a.compareTo(b) <= 0 ? b : a;
        }

        static String id(String description, String category) {
            return description.length() + ":" + description + category;
        }

        static Pair parse(JsonElement value) {
            if (!value.isJsonArray() || value.getAsJsonArray().size() != 2) return null;
            String a = parseId(value.getAsJsonArray().get(0));
            String b = parseId(value.getAsJsonArray().get(1));
            return a == null || b == null ? null : new Pair(a, b);
        }

        static String parseId(JsonElement value) {
            if (!value.isJsonObject()) return null;
            JsonObject object = value.getAsJsonObject();
            JsonElement description = object.get("description");
            JsonElement category = object.get("category");
            if (description == null || category == null
                    || !description.isJsonPrimitive()
                    || !category.isJsonPrimitive()
                    || !description.getAsJsonPrimitive().isString()
                    || !category.getAsJsonPrimitive().isString())
                return null;
            return id(description.getAsString(), category.getAsString());
        }

        static JsonObject bindingJson(String id) {
            int split = id.indexOf(':');
            int length = Integer.parseInt(id.substring(0, split));
            JsonObject result = new JsonObject();
            result.addProperty("description", id.substring(split + 1, split + 1 + length));
            result.addProperty("category", id.substring(split + 1 + length));
            return result;
        }

        JsonArray toJson() {
            JsonArray result = new JsonArray();
            result.add(bindingJson(first));
            result.add(bindingJson(second));
            return result;
        }

        @Override
        public boolean equals(Object value) {
            if (!(value instanceof Pair other)) return false;
            return first.equals(other.first) && second.equals(other.second);
        }

        @Override
        public int hashCode() {
            return Objects.hash(first, second);
        }
    }
}
