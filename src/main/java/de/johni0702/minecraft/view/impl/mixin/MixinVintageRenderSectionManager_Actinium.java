package de.johni0702.minecraft.view.impl.mixin;

import de.johni0702.minecraft.view.client.render.RenderPass;
import de.johni0702.minecraft.view.impl.compat.celeritas.CeleritasPassClassifier;
import de.johni0702.minecraft.view.impl.client.render.ViewRenderManager;
import com.dhj.actinium.render.terrain.VintageRenderSectionManager;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Actinium variant of {@link MixinVintageRenderSectionManager_Celeritas}: Actinium keeps the
 * vintage section manager under its own namespace instead of {@code org.taumc.celeritas}.
 */
@Mixin(value = VintageRenderSectionManager.class, remap = false)
public abstract class MixinVintageRenderSectionManager_Actinium {
    @Inject(
            method = "shouldUseOcclusionCulling(Lorg/embeddedt/embeddium/impl/render/viewport/Viewport;Z)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 1)
    private void betterportals$disablePortalSectionConnectivity(
            Viewport viewport,
            boolean spectator,
            CallbackInfoReturnable<Boolean> cir) {
        RenderPass current = ViewRenderManager.Companion.getINSTANCE().getCurrent();
        if (CeleritasPassClassifier.isSynthetic(current)) {
            cir.setReturnValue(false);
        }
    }
}
