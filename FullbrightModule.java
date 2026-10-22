package dev.nova.modules.world;

import dev.nova.mixin.SimpleOptionAccessor;
import dev.nova.module.Category;
import dev.nova.module.Module;
import dev.nova.setting.NumberSetting;

/** Fullbright через gamma (чисто визуал, сервер не видит). Исходная яркость восстанавливается при выключении. */
public final class FullbrightModule extends Module {
    private final NumberSetting gamma = number("Яркость", 10, 1, 15, 0.5);
    private double previous = 1.0;

    public FullbrightModule() { super("Fullbright", "Полная яркость без зелий", Category.WORLD); }

    @Override protected void onEnable() { previous = mc.options.getGamma().getValue(); }

    @Override public void onTick() {
        if (mc.options.getGamma().getValue() != gamma.get())
            ((SimpleOptionAccessor) (Object) mc.options.getGamma()).nova$setValueRaw(gamma.get());
    }

    @Override protected void onDisable() {
        ((SimpleOptionAccessor) (Object) mc.options.getGamma()).nova$setValueRaw(Math.min(1.0, previous));
    }
}
