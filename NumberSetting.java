package dev.nova.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class NumberSetting extends Setting<Double> {
    public final double min, max, step;

    public NumberSetting(String name, double def, double min, double max, double step) {
        super(name, def);
        this.min = min; this.max = max; this.step = step;
    }

    @Override public void set(Double v) {
        double clamped = Math.max(min, Math.min(max, v));
        value = Math.round(clamped / step) * step;
    }

    public int asInt() { return (int) Math.round(value); }
    public float asFloat() { return value.floatValue(); }
    /** 0..1 позиция для слайдера */
    public double progress() { return (value - min) / (max - min); }

    @Override public JsonElement toJson() { return new JsonPrimitive(value); }
    @Override public void fromJson(JsonElement e) { if (e.isJsonPrimitive()) set(e.getAsDouble()); }
}
