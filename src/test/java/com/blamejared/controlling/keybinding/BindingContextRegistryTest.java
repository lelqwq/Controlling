package com.blamejared.controlling.keybinding;

import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import net.minecraft.client.settings.KeyBinding;

import org.junit.Test;

import com.blamejared.controlling.api.KeyContext;

public class BindingContextRegistryTest {

    @Test
    public void savesAndResetsWhilePreservingOtherEntries() throws Exception {
        Path file = Files.createTempDirectory("contexts-test").resolve("contexts.json");
        Files.write(
                file,
                ("{\"version\":1,\"extra\":true,\"contexts\":{"
                        + "\"key.other\":[\"gui\"],\"key.test\":{\"category\":\"category.test\",\"custom\":42,\"contexts\":[\"any\"]}}}")
                                .getBytes(StandardCharsets.UTF_8));
        KeyBinding key = new KeyBinding("key.test", 34, "category.test");
        assertTrue(
                BindingContextRegistry.updateUserFile(
                        file,
                        key,
                        Arrays.asList(KeyContext.WORLD, KeyContext.screen("example.UnavailableScreen")),
                        false));
        String saved = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        assertTrue(saved.contains("key.other"));
        assertTrue(saved.contains("category.test"));
        assertTrue(saved.contains("custom"));
        assertTrue(saved.contains("example.UnavailableScreen"));
        assertTrue(BindingContextRegistry.updateUserFile(file, key, null, false));
        saved = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        assertFalse(saved.contains("key.test"));
        assertTrue(saved.contains("key.other"));
        assertTrue(saved.contains("extra"));
    }

    @Test
    public void rejectsMalformedFileWithoutOverwritingIt() throws Exception {
        Path file = Files.createTempDirectory("contexts-test").resolve("contexts.json");
        String invalid = "{\"contexts\": [broken";
        Files.write(file, invalid.getBytes(StandardCharsets.UTF_8));
        KeyBinding key = new KeyBinding("key.test", 34, "category.test");
        assertFalse(BindingContextRegistry.updateUserFile(file, key, Arrays.asList(KeyContext.GUI), false));
        assertEquals(invalid, new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
    }

    @Test
    public void clearsOverridesButPreservesRootMetadata() throws Exception {
        Path file = Files.createTempDirectory("contexts-test").resolve("contexts.json");
        String original = "{\"version\":1,\"extra\":true,\"contexts\":{\"key.one\":[\"world\"],\"key.two\":[\"gui\"]}}";
        Files.write(file, original.getBytes(StandardCharsets.UTF_8));
        assertTrue(BindingContextRegistry.updateUserFile(file, null, null, true));
        String saved = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        assertFalse(saved.contains("key.one"));
        assertFalse(saved.contains("key.two"));
        assertTrue(saved.contains("extra"));
    }

    @Test
    public void availableContextsIncludeGeneralAndApiDeclarations() {
        KeyBinding key = new KeyBinding("key.api.test", 35, "category.test");
        KeyContext custom = KeyContext.custom("建筑模式 / 自定义 123!");
        BindingContextRegistry.setApiContexts(key, custom);
        try {
            java.util.List<KeyContext> available = BindingContextRegistry.collectAvailableContexts();
            assertTrue(available.contains(KeyContext.ANY));
            assertTrue(available.contains(KeyContext.WORLD));
            assertTrue(available.contains(KeyContext.GUI));
            assertTrue(available.contains(custom));
        } finally {
            BindingContextRegistry.setApiContexts(key);
        }
    }

    @Test
    public void customContextsRemainAvailableAfterResetAndDoNotDuplicate() throws Exception {
        Path file = Files.createTempDirectory("contexts-test").resolve("contexts.json");
        KeyBinding key = new KeyBinding("key.custom", 36, "category.test");
        KeyContext custom = KeyContext.custom("建筑模式 / 自定义 123!");
        assertTrue(
                BindingContextRegistry.updateUserFile(file, key, Arrays.asList(custom), false, Arrays.asList(custom)));
        assertTrue(
                BindingContextRegistry.updateUserFile(file, key, Arrays.asList(custom), false, Arrays.asList(custom)));
        assertTrue(BindingContextRegistry.updateUserFile(file, null, null, true));
        String saved = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        assertTrue(saved.contains("customContexts"));
        assertTrue(saved.contains("建筑模式 / 自定义 123!"));
        assertEquals(saved.indexOf("建筑模式 / 自定义 123!"), saved.lastIndexOf("建筑模式 / 自定义 123!"));
        assertTrue(BindingContextRegistry.collectAvailableContexts().contains(custom));
    }

    @Test
    public void namedContextsConflictOnlyWithSameNameOrAnywhere() {
        KeyContext building = KeyContext.custom("建筑模式 / 自定义 123!");
        assertTrue(building.intersects(KeyContext.custom("建筑模式 / 自定义 123!")));
        assertTrue(building.intersects(KeyContext.ANY));
        assertTrue(KeyContext.ANY.intersects(building));
        assertFalse(building.intersects(KeyContext.custom("地图界面")));
        assertFalse(building.intersects(KeyContext.WORLD));
        assertFalse(building.intersects(KeyContext.GUI));
        assertFalse(KeyContext.GUI.intersects(building));
    }

    @Test
    public void deletingCustomContextRemovesReferencesAndPreservesOthers() throws Exception {
        Path file = Files.createTempDirectory("contexts-test").resolve("contexts.json");
        String input = "{\"version\":1,\"customContexts\":[{\"custom\":\"remove\"},{\"custom\":\"keep\"}],"
                + "\"contexts\":{\"key.one\":[{\"custom\":\"remove\"},\"world\"],"
                + "\"key.two\":{\"category\":\"category.test\",\"contexts\":[{\"custom\":\"remove\"}]},"
                + "\"key.three\":{\"custom\":\"keep\"}}}";
        Files.write(file, input.getBytes(StandardCharsets.UTF_8));
        KeyBinding key = new KeyBinding("key.current", 37, "category.test");
        KeyContext removed = KeyContext.custom("remove");
        assertTrue(
                BindingContextRegistry.updateUserFile(
                        file,
                        key,
                        Arrays.asList(KeyContext.GUI),
                        false,
                        java.util.Collections.emptyList(),
                        Arrays.asList(removed)));
        String saved = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        assertFalse(saved.contains("remove"));
        assertFalse(saved.contains("key.two"));
        assertTrue(saved.contains("key.one"));
        assertTrue(saved.contains("world"));
        assertTrue(saved.contains("key.three"));
        assertTrue(saved.contains("keep"));
        assertFalse(BindingContextRegistry.collectAvailableContexts().contains(removed));
    }

    @Test
    public void cannotDeleteAnApiDeclaredContext() throws Exception {
        Path file = Files.createTempDirectory("contexts-test").resolve("contexts.json");
        String input = "{\"customContexts\":[{\"custom\":\"protected\"}],\"contexts\":{}}";
        Files.write(file, input.getBytes(StandardCharsets.UTF_8));
        KeyBinding key = new KeyBinding("key.protected", 38, "category.test");
        KeyContext preset = KeyContext.custom("protected");
        BindingContextRegistry.setApiContexts(key, preset);
        try {
            assertFalse(
                    BindingContextRegistry.updateUserFile(
                            file,
                            key,
                            null,
                            false,
                            java.util.Collections.emptyList(),
                            Arrays.asList(preset)));
            assertEquals(input, new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
        } finally {
            BindingContextRegistry.setApiContexts(key);
        }
    }
}
