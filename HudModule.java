package dev.nova.hud;

import dev.nova.module.Category;
import dev.nova.module.Module;
import dev.nova.render.Render;
import dev.nova.render.Theme;
import dev.nova.setting.BoolSetting;
import net.minecraft.client.gui.DrawContext;

/**
 * HUD-элемент: позиция хранится в долях экрана (0..1), поэтому не ломается при смене разрешения/GUI scale.
 * Наследник реализует draw() и возвращает свой размер через width()/height().
 */
public abstract class HudModule extends Module {
    public float x, y;        // 0..1 от размера экрана
    public float scale = 1f;
    protected final BoolSetting background = bool("Фон", true);

    protected HudModule(String name, String description, float defX, float defY) {
        this(name, description, Category.HUD, defX, defY);
    }

    protected HudModule(String name, String description, Category category, float defX, float defY) {
        super(name, description, category);
        this.x = defX;
        this.y = defY;
    }

    public abstract float width();
    public abstract float height();
    /** Рисует в локальных координатах (0,0 = левый верх элемента). preview=true в HUD-редакторе. */
    protected abstract void draw(DrawContext ctx, float delta, boolean preview);

    public float px() { return clamp(x * mc.getWindow().getScaledWidth(), mc.getWindow().getScaledWidth() - width() * scale); }
    public float py() { return clamp(y * mc.getWindow().getScaledHeight(), mc.getWindow().getScaledHeight() - height() * scale); }

    private static float clamp(float v, float max) { return Math.max(0, Math.min(v, Math.max(0, max))); }

    @Override
    public final void onRender2D(DrawContext ctx, float delta) {
        if (mc.currentScreen instanceof HudEditorScreen) return; // редактор рисует сам
        renderAt(ctx, delta, false);
    }

    public void renderAt(DrawContext ctx, float delta, boolean preview) {
        ctx.getMatrices().push();
        ctx.getMatrices().translate(px(), py(), 0);
        ctx.getMatrices().scale(scale, scale, 1);
        if (background.get()) Render.rounded(ctx, 0, 0, width(), height(), 2, Theme.HUD_BG);
        draw(ctx, delta, preview);
        ctx.getMatrices().pop();
    }

    public void setPixelPos(float px, float py) {
        x = px / mc.getWindow().getScaledWidth();
        y = py / mc.getWindow().getScaledHeight();
    }
}
