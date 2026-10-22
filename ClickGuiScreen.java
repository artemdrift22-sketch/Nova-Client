package dev.nova.gui;

import dev.nova.NovaClient;
import dev.nova.module.Category;
import dev.nova.module.Module;
import dev.nova.modules.client.InterfaceModule;
import dev.nova.render.AnimationUtils;
import dev.nova.render.AnimationUtils.Animation;
import dev.nova.render.AnimationUtils.Smooth;
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
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

/**
 * Nova Visuals ClickGUI v0.4 — Glassmorphism + анимации.
 *  - Открытие: 300 мс easeOutCubic, выезд снизу (Y 20 -> 0) + fade 0 -> 255. Закрытие — обратная анимация.
 *  - Размытие мира позади — настоящий blur-шейдер Minecraft, сила из Interface -> Размытие меню.
 *  - Смена вкладки / открытие настроек: старый контент уезжает в сторону и гаснет, новый въезжает.
 *  - Все анимации по реальному времени (deltaTime), не зависят от FPS.
 * ЛКМ — вкл/выкл, ПКМ — настройки, СКМ — бинд, печать — поиск.
 */
public final class ClickGuiScreen extends Screen {
    private static final float W = 600, H = 400, SIDE = 150;
    private static final float CARD_W = 100, CARD_H = 40, GAP = 10, COLS = 4;
    private static final float GX = 158, GTOP = 62, GBOTTOM = H - 28, GW = 430;
    private static final float ROW = 24, PICK_H = 66;
    private static final float SLIDE = 36f;
    private static final Category[] ORDER = {Category.WORLD, Category.HUD, Category.PVP, Category.COSMETIC, Category.QOL, Category.CLIENT};
    private static final int[] PRESETS = {0xFF00E5FF, 0xFFFFD700, 0xFF8B5CF6, 0xFF3B82F6, 0xFF22C55E, 0xFFF97316, 0xFFEF4444, 0xFFEC4899, 0xFFFFFFFF};

    private static Category current = Category.WORLD;

    private record Hit(float x, float y, float w, float h, IntConsumer action) {}
    private final List<Hit> hits = new ArrayList<>();
    private final Map<String, Smooth> smooths = new HashMap<>();
    private final Map<ColorSetting, float[]> hsvCache = new HashMap<>();

    // открытие / закрытие
    private Animation open = new Animation(300, AnimationUtils::easeOutCubic).set(0f).animateTo(1f);
    private boolean closing;
    private Integer savedBlur;

    // переход между страницами (категория / поиск / настройки модуля)
    private final Animation page = new Animation(260, AnimationUtils::easeOutCubic).set(1f);
    private Object prevPage;
    private float prevScroll;
    private int pageDir = 1;

    private Module settingsOf;
    private Module binding;
    private ColorSetting expanded;
    private DoubleConsumer drag;
    private float dragX0, dragW;
    private String search = "";
    private float scrollT, settingsT;
    private final Smooth scrollS = new Smooth(18f), settingsS = new Smooth(18f), selY = new Smooth(16f);
    private float s = 1f, ox, oy;

    // запись хитбоксов: только для активной страницы, со смещением анимации
    private boolean rec = true;
    private float hitDx;

    public ClickGuiScreen() { super(Text.literal("Nova Visuals")); }

    // ===================== lifecycle =====================

    @Override
    protected void init() {
        super.init();
        if (savedBlur == null && client != null) {
            try {
                savedBlur = client.options.getMenuBackgroundBlurriness().getValue();
                InterfaceModule i = InterfaceModule.get();
                if (i != null) client.options.getMenuBackgroundBlurriness().setValue(i.blur.asInt());
            } catch (Throwable ignored) {}
        }
    }

    @Override
    public void removed() {
        if (savedBlur != null && client != null) {
            try { client.options.getMenuBackgroundBlurriness().setValue(savedBlur); } catch (Throwable ignored) {}
            savedBlur = null;
        }
        NovaClient.config().save();
        super.removed();
    }

    /** Esc / клавиша меню — сначала анимация закрытия, потом само закрытие. */
    @Override
    public void close() {
        if (closing) return;
        closing = true;
        float cur = open.get();
        open = new Animation(180, AnimationUtils::easeInCubic).set(cur).animateTo(0f);
    }

    @Override public boolean shouldPause() { return false; }

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

    private static int indexOf(Category c) {
        for (int i = 0; i < ORDER.length; i++) if (ORDER[i] == c) return i;
        return 0;
    }

    /** Сглаженное значение по ключу (ховеры, тумблеры, слайдеры). */
    private float sm(String key, Object o, float target, float sharp) {
        String k = key + "#" + System.identityHashCode(o);
        return smooths.computeIfAbsent(k, x -> new Smooth(sharp).set(target)).update(target);
    }

