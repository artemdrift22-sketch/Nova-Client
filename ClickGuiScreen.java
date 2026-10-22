package dev.nova.gui;

import dev.nova.NovaClient;
import dev.nova.module.Category;
import dev.nova.module.Module;
import dev.nova.render.Anim;
import dev.nova.render.Render;
import dev.nova.render.Render2DUtils;
import dev.nova.render.ThemeManager;
import dev.nova.setting.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.function.IntConsumer;

/**
 * Nova Visuals ClickGUI (Glassmorphism).
 * Стеклянная панель 600x400 по центру, слева категории, справа сетка карточек 100x40.
 * Если экран меньше (большой GUI scale) — всё меню пропорционально уменьшается.
 * ЛКМ — вкл/выкл, ПКМ — настройки, СКМ — бинд, печать — поиск.
 */
public final class ClickGuiScreen extends Screen {
    private static final float W = 600, H = 400, SIDE = 150;
    private static final float CARD_W = 100, CARD_H = 40, GAP = 10, COLS = 4;
    private static final float GX = 158, GTOP = 62, GBOTTOM = H - 28, GW = 430;
    private static final float ROW = 24;
    private static final Category[] ORDER = {Category.WORLD, Category.HUD, Category.PVP, Category.COSMETIC, Category.QOL, Category.CLIENT};

    private static Category current = Category.WORLD;

    private record Hit(float x, float y, float w, float h, IntConsumer action) {}
    private final List<Hit> hits = new ArrayList<>();
    private final Map<Object, Anim> anims = new HashMap<>();
    private final Anim open = new Anim(4f).target(1f);

    private Module settingsOf;
    private Module binding;
    private NumberSetting draggingSlider;
    private float sliderX0, sliderW;
    private String search = "";
    private float scroll, settingsScroll;
    private float s = 1f, ox, oy;

    public ClickGuiScreen() { super(Text.literal("Nova Visuals")); }

    // ===================== helpers =====================

    private static String title(Category c) {
        return switch (c) {
            case WORLD -> "Visuals";
            case HUD -> "HUD";
            case PVP -> "PvP";
            case COSMETIC -> "Cosmetics";
            case QOL -> "Utility";
            case CLIENT -> "Settings";
        };
    }

    private static String icon(Category c) {
        return switch (c) {
            case WORLD -> "◈";
            case HUD -> "▣";
            case PVP -> "⚔";
            case COSMETIC -> "✦";
            case QOL -> "⚙";
            case CLIENT -> "☰";
        };
    }

    private Anim anim(Object key, float speed) { return anims.computeIfAbsent(key, k -> new Anim(speed)); }

    private void hit(float x, float y, float w, float h, IntConsumer a) { hits.add(new Hit(x, y, w, h, a)); }

    private float lx(double mx) { return (float) ((mx - ox) / s); }
    private float ly(double my) { return (float) ((my - oy) / s); }

    private static void text(DrawContext ctx, String str, float x, float y, int color) {
        Render.text(ctx, str, x, y, color, false);
    }

