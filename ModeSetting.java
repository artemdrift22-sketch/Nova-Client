package dev.nova.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.List;

public final class ModeSetting extends Setting<String> {
    public final List<String> modes;

    public ModeSetting(String name, String def, String... modes) {
        super(name, def);
        this.modes = List.of(modes);
    }

    public void cycle(int dir) {
        int i = modes.indexOf(value);
        value = modes.get(Math.floorMod(i + dir, modes.size()));
    }

    public boolean is(String mode) { return value.equalsIgnoreCase(mode); }

    @Override public JsonElement toJson() { return new JsonPrimitive(value); }
    @Override public void fromJson(JsonElement e) {
        if (e.isJsonPrimitive() && modes.contains(e.getAsString())) value = e.getAsString();
    }
}
