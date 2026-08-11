package de.johni0702.minecraft.betterportals.impl.mixin;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static de.johni0702.minecraft.betterportals.common.ExtensionsKt.findPortal;

@Mixin(Item.class)
public abstract class MixinItem {
    @Inject(method = "rayTrace", at = @At("RETURN"), cancellable = true)
    private void blockInteractionThroughPortals(World worldIn, EntityPlayer playerIn, boolean useLiquids, CallbackInfoReturnable<RayTraceResult> cir) {
        RayTraceResult result = cir.getReturnValue();
        if (result == null || result.typeOfHit != RayTraceResult.Type.BLOCK || result.hitVec == null) {
            return;
        }
        // Items like buckets, lily pads or monster spawners perform their own (non-portal-aware) ray trace to
        // determine the block they act on. If that ray passes through a portal, the item would act on a block in
        // the local world behind the portal even though the player is looking through the portal (e.g. dumping a
        // water bucket through the portal), so cancel it by turning the result into a MISS.
        Vec3d eyePos = playerIn.getPositionEyes(1.0F);
        if (findPortal(worldIn, eyePos, result.hitVec).getThird() != null) {
            cir.setReturnValue(new RayTraceResult(RayTraceResult.Type.MISS, result.hitVec, null, result.getBlockPos()));
        }
    }
}
