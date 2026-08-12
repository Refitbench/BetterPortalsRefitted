package de.johni0702.minecraft.betterportals.impl.vanilla.client.renderer

import de.johni0702.minecraft.betterportals.client.render.OneWayFramedPortalRenderer
import de.johni0702.minecraft.betterportals.common.FinitePortal
import de.johni0702.minecraft.betterportals.common.minus
import de.johni0702.minecraft.betterportals.common.plus
import de.johni0702.minecraft.betterportals.common.to3d
import de.johni0702.minecraft.view.client.render.RenderPass
import net.minecraft.client.renderer.BufferBuilder
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.tileentity.TileEntityEndPortalRenderer
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.tileentity.TileEntityEndPortal
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL14

class EndPortalRenderer(
        textureOpacity: () -> Double = { 0.0 },
        /**
         * Amount by which the top (UP) far face is pulled down from the full block cell height (1.0) so that it
         * meets the top of the portal frame. The vanilla end portal frame ([net.minecraft.block.BlockEndPortalFrame]
         * AABB_BLOCK) is only 13/16 = 0.8125 of a block tall, so without this inset a 3/16 gap between the frame
         * and the portal surface shows the local world behind the portal. Only relevant when the portal is viewed
         * from below; the starfield overlay is aligned with the same plane.
         */
        private val farFaceInset: Double = 0.0
) : OneWayFramedPortalRenderer(textureOpacity) {
    companion object {
        /** Inset needed to align the portal surface with the top of the vanilla end portal frame. */
        const val END_FRAME_FAR_FACE_INSET = 1.0 - 13.0 / 16.0 // BlockEndPortalFrame is 13/16 tall
    }

    private val tileEntityRenderer = TileEntityEndPortalRenderer().also {
        it.setRendererDispatcher(TileEntityRendererDispatcher.instance)
    }
    private var firstPass = false
    private var opacity = 0.0
    private val dummyTileEntity = object : TileEntityEndPortal() {
        override fun shouldRenderFace(face: EnumFacing): Boolean {
            if (firstPass) {
                GlStateManager.blendFunc(GlStateManager.SourceFactor.CONSTANT_ALPHA, GlStateManager.DestFactor.ONE_MINUS_CONSTANT_ALPHA)
                GL14.glBlendColor(0f, 0f, 0f, opacity.toFloat())
                firstPass = false
            }
            // The vanilla TESR's UP face is wound to be visible from above and its DOWN face from below, so draw
            // the face whose winding faces the camera (viewFacing) and translate it onto the far-face plane where
            // the remote world is rendered (see renderPortalBlocks). The far side of that quad is then back-face
            // culled, so the starfield stays confined to the window instead of floating inside the frame.
            return face == viewFacing
        }
    }

    private var partialTicks = 0f

    override fun portalFaceInset(facing: EnumFacing): Double =
        if (facing == EnumFacing.UP) farFaceInset else 0.0

    override fun portalFaceHeight(facing: EnumFacing): Double =
        if (facing.axis == EnumFacing.Axis.Y) 1.0 else 1.0 - farFaceInset

    override fun renderPortalSurface(portal: FinitePortal, pos: Vec3d, renderPass: RenderPass, haveContent: Boolean) {
        super.renderPortalSurface(portal, pos, renderPass, haveContent)
        // The portal frame is only 13/16 tall, so the top band of the portal cells (above the frame) would stay
        // see-through: when looking at the portal from below, the local world would be visible through it. Seal
        // that band with a wall of remote content, matching the far face.
        //
        // Only do this when viewed from below: from above, the hole's walls are the (solid) frame, and sealing the
        // band there would cover the actual corner blocks of the local world with remote content (with an obvious
        // lighting difference) for no benefit.
        if (haveContent && viewFacing == EnumFacing.DOWN && farFaceInset > 0.0) {
            sealTopBand(portal, pos)
        }
    }

    private fun sealTopBand(portal: FinitePortal, pos: Vec3d) {
        val offset = pos - Vec3d(0.5, 0.5, 0.5)
        val blocks = portal.blocks.map { it.rotate(portal.localRotation) }.toSet()

        val tessellator = Tessellator.getInstance()
        with(tessellator.buffer) {
            begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)
            blocks.forEach { relativePos ->
                setTranslation(offset.x + relativePos.x, offset.y + relativePos.y, offset.z + relativePos.z)
                EnumFacing.HORIZONTALS.forEach { facing ->
                    if (blocks.contains(relativePos.offset(facing))) return@forEach
                    renderTopBandFace(this, facing)
                }
            }
            setTranslation(0.0, 0.0, 0.0)
        }

        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL)
        GL11.glPolygonOffset(-1f, -1f)
        tessellator.draw()
        GL11.glPolygonOffset(0f, 0f)
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL)
    }

    /**
     * Vertical wall closing the see-through top band (1.0 - farFaceInset .. 1.0) of a portal cell's exposed side.
     * Uses the same vertex pattern (and thus the same visible side) as the regular portal faces, with the vertical
     * extent compressed into the band above the frame.
     */
    private fun renderTopBandFace(bufferBuilder: BufferBuilder, facing: EnumFacing) {
        val yMin = 1.0 - farFaceInset
        val xF = facing.frontOffsetX * 0.5
        val yF = facing.frontOffsetY * 0.5
        val zF = facing.frontOffsetZ * 0.5
        var rotFacing = if (facing.axis == EnumFacing.Axis.Y) EnumFacing.NORTH else EnumFacing.UP
        (0..3).map { _ ->
            val nextRotFacing = rotFacing.rotateAround(facing.axis).let {
                if (facing.axisDirection == EnumFacing.AxisDirection.POSITIVE) it else it.opposite
            }
            val yRel = yF + rotFacing.frontOffsetY * 0.5 + nextRotFacing.frontOffsetY * 0.5 + 0.5
            bufferBuilder.pos(
                xF + rotFacing.frontOffsetX * 0.5 + nextRotFacing.frontOffsetX * 0.5 + 0.5,
                yMin + yRel * farFaceInset,
                zF + rotFacing.frontOffsetZ * 0.5 + nextRotFacing.frontOffsetZ * 0.5 + 0.5
            ).endVertex()
            rotFacing = nextRotFacing
        }
    }

    override fun doRenderTransparent(portal: FinitePortal, pos: Vec3d, partialTicks: Float) {
        this.partialTicks = partialTicks
        super.doRenderTransparent(portal, pos, partialTicks)
    }

    override fun renderPortalBlocks(portal: FinitePortal, pos: Vec3d, opacity: Double) {
        this.opacity = opacity
        val offset = pos - Vec3d(0.5, 0.5, 0.5)

        // The TESR draws its UP face at y+0.75 and its DOWN face at y+0 within the block cell. The far face (the
        // plane of the remote world) is at y+0 (when viewed from above) respectively 1.0 - farFaceInset (when
        // viewed from below). We draw the face whose winding faces the camera (see shouldRenderFace) and shift it
        // onto that plane.
        val translateY = when (viewFacing) {
            EnumFacing.UP -> -0.75
            else -> 1.0 - farFaceInset
        }

        val blocks = portal.blocks.map { it.rotate(portal.localRotation) }
        blocks.forEach { relativePos ->
            with(offset + relativePos.to3d()) {
                firstPass = true
                // The remote view quads are drawn with glPolygonOffset(-1, -1) in the solid pass, so a starfield
                // quad on the exact same plane would be depth-rejected (it ends up one depth unit behind them).
                // Pull it at least as far toward the camera so it passes the depth test and blends over the view.
                GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL)
                GL11.glPolygonOffset(-2f, -2f)
                tileEntityRenderer.render(dummyTileEntity, x, y + translateY, z, partialTicks, 0, 1f)
                GL11.glPolygonOffset(0f, 0f)
                GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL)
            }
        }
    }
}