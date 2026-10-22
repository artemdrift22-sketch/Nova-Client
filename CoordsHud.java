package dev.nova.modules.hud;

import dev.nova.hud.HudModule;
import dev.nova.render.Render;
import dev.nova.render.Theme;
import dev.nova.setting.BoolSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.world.World;

import java.util.Locale;

/** XYZ + направление + пересчёт в Незер/Верхний мир. */
public final class CoordsHud extends HudModule {
    private final BoolSetting direction = bool("Направление", true);
    private final BoolSetting other = bool("Коорды другого мира", true);

    public CoordsHud() { super("Coords", "Координаты игрока", 0.005f, 0.88f); }

    private String main() {
        if (mc.player == null) return "XYZ: 0 64 0";
        String s = String.format(Locale.ROOT, "XYZ: %d %d %d", mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ());
        if (direction.get()) s += " [" + mc.player.getHorizontalFacing().asString().toUpperCase(Locale.ROOT) + "]";
        return s;
    }

    private String second() {
        if (!other.get() || mc.player == null || mc.world == null) return null;
        boolean nether = mc.world.getRegistryKey() == World.NETHER;
        if (!nether && mc.world.getRegistryKey() != World.OVERWORLD) return null;
        double k = nether ? 8 : 1 / 8.0;
        return String.format(Locale.ROOT, "%s: %d %d", nether ? "Верх" : "Незер", (int) Math.floor(mc.player.getX() * k), (int) Math.floor(mc.player.getZ() * k));
    }

    @Override public float width() {
        String s2 = second();
        return Math.max(Render.width(main()), s2 == null ? 0 : Render.width(s2)) + 8;
    }
    @Override public float height() { return (second() == null ? 1 : 2) * (Render.height() + 2) + 4; }

    @Override protected void draw(DrawContext ctx, float delta, boolean preview) {
        Render.text(ctx, main(), 4, 3, Theme.TEXT, true);
        String s2 = second();
        if (s2 != null) Render.text(ctx, s2, 4, 3 + Render.height() + 2, Theme.TEXT_DIM, true);
    }
}
