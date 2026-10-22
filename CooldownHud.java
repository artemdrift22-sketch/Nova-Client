package dev.nova.modules.pvp;

import dev.nova.hud.HudModule;
import dev.nova.module.Category;
import dev.nova.render.Render;
import dev.nova.render.Theme;
import dev.nova.setting.BoolSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/** Кулдаун атаки, натяжение лука, кулдауны щита/перла/гепла. */
public final class CooldownHud extends HudModule {
    @Override protected String header() { return "Cooldowns"; }

    private static final int W = 90;
    private final BoolSetting attack = bool("Атака", true);
    private final BoolSetting bow = bool("Лук", true);
    private final BoolSetting items = bool("Предметы", true);
    private static final ItemStack[] TRACKED = {new ItemStack(Items.SHIELD), new ItemStack(Items.ENDER_PEARL), new ItemStack(Items.CHORUS_FRUIT), new ItemStack(Items.WIND_CHARGE)};

    public CooldownHud() { super("Cooldowns", "Кулдаун атаки, лук, предметы", Category.PVP, 0.5f - 45f / 854f, 0.6f); background.set(false); }

    @Override public float width() { return W; }
    @Override public float height() { return 40; }

    private void bar(DrawContext c, float y, float f, int col) {
        Render.rounded(c, 0, y, W, 4, 1, 0x60000000);
        Render.rect(c, 0, y, W * Math.max(0, Math.min(1, f)), 4, col);
    }

    @Override protected void draw(DrawContext ctx, float delta, boolean preview) {
        if (mc.player == null) return;
        float y = 0;
        if (attack.get()) {
            float f = mc.player.getAttackCooldownProgress(delta);
            if (f < 1 || preview) bar(ctx, y, f, f >= 1 ? 0xFF22C55E : Theme.accent());
            y += 6;
        }
        if (bow.get() && mc.player.isUsingItem() && mc.player.getActiveItem().getItem() instanceof BowItem) {
            float f = BowItem.getPullProgress(mc.player.getItemUseTime());
            bar(ctx, y, f, f >= 1 ? 0xFFFACC15 : 0xFFF97316);
            y += 6;
        }
        if (items.get()) {
            int x = 0;
            for (ItemStack st : TRACKED) {
                float f = mc.player.getItemCooldownManager().getCooldownProgress(st, delta);
                if (f <= 0) continue;
                ctx.drawItem(st, x, (int) y + 2);
                Render.rect(ctx, x, y + 2 + 16 * (1 - f), 16, 16 * f, 0x90000000);
                x += 18;
            }
        }
    }
}
