package dev.nova.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** ARGB цвет + опциональный rainbow-режим. */
public final class ColorSetting extends Setting<Integer> {
    private boolean rainbow;

    public ColorSetting(String name, int argb) { super(name, argb); }

    public boolean rainbow() { return rainbow; }
    public void setRainbow(boolean r) { rainbow = r; }

    /** Итоговый цвет с учётом rainbow, offset сдвигает фазу (для градиента по строкам). */
    public int color(long offset) {
        if (!rainbow) return value;
        float hue = ((System.currentTimeMillis() + offset) % 4000L) / 4000f;
        int rgb = java.awt.Color.HSBtoRGB(hue, 0.65f, 1f) & 0xFFFFFF;
        return (value & 0xFF000000) | rgb;
    }
    public int color() { return color(0); }

    @Override public JsonElement toJson() { return new JsonPrimitive(String.format("#%08X%s", value, rainbow ? "R" : "")); }
    @Override public void fromJson(JsonElement e) {
        if (!e.isJsonPrimitive()) return;
        String s = e.getAsString();
        rainbow = s.endsWith("R");
        try { value = (int) Long.parseLong(s.replace("#", "").replace("R", ""), 16); } catch (NumberFormatException ignored) {}
    }
}
