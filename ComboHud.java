package dev.nova.modules.pvp;

import dev.nova.hud.HudModule;
import dev.nova.module.Category;
import dev.nova.render.Render;
import dev.nova.render.Theme;
import dev.nova.util.CombatTracker;
import net.minecraft.client.gui.DrawContext;

public final class ComboHud extends HudModule {
    public ComboHud() { super("ComboCounter", "Серия ударов без получения урона", Category.PVP, 0.005f, 0.62f); }
    private String text() { return CombatTracker.combo + " комбо"; }
    @Override public float width() { return Render.width("99 комбо") + 8; }
    @Override public float height() { return Render.height() + 6; }
    @Override protected void draw(DrawContext ctx, float delta, boolean preview) {
        Render.text(ctx, text(), 4, 3, CombatTracker.combo >= 3 ? Theme.accent() : Theme.TEXT, true);
    }
}
