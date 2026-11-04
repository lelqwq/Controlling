# Controlling

[中文](#中文) | [English](#english)

## 中文

Controlling 是一款适用于 Minecraft 1.7.10 的客户端体验优化模组，它将原版控制界面替换为可搜索、可筛选的按键绑定界面，并内置组合键支持。

### 功能

- 支持按分类名称、按键绑定名称或按键名称搜索按键绑定。
- 在控制列表中直接高亮搜索匹配项。
- 可筛选为仅显示冲突的绑定，或仅显示未绑定的绑定。
- 支持按原版顺序、A-Z 或 Z-A 排序按键绑定。
- 支持重置单个按键绑定，或确认后重置全部按键绑定。
- 可在 QWERTY 与 AZERTY 预设之间切换默认移动键。
- 支持使用修饰键（`Ctrl`、`Shift`、`Alt`）的组合按键绑定。
- 可通过带主键盘、小键盘和辅助键页的可视化键盘界面绑定按键。

与 ModernKeybinding（`mkb`）不兼容，因为组合键支持现已内置。

### 致谢

可视化键盘界面包含改编自 [Keyboard Wizard](https://github.com/VulpesStella/KeyboardWizard-Legacy) 的代码，原作者为 MrNerdy42，由 TachibanaSherry 移植。MIT 许可声明见 `THIRD_PARTY_NOTICES.md`。

### API 接口

Controlling 在 `com.blamejared.controlling.api.ControllingApi` 中提供了一小组客户端 API，用于组合键绑定。

```java
import com.blamejared.controlling.api.ComboModifier;
import com.blamejared.controlling.api.ControllingApi;
import org.lwjgl.input.Keyboard;

// 将 Ctrl 设为某个默认键码为 Keyboard.KEY_G 的按键的默认修饰键。
ControllingApi.setDefaultComboKeyBinding(myKeyBinding, ComboModifier.CONTROL);

// 将运行时组合键绑定设为 Shift + G。
ControllingApi.setComboKeyBinding(myKeyBinding, ComboModifier.SHIFT, Keyboard.KEY_G);
```

#### 按键场景

仅在特定场景下生效的绑定可以声明其使用场景，这样其他使用相同按键但场景无关的绑定就不会被报告为冲突。

```java
import com.blamejared.controlling.api.KeyContext;

// 仅在容器界面打开时生效。
ControllingApi.setBindingContexts(myContainerKey, KeyContext.screen(GuiContainer.class));

// 避免对另一个模组的界面产生编译期依赖。
ControllingApi.setBindingContexts(myGuiKey, KeyContext.screen("com.example.mods.MyScreen"));

// 在任意场景生效（未声明上下文的绑定默认值）。
ControllingApi.setBindingContexts(myGlobalKey, KeyContext.ANY);
```

内置映射覆盖 NEI 2.8.171（GTNH 2.9.0 内置版本）。用户可在 `config/controlling/key_contexts.json` 中覆盖或添加映射：

```json
{
  "version": 1,
  "contexts": {
    "nei.options.keys.bookmark.add": [{ "screen": "net.minecraft.client.gui.inventory.GuiContainer" }],
    "key.forward": ["world"],
    "my.mod.key": ["gui"]
  }
}
```

来源优先级依次为：用户配置、API 声明、内置表，最后是默认值（原版绑定为 `world`，其余为 `any`）。

---

## English

Controlling is a client-side quality-of-life mod for Minecraft 1.7.10 that replaces the vanilla controls screen with a searchable, filterable keybinding UI and built-in combo key support.

### Features

- Search keybindings by category name, keybind name, or key name.
- Highlight search matches directly in the controls list.
- Filter to only conflicting bindings or only unbound bindings.
- Sort keybindings in vanilla order, A-Z, or Z-A.
- Reset individual keybindings or confirm-reset all keybindings.
- Toggle default movement keys between QWERTY and AZERTY presets.
- Use combo keybindings with modifier keys (`Ctrl`, `Shift`, `Alt`).
- Bind keys from a visual keyboard overlay with main, numpad, and auxiliary key pages.

Incompatible with ModernKeybinding (`mkb`) because combo support is now built in.

### Credits

The visual keyboard overlay includes code adapted from [Keyboard Wizard](https://github.com/VulpesStella/KeyboardWizard-Legacy), originally by MrNerdy42 and backported by TachibanaSherry. See `THIRD_PARTY_NOTICES.md` for the MIT license notice.

### API

Controlling exposes a small client-side API for combo keybindings in `com.blamejared.controlling.api.ControllingApi`.

```java
import com.blamejared.controlling.api.ComboModifier;
import com.blamejared.controlling.api.ControllingApi;
import org.lwjgl.input.Keyboard;

// Set Ctrl as the default modifier for a key whose default key code is Keyboard.KEY_G.
ControllingApi.setDefaultComboKeyBinding(myKeyBinding, ComboModifier.CONTROL);

// Set the runtime combo binding to Shift + G.
ControllingApi.setComboKeyBinding(myKeyBinding, ComboModifier.SHIFT, Keyboard.KEY_G);
```

#### Key contexts

Bindings that are only active in a specific scenario can declare that context so that unrelated bindings using
the same key are not reported as conflicts.

```java
import com.blamejared.controlling.api.KeyContext;

// Only active while a container GUI is open.
ControllingApi.setBindingContexts(myContainerKey, KeyContext.screen(GuiContainer.class));

// Avoids a compile time dependency on another mod's screen.
ControllingApi.setBindingContexts(myGuiKey, KeyContext.screen("com.example.mods.MyScreen"));

// Active anywhere (the default for undeclared bindings).
ControllingApi.setBindingContexts(myGlobalKey, KeyContext.ANY);
```

Built-in mappings cover NEI 2.8.171 (shipped by GTNH 2.9.0). Users can override or add mappings in
`config/controlling/key_contexts.json`:

```json
{
  "version": 1,
  "contexts": {
    "nei.options.keys.bookmark.add": [{ "screen": "net.minecraft.client.gui.inventory.GuiContainer" }],
    "key.forward": ["world"],
    "my.mod.key": ["gui"]
  }
}
```

The source order is: user config, API declaration, built-in table, then the default (vanilla bindings are
`world`, everything else is `any`).
