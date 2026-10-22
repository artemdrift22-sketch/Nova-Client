package dev.nova.modules.hud;

import dev.nova.hud.HudModule;
import dev.nova.render.Anim;
import dev.nova.render.Render;
import dev.nova.render.Theme;
import dev.nova.setting.BoolSetting;
import dev.nova.util.CpsTracker;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;

import java.util.function.BooleanSupplier;

/** WASD + ЛКМ/ПКМ (с CPS) + пробел, с плавной подсветкой нажатия. */
public final class KeystrokesHud extends HudModule {
    private static final int S = 20, G = 2;
    private final BoolSetting mouse = bool("Кнопки мыши", true);
    private final BoolSetting space = bool("Пробел", true);
    private final Anim[] anims = {new Anim(8), new Anim(8), new Anim(8), new Anim(8), new Anim(8), new Anim(8), new Anim(8)};

    public KeystrokesHud() { super("Keystrokes", "Нажатия клавиш", 0.005f, 0.2f); background.set(false); }

    @Override public float width() { return S * 3 + G * 2; }
    @Override public float height() {
        float h = S * 2 + G;
        if (mouse.get()) h += S + G;
        if (space.get()) h += 10 + G;
        return h;
    }

    private void key(DrawContext ctx, int i, float x, float y, float w, float h, String label, BooleanSupplier down) {
        float a = anims[i].target(down.getAsBoolean() ? 1 : 0).get();
        Render.rounded(ctx, x, y, w, h, 2, Render.lerpColor(Theme.HUD_BG, 0xD0FFFFFF, a));
        int tc = Render.lerpColor(Theme.TEXT, 0xFF000000, a);
        String[] lines = label.split("\n");
        float ty = y + (h - lines.length * (Render.height() + 1)) / 2f + 1;
        for (String l : lines) {
            Render.text(ctx, l, x + (w - Render.width(l)) / 2f, ty, tc, false);
            ty += Render.height() + 1;
        }
    }

    private static BooleanSupplier k(KeyBinding b) { return b::isPressed; }

    @Override protected void draw(DrawContext ctx, float delta, boolean preview) {
        var o = mc.options;
        key(ctx, 0, S + G, 0, S, S, "W", k(o.forwardKey));
        key(ctx, 1, 0, S + G, S, S, "A", k(o.leftKey));
        key(ctx, 2, S + G, S + G, S, S, "S", k(o.backKey));
        key(ctx, 3, 2 * (S + G), S + G, S, S, "D", k(o.rightKey));
        float y = 2 * (S + G);
        if (mouse.get()) {
            float half = (width() - G) / 2f;
            key(ctx, 4, 0, y, half, S, "ЛКМ\n" + CpsTracker.left(), CpsTracker::leftDown);
            key(ctx, 5, half + G, y, half, S, "ПКМ\n" + CpsTracker.right(), CpsTracker::rightDown);
            y += S + G;
        }
        if (space.get()) key(ctx, 6, 0, y, width(), 10, "——", k(o.jumpKey));
    }
}
