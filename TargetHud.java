package dev.nova.modules.pvp;

import dev.nova.hud.HudModule;
import dev.nova.module.Category;
import dev.nova.render.AnimationUtils.Smooth;
import dev.nova.render.Render;
import dev.nova.render.Render2DUtils;
import dev.nova.render.ThemeManager;
import dev.nova.setting.BoolSetting;
import dev.nova.util.CombatTracker;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

import java.util.Locale;

/**
 * Target HUD в стиле Pulse: голова, ник, плавная полоса ХП (градиент + след урона), абсорбция,
 * дистанция, броня. Появляется/исчезает плавно, последняя цель держится на время fade-out.
 */
public final class TargetHud extends HudModule {
    private static final int W = 150, H = 46;
    private final BoolSetting armor = bool("Броня", true);
    private final BoolSetting distance = bool("Дистанция", true);
    private final BoolSetting winning = bool("Кто побеждает", true);

    private final Smooth hp = new Smooth(10f), trail = new Smooth(3f), absorb = new Smooth(10f);
    private LivingEntity last;

    public TargetHud() { super("TargetHUD", "Информация о текущей цели", Category.PVP, 0.55f, 0.55f); }

    @Override public float width() { return W; }
    @Override public float height() { return H; }

    @Override protected void update(boolean preview) {
        LivingEntity t = CombatTracker.target;
        if (t == null && preview && mc.player != null) t = mc.player;
        if (t != null && t != last) {
            float f = t.getHealth() / Math.max(1, t.getMaxHealth());
            hp.set(f); trail.set(f); absorb.set(0);
            last = t;
        }
    }

    @Override protected boolean shouldShow(boolean preview) { return CombatTracker.target != null; }

    @Override protected void draw(DrawContext ctx, float delta, boolean preview) {
        LivingEntity t = last;
        if (t == null) return;

        // голова
        Render2DUtils.drawRoundedRect(ctx, PAD - 1, PAD - 1, 36, 36, 5, ThemeManager.rgba(0, 0, 0, 80));
        if (t instanceof AbstractClientPlayerEntity p) PlayerSkinDrawer.draw(ctx, p.getSkinTextures(), (int) PAD, (int) PAD, 34);
        else {
            Render2DUtils.drawRoundedRect(ctx, PAD, PAD, 34, 34, 4, ThemeManager.withAlpha(ThemeManager.ACCENT, 40));
            String n = t.getDisplayName().getString();
            String l = n.isEmpty() ? "?" : n.substring(0, 1);
            Render.text(ctx, l, PAD + 17 - Render.width(l) / 2f, PAD + 13, ThemeManager.TEXT, true);
        }
        if (t.hurtTime > 0) Render2DUtils.drawRoundedRect(ctx, PAD, PAD, 34, 34, 4, ThemeManager.withAlpha(ThemeManager.RED, t.hurtTime * 12));

        float tx = PAD + 40, bw = W - tx - PAD;
        Render.text(ctx, Render.width(t.getDisplayName().getString()) > bw ? t.getDisplayName().getString().substring(0, Math.min(14, t.getDisplayName().getString().length())) : t.getDisplayName().getString(), tx, PAD, ThemeManager.TEXT, false);

        float max = Math.max(1, t.getMaxHealth());
        float f = Math.max(0, Math.min(1, t.getHealth() / max));
        float sh = hp.update(f), tr = trail.update(f);
        float ab = absorb.update(Math.min(1, t.getAbsorptionAmount() / max));
        float by = PAD + 12;
        Render2DUtils.drawRoundedRect(ctx, tx, by, bw, 6, 3, ThemeManager.rgba(0, 0, 0, 110));
        if (tr > sh) Render2DUtils.drawRoundedRect(ctx, tx, by, Math.max(6, bw * tr), 6, 3, ThemeManager.withAlpha(ThemeManager.TEXT, 70));
        int hpColor = ThemeManager.lerp(ThemeManager.RED, ThemeManager.GREEN, sh);
        Render2DUtils.drawGlow(ctx, tx, by, Math.max(6, bw * sh), 6, 3, 3, ThemeManager.withAlpha(hpColor, 70));
        Render2DUtils.drawRoundedGradientH(ctx, tx, by, Math.max(6, bw * sh), 6, 3, ThemeManager.withAlpha(hpColor, 200), hpColor);
        if (ab > 0.01f) Render2DUtils.drawRoundedRect(ctx, tx, by + 4, Math.max(3, bw * ab), 2, 1, ThemeManager.GOLD);

        String hpText = String.format(Locale.ROOT, "%.1f", t.getHealth() + t.getAbsorptionAmount());
        float hx = Render.text(ctx, hpText, tx, by + 9, ThemeManager.TEXT, false);
        Render.text(ctx, " ❤", hx, by + 9, ThemeManager.RED, false);
        if (distance.get() && mc.player != null && t != mc.player) {
            String dist = String.format(Locale.ROOT, "%.1fм", mc.player.distanceTo(t));
            Render.text(ctx, dist, W - PAD - Render.width(dist), by + 9, ThemeManager.TEXT_DIM, false);
        }
        if (winning.get() && mc.player != null && t != mc.player && !armor.get()) {
            boolean win = mc.player.getHealth() + mc.player.getAbsorptionAmount() >= t.getHealth() + t.getAbsorptionAmount();
            Render.text(ctx, win ? "Выигрываешь" : "Проигрываешь", tx, by + 20, win ? ThemeManager.GREEN : ThemeManager.RED, false);
        }

        if (armor.get()) {
            EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};
            float ax = tx, ay = by + 19;
            ctx.getMatrices().push();
            ctx.getMatrices().translate(ax, ay, 0);
            ctx.getMatrices().scale(0.6f, 0.6f, 1);
            for (int i = 0; i < slots.length; i++) {
                ItemStack st = t.getEquippedStack(slots[i]);
                if (!st.isEmpty()) ctx.drawItem(st, i * 18, 0);
            }
            ctx.getMatrices().pop();
            if (winning.get() && mc.player != null && t != mc.player) {
                boolean win = mc.player.getHealth() + mc.player.getAbsorptionAmount() >= t.getHealth() + t.getAbsorptionAmount();
                Render2DUtils.drawCircle(ctx, W - PAD - 3, ay + 5, 2.5f, win ? ThemeManager.GREEN : ThemeManager.RED);
            }
        }
    }
}
