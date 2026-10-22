package dev.nova.modules.pvp;

import dev.nova.hud.HudModule;
import dev.nova.module.Category;
import dev.nova.render.Render;
import dev.nova.render.Theme;
import dev.nova.setting.BoolSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;

/** Счётчики тотемов/геплов/перлов/стрел/опыта + детект «тотем лопнул». */
public final class CountersHud extends HudModule {
    @Override protected String header() { return "Counters"; }

    private static final Item[] ITEMS = {Items.TOTEM_OF_UNDYING, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE, Items.ENDER_PEARL, Items.ARROW, Items.EXPERIENCE_BOTTLE, Items.END_CRYSTAL, Items.OBSIDIAN};
    private final BoolSetting popAlert = bool("Алерт попа тотема", true);
    private final BoolSetting hideZero = bool("Скрывать нули", true);
    private int lastTotems = -1, pops;
    private long popTime;

    public CountersHud() { super("Counters", "Тотемы, геплы, перлы, стрелы и попы", Category.PVP, 0.9f, 0.4f); }

    private int count(Item it) { return mc.player == null ? 0 : mc.player.getInventory().count(it); }

    @Override public void onTick() {
        if (!inGame()) { lastTotems = -1; return; }
        int t = count(Items.TOTEM_OF_UNDYING);
        // поп = тотемов стало меньше и при этом игрок только что получил урон
        if (lastTotems > t && mc.player.hurtTime > 0) {
            pops++;
            popTime = System.currentTimeMillis();
            if (popAlert.get()) mc.player.playSound(SoundEvents.ITEM_TOTEM_USE, 0.4f, 1.4f);
        }
        if (!mc.player.isAlive()) pops = 0;
        lastTotems = t;
    }

    private int rows(boolean preview) {
        int r = 0;
        for (Item it : ITEMS) if (preview || !hideZero.get() || count(it) > 0) r++;
        return Math.max(1, r);
    }

    @Override public float width() { return 44; }
    @Override public float height() { return rows(true) * 17 + 14; }

    @Override protected void draw(DrawContext ctx, float delta, boolean preview) {
        int y = 2;
        for (Item it : ITEMS) {
            int n = count(it);
            if (!preview && hideZero.get() && n == 0) continue;
            ctx.drawItem(new ItemStack(it), 2, y);
            Render.text(ctx, String.valueOf(n), 21, y + 5, n == 0 ? Theme.TEXT_DIM : Theme.TEXT, true);
            y += 17;
        }
        boolean recent = System.currentTimeMillis() - popTime < 2500;
        Render.text(ctx, "Попы: " + pops, 2, y + 1, recent ? 0xFFFACC15 : Theme.TEXT_DIM, true);
    }
}
