package dev.nova.hud;

import dev.nova.render.AnimationUtils.Smooth;

import java.util.*;

/**
 * Список строк HUD с анимацией появления/исчезновения и плавной перестановкой.
 * begin() -> put(...) для каждой живой строки -> end(order). Удалённые строки плавно гаснут.
 */
public final class AnimatedList {
    public static final class Entry {
        public final String key;
        public String left = "", right = "";
        public int color = 0xFFFFFFFF;
        public long sort;
        boolean alive;
        final Smooth t = new Smooth(14f).set(0f);
        final Smooth y = new Smooth(16f);
        boolean placed;

        Entry(String key) { this.key = key; }

        /** 0..1 — прогресс появления. */
        public float t() { return t.get(); }
        /** Сглаженная Y-позиция строки. */
        public float y() { return y.get(); }
        public boolean alive() { return alive; }
    }

    private final LinkedHashMap<String, Entry> map = new LinkedHashMap<>();
    private List<Entry> last = List.of();

    public void begin() { for (Entry e : map.values()) e.alive = false; }

    public Entry put(String key, String left, String right, int color) {
        Entry e = map.computeIfAbsent(key, Entry::new);
        e.left = left;
        e.right = right;
        e.color = color;
        e.alive = true;
        return e;
    }

    /** Обновляет анимации и возвращает строки в порядке order; rowH — высота строки. */
    public List<Entry> end(Comparator<Entry> order, float rowH) {
        Iterator<Entry> it = map.values().iterator();
        while (it.hasNext()) {
            Entry e = it.next();
            float v = e.t.update(e.alive ? 1f : 0f);
            if (!e.alive && v < 0.02f) it.remove();
        }
        List<Entry> list = new ArrayList<>(map.values());
        if (order != null) list.sort(order);
        float y = 0;
        for (Entry e : list) {
            if (!e.placed) { e.y.set(y); e.placed = true; }
            e.y.update(y);
            y += rowH * e.t.get();
        }
        last = list;
        return list;
    }

    public List<Entry> entries() { return last; }

    public float height(float rowH) {
        float h = 0;
        for (Entry e : last) h += rowH * e.t.get();
        return h;
    }

    public boolean isEmpty() { return map.isEmpty(); }
    public boolean anyAlive() { for (Entry e : map.values()) if (e.alive) return true; return false; }
}
