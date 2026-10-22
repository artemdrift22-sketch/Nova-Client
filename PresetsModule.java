package dev.nova.modules.client;

import dev.nova.NovaClient;
import dev.nova.module.Module;
import dev.nova.module.Category;
import dev.nova.setting.ModeSetting;
import net.minecraft.text.Text;

import java.util.Set;

/** Пресеты и профили: включи модуль, чтобы применить выбранный набор модулей. Профиль = отдельный json-конфиг. */
public final class PresetsModule extends Module {
    private final ModeSetting preset = mode("Пресет", "PvP", "PvP", "Анархия", "Ванила+", "Стример", "Минимал");
    private final ModeSetting profile = mode("Профиль", "default", "default", "pvp", "anarchy", "vanilla");

    private static final Set<String> ALWAYS = Set.of("ClickGUI", "HudEditor", "Presets", "ServerMode");

    public PresetsModule() { super("Presets", "Готовые наборы модулей и профили конфига", Category.CLIENT); }

    private Set<String> modulesFor(String p) {
        return switch (p) {
            case "PvP" -> Set.of("Watermark", "FPS", "Ping", "CPS", "Keystrokes", "ArmorHUD", "TargetHUD", "ComboCounter", "ReachDisplay", "Cooldowns", "Counters", "Crosshair", "HealthBars", "ToggleSprint", "NoOverlays", "Zoom", "LowHPWarning");
            case "Анархия" -> Set.of("Watermark", "ModuleList", "FPS", "Ping", "Coords", "ArmorHUD", "PotionHUD", "TargetHUD", "Counters", "Cooldowns", "InventoryHUD", "HealthBars", "ToggleSprint", "Fullbright", "Zoom", "ChatAlerts", "LowHPWarning", "NoOverlays");
            case "Ванила+" -> Set.of("FPS", "Coords", "ArmorHUD", "HeldItem", "LookInfo", "Zoom", "ToggleSprint");
            case "Стример" -> Set.of("Watermark", "FPS", "Keystrokes", "CPS", "TargetHUD", "HealthBars", "Crosshair", "HideNametags");
            default -> Set.of("FPS", "Crosshair");
        };
    }

    @Override protected void onEnable() {
        var cfg = NovaClient.config();
        if (!profile.get().equals(cfg.profile())) cfg.switchProfile(profile.get());
        Set<String> on = modulesFor(preset.get());
        for (Module m : NovaClient.modules().all()) {
            if (ALWAYS.contains(m.name())) continue;
            m.setEnabled(on.contains(m.name()));
        }
        cfg.save();
        if (mc.player != null) mc.player.sendMessage(Text.literal("§d[Nova] §fПресет §d" + preset.get() + "§f применён → профиль §7" + profile.get()), true);
        setEnabled(false);
    }
}
