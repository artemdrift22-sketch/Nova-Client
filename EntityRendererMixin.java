package dev.nova.mixin;

import dev.nova.modules.qol.HideNametagsModule;
import dev.nova.util.Hooks;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @Inject(method = "hasLabel", at = @At("HEAD"), cancellable = true)
    private void nova$hasLabel(Entity entity, double distance, CallbackInfoReturnable<Boolean> cir) {
        HideNametagsModule m = Hooks.get(HideNametagsModule.class);
        if (m == null || !m.enabled()) return;
        if (entity instanceof PlayerEntity ? m.players.get() : m.mobs.get()) cir.setReturnValue(false);
    }
}
