package dev.nova.modules.hud;

import dev.nova.hud.HudModule;
import dev.nova.render.Render;
import dev.nova.render.Theme;
import net.minecraft.block.BlockState;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

import java.util.Locale;

/** Entity Info + Block Info: на что смотрит игрок (только то, что и так видно). */
public final class LookInfoHud extends HudModule {
    private String title = "", sub = "";
    private ItemStack icon = ItemStack.EMPTY;

    public LookInfoHud() { super("LookInfo", "Инфо о блоке/сущности под прицелом", 0.42f, 0.01f); }

    @Override public void onTick() {
        title = ""; sub = ""; icon = ItemStack.EMPTY;
        if (!inGame() || mc.crosshairTarget == null) return;
        HitResult hit = mc.crosshairTarget;
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockState st = mc.world.getBlockState(((BlockHitResult) hit).getBlockPos());
            if (st.isAir()) return;
            title = st.getBlock().getName().getString();
            sub = Registries.BLOCK.getId(st.getBlock()).toString();
            icon = new ItemStack(st.getBlock().asItem());
        } else if (hit.getType() == HitResult.Type.ENTITY) {
            Entity e = ((EntityHitResult) hit).getEntity();
            title = e.getDisplayName().getString();
            sub = e instanceof LivingEntity l
                ? String.format(Locale.ROOT, "❤ %.1f / %.0f", l.getHealth(), l.getMaxHealth())
                : Registries.ENTITY_TYPE.getId(e.getType()).toString();
        }
    }

    @Override public float width() { return Math.max(80, Math.max(Render.width(title), Render.width(sub)) + 26); }
    @Override public float height() { return 22; }
    @Override protected void draw(DrawContext ctx, float delta, boolean preview) {
        String t = title.isEmpty() && preview ? "Алмазная руда" : title;
        String s = sub.isEmpty() && preview ? "minecraft:diamond_ore" : sub;
        if (t.isEmpty()) return;
        if (!icon.isEmpty()) ctx.drawItem(icon, 3, 3);
        Render.text(ctx, t, 22, 2, Theme.TEXT, true);
        Render.text(ctx, s, 22, 12, Theme.TEXT_DIM, false);
    }
}
