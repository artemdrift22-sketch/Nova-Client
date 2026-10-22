package dev.nova.module;

import dev.nova.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/** Базовый модуль. Наследники переопределяют onEnable/onDisable/onTick/onRender2D. */
public abstract class Module {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    private final String name;
    private final String description;
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<>();
    private boolean enabled;
    private int key = -1; // GLFW key, -1 = без бинда

    protected Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    protected <S extends Setting<?>> S add(S setting) { settings.add(setting); return setting; }
    protected BoolSetting bool(String n, boolean def) { return add(new BoolSetting(n, def)); }
    protected NumberSetting number(String n, double def, double min, double max, double step) { return add(new NumberSetting(n, def, min, max, step)); }
    protected ModeSetting mode(String n, String def, String... modes) { return add(new ModeSetting(n, def, modes)); }
    protected ColorSetting color(String n, int argb) { return add(new ColorSetting(n, argb)); }

    public void toggle() { setEnabled(!enabled); }

    public void setEnabled(boolean value) {
        if (enabled == value) return;
        enabled = value;
        if (value) onEnable(); else onDisable();
    }

    protected void onEnable() {}
    protected void onDisable() {}
    public void onTick() {}
    public void onRender2D(DrawContext ctx, float delta) {}
    public void onRender3D(net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext ctx) {}

    protected boolean inGame() { return mc.player != null && mc.world != null; }

    public String name() { return name; }
    public String description() { return description; }
    public Category category() { return category; }
    public List<Setting<?>> settings() { return settings; }
    public boolean enabled() { return enabled; }
    public int key() { return key; }
    public void setKey(int key) { this.key = key; }
}