    private static void text(DrawContext ctx, String str, float x, float y, int color, float scale) {
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x, y, 0);
        ctx.getMatrices().scale(scale, scale, 1f);
        Render.text(ctx, str, 0, 0, color, false);
        ctx.getMatrices().pop();
    }

    private static String trim(String str, int maxW) {
        if (Render.width(str) <= maxW) return str;
        return Render.font().trimToWidth(str, Math.max(0, maxW - Render.width("…"))) + "…";
    }

    private static String keyName(int key) {
        return InputUtil.Type.KEYSYM.createFromCode(key).getLocalizedText().getString().toUpperCase(Locale.ROOT);
    }

    private List<Module> modules() {
        if (!search.isEmpty()) {
            String q = search.toLowerCase(Locale.ROOT);
            return NovaClient.modules().all().stream()
                .filter(m -> m.name().toLowerCase(Locale.ROOT).contains(q) || m.description().toLowerCase(Locale.ROOT).contains(q))
                .toList();
        }
        return NovaClient.modules().byCategory(current);
    }

    // ===================== render =====================

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta); // ванильный blur-шейдер + затемнение мира позади меню

        s = Math.min(1f, Math.min((width - 16) / W, (height - 16) / H));
        ox = (width - W * s) / 2f;
        oy = (height - H * s) / 2f;
        float a = open.get();
        float mx = lx(mouseX), my = ly(mouseY) + (1 - a) * 14;
        hits.clear();

        if (draggingSlider != null) {
            float t = (mx - sliderX0) / sliderW;
            draggingSlider.set(draggingSlider.min + Math.max(0, Math.min(1, t)) * (draggingSlider.max - draggingSlider.min));
        }

        ctx.getMatrices().push();
        ctx.getMatrices().translate(ox, oy - (1 - a) * 14 * s, 0);
        ctx.getMatrices().scale(s, s, 1f);

        Render2DUtils.drawGlassPanel(ctx, 0, 0, W, H, ThemeManager.RADIUS_PANEL);
        drawSidebar(ctx, mx, my);
        if (settingsOf == null) drawGrid(ctx, mx, my); else drawSettings(ctx, mx, my);
        drawSearch(ctx);

        ctx.getMatrices().pop();
    }

    private void drawSidebar(DrawContext ctx, float mx, float my) {
        Render2DUtils.drawRoundedRect(ctx, 6, 6, SIDE - 6, H - 12, 9, ThemeManager.SIDEBAR);

        // логотип
        Render2DUtils.drawNovaLogo(ctx, 18, 17, 20);
        text(ctx, "NOVA", 46, 17, ThemeManager.TEXT, 1.5f);
        text(ctx, "VISUALS", 47, 32, ThemeManager.GOLD, 0.75f);
        Render2DUtils.drawGradient(ctx, 16, 50, SIDE - 26, 1, ThemeManager.withAlpha(ThemeManager.CYAN, 90), ThemeManager.withAlpha(ThemeManager.CYAN, 90));

        float y = 60;
        for (Category c : ORDER) {
            boolean sel = c == current && search.isEmpty();
            boolean hover = Render.hovered(mx, my, 14, y, SIDE - 22, 22);
            float t = anim(c, 6f).target(sel ? 1f : 0f).get();
            float hv = anim("cat_h_" + c, 8f).target(hover ? 1f : 0f).get();

            int bg = ThemeManager.lerp(ThemeManager.withAlpha(ThemeManager.TEXT, (int) (10 * hv)), ThemeManager.withAlpha(ThemeManager.CYAN, 34), t);
            Render2DUtils.drawRoundedRect(ctx, 14, y, SIDE - 22, 22, ThemeManager.RADIUS_BUTTON, bg);
            if (t > 0.01f) {
                Render2DUtils.drawGlow(ctx, 14, y + 6, 2, 10, 1, 3, ThemeManager.withAlpha(ThemeManager.GOLD, (int) (110 * t)));
                Render2DUtils.drawRoundedRect(ctx, 14, y + 6, 2, 10, 1, ThemeManager.fade(ThemeManager.GOLD, t));
            }
            int tc = ThemeManager.lerp(ThemeManager.TEXT_DIM, ThemeManager.TEXT, Math.max(t, hv * 0.6f));
            text(ctx, icon(c), 22 + 3 * t, y + 7, sel ? ThemeManager.CYAN : tc);
            text(ctx, title(c), 36 + 3 * t, y + 7, tc);
            String cnt = String.valueOf(NovaClient.modules().byCategory(c).size());
            text(ctx, cnt, SIDE - 14 - Render.width(cnt), y + 7, ThemeManager.withAlpha(ThemeManager.TEXT_DIM, 160));

            final Category cat = c;
            hit(14, y, SIDE - 22, 22, b -> { current = cat; settingsOf = null; search = ""; scroll = 0; });
            y += 26;
        }

        // карточка игрока внизу
        float py = H - 44;
        Render2DUtils.drawRoundedRect(ctx, 14, py, SIDE - 22, 30, ThemeManager.RADIUS_BUTTON, ThemeManager.CARD);
        Render2DUtils.drawRoundedOutline(ctx, 14, py, SIDE - 22, 30, ThemeManager.RADIUS_BUTTON, 1, ThemeManager.STROKE);
        Render2DUtils.drawCircle(ctx, 29, py + 15, 9, ThemeManager.withAlpha(ThemeManager.CYAN, 40));
        Render2DUtils.drawNovaStar(ctx, 23, py + 9, 12, ThemeManager.GOLD);
        String name = client != null && client.player != null ? client.player.getName().getString() : "Player";
        text(ctx, trim(name, 84), 44, py + 6, ThemeManager.TEXT);
        text(ctx, "Nova " + NovaClient.VERSION, 44, py + 17, ThemeManager.TEXT_DIM, 0.75f);
    }

    private void drawSearch(DrawContext ctx) {
        float x = W - 12 - 150, y = 16;
        boolean active = !search.isEmpty();
        Render2DUtils.drawRoundedRect(ctx, x, y, 150, 20, ThemeManager.RADIUS_BUTTON, ThemeManager.CARD);
        Render2DUtils.drawRoundedOutline(ctx, x, y, 150, 20, ThemeManager.RADIUS_BUTTON, 1, active ? ThemeManager.withAlpha(ThemeManager.CYAN, 160) : ThemeManager.STROKE);
        text(ctx, "⌕", x + 7, y + 6, ThemeManager.TEXT_DIM);
        boolean caret = (System.currentTimeMillis() / 500) % 2 == 0;
        text(ctx, active ? trim(search, 118) + (caret ? "_" : "") : "Поиск модулей...", x + 19, y + 6, active ? ThemeManager.TEXT : ThemeManager.withAlpha(ThemeManager.TEXT_DIM, 150));
    }

    private void drawHeader(DrawContext ctx, String title, String sub) {
        text(ctx, title, GX, 17, ThemeManager.TEXT, 1.6f);
        text(ctx, sub, GX, 35, ThemeManager.TEXT_DIM, 0.75f);
        Render2DUtils.drawGradient(ctx, GX, 50, GW, 1, ThemeManager.withAlpha(ThemeManager.TEXT, 30), ThemeManager.withAlpha(ThemeManager.TEXT, 30));
    }

    // ---------- сетка модулей ----------
    private void drawGrid(DrawContext ctx, float mx, float my) {
        List<Module> mods = modules();
        String title = search.isEmpty() ? title(current) : "Поиск";
        drawHeader(ctx, title, mods.size() + " модулей  ·  ЛКМ — вкл/выкл  ·  ПКМ — настройки  ·  СКМ — бинд");

        if (mods.isEmpty()) {
            String msg = search.isEmpty() && current == Category.COSMETIC ? "Скоро: плащи, крылья, шляпы, ауры" : "Ничего не найдено";
            Render2DUtils.drawNovaStar(ctx, GX + GW / 2f - 12, 160, 24, ThemeManager.withAlpha(ThemeManager.TEXT, 60));
            text(ctx, msg, GX + (GW - Render.width(msg)) / 2f, 194, ThemeManager.TEXT_DIM);
            return;
        }

        int rows = (int) Math.ceil(mods.size() / COLS);
        float content = rows * (CARD_H + GAP) - GAP;
        float maxScroll = Math.max(0, content - (GBOTTOM - GTOP) + 6);
        scroll = Math.max(0, Math.min(maxScroll, scroll));

        Module hovered = null;
        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            float x = GX + (i % (int) COLS) * (CARD_W + GAP);
            float y = GTOP + 4 + (i / (int) COLS) * (CARD_H + GAP) - scroll;
            if (y < GTOP - 1 || y + CARD_H > GBOTTOM + 1) continue; // вне области прокрутки
            boolean hover = Render.hovered(mx, my, x, y, CARD_W, CARD_H);
            if (hover) hovered = m;
            drawCard(ctx, m, x, y, hover);
            final Module mod = m;
            hit(x, y, CARD_W, CARD_H, b -> {
                if (b == 0) mod.toggle();
                else if (b == 1) { settingsOf = mod; settingsScroll = 0; }
                else if (b == 2) binding = mod;
            });
        }

        // полоса прокрутки
        if (maxScroll > 0) {
            float track = GBOTTOM - GTOP;
            float th = Math.max(20, track * track / (content + 6));
            float ty = GTOP + (track - th) * (scroll / maxScroll);
            Render2DUtils.drawRoundedRect(ctx, W - 7, ty, 2, th, 1, ThemeManager.withAlpha(ThemeManager.TEXT, 60));
        }

        String hint = hovered != null ? hovered.description() : "";
        if (binding != null) hint = "Нажми клавишу для «" + binding.name() + "»  ·  Esc/Del — сбросить";
        if (hint != null && !hint.isEmpty())
            text(ctx, trim(hint, (int) GW), GX, H - 18, binding != null ? ThemeManager.GOLD : ThemeManager.TEXT_DIM);
    }

    private void drawCard(DrawContext ctx, Module m, float x, float y, boolean hover) {
        float t = anim(m, 7f).target(hover ? 1f : 0f).get();
        float on = anim("on_" + m.name(), 6f).target(m.enabled() ? 1f : 0f).get();
        float scale = 1f + 0.05f * t;
        float cx = x + CARD_W / 2f, cy = y + CARD_H / 2f;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(cx, cy, 0);
        ctx.getMatrices().scale(scale, scale, 1f);
        ctx.getMatrices().translate(-cx, -cy, 0);

        float r = ThemeManager.RADIUS_BUTTON;
        if (t > 0.01f) Render2DUtils.drawGlow(ctx, x, y, CARD_W, CARD_H, r, 5f, ThemeManager.withAlpha(ThemeManager.CYAN, (int) (70 * t)));
        int base = ThemeManager.lerp(ThemeManager.CARD, ThemeManager.CARD_ON, on);
        Render2DUtils.drawRoundedGradient(ctx, x, y, CARD_W, CARD_H, r, ThemeManager.lerp(base, ThemeManager.CARD_HOVER, t), base);
        Render2DUtils.drawRoundedOutline(ctx, x, y, CARD_W, CARD_H, r, 1f, ThemeManager.lerp(ThemeManager.STROKE, ThemeManager.CYAN, t));

        text(ctx, trim(m.name(), 62), x + 8, y + 8, ThemeManager.TEXT);
        Render2DUtils.drawToggle(ctx, x + CARD_W - 8 - 20, y + 8, 20, 10, on);

        String bottom;
        int bc = ThemeManager.TEXT_DIM;
        if (binding == m) { bottom = "Клавиша..."; bc = ThemeManager.GOLD; }
        else bottom = trim(m.description(), m.key() >= 0 ? 58 : 84);
        text(ctx, bottom, x + 8, y + 25, bc, 0.75f);
        if (m.key() >= 0 && binding != m) {
            String k = trim(keyName(m.key()), 24);
            float kw = Render.width(k) * 0.75f + 6;
            Render2DUtils.drawRoundedRect(ctx, x + CARD_W - 8 - kw, y + 23, kw, 10, 3, ThemeManager.withAlpha(ThemeManager.GOLD, 40));
            text(ctx, k, x + CARD_W - 5 - kw, y + 25, ThemeManager.GOLD, 0.75f);
        }
        ctx.getMatrices().pop();
    }

    // ---------- настройки модуля ----------
    private void drawSettings(DrawContext ctx, float mx, float my) {
        Module m = settingsOf;
        drawHeader(ctx, m.name(), m.description());

        // назад
        boolean bh = Render.hovered(mx, my, GX, GTOP, 62, 18);
        Render2DUtils.drawRoundedRect(ctx, GX, GTOP, 62, 18, ThemeManager.RADIUS_BUTTON, bh ? ThemeManager.CARD_HOVER : ThemeManager.CARD);
        Render2DUtils.drawRoundedOutline(ctx, GX, GTOP, 62, 18, ThemeManager.RADIUS_BUTTON, 1, bh ? ThemeManager.CYAN : ThemeManager.STROKE);
        text(ctx, "‹ Назад", GX + 12, GTOP + 5, ThemeManager.TEXT);
        hit(GX, GTOP, 62, 18, b -> settingsOf = null);

        // главный тумблер модуля
        float on = anim("on_" + m.name(), 6f).target(m.enabled() ? 1f : 0f).get();
        text(ctx, m.enabled() ? "Включён" : "Выключен", GX + GW - 34 - Render.width(m.enabled() ? "Включён" : "Выключен"), GTOP + 5, m.enabled() ? ThemeManager.GOLD : ThemeManager.TEXT_DIM);
        Render2DUtils.drawToggle(ctx, GX + GW - 26, GTOP + 3, 24, 12, on);
        hit(GX + GW - 100, GTOP, 100, 18, b -> m.toggle());

        List<Setting<?>> list = m.settings().stream().filter(Setting::visible).toList();
        float top = GTOP + 26;
        float content = (list.size() + 1) * ROW;
        float maxScroll = Math.max(0, content - (GBOTTOM - top));
        settingsScroll = Math.max(0, Math.min(maxScroll, settingsScroll));

        float y = top - settingsScroll;
        for (Setting<?> st : list) {
            if (y >= top - 1 && y + ROW - 4 <= GBOTTOM + 1) drawSettingRow(ctx, st, y, mx, my);
            y += ROW;
        }
        // строка бинда
        if (y >= top - 1 && y + ROW - 4 <= GBOTTOM + 1) {
            row(ctx, y, mx, my);
            text(ctx, "Клавиша", GX + 10, y + 6, ThemeManager.TEXT);
            String k = binding == m ? "Нажми клавишу..." : (m.key() >= 0 ? keyName(m.key()) : "Нет");
            text(ctx, k, GX + GW - 10 - Render.width(k), y + 6, binding == m ? ThemeManager.GOLD : ThemeManager.CYAN);
            hit(GX, y, GW, ROW - 4, b -> { if (b == 1) m.setKey(-1); else binding = m; });
        }
    }

    private void row(DrawContext ctx, float y, float mx, float my) {
        boolean h = Render.hovered(mx, my, GX, y, GW, ROW - 4);
        Render2DUtils.drawRoundedRect(ctx, GX, y, GW, ROW - 4, ThemeManager.RADIUS_BUTTON, h ? ThemeManager.CARD_HOVER : ThemeManager.CARD);
    }

    private void drawSettingRow(DrawContext ctx, Setting<?> st, float y, float mx, float my) {
        row(ctx, y, mx, my);
        text(ctx, st.name(), GX + 10, y + 6, ThemeManager.TEXT);
        float rh = ROW - 4;

        if (st instanceof BoolSetting b) {
            float t = anim(st, 6f).target(b.get() ? 1f : 0f).get();
            Render2DUtils.drawToggle(ctx, GX + GW - 32, y + 5, 22, 10, t);
            hit(GX, y, GW, rh, k -> b.toggle());

        } else if (st instanceof NumberSetting n) {
            float sx = GX + 200, sw = GW - 200 - 60;
            float p = (float) n.progress();
            Render2DUtils.drawRoundedRect(ctx, sx, y + 8, sw, 4, 2, ThemeManager.withAlpha(ThemeManager.TEXT, 40));
            Render2DUtils.drawRoundedGradient(ctx, sx, y + 8, Math.max(4, sw * p), 4, 2, ThemeManager.CYAN, ThemeManager.withAlpha(ThemeManager.CYAN, 200));
            Render2DUtils.drawGlow(ctx, sx + sw * p - 4, y + 6, 8, 8, 4, 3, ThemeManager.withAlpha(ThemeManager.CYAN, 90));
            Render2DUtils.drawCircle(ctx, sx + sw * p, y + 10, 4, ThemeManager.TEXT);
            String v = n.step >= 1 ? String.valueOf(n.asInt()) : String.format(Locale.ROOT, "%.2f", n.get());
            text(ctx, v, GX + GW - 10 - Render.width(v), y + 6, ThemeManager.CYAN);
            hit(GX, y, GW, rh, k -> {
                if (k == 1) { n.reset(); return; }
                draggingSlider = n; sliderX0 = sx; sliderW = sw;
            });

        } else if (st instanceof ModeSetting md) {
            String v = "‹  " + md.get() + "  ›";
            float vw = Render.width(v) + 12;
            Render2DUtils.drawRoundedRect(ctx, GX + GW - 10 - vw, y + 3, vw, 14, 5, ThemeManager.withAlpha(ThemeManager.CYAN, 30));
            text(ctx, v, GX + GW - 4 - vw, y + 6, ThemeManager.CYAN);
            hit(GX, y, GW, rh, k -> md.cycle(k == 1 ? -1 : 1));

        } else if (st instanceof ColorSetting col) {
            float sx = GX + GW - 34;
            Render2DUtils.drawRoundedRect(ctx, sx, y + 4, 24, 12, 4, col.color() | 0xFF000000);
            Render2DUtils.drawRoundedOutline(ctx, sx, y + 4, 24, 12, 4, 1, ThemeManager.STROKE);
            if (col.rainbow()) text(ctx, "RGB", sx - 6 - Render.width("RGB"), y + 6, ThemeManager.GOLD);
            hit(GX, y, GW, rh, k -> { if (k == 1) col.setRainbow(!col.rainbow()); else col.set(nextPreset(col.get())); });
        }
    }

    private static final int[] PRESETS = {0xFF00E5FF, 0xFFFFD700, 0xFF8B5CF6, 0xFF3B82F6, 0xFF22C55E, 0xFFF97316, 0xFFEF4444, 0xFFEC4899, 0xFFFFFFFF};
    private static int nextPreset(int cur) {
        for (int i = 0; i < PRESETS.length; i++) if (PRESETS[i] == cur) return PRESETS[(i + 1) % PRESETS.length];
        return PRESETS[0];
    }

    // ===================== input =====================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float mx = lx(mouseX), my = ly(mouseY);
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (Render.hovered(mx, my, h.x, h.y, h.w, h.h)) { h.action.accept(button); return true; }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        draggingSlider = null;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        if (settingsOf != null) settingsScroll -= (float) v * 20; else scroll -= (float) v * 20;
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (binding != null) {
            binding.setKey(key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE ? -1 : key);
            binding = null;
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) { search = search.substring(0, search.length() - 1); return true; }
        if (key == GLFW.GLFW_KEY_ESCAPE && settingsOf != null) { settingsOf = null; return true; }
        if (key == GLFW.GLFW_KEY_ESCAPE && !search.isEmpty()) { search = ""; return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(char chr, int mods) {
        if (binding == null && settingsOf == null && (Character.isLetterOrDigit(chr) || chr == ' ') && search.length() < 24) {
            search += chr; scroll = 0; return true;
        }
        return super.charTyped(chr, mods);
    }

    @Override public void close() { NovaClient.config().save(); super.close(); }
    @Override public boolean shouldPause() { return false; }
}
