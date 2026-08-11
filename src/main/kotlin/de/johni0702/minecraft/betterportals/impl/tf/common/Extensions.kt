package de.johni0702.minecraft.betterportals.impl.tf.common

import de.johni0702.minecraft.betterportals.client.render.RenderOneWayPortalEntity
import de.johni0702.minecraft.betterportals.common.PortalConfiguration
import de.johni0702.minecraft.betterportals.common.entity.PortalEntityAccessor
import de.johni0702.minecraft.betterportals.common.portalManager
import de.johni0702.minecraft.betterportals.impl.tf.client.renderer.TFPortalRenderer
import de.johni0702.minecraft.betterportals.impl.tf.common.blocks.BlockBetterTFPortal
import de.johni0702.minecraft.betterportals.impl.tf.common.entity.TFPortalEntity
import net.minecraft.block.Block
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.client.registry.RenderingRegistry
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.registry.EntityEntry
import net.minecraftforge.fml.common.registry.EntityEntryBuilder
import net.minecraftforge.registries.IForgeRegistry
import org.apache.logging.log4j.LogManager

internal val LOGGER = LogManager.getLogger("betterportals/twilightforest")
const val MOD_ID = "betterportals"
const val TF_MOD_ID = "twilightforest"

internal lateinit var TF_PORTAL_CONFIG: PortalConfiguration

fun initTwilightForest(
        mod: Any,
        clientPreInit: (() -> Unit) -> Unit,
        init: (() -> Unit) -> Unit,
        registerBlocks: (IForgeRegistry<Block>.() -> Unit) -> Unit,
        registerEntities: (IForgeRegistry<EntityEntry>.() -> Unit) -> Unit,
        configTwilightForestPortals: PortalConfiguration
) {
    TF_PORTAL_CONFIG = configTwilightForestPortals

    clientPreInit {
        RenderingRegistry.registerEntityRenderingHandler(TFPortalEntity::class.java) {
            RenderOneWayPortalEntity(it, TFPortalRenderer(configTwilightForestPortals.opacity))
        }
    }

    registerBlocks {
        register(BlockBetterTFPortal(mod))
    }

    registerEntities {
        register(
                EntityEntryBuilder.create<TFPortalEntity>()
                        .entity(TFPortalEntity::class.java)
                        .id(ResourceLocation(MOD_ID, "tf_portal"), 3)
                        .name("tf_portal")
                        .tracker(256, Int.MAX_VALUE, false)
                        .build()
        )
    }

    init {
        MinecraftForge.EVENT_BUS.register(object {
            @SubscribeEvent
            fun onWorld(event: WorldEvent.Load) {
                val world = event.world
                event.world.portalManager.registerPortals(PortalEntityAccessor(TFPortalEntity::class.java, world))
            }
        })
    }
}