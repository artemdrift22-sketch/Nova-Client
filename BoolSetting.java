package dev.nova.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class BoolSetting extends Setting<Boolean> {
    public BoolSetting(String name, boolean def) { super(name, def); }
    public void toggle() { value = !value; }
    @Override public JsonElement toJson() { return new JsonPrimitive(value); }
    @Override public void fromJson(JsonElement e) { if (e.isJsonPrimitive()) value = e.getAsBoolean(); }
}
