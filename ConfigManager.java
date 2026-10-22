package dev.nova.config;

import com.google.gson.*;
import dev.nova.NovaClient;
import dev.nova.hud.HudModule;
import dev.nova.module.Module;
import dev.nova.module.ModuleManager;
import dev.nova.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** Конфиг: .minecraft/config/nova/<profile>.json. Сохранение атомарное (tmp + move), битый файл -> бэкап. */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final ModuleManager modules;
    private final Path dir = FabricLoader.getInstance().getConfigDir().resolve("nova");
    private String profile = "default";
    /** 4 = интерфейс v0.4 (Pulse). При переходе со старого конфига убираем панели, перекрывавшие руки. */
    private static final int UI_VERSION = 4;

    public ConfigManager(ModuleManager modules) { this.modules = modules; }

    private Path file() { return dir.resolve(profile + ".json"); }

    public void save() {
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        root.addProperty("ui", UI_VERSION);
        JsonObject mods = new JsonObject();
        for (Module m : modules.all()) {
            JsonObject o = new JsonObject();
            o.addProperty("enabled", m.enabled());
            o.addProperty("key", m.key());
            if (m instanceof HudModule h) {
                o.addProperty("x", h.x);
                o.addProperty("y", h.y);
                o.addProperty("scale", h.scale);
            }
            JsonObject s = new JsonObject();
            for (Setting<?> st : m.settings()) s.add(st.name(), st.toJson());
            o.add("settings", s);
            mods.add(m.name(), o);
        }
        root.add("modules", mods);
        try {
            Files.createDirectories(dir);
            Path tmp = dir.resolve(profile + ".json.tmp");
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            Files.move(tmp, file(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            NovaClient.LOG.error("Config save failed", e);
        }
    }

    public void load() {
        Path f = file();
        if (!Files.exists(f)) { applyDefaults(); return; }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject mods = root.getAsJsonObject("modules");
            for (Module m : modules.all()) {
                if (mods == null || !mods.has(m.name())) continue;
                JsonObject o = mods.getAsJsonObject(m.name());
                if (o.has("key")) m.setKey(o.get("key").getAsInt());
                if (m instanceof HudModule h) {
                    if (o.has("x")) h.x = o.get("x").getAsFloat();
                    if (o.has("y")) h.y = o.get("y").getAsFloat();
                    if (o.has("scale")) h.scale = o.get("scale").getAsFloat();
                }
                JsonObject s = o.getAsJsonObject("settings");
                if (s != null) for (Setting<?> st : m.settings()) if (s.has(st.name())) st.fromJson(s.get(st.name()));
                if (o.has("enabled")) m.setEnabled(o.get("enabled").getAsBoolean());
            }
            int ui = root.has("ui") ? root.get("ui").getAsInt() : 0;
            if (ui < UI_VERSION) migrateUi();
        } catch (Exception e) {
            NovaClient.LOG.error("Config broken, backing up and resetting", e);
            try { Files.move(f, dir.resolve(profile + ".broken.json"), StandardCopyOption.REPLACE_EXISTING); } catch (IOException ignored) {}
            applyDefaults();
        }
    }

    /** Первый запуск: включаем базовый набор, чтобы мод сразу выглядел живым. */
    private void applyDefaults() {
        for (String n : new String[]{"Interface", "Watermark", "ModuleList", "Coords", "Hotkeys", "Potions", "TargetHUD", "Hotbar"})
            modules.byName(n).ifPresent(m -> m.setEnabled(true));
    }

    /** Переход на интерфейс v0.4: выключаем старые нижние панели и возвращаем позиции по умолчанию. */
    private void migrateUi() {
        for (String n : new String[]{"ArmorHUD", "HealthBars", "HeldItem", "InventoryHUD"})
            modules.byName(n).ifPresent(m -> {
                m.setEnabled(false);
                if (m instanceof HudModule h) h.resetPos();
            });
        for (String n : new String[]{"Interface", "Hotkeys", "Hotbar"})
            modules.byName(n).ifPresent(m -> m.setEnabled(true));
        for (Module m : modules.all()) if (m instanceof HudModule h && (h.y > 0.7f && h.x > 0.3f && h.x < 0.65f)) h.resetPos();
        save();
    }

    public String profile() { return profile; }
    public void switchProfile(String name) { save(); profile = name; load(); }
}
