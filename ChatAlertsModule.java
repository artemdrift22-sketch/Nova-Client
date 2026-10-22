package dev.nova.modules.qol;

import dev.nova.module.Category;
import dev.nova.module.Module;
import dev.nova.setting.BoolSetting;
import dev.nova.setting.NumberSetting;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.Locale;

/** Звук, когда в чате упоминают твой ник. */
public final class ChatAlertsModule extends Module {
    private final BoolSetting system = bool("Системные сообщения", true);
    private final NumberSetting pitch = number("Тон", 1.5, 0.5, 2, 0.1);
    private long lastPing;

    public ChatAlertsModule() {
        super("ChatAlerts", "Звук при упоминании ника в чате", Category.QOL);
        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, time) -> check(message));
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> { if (!overlay && system.get()) check(message); });
    }

    private void check(Text message) {
        if (!enabled() || mc.player == null) return;
        String me = mc.player.getGameProfile().getName().toLowerCase(Locale.ROOT);
        String text = message.getString().toLowerCase(Locale.ROOT);
        if (!text.contains(me) || text.startsWith("<" + me + ">")) return; // свои сообщения не считаем
        long now = System.currentTimeMillis();
        if (now - lastPing < 1000) return;
        lastPing = now;
        mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, pitch.asFloat()));
    }
}
