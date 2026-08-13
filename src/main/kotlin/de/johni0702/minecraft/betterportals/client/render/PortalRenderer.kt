package de.johni0702.minecraft.betterportals.client.render

import de.johni0702.minecraft.betterportals.common.Portal
import de.johni0702.minecraft.betterportals.common.minus
import de.johni0702.minecraft.betterportals.common.to3dMid
import de.johni0702.minecraft.betterportals.common.toFacing
import de.johni0702.minecraft.view.client.ClientViewAPI
import de.johni0702.minecraft.view.client.render.RenderPass
import de.johni0702.minecraft.view.client.render.occlusionDetail
import de.johni0702.minecraft.view.impl.compat.celeritas.CeleritasTerrainDetail
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.shader.Framebuffer
import net.minecraft.client.shader.ShaderManager
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.Vec3d
import org.apache.logging.log4j.LogManager

abstract class PortalRenderer<in P: Portal> {
    companion object {
        private val mc = Minecraft.getMinecraft()
        // Needs to be lazy as it'll fail until MC has loaded its resources (and it loads renderer before that)
        private val shader by lazy { ShaderManager(mc.resourceManager, "betterportals:render_portal") }

        private val iChunWorldPortalsRenderLevel by lazy {
            try {
                val field = Class.forName("me.ichun.mods.ichunutil.common.module.worldportals.client.render.WorldPortalRenderer")
                        .getDeclaredField("renderLevel")
                ({ field[null] as Int })
            } catch (e: ClassNotFoundException) {
                LogManager.getLogger().debug("iChunUtil not found or unsupported version: ", e)
                ({ 0 })
            } catch (e: NoSuchFieldException) {
                LogManager.getLogger().warn("Unsupported iChunUtil version: ", e)
                ({ 0 })
            }
        }
    }

    /**
     * Side of the portal on which the camera resides.
     *
     * Updated during [render] and [renderTransparent] before any other method calls.
     */
    protected var viewFacing = EnumFacing.UP
        private set

    private fun isRemoteEnd(portal: P): Boolean {
        val renderPass = ClientViewAPI.instance.getRenderPassManager(mc).current ?: return false

        viewFacing = portal.localFacing.axis.toFacing(renderPass.camera.viewPosition - portal.localPosition.to3dMid())

        val parentPortal = renderPass.portalDetail?.parent
        // Skip rendering of portal if it's the remote to the portal we're looking through
        return parentPortal?.isTarget(portal) == true
    }

    /**
     * Renders the given portal at the given position relative to the camera.
     */
    open fun render(portal: P, pos: Vec3d, partialTicks: Float) {
        val renderPass = ClientViewAPI.instance.getRenderPassManager(mc).current ?: return

        if (isRemoteEnd(portal)) {
            return
        }

        val portalPass = renderPass.children.find {
            it.portalDetail?.parent == portal
        }

        if (iChunWorldPortalsRenderLevel() > 0) {
            renderPortal(portal, pos, null, renderPass)
            return
        }

        val occlusionQuery = portalPass?.occlusionDetail?.occlusionQuery
        occlusionQuery?.begin()

        val terrainDetail = portalPass?.get(CeleritasTerrainDetail::class.java)
        val framebuffer = if (terrainDetail == null || terrainDetail.isReady || terrainDetail.isTimedOut) {
            portalPass?.framebuffer
        } else {
            null
        }
        renderPortal(portal, pos, framebuffer, renderPass)

        occlusionQuery?.end()
    }

    protected open fun renderPortal(portal: P, pos: Vec3d, framebuffer: Framebuffer?, renderPass: RenderPass) {
        if (framebuffer == null) {
            GlStateManager.disableTexture2D()
            GlStateManager.color(0f, 0f, 0f)
        } else {
            shader.addSamplerTexture("sampler", framebuffer)
            shader.getShaderUniformOrDefault("screenSize")
                    .set(framebuffer.framebufferWidth.toFloat(), framebuffer.framebufferHeight.toFloat())
            val portalPass = renderPass.children.find {
                it.portalDetail?.parent == portal
            }
            val fogDetail = portalPass?.portalFogDetail
            shader.getShaderUniformOrDefault("fogDensity").set(fogDetail?.density?.toFloat() ?: 0f)
            with (fogDetail?.color ?: Vec3d.ZERO) {
                shader.getShaderUniformOrDefault("fogColor").set(x.toFloat(), y.toFloat(), z.toFloat())
            }
            // Fade the remote view out (e.g. one-way portals fading after use) by fading its alpha,
            // so the background (sky) shows through instead of the view darkening to black.
            val opacity = portalOpacity(portal)
            shader.getShaderUniformOrDefault("opacity").set(opacity.toFloat())
            shader.useShader()
            if (opacity < 1.0) {
                // Enable alpha blending for the fade: alpha < 1 blends with what's already in the framebuffer
                // (the sky), making the portal transparent instead of a black rectangle.
                GlStateManager.enableBlend()
                GlStateManager.blendFunc(
                        GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA)
            }
        }
        renderPortalSurface(portal, pos, renderPass, framebuffer != null)
        if (framebuffer == null) {
            GlStateManager.color(1f, 1f, 1f)
            GlStateManager.enableTexture2D()
        } else {
            shader.endShader()
            if (portalOpacity(portal) < 1.0) {
                // Restore the default blend state so the fade does not leak into subsequent draws.
                GlStateManager.disableBlend()
                GlStateManager.tryBlendFuncSeparate(
                        GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                        GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA)
            }
        }
    }

    /**
     * Opacity (0..1) with which the portal surface should be rendered. Subclasses may override this to fade the
     * portal out (e.g. one-way portals fading after being used). Defaults to fully opaque.
     */
    protected open fun portalOpacity(portal: P): Double = 1.0

    protected abstract fun renderPortalSurface(portal: P, pos: Vec3d, renderPass: RenderPass, haveContent: Boolean)

    open fun renderTransparent(portal: P, pos: Vec3d, partialTicks: Float) {
        if (isRemoteEnd(portal)) {
            return
        }
        doRenderTransparent(portal, pos, partialTicks)
    }

    open fun doRenderTransparent(portal: P, pos: Vec3d, partialTicks: Float) {
    }
}
