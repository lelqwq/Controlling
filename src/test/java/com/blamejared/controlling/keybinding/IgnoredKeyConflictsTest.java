package com.blamejared.controlling.keybinding;

import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import net.minecraft.client.settings.KeyBinding;

import org.junit.Test;

public class IgnoredKeyConflictsTest {

    @Test
    public void pairIsSymmetricAndOtherBindingsRemainUnaffected() throws Exception {
        Path file = Files.createTempDirectory("ignored-conflicts").resolve("ignored.json");
        KeyBinding first = new KeyBinding("bookmark.add", 34, "nei");
        KeyBinding second = new KeyBinding("bookmark.remove", 34, "nei");
        KeyBinding third = new KeyBinding("other.action", 34, "mod");
        assertTrue(IgnoredKeyConflicts.save(file, first, Collections.singletonMap(second, true)));
        assertTrue(IgnoredKeyConflicts.isIgnored(first, second));
        assertTrue(IgnoredKeyConflicts.isIgnored(second, first));
        assertFalse(IgnoredKeyConflicts.isIgnored(first, third));
        assertFalse(IgnoredKeyConflicts.isIgnored(second, third));
        assertTrue(IgnoredKeyConflicts.save(file, first, Collections.singletonMap(third, true)));
        assertTrue(IgnoredKeyConflicts.isIgnored(first, second));
        assertTrue(IgnoredKeyConflicts.isIgnored(first, third));
        assertTrue(IgnoredKeyConflicts.save(file, second, Collections.singletonMap(first, false)));
        assertFalse(IgnoredKeyConflicts.isIgnored(first, second));
        assertTrue(IgnoredKeyConflicts.isIgnored(first, third));
    }

    @Test
    public void invalidConfigIsPreserved() throws Exception {
        Path file = Files.createTempDirectory("ignored-conflicts").resolve("ignored.json");
        String invalid = "{\"pairs\":[broken";
        Files.write(file, invalid.getBytes(StandardCharsets.UTF_8));
        KeyBinding first = new KeyBinding("one", 34, "category");
        KeyBinding second = new KeyBinding("two", 34, "category");
        assertFalse(IgnoredKeyConflicts.save(file, first, Collections.singletonMap(second, true)));
        assertEquals(invalid, new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
    }
}
