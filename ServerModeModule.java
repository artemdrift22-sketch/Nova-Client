package dev.nova.modules.client;

import dev.nova.NovaClient;
import dev.nova.module.Category;
import dev.nova.module.Module;
import dev.nova.modules.world.FullbrightModule;
import dev.nova.modules.world.TimeWeatherModule;
import dev.nova.setting.BoolSetting;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Режим сервера: при входе на мультиплеер выключает модули, которые на строгих серверах могут не приветствоваться
 * (Fullbright, смена времени). После выхода возвращает как было.
 */
public final class ServerModeModule extends Module {
    private final BoolSetting strict = bool("Строгий режим", true);
    private final BoolSetting notify = bool("Уведомлять", true);
    private final List<Class<? extends Module>> risky = List.of(FullbrightModule.class, TimeWeatherModule.class);
    private boolean[] restored;

    public ServerModeModule() {
        super("ServerMode", "Автовыключение спорных модулей на серверах", Category.CLIENT);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(this::onJoin));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(this::onLeave));
    }

    private void onJoin() {
        if (!enabled() || !strict.get() || mc.isInSingleplayer()) return;
        restored = new boolean[risky.size()];
        int off = 0;
        for (int i = 0; i < risky.size(); i++) {
            Module m = NovaClient.modules().get(risky.get(i));
            restored[i] = m.enabled();
            if (m.enabled()) { m.setEnabled(false); off++; }
        }
        if (off > 0 && notify.get() && mc.player != null)
            mc.player.sendMessage(Text.literal("§d[Nova] §fРежим сервера: выключено модулей: " + off), false);
    }

    private void onLeave() {
        if (restored == null) return;
        for (int i = 0; i < risky.size(); i++) if (restored[i]) NovaClient.modules().get(risky.get(i)).setEnabled(true);
        restored = null;
    }
}
