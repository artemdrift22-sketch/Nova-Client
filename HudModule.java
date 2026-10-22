package dev.nova.hud;

import dev.nova.module.Category;
import dev.nova.module.Module;
import dev.nova.render.Render;
import dev.nova.render.Render2DUtils;
import dev.nova.render.ThemeManager;
import dev.nova.setting.BoolSetting;
import net.minecraft.client.gui.DrawContext;

/**
 * HUD-элемент в стиле Nova Glass: стеклянная панель + (опционально) заголовок со звездой.
 * Позиция хранится в долях экрана (0..1). Наследник реализует draw() и width()/height() контента.
 */
public abstract class HudModule extends Module {
    public static final float HEADER_H = 15;

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
    /** Рисует в локальных координатах контента (0,0 = левый верх под заголовком). */
    protected abstract void draw(DrawContext ctx, float delta, boolean preview);

    /** Заголовок панели (как "Potions" / "Hot Keys" на референсе). null = без заголовка. */
    protected String header() { return null; }

    public float fullWidth() {
        String h = header();
        return h == null ? width() : Math.max(width(), Render.width(h) + 26);
    }

    public float fullHeight() { return height() + (header() != null ? HEADER_H : 0); }

    public float px() { return clamp(x * mc.getWindow().getScaledWidth(), mc.getWindow().getScaledWidth() - fullWidth() * scale); }
    public float py() { return clamp(y * mc.getWindow().getScaledHeight(), mc.getWindow().getScaledHeight() - fullHeight() * scale); }

    private static float clamp(float v, float max) { return Math.max(0, Math.min(v, Math.max(0, max))); }

    @Override
    public final void onRender2D(DrawContext ctx, float delta) {
        if (mc.currentScreen instanceof HudEditorScreen) return; // редактор рисует сам
        renderAt(ctx, delta, false);
    }

    public void renderAt(DrawContext ctx, float delta, boolean preview) {
        float fw = fullWidth(), fh = fullHeight();
        ctx.getMatrices().push();
        ctx.getMatrices().translate(px(), py(), 0);
        ctx.getMatrices().scale(scale, scale, 1);
        if (background.get()) Render2DUtils.drawGlassPanel(ctx, 0, 0, fw, fh, ThemeManager.RADIUS_HUD);
        String h = header();
        if (h != null) {
            Render2DUtils.drawNovaStar(ctx, 5, 3.5f, 8, ThemeManager.GOLD);
            Render.text(ctx, h, 17, 4, ThemeManager.TEXT, false);
            Render2DUtils.drawGradient(ctx, 5, HEADER_H - 2, fw - 10, 1, ThemeManager.withAlpha(ThemeManager.TEXT, 34), ThemeManager.withAlpha(ThemeManager.TEXT, 34));
            ctx.getMatrices().translate(0, HEADER_H, 0);
        }
        draw(ctx, delta, preview);
        ctx.getMatrices().pop();
    }

    public void setPixelPos(float px, float py) {
        x = px / mc.getWindow().getScaledWidth();
        y = py / mc.getWindow().getScaledHeight();
    }
}
