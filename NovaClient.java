package dev.nova;

import dev.nova.config.ConfigManager;
import dev.nova.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Точка входа Nova. Этап 0-1: ядро, модули, настройки, конфиг. */
public final class NovaClient implements ClientModInitializer {
    public static final String ID = "nova";
    public static final String NAME = "Nova";
    public static final String VERSION = "0.2.1";
    public static final Logger LOG = LoggerFactory.getLogger(NAME);

    private static ModuleManager modules;
    private static ConfigManager config;

    @Override
    public void onInitializeClient() {
        dev.nova.util.CombatTracker.init();
        modules = new ModuleManager();
        modules.init();
        config = new ConfigManager(modules);
        config.load();

        ClientTickEvents.END_CLIENT_TICK.register(modules::onTick);
        ClientTickEvents.END_CLIENT_TICK.register(c -> dev.nova.modules.client.ClickGuiModule.tickOpen());
        HudRenderCallback.EVENT.register((ctx, tick) -> modules.onRender2D(ctx, tick.getTickDelta(true)));
        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> config.save());

        LOG.info("{} {} loaded: {} modules", NAME, VERSION, modules.all().size());
    }

    public static ModuleManager modules() { return modules; }
    public static ConfigManager config() { return config; }
}
