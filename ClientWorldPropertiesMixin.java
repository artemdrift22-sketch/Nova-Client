package dev.nova.mixin;

import dev.nova.modules.world.TimeWeatherModule;
import dev.nova.util.Hooks;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Time Changer: подменяем время суток только для рендера на клиенте. */
@Mixin(ClientWorld.Properties.class)
public abstract class ClientWorldPropertiesMixin {
    @Inject(method = "getTimeOfDay", at = @At("RETURN"), cancellable = true)
    private void nova$time(CallbackInfoReturnable<Long> cir) {
        TimeWeatherModule m = Hooks.get(TimeWeatherModule.class);
        if (m != null && m.enabled() && m.changeTime.get()) cir.setReturnValue(m.time());
    }
}
