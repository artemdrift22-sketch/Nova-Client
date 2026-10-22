package dev.nova.modules.hud;

import dev.nova.hud.HudModule;
import dev.nova.render.Render;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

/** Свой инвентарь (27 слотов без хотбара) на экране. */
public final class InventoryHud extends HudModule {
    public InventoryHud() { super("InventoryHUD", "Твой инвентарь на экране", 0.7f, 0.02f); }
    @Override public float width() { return 9 * 18 + 4; }
    @Override public float height() { return 3 * 18 + 4; }
    @Override protected void draw(DrawContext ctx, float delta, boolean preview) {
        for (int i = 0; i < 27; i++) {
            int x = 2 + (i % 9) * 18, y = 2 + (i / 9) * 18;
            Render.rect(ctx, x, y, 17, 17, 0x30FFFFFF);
            if (mc.player == null) continue;
            ItemStack st = mc.player.getInventory().getStack(9 + i);
            if (st.isEmpty()) continue;
            ctx.drawItem(st, x, y);
            ctx.drawStackOverlay(Render.font(), st, x, y);
        }
    }
}
