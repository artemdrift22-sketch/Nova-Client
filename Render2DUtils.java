package dev.nova.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;

/**
 * 2D-движок Nova: всё рисуется треугольниками через Tessellator/BufferBuilder (POSITION_COLOR).
 *
 * Триангуляция скругления:
 *  1. Строим контур — 4 дуги по углам (каждая из N сегментов), обход по часовой.
 *  2. Заливка = веер треугольников из центра: (центр, p[i], p[i+1]).
 *  3. Обводка / тень / свечение = "кольцо" между двумя контурами с одинаковым числом точек:
 *     каждый отрезок пары контуров -> 2 треугольника (квад).
 *
 * Перед каждой фигурой вызывается ctx.draw(): DrawContext буферизует текст и fill(),
 * сброс сохраняет правильный порядок слоёв (текст, нарисованный раньше, не окажется поверх).
 */
public final class Render2DUtils {
    private Render2DUtils() {}

    // ===================== низкий уровень =====================

    private static BufferBuilder begin(DrawContext ctx) {
        ctx.draw();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        return Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
    }

    private static void end(BufferBuilder bb) {
        BuiltBuffer built = bb.endNullable();
        if (built != null) BufferRenderer.drawWithGlobalProgram(built);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static Matrix4f mat(DrawContext ctx) { return ctx.getMatrices().peek().getPositionMatrix(); }

    private static void v(BufferBuilder bb, Matrix4f m, float x, float y, int color) {
        bb.vertex(m, x, y, 0f).color(color);
    }

    private static int segments(float r) {
        return Math.max(3, Math.min(14, (int) Math.ceil(r * 1.2f)));
    }

    /**
     * Контур скруглённого прямоугольника. grow > 0 — расширить наружу (тень/свечение), grow < 0 — внутрь (обводка).
     * Центры дуг фиксированы, меняется только радиус: так у всех колец одинаковое число точек.
     */
    private static float[] contour(float x, float y, float w, float h, float r, float grow, int seg) {
        float[] pts = new float[4 * (seg + 1) * 2];
        float[][] centers = {
            {x + r, y + r},         // TL: 180..270
            {x + w - r, y + r},     // TR: 270..360
            {x + w - r, y + h - r}, // BR: 0..90
            {x + r, y + h - r}      // BL: 90..180
        };
        float[] startDeg = {180f, 270f, 0f, 90f};
        float rr = Math.max(0f, r + grow);
        int i = 0;
        for (int c = 0; c < 4; c++) {
            for (int s = 0; s <= seg; s++) {
                double a = Math.toRadians(startDeg[c] + 90f * s / seg);
                float px = centers[c][0] + (float) Math.cos(a) * rr;
                float py = centers[c][1] + (float) Math.sin(a) * rr;
                // при r == 0 все точки дуги совпадают с углом — получаются вырожденные (пустые) треугольники, это нормально
                pts[i++] = px;
                pts[i++] = py;
            }
        }
        return pts;
    }

    private static float clampR(float r, float w, float h) { return Math.max(0f, Math.min(r, Math.min(w, h) / 2f)); }

    private static void fan(BufferBuilder bb, Matrix4f m, float[] p, float cx, float cy, float y, float h, int top, int bottom) {
        int n = p.length / 2;
        int cc = ThemeManager.lerp(top, bottom, 0.5f);
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            float x1 = p[i * 2], y1 = p[i * 2 + 1], x2 = p[j * 2], y2 = p[j * 2 + 1];
            v(bb, m, cx, cy, cc);
            v(bb, m, x1, y1, ThemeManager.lerp(top, bottom, (y1 - y) / h));
            v(bb, m, x2, y2, ThemeManager.lerp(top, bottom, (y2 - y) / h));
        }
    }

