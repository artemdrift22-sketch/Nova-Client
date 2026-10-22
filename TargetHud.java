package dev.nova.modules.pvp;

import dev.nova.hud.HudModule;
import dev.nova.module.Category;
import dev.nova.render.Render;
import dev.nova.render.Theme;
import dev.nova.util.CombatTracker;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

import java.util.Locale;

/** Target HUD: голова, ник, ХП с анимацией, дистанция, броня и «кто побеждает». */
public final class TargetHud extends HudModule {
    private static final int W = 140, H = 44;
    private float hpShown = 20;

    public TargetHud() { super("TargetHUD", "Информация о текущей цели", Category.PVP, 0.55f, 0.55f); }

    @Override public float width() { return W; }
    @Override public float height() { return H; }

    @Override public void renderAt(DrawContext ctx, float delta, boolean preview) {
        if (CombatTracker.target == null && !preview) return;
        super.renderAt(ctx, delta, preview);
    }

    @Override protected void draw(DrawContext ctx, float delta, boolean preview) {
        LivingEntity t = CombatTracker.target;
        if (t == null && mc.player != null && preview) t = mc.player;
        if (t == null) return;

        if (t instanceof AbstractClientPlayerEntity p) PlayerSkinDrawer.draw(ctx, p.getSkinTextures(), 4, 4, 32);
        else {
            Render.rounded(ctx, 4, 4, 32, 32, 3, 0x40FFFFFF);
            String l = t.getDisplayName().getString().substring(0, 1);
            Render.text(ctx, l, 20 - Render.width(l) / 2f, 16, Theme.TEXT, true);
        }
        if (t.hurtTime > 0) Render.rect(ctx, 4, 4, 32, 32, (t.hurtTime * 12 << 24) | 0xFF0000);

        Render.text(ctx, t.getDisplayName().getString(), 40, 5, Theme.TEXT, true);
        float hp = t.getHealth(), max = t.getMaxHealth();
        hpShown += (hp - hpShown) * 0.15f;
        float f = Math.max(0, Math.min(1, hpShown / max));
        Render.rounded(ctx, 40, 16, W - 44, 6, 2, 0x60000000);
        Render.gradientH(ctx, 41, 17, (int) ((W - 46) * f), 4, 0xFFEF4444, Theme.accent());

        String hpText = String.format(Locale.ROOT, "%.1f ❤", hp + t.getAbsorptionAmount());
        Render.text(ctx, hpText, 40, 25, Theme.TEXT, false);
        if (mc.player != null && t != mc.player) {
            String dist = String.format(Locale.ROOT, "%.1fм", mc.player.distanceTo(t));
            Render.text(ctx, dist, W - 4 - Render.width(dist), 25, Theme.TEXT_DIM, false);
            boolean winning = mc.player.getHealth() + mc.player.getAbsorptionAmount() >= hp + t.getAbsorptionAmount();
            String w = winning ? "Выигрываешь" : "Проигрываешь";
            Render.text(ctx, w, 40, 34, winning ? 0xFF22C55E : 0xFFEF4444, false);
        }
        // броня цели маленькими иконками
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND};
        ctx.getMatrices().push();
        ctx.getMatrices().translate(W - 52, 31, 0);
        ctx.getMatrices().scale(0.6f, 0.6f, 1);
        for (int i = 0; i < slots.length; i++) {
            ItemStack st = t.getEquippedStack(slots[i]);
            if (!st.isEmpty()) ctx.drawItem(st, i * 16, 0);
        }
        ctx.getMatrices().pop();
    }
}
