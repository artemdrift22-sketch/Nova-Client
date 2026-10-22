package dev.nova.modules.hud;

import dev.nova.hud.HudModule;
import dev.nova.render.Render;
import dev.nova.render.ThemeManager;
import dev.nova.setting.BoolSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Координаты: тёмная плашка r6, RGBA(10,10,15,150), текст слева с отступом 5px. */
public final class CoordsHud extends HudModule {
    private final BoolSetting direction = bool("Направление", true);
    private final BoolSetting other = bool("Коорды другого мира", true);
    private final BoolSetting biome = bool("Биом", false);

    public CoordsHud() { super("Coords", "Координаты игрока", 0.005f, 0.88f); }

    /** Строки: [метка, значение]. */
    private List<String[]> lines() {
        List<String[]> out = new ArrayList<>();
        if (mc.player == null || mc.world == null) { out.add(new String[]{"XYZ", "0 64 0"}); return out; }
        String xyz = String.format(Locale.ROOT, "%d %d %d", mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ());
        if (direction.get()) xyz += "  " + mc.player.getHorizontalFacing().asString().toUpperCase(Locale.ROOT);
        out.add(new String[]{"XYZ", xyz});
        if (other.get()) {
            boolean nether = mc.world.getRegistryKey() == World.NETHER;
            if (nether || mc.world.getRegistryKey() == World.OVERWORLD) {
                double k = nether ? 8 : 1 / 8.0;
                out.add(new String[]{nether ? "Верх" : "Незер", String.format(Locale.ROOT, "%d %d", (int) Math.floor(mc.player.getX() * k), (int) Math.floor(mc.player.getZ() * k))});
            }
        }
        if (biome.get()) {
            String b = mc.world.getBiome(mc.player.getBlockPos()).getKey().map(key -> key.getValue().getPath()).orElse("?");
            out.add(new String[]{"Биом", b.replace('_', ' ')});
        }
        return out;
    }

    private float labelW(List<String[]> ls) {
        float w = 0;
        for (String[] l : ls) w = Math.max(w, Render.width(l[0]));
        return w;
    }

    @Override public float width() {
        List<String[]> ls = lines();
        float lw = labelW(ls), vw = 0;
        for (String[] l : ls) vw = Math.max(vw, Render.width(l[1]));
        return PAD + lw + 6 + vw + PAD;
    }

    @Override public float height() { return lines().size() * (Render.height() + 2) + PAD * 2 - 2; }

    @Override protected void draw(DrawContext ctx, float delta, boolean preview) {
        List<String[]> ls = lines();
        float lw = labelW(ls);
        float y = PAD;
        for (String[] l : ls) {
            Render.text(ctx, l[0], PAD, y, ThemeManager.ACCENT, false);
            Render.text(ctx, l[1], PAD + lw + 6, y, ThemeManager.TEXT, false);
            y += Render.height() + 2;
        }
    }
}
