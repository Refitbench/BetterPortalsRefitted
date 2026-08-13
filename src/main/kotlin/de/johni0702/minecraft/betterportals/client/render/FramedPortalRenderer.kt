package de.johni0702.minecraft.betterportals.client.render

import de.johni0702.minecraft.betterportals.common.*
import de.johni0702.minecraft.betterportals.common.axis
import de.johni0702.minecraft.betterportals.common.opposite
import de.johni0702.minecraft.betterportals.common.plus
import de.johni0702.minecraft.betterportals.common.provideDelegate
import de.johni0702.minecraft.betterportals.common.rotate
import de.johni0702.minecraft.betterportals.common.with
import de.johni0702.minecraft.view.client.render.RenderPass
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.BufferBuilder
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11
import kotlin.text.toDouble

open class FramedPortalRenderer(
    val textureOpacity: () -> Double = { 0.0 },
    private val portalSprite: () -> TextureAtlasSprite? = { null }
) : PortalRenderer<FinitePortal>() {
    override fun renderPortalSurface(portal: FinitePortal, pos: Vec3d, renderPass: RenderPass, haveContent: Boolean) {
        val offset = pos - Vec3d(0.5, 0.5, 0.5)
        val blocks = portal.blocks.mapTo(mutableSetOf()) { it.rotate(portal.localRotation) }

        val tessellator = Tessellator.getInstance()
        with(tessellator.buffer) {
            begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)

            blocks.forEach { pos ->
                setTranslation(offset.x + pos.x, offset.y + pos.y, offset.z + pos.z)
                if (haveContent) {
                    // When we have remote content for the portal, then render as much as possible as to minimize
                    // entity rendering artifacts inside the portal in the remote world (since they're only rendered
                    // where we draw the portal surface).
                    EnumFacing.VALUES.forEach facing@{ facing ->
                        if (blocks.contains(pos.offset(facing))) return@facing
                        if (facing == viewFacing) return@facing

                        renderPartialPortalFace(this, facing)
                    }
                } else {
                    // otherwise preserve as much of the local frame as feasible and only render black on the far side
                    // to prevent the player from seeing that side.
                    renderPartialPortalFace(this, viewFacing.opposite)
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

    protected open fun renderPartialPortalFace(bufferBuilder: BufferBuilder, facing: EnumFacing) {
        // Drawing a cube has never been easier
        val inset = portalFaceInset(facing)
        val height = portalFaceHeight(facing)
        val xF = facing.frontOffsetX * (0.5 - inset)
        val yF = facing.frontOffsetY * (0.5 - inset)
        val zF = facing.frontOffsetZ * (0.5 - inset)
        var rotFacing = if (facing.axis == EnumFacing.Axis.Y) EnumFacing.NORTH else EnumFacing.UP
        (0..3).map { _ ->
            val nextRotFacing = rotFacing.rotateAround(facing.axis).let {
                if (facing.axisDirection == EnumFacing.AxisDirection.POSITIVE) it else it.opposite
            }
            bufferBuilder.pos(
                xF + rotFacing.frontOffsetX * 0.5 + nextRotFacing.frontOffsetX * 0.5 + 0.5,
                yF + (rotFacing.frontOffsetY * 0.5 + nextRotFacing.frontOffsetY * 0.5 + 0.5) * height,
                zF + rotFacing.frontOffsetZ * 0.5 + nextRotFacing.frontOffsetZ * 0.5 + 0.5
            ).endVertex()
            rotFacing = nextRotFacing
        }
    }

    /**
     * Vertical extent of a side face, as a fraction of the full block cell height. Horizontal (UP/DOWN) faces are
     * unaffected by this value (their plane is determined by [portalFaceInset]); side faces are drawn from y=0 up
     * to this height. Used by portals whose frame is shorter than a full block so that the rendered surface does
     * not extend above the frame.
     */
    protected open fun portalFaceHeight(facing: EnumFacing): Double = 1.0

    /**
     * How far the face with the given normal is pulled inward from the block cell boundary (i.e. how much shorter
     * the portal surface is along this direction). Used e.g. for portals whose frame is not a full block tall, so
     * that the portal surface meets the frame instead of leaving a gap through which the local world is visible.
     */
    protected open fun portalFaceInset(facing: EnumFacing): Double = 0.0

    override fun doRenderTransparent(portal: FinitePortal, pos: Vec3d, partialTicks: Float) {
        super.doRenderTransparent(portal, pos, partialTicks)
        // Blend the transparent overlay (e.g. the vanilla end portal starfield) out together with the remote view
        // so the whole portal fades uniformly instead of the overlay popping away first.
        val opacity = textureOpacity() * portalOpacity(portal)
        if (opacity > 0) {
            renderPortalBlocks(portal, pos, opacity)
        }
    }

    protected open fun renderPortalBlocks(portal: FinitePortal, pos: Vec3d, opacity: Double) {
        val offset = pos - Vec3d(0.5, 0.5, 0.5)

        val tessellator = Tessellator.getInstance()
        with(tessellator.buffer) {
            begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK)

            val blocks = portal.blocks.map { it.rotate(portal.localRotation) }
            blocks.forEach { pos ->
                setTranslation(offset.x + pos.x, offset.y + pos.y, offset.z + pos.z)
                renderPortalBlock(portal, pos, opacity, this)
            }

            setTranslation(0.0, 0.0, 0.0)
        }

        val mc = Minecraft.getMinecraft()
        GlStateManager.color(1f, 1f, 1f)
        mc.renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE)
        RenderHelper.disableStandardItemLighting()
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GlStateManager.enableBlend()
        tessellator.draw()
        GlStateManager.disableBlend()
        RenderHelper.enableStandardItemLighting()
    }

    protected open fun renderPortalBlock(portal: FinitePortal, relativePos: BlockPos, opacity: Double, bufferBuilder: BufferBuilder) {
        val sprite = portalSprite() ?: return
        val spriteUVs = getPortalSpriteUVs(sprite)
        bufferBuilder.markSpriteAsActive(sprite)
        val facing = viewFacing.opposite
        var rotFacing = if (facing.axis == EnumFacing.Axis.Y) EnumFacing.NORTH else EnumFacing.UP
        (0..3).map { i ->
            val nextRotFacing = rotFacing.rotateAround(facing.axis).let {
                if (facing.axisDirection == EnumFacing.AxisDirection.POSITIVE) it else it.opposite
            }
            with(bufferBuilder) {
                with(rotFacing.directionVec.to3d() * 0.5 + nextRotFacing.directionVec.to3d() * 0.5 + Vec3d(0.5, 0.5, 0.5)) {
                    pos(x, y, z)
                }
                color(1f, 1f, 1f, opacity.toFloat())
                spriteUVs[i].let { (u, v) -> tex(u, v) }
                lightmap(240, 240)
                endVertex()
            }
            rotFacing = nextRotFacing
        }
    }

    protected open fun getPortalSpriteUVs(sprite: TextureAtlasSprite): Array<Pair<Double, Double>> = arrayOf(
        Pair(sprite.minU.toDouble(), sprite.minV.toDouble()),
        Pair(sprite.minU.toDouble(), sprite.maxV.toDouble()),
        Pair(sprite.maxU.toDouble(), sprite.maxV.toDouble()),
        Pair(sprite.maxU.toDouble(), sprite.minV.toDouble())
    )
}

private fun BufferBuilder.markSpriteAsActive(sprite: TextureAtlasSprite) {
    // See https://github.com/Johni0702/BetterPortals/issues/170#issuecomment-514312479
    methodMarkNeedsAnimationUpdate?.invoke(sprite)

    // See https://github.com/sp614x/optifine/issues/2633
    methodSetSprite?.invoke(this, sprite)
}

// VanillaFix
private val methodMarkNeedsAnimationUpdate by lazy {
    try {
        TextureAtlasSprite::class.java.getDeclaredMethod("markNeedsAnimationUpdate")
    } catch (e: NoSuchMethodException) {
        null
    }
}

// Optifine
private val methodSetSprite by lazy {
    try {
        BufferBuilder::class.java.getDeclaredMethod("setSprite", TextureAtlasSprite::class.java)
    } catch (e: NoSuchMethodException) {
        null
    }
}
