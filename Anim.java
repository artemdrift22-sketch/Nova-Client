package dev.nova.render;

/** Плавная анимация 0..1, не зависит от FPS. */
public final class Anim {
    private float value, target;
    private long last = System.nanoTime();
    private final float speed; // единиц в секунду

    public Anim(float speed) { this.speed = speed; }

    public Anim target(float t) { target = t; return this; }

    public float get() {
        long now = System.nanoTime();
        float dt = (now - last) / 1_000_000_000f;
        last = now;
        float step = speed * dt;
        if (Math.abs(target - value) <= step) value = target;
        else value += Math.signum(target - value) * step;
        return ease(value);
    }

    private static float ease(float t) { return 1 - (1 - t) * (1 - t) * (1 - t); } // easeOutCubic
}