    private static void ring(BufferBuilder bb, Matrix4f m, float[] outer, float[] inner, int colOuter, int colInner) {
        int n = outer.length / 2;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            float ox1 = outer[i * 2], oy1 = outer[i * 2 + 1], ox2 = outer[j * 2], oy2 = outer[j * 2 + 1];
            float ix1 = inner[i * 2], iy1 = inner[i * 2 + 1], ix2 = inner[j * 2], iy2 = inner[j * 2 + 1];
            v(bb, m, ox1, oy1, colOuter); v(bb, m, ox2, oy2, colOuter); v(bb, m, ix2, iy2, colInner);
            v(bb, m, ox1, oy1, colOuter); v(bb, m, ix2, iy2, colInner); v(bb, m, ix1, iy1, colInner);
        }
    }

    // ===================== API =====================

    /** Скруглённый прямоугольник одним цветом. */
    public static void drawRoundedRect(DrawContext ctx, float x, float y, float w, float h, float radius, int color) {
        drawRoundedGradient(ctx, x, y, w, h, radius, color, color);
    }

    /** Скруглённый прямоугольник с вертикальным градиентом. */
    public static void drawRoundedGradient(DrawContext ctx, float x, float y, float w, float h, float radius, int top, int bottom) {
        if (w <= 0 || h <= 0) return;
        float r = clampR(radius, w, h);
        int seg = segments(r);
        float[] p = contour(x, y, w, h, r, 0f, seg);
        BufferBuilder bb = begin(ctx);
        fan(bb, mat(ctx), p, x + w / 2f, y + h / 2f, y, h, top, bottom);
        end(bb);
    }

    /** Вертикальный градиент (прямоугольник = 2 треугольника). */
    public static void drawGradient(DrawContext ctx, float x, float y, float w, float h, int colorTop, int colorBottom) {
        if (w <= 0 || h <= 0) return;
        Matrix4f m = mat(ctx);
        BufferBuilder bb = begin(ctx);
        v(bb, m, x, y, colorTop); v(bb, m, x, y + h, colorBottom); v(bb, m, x + w, y + h, colorBottom);
        v(bb, m, x, y, colorTop); v(bb, m, x + w, y + h, colorBottom); v(bb, m, x + w, y, colorTop);
        end(bb);
    }

    /** Обводка скруглённого прямоугольника толщиной thickness (рисуется внутрь). */
    public static void drawRoundedOutline(DrawContext ctx, float x, float y, float w, float h, float radius, float thickness, int color) {
        if (w <= 0 || h <= 0) return;
        float r = clampR(radius, w, h);
        int seg = segments(r);
        float[] outer = contour(x, y, w, h, r, 0f, seg);
        float[] inner = contour(x + thickness, y + thickness, w - 2 * thickness, h - 2 * thickness, Math.max(0f, r - thickness), 0f, seg);
        BufferBuilder bb = begin(ctx);
        ring(bb, mat(ctx), outer, inner, color, color);
        end(bb);
    }

    /** Мягкое свечение наружу: от color у края до прозрачного на расстоянии size. */
    public static void drawGlow(DrawContext ctx, float x, float y, float w, float h, float radius, float size, int color) {
        if (w <= 0 || h <= 0 || size <= 0) return;
        float r = clampR(radius, w, h);
        int seg = segments(r + size);
        float[] inner = contour(x, y, w, h, r, 0f, seg);
        float[] outer = contour(x - size, y - size, w + 2 * size, h + 2 * size, r + size, 0f, seg);
        BufferBuilder bb = begin(ctx);
        ring(bb, mat(ctx), outer, inner, ThemeManager.withAlpha(color, 0), color);
        end(bb);
    }

    /** Мягкая тень (radius по умолчанию — как у панели). */
    public static void drawShadow(DrawContext ctx, float x, float y, float w, float h, float blurRadius) {
        drawShadow(ctx, x, y, w, h, ThemeManager.RADIUS_PANEL, blurRadius);
    }

    public static void drawShadow(DrawContext ctx, float x, float y, float w, float h, float radius, float blurRadius) {
        // тень чуть смещена вниз — свет сверху
        drawGlow(ctx, x, y + blurRadius * 0.25f, w, h, radius, blurRadius, ThemeManager.SHADOW);
    }

    /** Стеклянная панель меню (radius 12). */
    public static void drawGlassPanel(DrawContext ctx, float x, float y, float w, float h) {
        drawGlassPanel(ctx, x, y, w, h, ThemeManager.RADIUS_PANEL);
    }

    /**
     * Стекло = тень + полупрозрачный градиент + блик сверху + обводка 1px белым 20%.
     * Размытие: для экранов (ClickGUI) используется ванильный шейдер blur — Screen.render() применяет его
     * к миру позади меню (настройка "Размытие фона меню"). В HUD честный blur каждого кадра слишком дорогой,
     * поэтому там стекло имитируется плотным градиентом.
     */
    public static void drawGlassPanel(DrawContext ctx, float x, float y, float w, float h, float radius) {
        drawShadow(ctx, x, y, w, h, radius, Math.min(10f, radius + 2f));
        drawRoundedGradient(ctx, x, y, w, h, radius, ThemeManager.BG_TOP, ThemeManager.BG);
        // блик в верхней части стекла
        float hl = Math.min(h * 0.45f, 18f);
        drawRoundedGradient(ctx, x + 1, y + 1, w - 2, hl, Math.max(0f, radius - 1), ThemeManager.rgba(255, 255, 255, 18), ThemeManager.rgba(255, 255, 255, 0));
        drawRoundedOutline(ctx, x, y, w, h, radius, ThemeManager.STROKE_WIDTH, ThemeManager.STROKE);
    }

    /** Круг (для ручек тумблеров). */
    public static void drawCircle(DrawContext ctx, float cx, float cy, float radius, int color) {
        drawRoundedRect(ctx, cx - radius, cy - radius, radius * 2, radius * 2, radius, color);
    }

    /**
     * Круглый тумблер. t = 0 (выкл) .. 1 (вкл), можно анимировать.
     * Выкл — серый, вкл — светится золотым.
     */
    public static void drawToggle(DrawContext ctx, float x, float y, float w, float h, float t) {
        int track = ThemeManager.lerp(ThemeManager.TOGGLE_OFF, ThemeManager.withAlpha(ThemeManager.GOLD, 120), t);
        if (t > 0.01f) drawGlow(ctx, x, y, w, h, h / 2f, 4f, ThemeManager.withAlpha(ThemeManager.GOLD, (int) (90 * t)));
        drawRoundedRect(ctx, x, y, w, h, h / 2f, track);
        float r = h / 2f - 1.5f;
        float kx = x + h / 2f + (w - h) * t;
        int knob = ThemeManager.lerp(ThemeManager.KNOB_OFF, ThemeManager.GOLD, t);
        drawCircle(ctx, kx, y + h / 2f, r, knob);
    }

    /** Звезда Nova белого цвета. (x, y) — левый верхний угол квадрата size x size. */
    public static void drawNovaStar(DrawContext ctx, float x, float y, float size) {
        drawNovaStar(ctx, x, y, size, ThemeManager.TEXT);
    }

    /**
     * 5-конечная звезда: 10 вершин (чередуются внешний радиус R и внутренний R*0.42),
     * первая вершина смотрит вверх (-90°). Триангуляция — веер из центра: 10 треугольников.
     */
    public static void drawNovaStar(DrawContext ctx, float x, float y, float size, int color) {
        float cx = x + size / 2f, cy = y + size / 2f + size * 0.04f;
        float outerR = size / 2f, innerR = outerR * 0.42f;
        float[] p = new float[20];
        for (int i = 0; i < 10; i++) {
            double a = Math.toRadians(-90 + i * 36);
            float r = (i % 2 == 0) ? outerR : innerR;
            p[i * 2] = cx + (float) Math.cos(a) * r;
            p[i * 2 + 1] = cy + (float) Math.sin(a) * r;
        }
        Matrix4f m = mat(ctx);
        BufferBuilder bb = begin(ctx);
        for (int i = 0; i < 10; i++) {
            int j = (i + 1) % 10;
            v(bb, m, cx, cy, color);
            v(bb, m, p[i * 2], p[i * 2 + 1], color);
            v(bb, m, p[j * 2], p[j * 2 + 1], color);
        }
        end(bb);
    }

    /** Звезда со свечением (логотип): золотой ореол + белая звезда. */
    public static void drawNovaLogo(DrawContext ctx, float x, float y, float size) {
        float g = size * 0.35f;
        drawNovaStar(ctx, x - g / 2f, y - g / 2f, size + g, ThemeManager.withAlpha(ThemeManager.GOLD, 70));
        drawNovaStar(ctx, x, y, size, ThemeManager.TEXT);
    }
}
