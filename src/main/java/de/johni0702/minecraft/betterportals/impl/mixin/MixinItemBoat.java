package de.johni0702.minecraft.betterportals.impl.mixin;

import net.minecraft.item.ItemBoat;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static de.johni0702.minecraft.betterportals.common.ExtensionsKt.findPortal;

@Mixin(ItemBoat.class)
public abstract class MixinItemBoat {
    @Redirect(
        method = "onItemRightClick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/World;rayTraceBlocks(Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Z)Lnet/minecraft/util/math/RayTraceResult;"
        )
    )
    private RayTraceResult blockRayTraceThroughPortals(World worldIn, Vec3d start, Vec3d end, boolean stopOnLiquid) {
        RayTraceResult result = worldIn.rayTraceBlocks(start, end, stopOnLiquid);
        if (result != null && result.typeOfHit == RayTraceResult.Type.BLOCK && result.hitVec != null
            && findPortal(worldIn, start, result.hitVec).getThird() != null) {
            // ItemBoat does its own (non-portal-aware) ray trace to determine where to spawn the boat. If the ray
            // passes through a portal, the boat would spawn in the local world behind the portal even though the
            // player is looking through the portal, so cancel it by turning the result into a MISS.
            return new RayTraceResult(RayTraceResult.Type.MISS, result.hitVec, null, result.getBlockPos());
        }
        return result;
    }
}