    private void hit(float x, float y, float w, float h, IntConsumer a) {
        if (rec && w > 0 && h > 0) hits.add(new Hit(x + hitDx, y, w, h, a));
    }

    /** Хитбокс, обрезанный по видимой области прокрутки. */
    private void hitClip(float x, float y, float w, float h, float top, float bottom, IntConsumer a) {
        float y0 = Math.max(y, top), y1 = Math.min(y + h, bottom);
        if (y1 > y0) hit(x, y0, w, y1 - y0, a);
    }

    private float lx(double mx) { return (float) ((mx - ox) / s); }
    private float ly(double my) { return (float) ((my - oy) / s) - 20f * (1f - open.get()); }

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
        String ell = "…";
        int lo = 0, hi = str.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            if (Render.width(str.substring(0, mid) + ell) <= maxW) lo = mid; else hi = mid - 1;
        }
        return str.substring(0, lo) + ell;
    }

    private static String keyName(int key) {
        return InputUtil.Type.KEYSYM.createFromCode(key).getLocalizedText().getString().toUpperCase(Locale.ROOT);
    }

    private List<Module> searchResults() {
        String q = search.toLowerCase(Locale.ROOT);
        return NovaClient.modules().all().stream()
            .filter(m -> m.name().toLowerCase(Locale.ROOT).contains(q) || m.description().toLowerCase(Locale.ROOT).contains(q))
            .toList();
    }

    private Object page() {
        if (settingsOf != null) return settingsOf;
        if (!search.isEmpty()) return "search";
        return current;
    }

    /** Смена страницы с анимацией слайда. dir: +1 — вперёд (влево), -1 — назад. */
    private void go(Runnable change, int dir) {
        Object before = page();
        float sc = settingsOf != null ? settingsS.get() : scrollS.get();
        change.run();
        if (page() != before && !page().equals(before)) {
            prevPage = before;
            prevScroll = sc;
            pageDir = dir == 0 ? 1 : dir;
            page.set(0f).animateTo(1f);
            drag = null;
            scrollS.set(scrollT);
            settingsS.set(settingsT);
        }
    }

    // ===================== render =====================

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (closing && open.done()) { super.close(); return; }
        ThemeManager.apply();
        super.render(ctx, mouseX, mouseY, delta); // ванильный blur-шейдер + затемнение мира позади меню

        s = Math.min(1f, Math.min((width - 16) / W, (height - 16) / H));
        ox = (width - W * s) / 2f;
        oy = (height - H * s) / 2f;
        float a = open.get();
        float yOff = 20f * (1f - a);
        float mx = lx(mouseX), my = ly(mouseY);
        hits.clear();

        if (drag != null && dragW > 0) drag.accept(AnimationUtils.clamp01((mx - dragX0) / dragW));

        ctx.getMatrices().push();
        ctx.getMatrices().translate(ox, oy + yOff * s, 0);
        ctx.getMatrices().scale(s, s, 1f);
        try {
            Render2DUtils.alpha = a;
            // мягкая чёрная тень под меню (два слоя, градиент вниз)
            Render2DUtils.drawShadow(ctx, 0, 6, W, H, ThemeManager.RADIUS_PANEL, 28f);
            // многослойное стекло: фон RGBA(15,15,20,220) + блик + белая обводка 30
            Render2DUtils.drawGlassPanel(ctx, 0, 0, W, H, ThemeManager.RADIUS_PANEL);
            rec = true;
            hitDx = 0;
            drawSidebar(ctx, mx, my);
            drawContent(ctx, mx, my, a);
            Render2DUtils.alpha = a;
            drawSearch(ctx, mx, my);
        } finally {
            Render2DUtils.alpha = 1f;
            rec = true;
            hitDx = 0;
            ctx.getMatrices().pop();
        }
    }

    private void drawContent(DrawContext ctx, float mx, float my, float a) {
        float t = page.get();
        ctx.enableScissor((int) SIDE + 2, 4, (int) W - 3, (int) H - 4);
        if (t < 1f && prevPage != null) {
            rec = false;
            float off = -pageDir * SLIDE * t;
            ctx.getMatrices().push();
            ctx.getMatrices().translate(off, 0, 0);
            Render2DUtils.alpha = a * (1f - t);
            drawPage(ctx, prevPage, prevScroll, -10000, -10000, false);
            ctx.getMatrices().pop();
        }
        float off = t < 1f ? pageDir * SLIDE * (1f - t) : 0f;
        rec = true;
        hitDx = off;
        ctx.getMatrices().push();
        ctx.getMatrices().translate(off, 0, 0);
        Render2DUtils.alpha = a * (prevPage != null ? t : 1f);
        Object p = page();
        float sc = p instanceof Module ? settingsS.update(settingsT) : scrollS.update(scrollT);
        drawPage(ctx, p, sc, mx - off, my, true);
        ctx.getMatrices().pop();
        hitDx = 0;
        Render2DUtils.alpha = a;
        ctx.disableScissor();
    }

    private void drawPage(DrawContext ctx, Object p, float sc, float mx, float my, boolean interactive) {
        if (p instanceof Module m) drawSettings(ctx, m, sc, mx, my, interactive);
        else drawGrid(ctx, p, sc, mx, my, interactive);
    }

    // ---------- sidebar ----------
    private void drawSidebar(DrawContext ctx, float mx, float my) {
        Render2DUtils.drawRoundedRect(ctx, 6, 6, SIDE - 6, H - 12, 9, ThemeManager.SIDEBAR);
        Render2DUtils.drawRoundedOutline(ctx, 6, 6, SIDE - 6, H - 12, 9, 1, ThemeManager.rgba(255, 255, 255, 10));

        // логотип со звездой
        Render2DUtils.drawGlow(ctx, 18, 17, 20, 20, 10, 6, ThemeManager.withAlpha(ThemeManager.GOLD, 40));
        Render2DUtils.drawNovaLogo(ctx, 18, 17, 20);
        text(ctx, "NOVA", 46, 17, ThemeManager.TEXT, 1.5f);
        text(ctx, "VISUALS", 47, 32, ThemeManager.GOLD, 0.75f);
        Render2DUtils.drawGradientH(ctx, 16, 50, SIDE - 26, 1, ThemeManager.withAlpha(ThemeManager.ACCENT, 140), ThemeManager.withAlpha(ThemeManager.ACCENT, 0));

        // активная плашка плавно едет к выбранной категории
        float targetY = 60 + indexOf(current) * 26;
        float sy = selY.update(targetY);
        float selA = sm("selA", this, search.isEmpty() ? 1f : 0f, 12f);
        if (selA > 0.01f) {
            Render2DUtils.drawRoundedGradientH(ctx, 14, sy, SIDE - 22, 22, ThemeManager.RADIUS_BUTTON,
                ThemeManager.withAlpha(ThemeManager.ACCENT, (int) (52 * selA)), ThemeManager.withAlpha(ThemeManager.ACCENT, (int) (12 * selA)));
            Render2DUtils.drawRoundedOutline(ctx, 14, sy, SIDE - 22, 22, ThemeManager.RADIUS_BUTTON, 1, ThemeManager.withAlpha(ThemeManager.ACCENT, (int) (60 * selA)));
            Render2DUtils.drawGlow(ctx, 14, sy + 6, 2, 10, 1, 4, ThemeManager.withAlpha(ThemeManager.GOLD, (int) (120 * selA)));
            Render2DUtils.drawRoundedRect(ctx, 14, sy + 6, 2, 10, 1, ThemeManager.fade(ThemeManager.GOLD, selA));
        }

        float y = 60;
        for (Category c : ORDER) {
            boolean sel = c == current && search.isEmpty();
            boolean hover = Render.hovered(mx, my, 14, y, SIDE - 22, 22);
            float t = sm("cat", c, sel ? 1f : 0f, 14f);
            float hv = sm("cath", c, hover ? 1f : 0f, 16f);
            if (hv > 0.01f && !sel) Render2DUtils.drawRoundedRect(ctx, 14, y, SIDE - 22, 22, ThemeManager.RADIUS_BUTTON, ThemeManager.withAlpha(ThemeManager.TEXT, (int) (12 * hv)));
            int tc = ThemeManager.lerp(ThemeManager.TEXT_DIM, ThemeManager.TEXT, Math.max(t, hv * 0.6f));
            text(ctx, icon(c), 22 + 3 * t + 1.5f * hv, y + 7, ThemeManager.lerp(tc, ThemeManager.ACCENT, t));
            text(ctx, title(c), 36 + 3 * t + 1.5f * hv, y + 7, tc);
            String cnt = String.valueOf(NovaClient.modules().byCategory(c).size());
            text(ctx, cnt, SIDE - 14 - Render.width(cnt), y + 7, ThemeManager.withAlpha(ThemeManager.TEXT_DIM, 160));

            final Category cat = c;
            hit(14, y, SIDE - 22, 22, b -> {
                int dir = Integer.signum(indexOf(cat) - indexOf(current));
                go(() -> { current = cat; settingsOf = null; search = ""; scrollT = 0; expanded = null; }, dir == 0 ? -1 : dir);
            });
            y += 26;
        }

        // карточка игрока
        float py = H - 44;
        Render2DUtils.drawRoundedRect(ctx, 14, py, SIDE - 22, 30, ThemeManager.RADIUS_BUTTON, ThemeManager.CARD);
        Render2DUtils.drawRoundedOutline(ctx, 14, py, SIDE - 22, 30, ThemeManager.RADIUS_BUTTON, 1, ThemeManager.STROKE);
        Render2DUtils.drawCircle(ctx, 29, py + 15, 9, ThemeManager.withAlpha(ThemeManager.ACCENT, 40));
        Render2DUtils.drawNovaStar(ctx, 23, py + 9, 12, ThemeManager.GOLD);
        String name = client != null && client.player != null ? client.player.getName().getString() : "Player";
        text(ctx, trim(name, 84), 44, py + 6, ThemeManager.TEXT);
        text(ctx, "Nova " + NovaClient.VERSION, 44, py + 17, ThemeManager.TEXT_DIM, 0.75f);
    }

    private void drawSearch(DrawContext ctx, float mx, float my) {
        float x = W - 12 - 150, y = 16;
        boolean active = !search.isEmpty();
        float f = sm("search", this, active ? 1f : 0f, 14f);
        float hv = sm("searchH", this, Render.hovered(mx, my, x, y, 150, 20) ? 1f : 0f, 16f);
        if (f > 0.01f) Render2DUtils.drawGlow(ctx, x, y, 150, 20, ThemeManager.RADIUS_BUTTON, 4, ThemeManager.withAlpha(ThemeManager.ACCENT, (int) (50 * f)));
        Render2DUtils.drawRoundedRect(ctx, x, y, 150, 20, ThemeManager.RADIUS_BUTTON, ThemeManager.lerp(ThemeManager.CARD, ThemeManager.CARD_HOVER, hv));
        Render2DUtils.drawRoundedOutline(ctx, x, y, 150, 20, ThemeManager.RADIUS_BUTTON, 1, ThemeManager.lerp(ThemeManager.STROKE, ThemeManager.withAlpha(ThemeManager.ACCENT, 170), f));
        text(ctx, "⌕", x + 7, y + 6, ThemeManager.lerp(ThemeManager.TEXT_DIM, ThemeManager.ACCENT, f));
        boolean caret = (System.currentTimeMillis() / 500) % 2 == 0;
        text(ctx, active ? trim(search, 118) + (caret ? "_" : "") : "Поиск модулей...", x + 19, y + 6, active ? ThemeManager.TEXT : ThemeManager.withAlpha(ThemeManager.TEXT_DIM, 150));
    }

    private void drawHeader(DrawContext ctx, String title, String sub) {
        text(ctx, title, GX, 17, ThemeManager.TEXT, 1.6f);
        text(ctx, trim(sub, (int) ((GW - 160) / 0.75f)), GX, 35, ThemeManager.TEXT_DIM, 0.75f);
        Render2DUtils.drawGradientH(ctx, GX, 50, GW, 1, ThemeManager.withAlpha(ThemeManager.TEXT, 40), ThemeManager.withAlpha(ThemeManager.TEXT, 0));
    }

    // ---------- сетка модулей ----------
    private void drawGrid(DrawContext ctx, Object p, float sc, float mx, float my, boolean interactive) {
        boolean isSearch = !(p instanceof Category);
        List<Module> mods = isSearch ? searchResults() : NovaClient.modules().byCategory((Category) p);
        String title = isSearch ? "Поиск" : title((Category) p);
        drawHeader(ctx, title, mods.size() + " модулей  ·  ЛКМ — вкл/выкл  ·  ПКМ — настройки  ·  СКМ — бинд");

        if (mods.isEmpty()) {
            String msg = !isSearch && p == Category.COSMETIC ? "Скоро: плащи, крылья, шляпы, ауры" : "Ничего не найдено";
            Render2DUtils.drawNovaStar(ctx, GX + GW / 2f - 12, 160, 24, ThemeManager.withAlpha(ThemeManager.TEXT, 60));
            text(ctx, msg, GX + (GW - Render.width(msg)) / 2f, 194, ThemeManager.TEXT_DIM);
            return;
        }

        int rows = (int) Math.ceil(mods.size() / COLS);
        float content = rows * (CARD_H + GAP) - GAP;
        float maxScroll = Math.max(0, content - (GBOTTOM - GTOP) + 8);
        if (interactive) scrollT = Math.max(0, Math.min(maxScroll, scrollT));

        Module hovered = null;
        ctx.enableScissor((int) GX - 8, (int) GTOP - 3, (int) (GX + GW + 8), (int) GBOTTOM + 3);
        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            float x = GX + (i % (int) COLS) * (CARD_W + GAP);
            float y = GTOP + 4 + (i / (int) COLS) * (CARD_H + GAP) - sc;
            if (y + CARD_H < GTOP - 6 || y > GBOTTOM + 6) continue;
            boolean hover = interactive && my >= GTOP && my <= GBOTTOM && Render.hovered(mx, my, x, y, CARD_W, CARD_H);
            if (hover) hovered = m;
            drawCard(ctx, m, x, y, hover);
            final Module mod = m;
            if (interactive) hitClip(x, y, CARD_W, CARD_H, GTOP, GBOTTOM, b -> {
                if (b == 0) mod.toggle();
                else if (b == 1) go(() -> { settingsOf = mod; settingsT = 0; expanded = null; }, 1);
                else if (b == 2) binding = mod;
            });
        }
        ctx.disableScissor();

        // полоса прокрутки
        if (maxScroll > 0) {
            float track = GBOTTOM - GTOP;
            float th = Math.max(20, track * track / (content + 8));
            float ty = GTOP + (track - th) * Math.max(0, Math.min(1, sc / maxScroll));
            Render2DUtils.drawRoundedRect(ctx, W - 8, ty, 3, th, 1.5f, ThemeManager.withAlpha(ThemeManager.TEXT, 60));
        }

        if (!interactive) return;
        String hint = hovered != null ? hovered.description() : "";
        if (binding != null) hint = "Нажми клавишу для «" + binding.name() + "»  ·  Esc/Del — сбросить";
        if (hint != null && !hint.isEmpty())
            text(ctx, trim(hint, (int) GW), GX, H - 18, binding != null ? ThemeManager.GOLD : ThemeManager.TEXT_DIM);
    }

    private void drawCard(DrawContext ctx, Module m, float x, float y, boolean hover) {
        float t = sm("card", m, hover ? 1f : 0f, 16f);
        float on = sm("on", m, m.enabled() ? 1f : 0f, 14f);
        float scale = 1f + 0.02f * t; // ТЗ: Scale 1.02
        float cx = x + CARD_W / 2f, cy = y + CARD_H / 2f;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(cx, cy, 0);
        ctx.getMatrices().scale(scale, scale, 1f);
        ctx.getMatrices().translate(-cx, -cy, 0);

        float r = ThemeManager.RADIUS_BUTTON;
        if (t > 0.01f) Render2DUtils.drawGlow(ctx, x, y, CARD_W, CARD_H, r, 4f, ThemeManager.withAlpha(ThemeManager.CYAN, (int) (55 * t)));
        int base = ThemeManager.lerp(ThemeManager.CARD, ThemeManager.CARD_ON, on);
        Render2DUtils.drawRoundedGradient(ctx, x, y, CARD_W, CARD_H, r, ThemeManager.lerp(base, ThemeManager.CARD_HOVER, t), base);
        // тонкая неоновая линия при наведении
        Render2DUtils.drawRoundedOutline(ctx, x, y, CARD_W, CARD_H, r, 1f, ThemeManager.lerp(ThemeManager.STROKE, ThemeManager.CYAN, t));
        if (on > 0.01f) Render2DUtils.drawRoundedRect(ctx, x + 1, y + CARD_H - 2.5f, (CARD_W - 2) * on, 1.5f, 0.75f, ThemeManager.withAlpha(ThemeManager.GOLD, (int) (150 * on)));

        text(ctx, trim(m.name(), 60), x + 8, y + 8, ThemeManager.TEXT);
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
    private float rowHeight(Setting<?> st) {
        if (st instanceof ColorSetting c) return ROW + PICK_H * sm("exp", c, expanded == c ? 1f : 0f, 14f);
        return ROW;
    }

    private void drawSettings(DrawContext ctx, Module m, float sc, float mx, float my, boolean interactive) {
        drawHeader(ctx, m.name(), m.description());

        // назад
        float bh = sm("back", m, interactive && Render.hovered(mx, my, GX, GTOP, 62, 18) ? 1f : 0f, 16f);
        Render2DUtils.drawRoundedRect(ctx, GX, GTOP, 62, 18, ThemeManager.RADIUS_BUTTON, ThemeManager.lerp(ThemeManager.CARD, ThemeManager.CARD_HOVER, bh));
        Render2DUtils.drawRoundedOutline(ctx, GX, GTOP, 62, 18, ThemeManager.RADIUS_BUTTON, 1, ThemeManager.lerp(ThemeManager.STROKE, ThemeManager.CYAN, bh));
        text(ctx, "‹ Назад", GX + 12 - 2 * bh, GTOP + 5, ThemeManager.TEXT);
        hit(GX, GTOP, 62, 18, b -> go(() -> { settingsOf = null; expanded = null; }, -1));

        // главный тумблер модуля
        float on = sm("on", m, m.enabled() ? 1f : 0f, 14f);
        String st0 = m.enabled() ? "Включён" : "Выключен";
        text(ctx, st0, GX + GW - 34 - Render.width(st0), GTOP + 5, ThemeManager.lerp(ThemeManager.TEXT_DIM, ThemeManager.GOLD, on));
        Render2DUtils.drawToggle(ctx, GX + GW - 26, GTOP + 3, 24, 12, on);
        hit(GX + GW - 100, GTOP, 100, 18, b -> m.toggle());

        List<Setting<?>> list = m.settings().stream().filter(Setting::visible).toList();
        float top = GTOP + 26;
        float content = ROW;
        for (Setting<?> st : list) content += rowHeight(st);
        float maxScroll = Math.max(0, content - (GBOTTOM - top));
        if (interactive) settingsT = Math.max(0, Math.min(maxScroll, settingsT));

        ctx.enableScissor((int) GX - 4, (int) top - 2, (int) (GX + GW + 4), (int) GBOTTOM + 2);
        float y = top - sc;
        for (Setting<?> st : list) {
            float rh = rowHeight(st);
            if (y + rh >= top - 2 && y <= GBOTTOM + 2) drawSettingRow(ctx, st, y, top, mx, my, interactive);
            y += rh;
        }
        // строка бинда
        if (y + ROW >= top && y <= GBOTTOM) {
            row(ctx, "bind", m, y, mx, my, interactive);
            text(ctx, "Клавиша", GX + 10, y + 6, ThemeManager.TEXT);
            String k = binding == m ? "Нажми клавишу..." : (m.key() >= 0 ? keyName(m.key()) : "Нет");
            float kw = Render.width(k) + 12;
            Render2DUtils.drawRoundedRect(ctx, GX + GW - 10 - kw, y + 3, kw, 14, 5, ThemeManager.withAlpha(binding == m ? ThemeManager.GOLD : ThemeManager.ACCENT, 30));
            text(ctx, k, GX + GW - 4 - kw, y + 6, binding == m ? ThemeManager.GOLD : ThemeManager.ACCENT);
            hitClip(GX, y, GW, ROW - 4, top, GBOTTOM, b -> { if (b == 1) m.setKey(-1); else binding = m; });
        }
        ctx.disableScissor();

        if (maxScroll > 0) {
            float track = GBOTTOM - top;
            float th = Math.max(20, track * track / (content + 6));
            float ty = top + (track - th) * Math.max(0, Math.min(1, sc / maxScroll));
            Render2DUtils.drawRoundedRect(ctx, W - 8, ty, 3, th, 1.5f, ThemeManager.withAlpha(ThemeManager.TEXT, 60));
        }
        if (interactive) text(ctx, "ЛКМ — изменить  ·  ПКМ — сброс / назад  ·  ПКМ на цвете — RGB", GX, H - 18, ThemeManager.TEXT_DIM, 0.75f);
    }

    private void row(DrawContext ctx, String key, Object o, float y, float mx, float my, boolean interactive) {
        boolean h = interactive && my >= GTOP + 26 && my <= GBOTTOM && Render.hovered(mx, my, GX, y, GW, ROW - 4);
        float t = sm("row" + key, o, h ? 1f : 0f, 16f);
        Render2DUtils.drawRoundedRect(ctx, GX, y, GW, ROW - 4, ThemeManager.RADIUS_BUTTON, ThemeManager.lerp(ThemeManager.CARD, ThemeManager.CARD_HOVER, t));
        if (t > 0.01f) Render2DUtils.drawRoundedOutline(ctx, GX, y, GW, ROW - 4, ThemeManager.RADIUS_BUTTON, 1, ThemeManager.withAlpha(ThemeManager.CYAN, (int) (90 * t)));
    }

    private void drawSettingRow(DrawContext ctx, Setting<?> st, float y, float top, float mx, float my, boolean interactive) {
        row(ctx, "s", st, y, mx, my, interactive);
        text(ctx, st.name(), GX + 10, y + 6, ThemeManager.TEXT);
        float rh = ROW - 4;

        if (st instanceof BoolSetting b) {
            float t = sm("bool", st, b.get() ? 1f : 0f, 14f);
            Render2DUtils.drawToggle(ctx, GX + GW - 32, y + 5, 22, 10, t);
            hitClip(GX, y, GW, rh, top, GBOTTOM, k -> b.toggle());

        } else if (st instanceof NumberSetting n) {
            float sx = GX + 200, sw = GW - 200 - 60;
            float p = sm("num", st, (float) n.progress(), 20f);
            boolean hv = interactive && Render.hovered(mx, my, sx - 4, y, sw + 8, rh);
            float kh = sm("numk", st, hv || drag != null ? 1f : 0f, 16f);
            Render2DUtils.drawRoundedRect(ctx, sx, y + 8, sw, 4, 2, ThemeManager.withAlpha(ThemeManager.TEXT, 34));
            Render2DUtils.drawRoundedGradientH(ctx, sx, y + 8, Math.max(4, sw * p), 4, 2, ThemeManager.withAlpha(ThemeManager.ACCENT, 170), ThemeManager.ACCENT);
            Render2DUtils.drawGlow(ctx, sx + sw * p - 4, y + 6, 8, 8, 4, 3 + 2 * kh, ThemeManager.withAlpha(ThemeManager.ACCENT, (int) (80 + 60 * kh)));
            Render2DUtils.drawCircle(ctx, sx + sw * p, y + 10, 4 + kh, ThemeManager.TEXT);
            String v = n.step >= 1 ? String.valueOf(n.asInt()) : String.format(Locale.ROOT, "%.2f", n.get());
            text(ctx, v, GX + GW - 10 - Render.width(v), y + 6, ThemeManager.ACCENT);
            hitClip(GX, y, GW, rh, top, GBOTTOM, k -> {
                if (k == 1) { n.reset(); return; }
                dragX0 = sx + hitDx; dragW = sw;
                drag = v2 -> n.set(n.min + v2 * (n.max - n.min));
            });

        } else if (st instanceof ModeSetting md) {
            float total = 0;
            for (String o : md.modes) total += Render.width(o) + 12 + 2;
            if (total <= 236 && md.modes.size() > 1) {
                // сегментный переключатель с плавно едущей подсветкой
                float x0 = GX + GW - 8 - total;
                Render2DUtils.drawRoundedRect(ctx, x0 - 2, y + 2, total + 2, 16, 6, ThemeManager.rgba(0, 0, 0, 60));
                float cx = x0, selX = x0, selW = 0;
                for (String o : md.modes) {
                    float w = Render.width(o) + 12;
                    if (md.get().equals(o)) { selX = cx; selW = w; }
                    cx += w + 2;
                }
                float ax = sm("segx", st, selX, 18f), aw = sm("segw", st, selW, 18f);
                Render2DUtils.drawRoundedRect(ctx, ax, y + 3, aw, 14, 5, ThemeManager.withAlpha(ThemeManager.ACCENT, 60));
                Render2DUtils.drawRoundedOutline(ctx, ax, y + 3, aw, 14, 5, 1, ThemeManager.withAlpha(ThemeManager.ACCENT, 120));
                cx = x0;
                for (String o : md.modes) {
                    float w = Render.width(o) + 12;
                    boolean sel = md.get().equals(o);
                    text(ctx, o, cx + 6, y + 6, sel ? ThemeManager.TEXT : ThemeManager.TEXT_DIM);
                    final String opt = o;
                    hitClip(cx, y, w, rh, top, GBOTTOM, k -> md.set(opt));
                    cx += w + 2;
                }
                hitClip(GX, y, GW - total - 12, rh, top, GBOTTOM, k -> md.cycle(k == 1 ? -1 : 1));
            } else {
                String v = "‹  " + md.get() + "  ›";
                float vw = Render.width(v) + 12;
                Render2DUtils.drawRoundedRect(ctx, GX + GW - 10 - vw, y + 3, vw, 14, 5, ThemeManager.withAlpha(ThemeManager.ACCENT, 30));
                text(ctx, v, GX + GW - 4 - vw, y + 6, ThemeManager.ACCENT);
                hitClip(GX, y, GW, rh, top, GBOTTOM, k -> md.cycle(k == 1 ? -1 : 1));
            }

        } else if (st instanceof ColorSetting col) {
            float sx = GX + GW - 34;
            float e = sm("exp", col, expanded == col ? 1f : 0f, 14f);
            Render2DUtils.drawGlow(ctx, sx, y + 4, 24, 12, 4, 3, ThemeManager.withAlpha(col.color(), 90));
            Render2DUtils.drawRoundedRect(ctx, sx, y + 4, 24, 12, 4, col.color() | 0xFF000000);
            Render2DUtils.drawRoundedOutline(ctx, sx, y + 4, 24, 12, 4, 1, ThemeManager.STROKE);
            String arrow = e > 0.5f ? "▴" : "▾";
            text(ctx, arrow, sx - 12, y + 6, ThemeManager.TEXT_DIM);
            if (col.rainbow()) text(ctx, "RGB", sx - 18 - Render.width("RGB"), y + 6, ThemeManager.GOLD);
            hitClip(GX, y, GW, rh, top, GBOTTOM, k -> {
                if (k == 1) col.setRainbow(!col.rainbow());
                else expanded = expanded == col ? null : col;
            });
            if (e > 0.02f) drawPicker(ctx, col, y + ROW - 2, e, top, mx, my);
        }
    }

    /** Пипетка: оттенок / насыщенность / яркость + быстрые пресеты. */
    private void drawPicker(DrawContext ctx, ColorSetting col, float py, float e, float top, float mx, float my) {
        float ph = (PICK_H - 4) * e;
        float clipTop = Math.max(py, top - 2);
        float clipBottom = Math.min(py + ph, GBOTTOM + 2);
        if (clipBottom <= clipTop) return;
        ctx.enableScissor((int) GX - 4, (int) Math.floor(clipTop), (int) (GX + GW + 4), (int) Math.ceil(clipBottom));

        float[] hsv = hsvCache.computeIfAbsent(col, c -> ThemeManager.toHsv(c.get()));
        if (drag == null && (ThemeManager.hsv(hsv[0], hsv[1], hsv[2], 255) & 0xFFFFFF) != (col.get() & 0xFFFFFF)) {
            float[] fresh = ThemeManager.toHsv(col.get());
            hsv[0] = fresh[1] > 0.01f && fresh[2] > 0.01f ? fresh[0] : hsv[0];
            hsv[1] = fresh[1];
            hsv[2] = fresh[2];
        }

        Render2DUtils.drawRoundedRect(ctx, GX, py, GW, PICK_H - 4, ThemeManager.RADIUS_BUTTON, ThemeManager.rgba(0, 0, 0, 50));
        Render2DUtils.drawRoundedOutline(ctx, GX, py, GW, PICK_H - 4, ThemeManager.RADIUS_BUTTON, 1, ThemeManager.rgba(255, 255, 255, 14));

        float bx = GX + 60, bw = GW - 60 - 70, bh = 6;
        String[] labels = {"Оттенок", "Насыщ.", "Яркость"};
        float[] ys = {py + 8, py + 22, py + 36};
        for (int i = 0; i < 3; i++) text(ctx, labels[i], GX + 10, ys[i] - 1, ThemeManager.TEXT_DIM, 0.75f);

        Render2DUtils.drawHueBar(ctx, bx, ys[0], bw, bh);
        Render2DUtils.drawRoundedGradientH(ctx, bx, ys[1], bw, bh, 3, ThemeManager.hsv(hsv[0], 0, hsv[2], 255), ThemeManager.hsv(hsv[0], 1, hsv[2], 255));
        Render2DUtils.drawRoundedGradientH(ctx, bx, ys[2], bw, bh, 3, 0xFF000000, ThemeManager.hsv(hsv[0], hsv[1], 1, 255));
        for (int i = 0; i < 3; i++) {
            float kx = bx + bw * hsv[i];
            Render2DUtils.drawCircle(ctx, kx, ys[i] + bh / 2f, 4.5f, ThemeManager.rgba(0, 0, 0, 90));
            Render2DUtils.drawCircle(ctx, kx, ys[i] + bh / 2f, 3.5f, ThemeManager.TEXT);
            final int idx = i;
            hitClip(bx - 4, ys[i] - 4, bw + 8, bh + 8, clipTop, clipBottom, k -> {
                dragX0 = bx + hitDx; dragW = bw;
                drag = v -> {
                    hsv[idx] = (float) v;
                    col.setRainbow(false);
                    col.set(ThemeManager.hsv(hsv[0], hsv[1], hsv[2], 255));
                };
            });
        }

        // превью + пресеты
        float pvx = GX + GW - 60;
        Render2DUtils.drawRoundedRect(ctx, pvx, py + 6, 50, 22, 5, col.color() | 0xFF000000);
        Render2DUtils.drawRoundedOutline(ctx, pvx, py + 6, 50, 22, 5, 1, ThemeManager.STROKE);
        String hex = String.format(Locale.ROOT, "#%06X", col.get() & 0xFFFFFF);
        text(ctx, hex, pvx + (50 - Render.width(hex) * 0.75f) / 2f, py + 32, ThemeManager.TEXT_DIM, 0.75f);
        float sx = bx;
        for (int c : PRESETS) {
            Render2DUtils.drawRoundedRect(ctx, sx, py + 48, 12, 8, 3, c);
            if ((col.get() | 0xFF000000) == c) Render2DUtils.drawRoundedOutline(ctx, sx - 1, py + 47, 14, 10, 3.5f, 1, ThemeManager.TEXT);
            final int preset = c;
            hitClip(sx, py + 48, 12, 8, clipTop, clipBottom, k -> { col.setRainbow(false); col.set(preset); hsvCache.remove(col); });
            sx += 16;
        }
        ctx.disableScissor();
    }

    // ===================== input =====================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (closing) return true;
        float mx = lx(mouseX), my = ly(mouseY);
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (Render.hovered(mx, my, h.x, h.y, h.w, h.h)) { h.action.accept(button); return true; }
        }
        if (button == 1 && settingsOf != null) { go(() -> { settingsOf = null; expanded = null; }, -1); return true; }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        drag = null;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        if (settingsOf != null) settingsT -= (float) v * 26; else scrollT -= (float) v * 26;
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (closing) return true;
        if (binding != null) {
            binding.setKey(key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE ? -1 : key);
            binding = null;
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
            if (search.length() == 1) go(() -> { search = ""; scrollT = 0; }, -1);
            else search = search.substring(0, search.length() - 1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE && settingsOf != null) { go(() -> { settingsOf = null; expanded = null; }, -1); return true; }
        if (key == GLFW.GLFW_KEY_ESCAPE && !search.isEmpty()) { go(() -> { search = ""; scrollT = 0; }, -1); return true; }
        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT || key == GLFW.GLFW_KEY_INSERT) { close(); return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(char chr, int mods) {
        if (binding == null && settingsOf == null && (Character.isLetterOrDigit(chr) || chr == ' ') && search.length() < 24) {
            if (search.isEmpty()) go(() -> { search += chr; scrollT = 0; }, 1);
            else { search += chr; scrollT = 0; }
            return true;
        }
        return super.charTyped(chr, mods);
    }
}
