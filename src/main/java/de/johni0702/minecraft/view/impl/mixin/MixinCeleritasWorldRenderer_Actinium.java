package de.johni0702.minecraft.view.impl.mixin;

import de.johni0702.minecraft.view.impl.client.render.ViewRenderManager;
import com.dhj.actinium.render.terrain.ActiniumWorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Actinium variant of {@link MixinCeleritasWorldRenderer_Celeritas}: Actinium's world renderer is
 * {@code com.dhj.actinium.render.terrain.ActiniumWorldRenderer} rather than the upstream
 * {@code org.taumc.celeritas.impl.render.terrain.CeleritasWorldRenderer}.
 */
@Mixin(value = ActiniumWorldRenderer.class, remap = false)
public abstract class MixinCeleritasWorldRenderer_Actinium {
    @Inject(method = "getEffectiveRenderDistance", at = @At("HEAD"), cancellable = true, require = 1)
    private void betterportals$useAllocationRenderDistance(CallbackInfoReturnable<Integer> cir) {
        ViewRenderManager manager = ViewRenderManager.Companion.getINSTANCE();
        if (manager.getCurrent() != null) {
            // Portal passes publish a temporary visual distance, but the persistent section manager must not
            // be resized for every recursive view.
            cir.setReturnValue(manager.getRealRenderDistanceChunks());
        }
    }
}
