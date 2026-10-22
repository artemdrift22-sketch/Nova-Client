package dev.nova.modules.hud;

import dev.nova.module.Category;
import dev.nova.module.Module;
import dev.nova.setting.BoolSetting;
import dev.nova.setting.NumberSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;

/** Пульсирующая красная рамка по краям экрана + звук, когда ХП ниже порога. */
public final class LowHealthWarning extends Module {
    private final NumberSetting threshold = number("Порог ХП", 6, 1, 15, 1);
    private final BoolSetting sound = bool("Звук", true);
    private boolean warned;

    public LowHealthWarning() { super("LowHPWarning", "Предупреждение о низком здоровье", Category.HUD); }

    private boolean low() { return inGame() && mc.player.getHealth() <= threshold.get() && mc.player.isAlive(); }

    @Override public void onTick() {
        if (low() && !warned && sound.get()) mc.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 1f, 0.6f);
        warned = low();
    }

    @Override public void onRender2D(DrawContext ctx, float delta) {
        if (!low()) return;
        float pulse = (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 180.0));
        int a = (int) (60 + 90 * pulse);
        int w = mc.getWindow().getScaledWidth(), h = mc.getWindow().getScaledHeight(), d = 28;
        int edge = (a << 24) | 0xDC2626, clear = 0x00DC2626;
        ctx.fillGradient(0, 0, w, d, edge, clear);
        ctx.fillGradient(0, h - d, w, h, clear, edge);
        for (int i = 0; i < d; i++) {
            int c = ((int) (a * (1 - i / (float) d)) << 24) | 0xDC2626;
            ctx.fill(i, 0, i + 1, h, c);
            ctx.fill(w - i - 1, 0, w - i, h, c);
        }
    }
}
