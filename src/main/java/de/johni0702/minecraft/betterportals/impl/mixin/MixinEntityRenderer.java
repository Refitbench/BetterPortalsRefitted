package de.johni0702.minecraft.betterportals.impl.mixin;

import de.johni0702.minecraft.betterportals.common.Mat4d;
import de.johni0702.minecraft.betterportals.impl.client.PostSetupFogEvent;
import kotlin.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.vecmath.Matrix4d;

import static de.johni0702.minecraft.betterportals.common.ExtensionsKt.findPortal;
import static de.johni0702.minecraft.betterportals.common.ExtensionsKt.getSyncPos;
import static de.johni0702.minecraft.betterportals.common.ExtensionsKt.inverse;
import static de.johni0702.minecraft.betterportals.common.ExtensionsKt.rayTraceBlocksWithPortals;
import static de.johni0702.minecraft.betterportals.common.ExtensionsKt.rayTracePortals;
import static de.johni0702.minecraft.betterportals.common.ExtensionsKt.times;
import static de.johni0702.minecraft.betterportals.common.ExtensionsKt.toMC;
import static de.johni0702.minecraft.betterportals.common.ExtensionsKt.toPoint;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {
    @Shadow @Final private Minecraft mc;

    @Inject(method = "setupFog", at = @At("RETURN"))
    private void postSetupFogInView(int start, float partialTicks, CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new PostSetupFogEvent());
    }

    @Inject(method = "getMouseOver", at = @At("RETURN"))
    private void blockInteractionThroughPortals(float partialTicks, CallbackInfo ci) {
        RayTraceResult target = mc.objectMouseOver;
        if (target == null || target.typeOfHit == RayTraceResult.Type.MISS || target.hitVec == null) {
            return;
        }
        World world = mc.world;
        Entity viewEntity = mc.getRenderViewEntity();
        if (world == null || viewEntity == null) {
            return;
        }
        // The mouse-over ray trace is (unlike the camera ray trace above) not portal-aware, so it passes right
        // through the portal and hits whatever is behind it in the local world. That would allow the player to
        // interact with local blocks/entities which are visually obscured by the portal (e.g. opening a chest or
        // shearing a sheep located directly behind the portal), even though cross-portal interaction is not
        // supported. Since the target is on the other side of the portal, cancel the interaction by making it a
        // MISS so that the player cannot interact with anything through a portal.
        Vec3d eyePos = viewEntity.getPositionEyes(partialTicks);
        if (findPortal(world, eyePos, target.hitVec).getThird() != null) {
            mc.objectMouseOver = new RayTraceResult(RayTraceResult.Type.MISS, target.hitVec, null, target.getBlockPos());
            mc.pointedEntity = null;
        }
    }

    @Redirect(
            method = "orientCamera",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/WorldClient;rayTraceBlocks(Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/RayTraceResult;"
            )
    )
    private RayTraceResult doRayTraceBlocksWithPortals(WorldClient clientWorld, Vec3d start, Vec3d end) {
        Pair<World, Matrix4d> result;
        World world = clientWorld;
        Entity viewEntity = mc.getRenderViewEntity();
        Entity vehicle = viewEntity.getLowestRidingEntity();
        Vec3d vehiclePos = getSyncPos(vehicle).addVector(0, vehicle.getEyeHeight(), 0);
        Vec3d eyePos = viewEntity.getPositionVector().addVector(0, viewEntity.getEyeHeight(), 0);
        Matrix4d matrix = Mat4d.id();

        if (!vehiclePos.equals(eyePos)) {
            result = rayTracePortals(world, vehiclePos, eyePos);
            world = result.getFirst();
            matrix = times(matrix, inverse(result.getSecond()));
            eyePos = toMC(times(result.getSecond(), toPoint(eyePos)));
            start = toMC(times(result.getSecond(), toPoint(start)));
            end = toMC(times(result.getSecond(), toPoint(end)));
        }

        if (!eyePos.equals(start)) {
            result = rayTracePortals(world, eyePos, start);
            world = result.getFirst();
            matrix = times(matrix, inverse(result.getSecond()));
            start = toMC(times(result.getSecond(), toPoint(start)));
            end = toMC(times(result.getSecond(), toPoint(end)));
        }

        RayTraceResult rayResult = rayTraceBlocksWithPortals(world, start, end, false, false, false);
        if (rayResult != null) {
            // Calling code only uses hitVec, so we need to transform only it
            rayResult.hitVec = toMC(times(matrix, toPoint(rayResult.hitVec)));
        }
        return rayResult;
    }
}
