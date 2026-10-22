package dev.nova.setting;

import com.google.gson.JsonElement;

import java.util.function.BooleanSupplier;

public abstract class Setting<T> {
    private final String name;
    protected T value;
    private final T def;
    private BooleanSupplier visible = () -> true;

    protected Setting(String name, T def) { this.name = name; this.value = def; this.def = def; }

    public String name() { return name; }
    public T get() { return value; }
    public void set(T v) { this.value = v; }
    public void reset() { this.value = def; }
    public boolean visible() { return visible.getAsBoolean(); }

    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S visibleIf(BooleanSupplier s) { this.visible = s; return (S) this; }

    public abstract JsonElement toJson();
    public abstract void fromJson(JsonElement e);
}
