package dev.nova.hud;

import dev.nova.NovaClient;
import dev.nova.render.Render;
import dev.nova.render.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Drag & drop редактор HUD: ЛКМ — тащить, колесо — масштаб, ПКМ — вкл/выкл, снап к краям и центру. */
public final class HudEditorScreen extends Screen {
    private static final int SNAP = 4;
    private HudModule dragging;
    private float offX, offY;
    private Float guideX, guideY;

    public HudEditorScreen() { super(Text.literal("Nova HUD Editor")); }

    private List<HudModule> elements() { return NovaClient.modules().hud(); }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0x60000000);
        if (guideX != null) ctx.fill(Math.round(guideX), 0, Math.round(guideX) + 1, height, Theme.accent());
        if (guideY != null) ctx.fill(0, Math.round(guideY), width, Math.round(guideY) + 1, Theme.accent());

        for (HudModule h : elements()) {
            float x = h.px(), y = h.py(), w = h.width() * h.scale, hh = h.height() * h.scale;
            if (h.enabled()) h.renderAt(ctx, delta, true);
            boolean hover = Render.hovered(mx, my, x, y, w, hh);
            int border = !h.enabled() ? 0x50FFFFFF : hover || h == dragging ? Theme.accent() : 0x90FFFFFF;
            Render.outline(ctx, x - 1, y - 1, w + 2, hh + 2, border);
            if (!h.enabled()) Render.text(ctx, h.name(), x + 2, y + 2, Theme.TEXT_DIM, false);
        }
        String hint = "ЛКМ — двигать  •  Колесо — размер  •  ПКМ — вкл/выкл  •  R — сброс  •  ESC — выход";
        Render.text(ctx, hint, (width - Render.width(hint)) / 2f, height - 14, Theme.TEXT_DIM, true);
        super.render(ctx, mx, my, delta);
    }

    private HudModule at(double mx, double my) {
        List<HudModule> list = elements();
        for (int i = list.size() - 1; i >= 0; i--) {
            HudModule h = list.get(i);
            if (Render.hovered(mx, my, h.px(), h.py(), h.width() * h.scale, h.height() * h.scale)) return h;
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        HudModule h = at(mx, my);
        if (h == null) return super.mouseClicked(mx, my, button);
        if (button == 0) { dragging = h; offX = (float) mx - h.px(); offY = (float) my - h.py(); }
        else if (button == 1) h.toggle();
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging == null) return super.mouseDragged(mx, my, button, dx, dy);
        float w = dragging.width() * dragging.scale, h = dragging.height() * dragging.scale;
        float nx = (float) mx - offX, ny = (float) my - offY;
        guideX = null; guideY = null;
        // снап к краям экрана и к центру
        if (Math.abs(nx) < SNAP) { nx = 0; }
        if (Math.abs(nx + w - width) < SNAP) { nx = width - w; }
        if (Math.abs(nx + w / 2 - width / 2f) < SNAP) { nx = width / 2f - w / 2; guideX = width / 2f; }
        if (Math.abs(ny) < SNAP) { ny = 0; }
        if (Math.abs(ny + h - height) < SNAP) { ny = height - h; }
        if (Math.abs(ny + h / 2 - height / 2f) < SNAP) { ny = height / 2f - h / 2; guideY = height / 2f; }
        dragging.setPixelPos(nx, ny);
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = null; guideX = null; guideY = null;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        HudModule el = at(mx, my);
        if (el == null) return false;
        el.scale = Math.max(0.5f, Math.min(2.5f, el.scale + (float) v * 0.1f));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_R) {
            HudModule el = at(client.mouse.getX() * width / client.getWindow().getWidth(),
                              client.mouse.getY() * height / client.getWindow().getHeight());
            if (el != null) el.scale = 1f;
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override public void close() { NovaClient.config().save(); super.close(); }
    @Override public boolean shouldPause() { return false; }
}
