package dev.nova.gui;

import dev.nova.NovaClient;
import dev.nova.module.Category;
import dev.nova.module.Module;
import dev.nova.render.Anim;
import dev.nova.render.Render;
import dev.nova.render.Theme;
import dev.nova.setting.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.*;

/**
 * ClickGUI: панели по категориям, перетаскиваются за заголовок.
 * ЛКМ по модулю — вкл/выкл, ПКМ — настройки, СКМ — бинд. Печать с клавиатуры = поиск.
 */
public final class ClickGuiScreen extends Screen {
    private static final int W = 110, HEADER = 16, ROW = 14;
    private static final Map<Category, float[]> POS = new EnumMap<>(Category.class); // позиции живут между открытиями
    private static final Set<Module> EXPANDED = new HashSet<>();

    private final Anim open = new Anim(5f).target(1f);
    private Category draggingPanel;
    private float dragOffX, dragOffY;
    private NumberSetting draggingSlider;
    private float sliderX;
    private Module binding;
    private String search = "";

    public ClickGuiScreen() { super(Text.literal("Nova")); }

    @Override
    protected void init() {
        for (Category c : Category.values()) POS.computeIfAbsent(c, k -> new float[]{10 + (W + 8) * POS.size(), 20});
    }

    private List<Module> visibleModules(Category c) {
        List<Module> list = NovaClient.modules().byCategory(c);
        if (search.isEmpty()) return list;
        String q = search.toLowerCase(Locale.ROOT);
        return list.stream().filter(m -> m.name().toLowerCase(Locale.ROOT).contains(q)).toList();
    }

