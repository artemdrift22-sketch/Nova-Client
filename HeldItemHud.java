package dev.nova.modules.hud;

import dev.nova.hud.HudModule;
import dev.nova.render.Render;
import dev.nova.render.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/** Предмет в руке: иконка, название, прочность, кол-во таких же в инвентаре. */
public final class HeldItemHud extends HudModule {
    public HeldItemHud() { super("HeldItem", "Предмет в руке и его прочность", 0.55f, 0.72f); }

    private ItemStack stack(boolean preview) {
        ItemStack st = mc.player == null ? ItemStack.EMPTY : mc.player.getMainHandStack();
        return st.isEmpty() && preview ? new ItemStack(Items.NETHERITE_PICKAXE) : st;
    }

    private String info(ItemStack st) {
        if (st.isDamageable()) return (st.getMaxDamage() - st.getDamage()) + " / " + st.getMaxDamage();
        if (mc.player == null) return "x" + st.getCount();
        return "x" + mc.player.getInventory().count(st.getItem()) + " всего";
    }

    @Override public float width() {
        ItemStack st = stack(true);
        return 24 + Math.max(Render.width(st.getName().getString()), Render.width(info(st))) + 4;
    }
    @Override public float height() { return 22; }

    @Override protected void draw(DrawContext ctx, float delta, boolean preview) {
        ItemStack st = stack(preview);
        if (st.isEmpty()) return;
        ctx.drawItem(st, 3, 3);
        Render.text(ctx, st.getName().getString(), 22, 2, Theme.TEXT, true);
        int col = Theme.TEXT_DIM;
        if (st.isDamageable()) {
            float f = (st.getMaxDamage() - st.getDamage()) / (float) st.getMaxDamage();
            col = f > 0.5f ? 0xFF22C55E : f > 0.2f ? 0xFFFACC15 : 0xFFEF4444;
        }
        Render.text(ctx, info(st), 22, 12, col, true);
    }
}
