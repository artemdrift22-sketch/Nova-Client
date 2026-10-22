package dev.nova.modules.cosmetic;

import dev.nova.module.Category;
import dev.nova.module.Module;
import dev.nova.setting.ColorSetting;
import dev.nova.setting.ModeSetting;
import net.minecraft.text.Text;

import java.util.Locale;

/** Звезда Nova в чате рядом с ником. Рендер — через ChatHudMixin. */
public final class ChatStarModule extends Module {
    private static ChatStarModule instance;
    private final ModeSetting where = mode("Где", "Мои сообщения", "Мои сообщения", "С моим ником", "Все");
    private final ColorSetting color = color("Цвет звезды", 0xFFFFD700);

    public ChatStarModule() {
        super("Nova Star", "★ рядом с твоим ником в чате", Category.COSMETIC);
        instance = this;
    }

    public static Text decorate(Text message) {
        ChatStarModule m = instance;
        if (m == null || !m.enabled() || message == null || mc.player == null) return message;
        String name = mc.player.getGameProfile().getName();
        String raw = message.getString();
        String low = raw.toLowerCase(Locale.ROOT), n = name.toLowerCase(Locale.ROOT);
        boolean match;
        if (m.where.is("Все")) match = true;
        else if (m.where.is("С моим ником")) match = low.contains(n);
        else {
            int idx = low.indexOf(n);
            match = idx >= 0 && idx < 32; // ник в начале строки = это твоё сообщение (<Nick>, [Ранг] Nick: ...)
        }
        if (!match) return message;
        return Text.empty()
            .append(Text.literal("★ ").withColor(m.color.color() & 0xFFFFFF))
            .append(message);
    }
}
