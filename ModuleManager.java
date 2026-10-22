package dev.nova.module;

import dev.nova.hud.HudModule;
import dev.nova.modules.client.ClickGuiModule;
import dev.nova.modules.client.HudEditorModule;
import dev.nova.modules.client.PresetsModule;
import dev.nova.modules.client.ServerModeModule;
import dev.nova.modules.client.InterfaceModule;
import dev.nova.modules.pvp.*;
import dev.nova.modules.qol.*;
import dev.nova.modules.world.TimeWeatherModule;
import dev.nova.modules.hud.*;
import dev.nova.modules.world.FullbrightModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;

import java.util.*;

public final class ModuleManager {
    private final List<Module> modules = new ArrayList<>();
    private final Map<Integer, Boolean> keyState = new HashMap<>();

    public void init() {
        // Клиент
        register(new InterfaceModule());
        register(new ClickGuiModule());
        register(new HudEditorModule());
        register(new PresetsModule());
        register(new ServerModeModule());
        // HUD
        register(new WatermarkHud());
        register(new ModuleListHud());
        register(new FpsHud());
        register(new PingHud());
        register(new CoordsHud());
        register(new KeybindsHud());
        register(new HotbarModule());
        register(new ArmorHud());
        register(new KeystrokesHud());
        register(new CpsHud());
        register(new PotionHud());
        register(new HealthBarsHud());
        register(new InventoryHud());
        register(new HeldItemHud());
        register(new SpeedHud());
        register(new LookInfoHud());
        register(new LowHealthWarning());
        register(new CrosshairModule());
        // PvP (только информация, без автоматизации)
        register(new TargetHud());
        register(new ComboHud());
        register(new ReachHud());
        register(new CooldownHud());
        register(new CountersHud());
        // Мир
        register(new FullbrightModule());
        register(new dev.nova.modules.world.HitboxesModule());
        register(new dev.nova.modules.cosmetic.ChatStarModule());
        register(new TimeWeatherModule());
        // QoL
        register(new ZoomModule());
        register(new ToggleSprintModule());
        register(new NoOverlaysModule());
        register(new HideNametagsModule());
        register(new ChatAlertsModule());
        register(new CopyCoordsModule());
    }

    private void register(Module m) { modules.add(m); }

    public void onTick(MinecraftClient client) {
        handleKeybinds(client);
        dev.nova.util.CombatTracker.tick(client);
        for (Module m : modules) if (m.enabled()) m.onTick();
    }

    public void onRender3D(net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext ctx) {
        for (Module m : all()) if (m.enabled()) {
            try { m.onRender3D(ctx); } catch (Throwable t) { /* не роняем игру */ }
        }
    }

    public void onRender2D(DrawContext ctx, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden || mc.player == null) return;
        dev.nova.render.ThemeManager.apply();
        for (Module m : modules) if (m.enabled()) {
            try { m.onRender2D(ctx, delta); } catch (Throwable t) { dev.nova.render.Render2DUtils.alpha = 1f; }
        }
    }

    /** Бинды работают только без открытого экрана (не мешают чату). Edge-detection: срабатывает на нажатие. */
    private void handleKeybinds(MinecraftClient mc) {
        if (mc.currentScreen != null || mc.getWindow() == null) return;
        long handle = mc.getWindow().getHandle();
        for (Module m : modules) {
            int key = m.key();
            if (key < 0) continue;
            boolean down = InputUtil.isKeyPressed(handle, key);
            boolean was = keyState.getOrDefault(key, false);
            if (down && !was) m.toggle();
            keyState.put(key, down);
        }
    }

    public List<Module> all() { return Collections.unmodifiableList(modules); }

    public List<Module> byCategory(Category c) {
        return modules.stream().filter(m -> m.category() == c).toList();
    }

    public List<HudModule> hud() {
        return modules.stream().filter(m -> m instanceof HudModule).map(m -> (HudModule) m).toList();
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T get(Class<T> type) {
        for (Module m : modules) if (type.isInstance(m)) return (T) m;
        throw new IllegalArgumentException("No module " + type);
    }

    public Optional<Module> byName(String name) {
        return modules.stream().filter(m -> m.name().equalsIgnoreCase(name)).findFirst();
    }
}
