package dev.nova.util;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ActionResult;

/** Данные о бое для Target HUD / Combo / Reach. Только наблюдение, ничего не автоматизирует. */
public final class CombatTracker {
    private CombatTracker() {}

    public static LivingEntity target;
    public static long lastHitTime;
    public static double lastReach;
    public static int combo;
    private static int lastHurtTime;

    public static void init() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient && entity instanceof LivingEntity living && living.isAlive()) {
                long now = System.currentTimeMillis();
                combo = now - lastHitTime < 2000 && living == target ? combo + 1 : 1;
                target = living;
                lastHitTime = now;
                var mc = MinecraftClient.getInstance();
                lastReach = mc.crosshairTarget != null ? player.getEyePos().distanceTo(mc.crosshairTarget.getPos()) : player.distanceTo(entity);
            }
            return ActionResult.PASS;
        });
    }

    /** Вызывается каждый тик: сброс комбо при получении урона и таймаут цели. */
    public static void tick(MinecraftClient mc) {
        if (mc.player == null) { target = null; combo = 0; return; }
        int ht = mc.player.hurtTime;
        if (ht > lastHurtTime) combo = 0;
        lastHurtTime = ht;
        long since = System.currentTimeMillis() - lastHitTime;
        if (since > 2000) combo = 0;
        if (target != null && (since > 8000 || !target.isAlive() || target.isRemoved() || mc.player.distanceTo(target) > 16)) target = null;
    }
}
