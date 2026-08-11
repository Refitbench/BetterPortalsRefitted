package de.johni0702.minecraft.betterportals.impl;

import net.minecraft.launchwrapper.Launch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class BetterPortalsMixinConfigPlugin implements IMixinConfigPlugin {
    private final Logger logger = LogManager.getLogger("mixin/betterportals");
    private final boolean hasKotlin = Launch.classLoader.isClassExist("kotlin.Pair");
    private final boolean hasOF = Launch.classLoader.isClassExist("optifine.OptiFineForgeTweaker");
    private final boolean hasCC = Launch.classLoader.isClassExist("io.github.opencubicchunks.cubicchunks.core.asm.coremod.CubicChunksCoreMod");
    private final boolean hasSponge = Launch.classLoader.isClassExist("org.spongepowered.common.SpongePlatform");
    private final boolean hasVC = Launch.classLoader.isClassExist("org.vivecraft.asm.VivecraftASMTransformer");
    private final boolean hasCeleritas = Launch.classLoader.isClassExist("org.taumc.celeritas.CeleritasVintage");
    private final boolean hasNothirium = Launch.classLoader.isClassExist("meldexun.nothirium.mc.renderer.ChunkRenderManager");
    private final boolean vcVR = hasVC && Launch.classLoader.isClassExist("org.vivecraft.provider.MCOpenVR");
    private final boolean vcNonVR = hasVC && !vcVR;

    {
        if (!hasKotlin) {
            logger.error("Couldn't find kotlin.Pair class, Forgelin is probably missing, skipping all mixins!");
        }
        logger.debug("hasKotlin: {}", hasKotlin);
        logger.debug("hasOF: {}", hasOF);
        logger.debug("hasCC: {}", hasCC);
        logger.debug("hasSponge: {}", hasSponge);
        logger.debug("hasVC: {} (VR: {})", hasVC, vcVR);
        logger.debug("hasCeleritas: {}", hasCeleritas);
        logger.debug("hasNothirium: {}", hasNothirium);
        if (hasCeleritas && hasNothirium) {
            logger.warn("Both Celeritas and Nothirium are installed; BetterPortals will use Celeritas terrain compatibility");
        }
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!hasKotlin) return false;
        if (hasCeleritas && isCeleritasIncompatibleTerrainMixin(mixinClassName)) {
            logger.debug("Skipping {} because Celeritas owns RenderGlobal terrain rendering", mixinClassName);
            return false;
        }
        if (hasNothirium && !hasCeleritas && isNothiriumIncompatibleTerrainMixin(mixinClassName)) {
            logger.debug("Skipping {} because Nothirium owns RenderGlobal terrain rendering", mixinClassName);
            return false;
        }
        if (mixinClassName.endsWith("_Celeritas")) return hasCeleritas;
        if (mixinClassName.endsWith("_Nothirium")) return hasNothirium && !hasCeleritas;
        if (vcVR) {
            if (mixinClassName.endsWith("MixinEntityRenderer_NoOF")) {
                return true;
            }
            if (mixinClassName.endsWith("MixinEntityRenderer_OF")) {
                return false;
            }
        }
        if (vcNonVR) {
            // Patreon file downloader exists in non-vr version as well
            if (mixinClassName.endsWith("AbstractClientPlayer_VC")) {
                return true;
            }
        }
        if (mixinClassName.endsWith("_OF")) return hasOF;
        if (mixinClassName.endsWith("_NoOF")) return !hasOF;
        if (mixinClassName.endsWith("_CC")) return hasCC;
        if (mixinClassName.endsWith("_NoCC")) return !hasCC;
        if (mixinClassName.endsWith("_Sponge")) return hasSponge;
        if (mixinClassName.endsWith("_NoSponge")) return !hasSponge;
        if (mixinClassName.endsWith("_VC")) return vcVR;
        if (mixinClassName.endsWith("_NoVC")) return !vcVR;
        return true;
    }

    private boolean isCeleritasIncompatibleTerrainMixin(String mixinClassName) {
        return mixinClassName.endsWith("view.impl.mixin.MixinRenderGlobal")
                || mixinClassName.endsWith("view.impl.mixin.MixinChunkCompileTaskGenerator")
                || mixinClassName.endsWith("view.impl.mixin.MixinChunkRenderWorker")
                || mixinClassName.endsWith("view.impl.mixin.MixinRenderChunk")
                || mixinClassName.endsWith("view.impl.mixin.MixinRenderChunk_OF")
                || mixinClassName.endsWith("view.impl.mixin.MixinViewFrustum")
                || mixinClassName.endsWith("view.impl.mixin.MixinViewFrustum_OF");
    }

    private boolean isNothiriumIncompatibleTerrainMixin(String mixinClassName) {
        return mixinClassName.endsWith("view.impl.mixin.MixinChunkCompileTaskGenerator")
                || mixinClassName.endsWith("view.impl.mixin.MixinChunkRenderWorker")
                || mixinClassName.endsWith("view.impl.mixin.MixinRenderChunk")
                || mixinClassName.endsWith("view.impl.mixin.MixinRenderChunk_OF")
                || mixinClassName.endsWith("view.impl.mixin.MixinViewFrustum")
                || mixinClassName.endsWith("view.impl.mixin.MixinViewFrustum_OF");
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    public BetterPortalsMixinConfigPlugin() throws IOException {
    }
}
