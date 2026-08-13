package de.johni0702.minecraft.betterportals.client.render

import de.johni0702.minecraft.betterportals.common.entity.OneWayPortalEntity
import de.johni0702.minecraft.betterportals.common.entity.PortalEntity
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.entity.Entity

open class RenderOneWayPortalEntity<E, out R: OneWayFramedPortalRenderer> (
        renderManager: RenderManager,
        portalRenderer: R
) : RenderPortalEntity<E, R>(renderManager, portalRenderer)
        where E: PortalEntity.OneWay,
              E: Entity
{
    override fun doRender(entity: E, x: Double, y: Double, z: Double, entityYaw: Float, partialTicks: Float) {
        portalRenderer.isTailEnd = entity.isTailEnd
        portalRenderer.isTailEndVisible = entity.isTailEndVisible
        portalRenderer.fade = (entity as? OneWayPortalEntity)?.fade ?: 1.0
        super.doRender(entity, x, y, z, entityYaw, partialTicks)
    }

    override fun renderMultipass(entity: E, x: Double, y: Double, z: Double, entityYaw: Float, partialTicks: Float) {
        portalRenderer.isTailEnd = entity.isTailEnd
        portalRenderer.isTailEndVisible = entity.isTailEndVisible
        portalRenderer.fade = (entity as? OneWayPortalEntity)?.fade ?: 1.0
        super.renderMultipass(entity, x, y, z, entityYaw, partialTicks)
    }
}