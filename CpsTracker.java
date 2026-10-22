package dev.nova.util;

import java.util.ArrayDeque;
import java.util.Deque;

/** Считает клики за последнюю секунду. Заполняется из MouseMixin (реальные события, а не поллинг раз в тик). */
public final class CpsTracker {
    private static final Deque<Long> LEFT = new ArrayDeque<>();
    private static final Deque<Long> RIGHT = new ArrayDeque<>();
    private static boolean leftDown, rightDown;

    private CpsTracker() {}

    public static void onButton(int button, boolean pressed) {
        if (button == 0) { leftDown = pressed; if (pressed) LEFT.addLast(System.currentTimeMillis()); }
        if (button == 1) { rightDown = pressed; if (pressed) RIGHT.addLast(System.currentTimeMillis()); }
    }

    private static int count(Deque<Long> q) {
        long now = System.currentTimeMillis();
        while (!q.isEmpty() && now - q.peekFirst() > 1000) q.pollFirst();
        return q.size();
    }

    public static int left() { return count(LEFT); }
    public static int right() { return count(RIGHT); }
    public static boolean leftDown() { return leftDown; }
    public static boolean rightDown() { return rightDown; }
}
