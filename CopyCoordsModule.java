package dev.nova.modules.qol;

import dev.nova.module.Category;
import dev.nova.module.Module;
import dev.nova.setting.ModeSetting;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

/** Повесь на клавишу в ClickGUI (СКМ): копирует координаты или данные предмета в буфер. */
public final class CopyCoordsModule extends Module {
    private final ModeSetting what = mode("Что копировать", "Координаты", "Координаты", "Предмет");

    public CopyCoordsModule() { super("CopyData", "Копирует коорды или данные предмета", Category.QOL); }

    @Override protected void onEnable() {
        if (inGame()) {
            String out;
            if (what.is("Предмет")) {
                ItemStack st = mc.player.getMainHandStack();
                out = Registries.ITEM.getId(st.getItem()) + " x" + st.getCount() + " " + st.getComponents();
            } else {
                out = mc.player.getBlockX() + " " + mc.player.getBlockY() + " " + mc.player.getBlockZ();
            }
            mc.keyboard.setClipboard(out);
            mc.player.sendMessage(Text.literal("§d[Nova] §fСкопировано: §7" + (out.length() > 60 ? out.substring(0, 60) + "..." : out)), true);
        }
        setEnabled(false);
    }
}
