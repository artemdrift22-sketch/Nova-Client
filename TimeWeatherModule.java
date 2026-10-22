package dev.nova.modules.world;

import dev.nova.module.Category;
import dev.nova.module.Module;
import dev.nova.setting.BoolSetting;
import dev.nova.setting.ModeSetting;
import dev.nova.setting.NumberSetting;

/** Time Changer + Weather Changer. Только клиентский рендер, серверное время не меняется. */
public final class TimeWeatherModule extends Module {
    public final BoolSetting changeTime = bool("Менять время", true);
    private final NumberSetting time = number("Время (тики)", 6000, 0, 23999, 100);
    private final BoolSetting cycle = bool("Свой цикл", false);
    private final NumberSetting cycleSpeed = number("Скорость цикла", 10, 1, 200, 1);
    private final ModeSetting weather = mode("Погода", "Сервер", "Сервер", "Ясно", "Дождь", "Гроза");
    private double cycleTime;

    public TimeWeatherModule() { super("Time&Weather", "Своё время суток и погода (визуально)", Category.WORLD); }

    public long time() { return cycle.get() ? (long) cycleTime % 24000 : (long) time.get().doubleValue(); }

    @Override public void onTick() {
        if (cycle.get()) cycleTime = (cycleTime + cycleSpeed.get()) % 24000;
        if (!inGame() || weather.is("Сервер")) return;
        float rain = weather.is("Ясно") ? 0 : 1;
        float thunder = weather.is("Гроза") ? 1 : 0;
        mc.world.setRainGradient(rain);
        mc.world.setThunderGradient(thunder);
    }

    @Override protected void onEnable() { cycleTime = time.get(); }
}
