package dev.nova.render;

import dev.nova.modules.client.InterfaceModule;

/**
 * Анимации Nova. Всё считается по реальному времени (System.nanoTime), а не по кадрам,
 * поэтому скорость одинаковая при 30 и при 300 FPS.
 */
public final class AnimationUtils {
    private AnimationUtils() {}

    @FunctionalInterface
    public interface Easing { float apply(float t); }

    // ===================== easing =====================
    public static float linear(float t) { return clamp01(t); }
    public static float easeInCubic(float t) { t = clamp01(t); return t * t * t; }
    public static float easeOutCubic(float t) { t = clamp01(t); float f = 1 - t; return 1 - f * f * f; }
    public static float easeInOutQuad(float t) { t = clamp01(t); return t < 0.5f ? 2 * t * t : 1 - (float) Math.pow(-2 * t + 2, 2) / 2; }
    public static float easeOutQuart(float t) { t = clamp01(t); float f = 1 - t; return 1 - f * f * f * f; }
    public static float easeInOutCubic(float t) { t = clamp01(t); return t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2; }
    /** Лёгкий «перелёт» и возврат — для появления элементов. */
    public static float easeOutBack(float t) {
        t = clamp01(t);
        float c1 = 1.70158f, c3 = c1 + 1;
        return 1 + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
    }

    public static float clamp01(float t) { return t < 0 ? 0 : (t > 1 ? 1 : t); }
    public static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    /** Множитель скорости из настроек Interface. Анимации выключены — всё мгновенно. */
    public static float speed() {
        InterfaceModule i = InterfaceModule.get();
        if (i == null) return 1f;
        return i.animations.get() ? i.animSpeed.asFloat() : 1000f;
    }

    private static long now() { return System.nanoTime(); }

    /** deltaTime между двумя вызовами (сек), ограничен 0.1 c, чтобы после лага не было рывков. */
    public static float dt(long lastNanos, long nowNanos) {
        return Math.min(0.1f, Math.max(0f, (nowNanos - lastNanos) / 1_000_000_000f));
    }

    /** Экспоненциальное сглаживание с учётом deltaTime: не зависит от FPS. */
    public static float approach(float current, float target, float sharpness, float dt) {
        return target + (current - target) * (float) Math.exp(-sharpness * dt * speed());
    }

    // ===================== Animation: от A до B за N мс =====================
    public static final class Animation {
        private final long durationNs;
        private final Easing easing;
        private float from, to;
        private long start;

        public Animation(long durationMs, Easing easing) {
            this.durationNs = durationMs * 1_000_000L;
            this.easing = easing;
            this.start = now() - durationNs;
        }

        /** Мгновенно поставить значение. */
        public Animation set(float v) { from = to = v; start = now() - durationNs * 4; return this; }

        /** Плавно поехать к target от текущего значения. */
        public Animation animateTo(float target) {
            if (target == to) return this;
            from = get();
            to = target;
            start = now();
            return this;
        }

        public float progress() {
            float d = durationNs / speed();
            return d <= 0 ? 1f : clamp01((now() - start) / d);
        }

        public float get() { return from + (to - from) * easing.apply(progress()); }
        public float target() { return to; }
        public boolean done() { return progress() >= 1f; }
    }

    // ===================== Smooth: «пружинка» к цели =====================
    public static final class Smooth {
        private final float sharpness;
        private float value;
        private long last = now();
        private boolean init;

        /** sharpness ~ 8 = мягко, 16 = быстро, 25 = почти мгновенно. */
        public Smooth(float sharpness) { this.sharpness = sharpness; }

        public float update(float target) {
            long n = now();
            float dt = dt(last, n);
            last = n;
            if (!init) { value = target; init = true; return value; }
            value = approach(value, target, sharpness, dt);
            if (Math.abs(value - target) < 0.001f) value = target;
            return value;
        }

        public float get() { return value; }
        public Smooth set(float v) { value = v; init = true; last = now(); return this; }
    }
}