    // ---------- render ----------
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        float a = open.get();
        ctx.fill(0, 0, width, height, Render.alpha(0x90000000, a));
        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, (1 - a) * 20, 0);

        if (draggingSlider != null) {
            double t = (mx - sliderX) / (W - 8.0);
            draggingSlider.set(draggingSlider.min + Math.max(0, Math.min(1, t)) * (draggingSlider.max - draggingSlider.min));
        }
        for (Category c : Category.values()) {
            List<Module> mods = visibleModules(c);
            if (mods.isEmpty()) continue;
            drawPanel(ctx, c, mods, mx, my);
        }
        ctx.getMatrices().pop();

        String bottom = search.isEmpty() ? "Печатай для поиска  •  ПКМ — настройки  •  СКМ — бинд" : "Поиск: " + search + "_";
        Render.text(ctx, bottom, (width - Render.width(bottom)) / 2f, height - 14, search.isEmpty() ? Theme.TEXT_DIM : Theme.TEXT, true);
        super.render(ctx, mx, my, delta);
    }

    private void drawPanel(DrawContext ctx, Category c, List<Module> mods, int mx, int my) {
        float[] p = POS.get(c);
        float x = p[0], y = p[1];
        float h = HEADER;
        for (Module m : mods) h += ROW + (EXPANDED.contains(m) ? settingsHeight(m) : 0);

        Render.rounded(ctx, x, y, W, h + 3, 3, Theme.PANEL);
        Render.gradientH(ctx, (int) x, (int) y, W, 2, Theme.accent(0), Theme.accent(600));
        Render.text(ctx, c.title, x + 6, y + 5, Theme.TEXT, true);
        String cnt = String.valueOf(mods.size());
        Render.text(ctx, cnt, x + W - 6 - Render.width(cnt), y + 5, Theme.TEXT_DIM, false);

        float cy = y + HEADER;
        for (Module m : mods) {
            boolean hover = Render.hovered(mx, my, x, cy, W, ROW);
            Render.rect(ctx, x + 2, cy, W - 4, ROW - 1, hover ? Theme.ITEM_HOVER : Theme.ITEM);
            if (m.enabled()) Render.rect(ctx, x + 2, cy, 2, ROW - 1, Theme.accent((long) (cy * 10)));
            String label = binding == m ? "Нажми клавишу..." : m.name();
            Render.text(ctx, label, x + 7, cy + 3, m.enabled() ? Theme.TEXT : Theme.TEXT_DIM, false);
            if (m.key() >= 0 && binding != m) {
                String k = keyName(m.key());
                Render.text(ctx, k, x + W - 6 - Render.width(k), cy + 3, Theme.TEXT_DIM, false);
            } else if (!m.settings().isEmpty()) {
                Render.text(ctx, EXPANDED.contains(m) ? "-" : "+", x + W - 10, cy + 3, Theme.TEXT_DIM, false);
            }
            cy += ROW;
            if (EXPANDED.contains(m)) cy = drawSettings(ctx, m, x, cy, mx, my);
            if (hover && binding == null) drawTooltip(ctx, m.description(), mx, my);
        }
    }

    private float drawSettings(DrawContext ctx, Module m, float x, float y, int mx, int my) {
        for (Setting<?> s : m.settings()) {
            if (!s.visible()) continue;
            Render.rect(ctx, x + 2, y, W - 4, ROW - 1, 0xFF1A1A22);
            if (s instanceof BoolSetting b) {
                Render.text(ctx, s.name(), x + 9, y + 3, Theme.TEXT_DIM, false);
                float bx = x + W - 20;
                Render.rounded(ctx, bx, y + 3, 14, 7, 3, b.get() ? Theme.accent() : 0xFF3A3A46);
                Render.rect(ctx, b.get() ? bx + 9 : bx + 1, y + 4, 4, 5, 0xFFFFFFFF);
            } else if (s instanceof NumberSetting n) {
                Render.rect(ctx, x + 4, y + ROW - 3, (float) ((W - 8) * n.progress()), 2, Theme.accent());
                String v = n.step >= 1 ? String.valueOf(n.asInt()) : String.format(Locale.ROOT, "%.2f", n.get());
                Render.text(ctx, s.name(), x + 9, y + 2, Theme.TEXT_DIM, false);
                Render.text(ctx, v, x + W - 6 - Render.width(v), y + 2, Theme.TEXT, false);
            } else if (s instanceof ModeSetting md) {
                Render.text(ctx, s.name(), x + 9, y + 3, Theme.TEXT_DIM, false);
                Render.text(ctx, md.get(), x + W - 6 - Render.width(md.get()), y + 3, Theme.accent(), false);
            } else if (s instanceof ColorSetting col) {
                Render.text(ctx, s.name() + (col.rainbow() ? " (RGB)" : ""), x + 9, y + 3, Theme.TEXT_DIM, false);
                Render.rect(ctx, x + W - 16, y + 3, 10, 7, col.color() | 0xFF000000);
            }
            y += ROW;
        }
        return y;
    }

    private void drawTooltip(DrawContext ctx, String text, int mx, int my) {
        if (text == null || text.isEmpty()) return;
        int w = Render.width(text) + 8;
        float tx = Math.min(mx + 8, width - w - 2);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, 0, 400);
        Render.rounded(ctx, tx, my - 14, w, 12, 2, 0xF0000000);
        Render.text(ctx, text, tx + 4, my - 12, Theme.TEXT, false);
        ctx.getMatrices().pop();
    }

    private int settingsHeight(Module m) {
        return (int) m.settings().stream().filter(Setting::visible).count() * ROW;
    }

    private static String keyName(int key) {
        return InputUtil.Type.KEYSYM.createFromCode(key).getLocalizedText().getString().toUpperCase(Locale.ROOT);
    }

    // ---------- input ----------
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (Category c : Category.values()) {
            List<Module> mods = visibleModules(c);
            if (mods.isEmpty()) continue;
            float[] p = POS.get(c);
            float x = p[0], y = p[1];
            if (Render.hovered(mx, my, x, y, W, HEADER) && button == 0) {
                draggingPanel = c; dragOffX = (float) mx - x; dragOffY = (float) my - y;
                return true;
            }
            float cy = y + HEADER;
            for (Module m : mods) {
                if (Render.hovered(mx, my, x, cy, W, ROW)) {
                    if (button == 0) m.toggle();
                    else if (button == 1 && !m.settings().isEmpty()) { if (!EXPANDED.remove(m)) EXPANDED.add(m); }
                    else if (button == 2) binding = m;
                    return true;
                }
                cy += ROW;
                if (EXPANDED.contains(m)) {
                    for (Setting<?> s : m.settings()) {
                        if (!s.visible()) continue;
                        if (Render.hovered(mx, my, x, cy, W, ROW)) { clickSetting(s, button, x); return true; }
                        cy += ROW;
                    }
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private void clickSetting(Setting<?> s, int button, float panelX) {
        if (s instanceof BoolSetting b) b.toggle();
        else if (s instanceof ModeSetting m) m.cycle(button == 1 ? -1 : 1);
        else if (s instanceof NumberSetting n) { if (button == 0) { draggingSlider = n; sliderX = panelX + 4; } else n.reset(); }
        else if (s instanceof ColorSetting c) {
            if (button == 1) c.setRainbow(!c.rainbow());
            else c.set(nextPreset(c.get()));
        }
    }

    private static final int[] PRESETS = {0xFF8B5CF6, 0xFF3B82F6, 0xFF22D3EE, 0xFF22C55E, 0xFFFACC15, 0xFFF97316, 0xFFEF4444, 0xFFEC4899, 0xFFFFFFFF};
    private static int nextPreset(int cur) {
        for (int i = 0; i < PRESETS.length; i++) if (PRESETS[i] == cur) return PRESETS[(i + 1) % PRESETS.length];
        return PRESETS[0];
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggingPanel != null) {
            float[] p = POS.get(draggingPanel);
            p[0] = Math.max(0, Math.min(width - W, (float) mx - dragOffX));
            p[1] = Math.max(0, Math.min(height - HEADER, (float) my - dragOffY));
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        draggingPanel = null; draggingSlider = null;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        for (float[] p : POS.values()) p[1] += (float) v * 12;
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
        if (key == GLFW.GLFW_KEY_ESCAPE && !search.isEmpty()) { search = ""; return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(char chr, int mods) {
        if (binding == null && (Character.isLetterOrDigit(chr) || chr == ' ') && search.length() < 24) { search += chr; return true; }
        return super.charTyped(chr, mods);
    }

    @Override public void close() { NovaClient.config().save(); super.close(); }
    @Override public boolean shouldPause() { return false; }
}
