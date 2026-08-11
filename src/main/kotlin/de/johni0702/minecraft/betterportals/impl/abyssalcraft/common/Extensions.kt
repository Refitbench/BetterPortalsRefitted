package de.johni0702.minecraft.betterportals.impl.abyssalcraft.common

import com.shinoow.abyssalcraft.api.block.ACBlocks
import de.johni0702.minecraft.betterportals.client.render.FramedPortalRenderer
import de.johni0702.minecraft.betterportals.client.render.RenderPortalEntity
import de.johni0702.minecraft.betterportals.common.PortalConfiguration
import de.johni0702.minecraft.betterportals.common.entity.PortalEntityAccessor
import de.johni0702.minecraft.betterportals.common.portalManager
import de.johni0702.minecraft.betterportals.impl.abyssalcraft.common.blocks.BlockBetterAbyssPortal
import de.johni0702.minecraft.betterportals.impl.abyssalcraft.common.blocks.BlockBetterDreadlandsPortal
import de.johni0702.minecraft.betterportals.impl.abyssalcraft.common.blocks.BlockBetterOmotholPortal
import de.johni0702.minecraft.betterportals.impl.abyssalcraft.common.entity.AbyssPortalEntity
import de.johni0702.minecraft.betterportals.impl.abyssalcraft.common.entity.DreadlandsPortalEntity
import de.johni0702.minecraft.betterportals.impl.abyssalcraft.common.entity.OmotholPortalEntity
import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.AxisAlignedBB
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.client.registry.RenderingRegistry
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.registry.EntityEntry
import net.minecraftforge.fml.common.registry.EntityEntryBuilder
import net.minecraftforge.registries.IForgeRegistry

internal val EMPTY_AABB = AxisAlignedBB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
const val MOD_ID = "betterportals"
const val ABYSSALCRAFT_MOD_ID = "abyssalcraft"

lateinit var ABYSSALCRAFT_PORTAL_CONFIG: PortalConfiguration

fun initAbyssalcraft(
        mod: Any,
        clientPreInit: (() -> Unit) -> Unit,
        init: (() -> Unit) -> Unit,
        registerBlocks: (IForgeRegistry<Block>.() -> Unit) -> Unit,
        registerEntities: (IForgeRegistry<EntityEntry>.() -> Unit) -> Unit,
        configAbyssalcraftPortals: PortalConfiguration
) {
    ABYSSALCRAFT_PORTAL_CONFIG = configAbyssalcraftPortals

    clientPreInit {
        RenderingRegistry.registerEntityRenderingHandler(AbyssPortalEntity::class.java) {
            RenderPortalEntity(it, FramedPortalRenderer(configAbyssalcraftPortals.opacity, {
                Minecraft.getMinecraft().textureMapBlocks.getAtlasSprite("$ABYSSALCRAFT_MOD_ID:blocks/ag")
            }))
        }
        RenderingRegistry.registerEntityRenderingHandler(DreadlandsPortalEntity::class.java) {
            RenderPortalEntity(it, FramedPortalRenderer(configAbyssalcraftPortals.opacity, {
                Minecraft.getMinecraft().textureMapBlocks.getAtlasSprite("$ABYSSALCRAFT_MOD_ID:blocks/dg")
            }))
        }
        RenderingRegistry.registerEntityRenderingHandler(OmotholPortalEntity::class.java) {
            RenderPortalEntity(it, FramedPortalRenderer(configAbyssalcraftPortals.opacity, {
                Minecraft.getMinecraft().textureMapBlocks.getAtlasSprite("$ABYSSALCRAFT_MOD_ID:blocks/og")
            }))
        }
    }

    registerBlocks {
        register(BlockBetterAbyssPortal(mod).also { ACBlocks.abyssal_gateway = it })
        register(BlockBetterDreadlandsPortal(mod).also { ACBlocks.dreaded_gateway = it })
        register(BlockBetterOmotholPortal(mod).also { ACBlocks.omothol_gateway = it })
    }

    registerEntities {
        register(
                EntityEntryBuilder.create<AbyssPortalEntity>()
                        .entity(AbyssPortalEntity::class.java)
                        .id(ResourceLocation(MOD_ID, "${ABYSSALCRAFT_MOD_ID}_abyssal_portal"), 5)
                        .name("${ABYSSALCRAFT_MOD_ID}_abyssal_portal")
                        .tracker(256, Int.MAX_VALUE, false)
                        .build()
        )
        register(
                EntityEntryBuilder.create<DreadlandsPortalEntity>()
                        .entity(DreadlandsPortalEntity::class.java)
                        .id(ResourceLocation(MOD_ID, "${ABYSSALCRAFT_MOD_ID}_dreadlands_portal"), 6)
                        .name("${ABYSSALCRAFT_MOD_ID}_dreadlands_portal")
                        .tracker(256, Int.MAX_VALUE, false)
                        .build()
        )
        register(
                EntityEntryBuilder.create<OmotholPortalEntity>()
                        .entity(OmotholPortalEntity::class.java)
                        .id(ResourceLocation(MOD_ID, "${ABYSSALCRAFT_MOD_ID}_omothol_portal"), 7)
                        .name("${ABYSSALCRAFT_MOD_ID}_omothol_portal")
                        .tracker(256, Int.MAX_VALUE, false)
                        .build()
        )
    }

    init {
        MinecraftForge.EVENT_BUS.register(object {
            @SubscribeEvent
            fun onWorld(event: WorldEvent.Load) {
                val world = event.world
                world.portalManager.registerPortals(PortalEntityAccessor(AbyssPortalEntity::class.java, world))
                world.portalManager.registerPortals(PortalEntityAccessor(DreadlandsPortalEntity::class.java, world))
                world.portalManager.registerPortals(PortalEntityAccessor(OmotholPortalEntity::class.java, world))
            }
        })
    }
}
