package dev.nova.hud;

import dev.nova.module.Category;
import dev.nova.module.Module;
import dev.nova.modules.client.InterfaceModule;
import dev.nova.render.AnimationUtils;
import dev.nova.render.AnimationUtils.Animation;
import dev.nova.render.AnimationUtils.Smooth;
import dev.nova.render.Render;
import dev.nova.render.Render2DUtils;
import dev.nova.render.ThemeManager;
import dev.nova.setting.BoolSetting;
import net.minecraft.client.gui.DrawContext;

/**
 * HUD-элемент Nova v0.4 (стиль Pulse).
 *  - Фон: Pulse (тёмная плашка RGBA 10,10,15,150 + обводка) / Стекло / Минимал — Interface -> Стиль HUD.
 *  - Появление: fade + scale 0.9 -> 1, исчезновение — плавный fade (shouldShow()).
 *  - Размер фона плавно подстраивается под контент (когда меняется текст / число строк).
 * Позиция хранится в долях экрана (0..1). Наследник реализует draw() и width()/height() контента.
 */
public abstract class HudModule extends Module {
    public static final float HEADER_H = 15;
    public static final float PAD = 5;

    public float x, y;        // 0..1 от размера экрана
    public float scale = 1f;
    public final float defX, defY;
    protected final BoolSetting background = bool("Фон", true);

    private final Animation appear = new Animation(260, AnimationUtils::easeOutCubic).set(0f);
    private final Smooth bgW = new Smooth(16f), bgH = new Smooth(16f);
    private long lastRender;

    protected HudModule(String name, String description, float defX, float defY) {
        this(name, description, Category.HUD, defX, defY);
    }

    protected HudModule(String name, String description, Category category, float defX, float defY) {
        super(name, description, category);
        this.x = defX;
        this.y = defY;
        this.defX = defX;
        this.defY = defY;
    }

    public abstract float width();
    public abstract float height();
    /** Рисует в локальных координатах контента (0,0 = левый верх под заголовком). */
    protected abstract void draw(DrawContext ctx, float delta, boolean preview);

    /** Заголовок панели ("Potions", "Hotkeys"...). null = без заголовка. */
    protected String header() { return null; }

    /** false -> элемент плавно гаснет (например TargetHUD без цели). */
    protected boolean shouldShow(boolean preview) { return true; }

    /** Вызывается раз за кадр перед отрисовкой (обновление анимированных списков и т.п.). */
    protected void update(boolean preview) {}

    protected boolean showHeader() {
        InterfaceModule i = InterfaceModule.get();
        return header() != null && (i == null || i.headers.get());
    }

    protected static String hudStyle() {
        InterfaceModule i = InterfaceModule.get();
        return i == null ? "Pulse" : i.hudStyle.get();
    }

    public float fullWidth() {
        return showHeader() ? Math.max(width(), Render.width(header()) + 30) : width();
    }

    public float fullHeight() { return height() + (showHeader() ? HEADER_H : 0); }

    public float px() { return clamp(x * mc.getWindow().getScaledWidth(), mc.getWindow().getScaledWidth() - fullWidth() * scale); }
    public float py() { return clamp(y * mc.getWindow().getScaledHeight(), mc.getWindow().getScaledHeight() - fullHeight() * scale); }

    private static float clamp(float v, float max) { return Math.max(0, Math.min(v, Math.max(0, max))); }

    public void resetPos() { x = defX; y = defY; scale = 1f; }

    @Override
    public final void onRender2D(DrawContext ctx, float delta) {
        if (mc.currentScreen instanceof HudEditorScreen) return; // редактор рисует сам
        renderAt(ctx, delta, false);
    }

    public void renderAt(DrawContext ctx, float delta, boolean preview) {
        update(preview);
        boolean show = preview || shouldShow(false);
        float fw = fullWidth(), fh = fullHeight();

        long now = System.currentTimeMillis();
        if (now - lastRender > 250) { // модуль только что включили / вернулись в игру — анимация появления заново
            appear.set(0f);
            bgW.set(fw);
            bgH.set(fh);
        }
        lastRender = now;
        float target = show ? 1f : 0f;
        if (appear.target() != target) appear.animateTo(target);
        float a = appear.get();
        if (a < 0.01f && !show) return;

        float w = bgW.update(fw), h = bgH.update(fh);
        float sc = 0.9f + 0.1f * a;

        float prevAlpha = Render2DUtils.alpha;
        ctx.getMatrices().push();
        try {
            Render2DUtils.alpha = prevAlpha * a;
            ctx.getMatrices().translate(px(), py(), 0);
            ctx.getMatrices().scale(scale, scale, 1);
            ctx.getMatrices().translate(w / 2f, h / 2f, 0);
            ctx.getMatrices().scale(sc, sc, 1);
            ctx.getMatrices().translate(-w / 2f, -h / 2f, 0);

            if (background.get()) drawBackground(ctx, w, h);
            if (showHeader()) {
                drawHeaderBar(ctx, header(), w);
                ctx.getMatrices().translate(0, HEADER_H, 0);
            }
            draw(ctx, delta, preview);
        } finally {
            Render2DUtils.alpha = prevAlpha;
            ctx.getMatrices().pop();
        }
    }

    protected void drawBackground(DrawContext ctx, float w, float h) {
        float r = ThemeManager.RADIUS_HUD;
        switch (hudStyle()) {
            case "Стекло" -> Render2DUtils.drawGlassPanel(ctx, 0, 0, w, h, r);
            case "Минимал" -> Render2DUtils.drawRoundedRect(ctx, 0, 0, w, h, r, ThemeManager.HUD_BG);
            default -> Render2DUtils.drawHudPanel(ctx, 0, 0, w, h, r, true);
        }
    }

    /** Шапка как на референсе: плитка со звездой + название + тонкая линия. */
    protected void drawHeaderBar(DrawContext ctx, String title, float w) {
        Render2DUtils.drawRoundedRect(ctx, 4, 3, 10, 10, 3, ThemeManager.withAlpha(ThemeManager.GOLD, 38));
        Render2DUtils.drawNovaStar(ctx, 5, 4, 8, ThemeManager.GOLD);
        Render.text(ctx, title, 19, 4, ThemeManager.TEXT, false);
        Render2DUtils.drawGradientH(ctx, 4, HEADER_H - 1.5f, w - 8, 1, ThemeManager.withAlpha(ThemeManager.ACCENT, 90), ThemeManager.withAlpha(ThemeManager.ACCENT, 0));
    }

    public void setPixelPos(float px, float py) {
        x = px / mc.getWindow().getScaledWidth();
        y = py / mc.getWindow().getScaledHeight();
    }
}
