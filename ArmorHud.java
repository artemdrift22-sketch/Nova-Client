package dev.nova.modules.hud;

import dev.nova.hud.HudModule;
import dev.nova.render.Render;
import dev.nova.setting.BoolSetting;
import dev.nova.setting.ModeSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/** Броня + предметы в руках с прочностью (%, числом или без). */
public final class ArmorHud extends HudModule {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};
    private final ModeSetting layout = mode("Расположение", "Горизонт.", "Горизонт.", "Вертик.");
    private final ModeSetting durability = mode("Прочность", "%", "%", "Число", "Скрыть");
    private final BoolSetting hands = bool("Предметы в руках", true);

    public ArmorHud() { super("ArmorHUD", "Броня и прочность", 0.005f, 0.55f); }

    private int count() { return hands.get() ? 6 : 4; }
    private boolean vertical() { return layout.is("Вертик."); }
    private static final int CELL_W = 34, CELL_H = 18;

    @Override public float width() { return vertical() ? CELL_W + 4 : count() * 20 + 4; }
    @Override public float height() { return vertical() ? count() * CELL_H + 4 : 30; }

    private ItemStack stack(EquipmentSlot s, boolean preview) {
        ItemStack st = mc.player == null ? ItemStack.EMPTY : mc.player.getEquippedStack(s);
        if (st.isEmpty() && preview) return switch (s) {
            case HEAD -> new ItemStack(Items.DIAMOND_HELMET);
            case CHEST -> new ItemStack(Items.DIAMOND_CHESTPLATE);
            case LEGS -> new ItemStack(Items.DIAMOND_LEGGINGS);
            case FEET -> new ItemStack(Items.DIAMOND_BOOTS);
            case MAINHAND -> new ItemStack(Items.DIAMOND_SWORD);
            default -> new ItemStack(Items.TOTEM_OF_UNDYING);
        };
        return st;
    }

    @Override protected void draw(DrawContext ctx, float delta, boolean preview) {
        for (int i = 0; i < count(); i++) {
            ItemStack st = stack(SLOTS[i], preview);
            int x = vertical() ? 2 : 2 + i * 20, y = vertical() ? 2 + i * CELL_H : 2;
            if (st.isEmpty()) continue;
            ctx.drawItem(st, x, y);
            ctx.drawStackOverlay(Render.font(), st, x, y);
            String d = durabilityText(st);
            if (d == null) continue;
            int color = durabilityColor(st);
            ctx.getMatrices().push();
            if (vertical()) {
                Render.text(ctx, d, x + 19, y + 5, color, true);
            } else {
                ctx.getMatrices().translate(x + 8, y + 18, 0);
                ctx.getMatrices().scale(0.6f, 0.6f, 1);
                Render.text(ctx, d, -Render.width(d) / 2f, 0, color, true);
            }
            ctx.getMatrices().pop();
        }
    }

    private String durabilityText(ItemStack st) {
        if (!st.isDamageable() || durability.is("Скрыть")) return null;
        int left = st.getMaxDamage() - st.getDamage();
        return durability.is("%") ? Math.round(left * 100f / st.getMaxDamage()) + "%" : String.valueOf(left);
    }

    private int durabilityColor(ItemStack st) {
        float f = (st.getMaxDamage() - st.getDamage()) / (float) st.getMaxDamage();
        return f > 0.5f ? 0xFF22C55E : f > 0.2f ? 0xFFFACC15 : 0xFFEF4444;
    }
}
