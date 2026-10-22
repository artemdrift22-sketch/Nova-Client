package dev.nova.mixin;

import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Позволяет выставить значение в обход валидатора (нужно для Fullbright: gamma > 1.0). */
@Mixin(SimpleOption.class)
public interface SimpleOptionAccessor {
    @Accessor("value")
    void nova$setValueRaw(Object value);
}
