package de.johni0702.minecraft.betterportals.impl.travelhuts.common

import de.johni0702.minecraft.betterportals.client.render.RenderPortalEntity
import de.johni0702.minecraft.betterportals.common.PortalConfiguration
import de.johni0702.minecraft.betterportals.common.entity.PortalEntityAccessor
import de.johni0702.minecraft.betterportals.common.portalManager
import de.johni0702.minecraft.betterportals.impl.travelhuts.client.renderer.TravelHutsPortalRenderer
import de.johni0702.minecraft.betterportals.impl.travelhuts.common.blocks.BlockBetterTravelHutsPortal
import de.johni0702.minecraft.betterportals.impl.travelhuts.common.entity.TravelHutsPortalEntity
import info.loenwind.travelhut.TravelHutMod
import net.minecraft.block.Block
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.client.registry.RenderingRegistry
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.registry.EntityEntry
import net.minecraftforge.fml.common.registry.EntityEntryBuilder
import net.minecraftforge.registries.IForgeRegistry

const val MOD_ID = "betterportals"
const val TRAVELHUTS_MOD_ID = "travelhut"

lateinit var TRAVELHUTS_PORTAL_CONFIG: PortalConfiguration

fun initTravelHuts(
        mod: Any,
        clientPreInit: (() -> Unit) -> Unit,
        init: (() -> Unit) -> Unit,
        registerBlocks: (IForgeRegistry<Block>.() -> Unit) -> Unit,
        registerEntities: (IForgeRegistry<EntityEntry>.() -> Unit) -> Unit,
        configTravelHutsPortals: PortalConfiguration
) {
    TRAVELHUTS_PORTAL_CONFIG = configTravelHutsPortals

    clientPreInit {
        RenderingRegistry.registerEntityRenderingHandler(TravelHutsPortalEntity::class.java) {
            RenderPortalEntity(it, TravelHutsPortalRenderer(configTravelHutsPortals.opacity))
        }
    }

    registerBlocks {
        register(BlockBetterTravelHutsPortal().also { TravelHutMod.blockHutPortal = it })
    }

    registerEntities {
        register(
                EntityEntryBuilder.create<TravelHutsPortalEntity>()
                        .entity(TravelHutsPortalEntity::class.java)
                        .id(ResourceLocation(MOD_ID, "travelhuts_portal"), 8)
                        .name("travelhuts_portal")
                        .tracker(256, Int.MAX_VALUE, false)
                        .build()
        )
    }

    init {
        MinecraftForge.EVENT_BUS.register(object {
            @SubscribeEvent
            fun onWorld(event: WorldEvent.Load) {
                val world = event.world
                world.portalManager.registerPortals(PortalEntityAccessor(TravelHutsPortalEntity::class.java, world))
            }
        })
    }
}
