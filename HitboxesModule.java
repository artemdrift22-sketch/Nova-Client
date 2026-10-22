package dev.nova.modules.world;

import dev.nova.module.Category;
import dev.nova.module.Module;
import dev.nova.render.Render3DUtils;
import dev.nova.render.ThemeManager;
import dev.nova.setting.BoolSetting;
import dev.nova.setting.NumberSetting;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

/** Стильные хитбоксы Nova (золото — игроки, голубой — мобы). Сквозь стены НЕ видно. */
public final class HitboxesModule extends Module {
    private final NumberSetting range = number("Дистанция", 32, 4, 64, 1);
    private final BoolSetting players = bool("Игроки", true);
    private final BoolSetting mobs = bool("Мобы", true);
    private final BoolSetting items = bool("Предметы", false);

    public HitboxesModule() { super("Hitboxes", "Контуры сущностей в стиле Nova", Category.WORLD); }

    @Override
    public void onRender3D(WorldRenderContext ctx) {
        if (!inGame()) return;
        double r2 = range.get() * range.get();
        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player && mc.options.getPerspective().isFirstPerson()) continue;
            if (e.squaredDistanceTo(mc.player) > r2) continue;
            int color;
            if (e instanceof PlayerEntity) { if (!players.get()) continue; color = ThemeManager.withAlpha(ThemeManager.GOLD, 230); }
            else if (e instanceof LivingEntity) { if (!mobs.get()) continue; color = ThemeManager.withAlpha(ThemeManager.CYAN, 210); }
            else if (e instanceof ItemEntity) { if (!items.get()) continue; color = ThemeManager.withAlpha(ThemeManager.TEXT, 150); }
            else continue;
            Render3DUtils.drawEntityBox(ctx, e, color);
        }
    }
}
