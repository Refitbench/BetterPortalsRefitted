package de.johni0702.minecraft.betterportals.impl

import de.johni0702.minecraft.betterportals.common.BetterPortalsAPI
import de.johni0702.minecraft.betterportals.common.PortalConfiguration
import de.johni0702.minecraft.betterportals.impl.common.initPortal
import de.johni0702.minecraft.betterportals.impl.transition.common.initTransition
import de.johni0702.minecraft.betterportals.impl.vanilla.common.initVanilla
import de.johni0702.minecraft.view.common.ViewAPI
import de.johni0702.minecraft.view.impl.ViewAPIImpl
import de.johni0702.minecraft.view.impl.common.initView
import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.FolderResourcePack
import net.minecraft.client.resources.IResourcePack
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.config.Config
import net.minecraftforge.common.config.ConfigManager
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.SidedProxy
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.registries.IForgeRegistry
import org.apache.logging.log4j.Logger
import java.io.File

const val MOD_ID = "betterportals"

lateinit var logger: Logger

@Mod(
    modid = MOD_ID,
    useMetadata = true,
    version = Reference.VERSION,
    modLanguage = "kotlin",
    modLanguageAdapter = "io.github.chaosunity.forgelin.KotlinAdapter",
    dependencies = "required-after:forgelin_continuous",
)
object BetterPortalsMod : ViewAPI by ViewAPIImpl, BetterPortalsAPI by BetterPortalsAPIImpl {
    internal val clientPreInitCallbacks = mutableListOf<() -> Unit>()
    internal val commonInitCallbacks = mutableListOf<() -> Unit>()
    internal val clientInitCallbacks = mutableListOf<() -> Unit>()
    internal val commonPostInitCallbacks = mutableListOf<() -> Unit>()
    internal val clientPostInitCallbacks = mutableListOf<() -> Unit>()
    private val registerBlockCallbacks = mutableListOf<IForgeRegistry<Block>.() -> Unit>()

    init {
        ConfigManager.sync(MOD_ID, Config.Type.INSTANCE)

        fun PortalConfig.toConfiguration() =
            PortalConfiguration(
                { opacity },
                { renderDistMin },
                { renderDistMax },
                { renderDistSizeMultiplier },
            )

        initView(
            init = { commonInitCallbacks.add(it) },
            clientInit = { clientInitCallbacks.add(it) },
            debugView = { BPConfig.debugView },
            debugLogging = { BPConfig.debugLogging },
        )

        initTransition(
            init = { commonInitCallbacks.add(it) },
            enable = BPConfig.enhanceThirdPartyTransfers,
            duration = { BPConfig.enhancedThirdPartyTransferSeconds },
        )

        initPortal(
            mod = this,
            init = { commonInitCallbacks.add(it) },
            clientInit = { clientInitCallbacks.add(it) },
            preventFallDamage = { BPConfig.preventFallDamage },
            dropRemoteSound = { !BPConfig.soundThroughPortals },
            maxRenderRecursion = { if (BPConfig.seeThroughPortals) BPConfig.recursionLimit else 0 },
        )

        initVanilla(
            mod = this,
            init = { commonInitCallbacks.add(it) },
            clientPreInit = { clientPreInitCallbacks.add(it) },
            registerBlocks = { registerBlockCallbacks.add(it) },
            enableNetherPortals = BPConfig.netherPortals.enabled,
            enableEndPortals = BPConfig.endPortals.enabled,
            configNetherPortals = BPConfig.netherPortals.toConfiguration(),
            configEndPortals = BPConfig.endPortals.toConfiguration(),
        )

        if (BPConfig.twilightForestPortals.enabled && Loader.isModLoaded("twilightforest")) {
            de.johni0702.minecraft.betterportals.impl.tf.common.initTwilightForest(
                mod = this,
                init = { commonInitCallbacks.add(it) },
                clientPreInit = { clientPreInitCallbacks.add(it) },
                registerBlocks = { registerBlockCallbacks.add(it) },
                configTwilightForestPortals = BPConfig.twilightForestPortals.toConfiguration(),
            )
        }

        if (BPConfig.mekanismPortals.enabled && Loader.isModLoaded("mekanism")) {
            de.johni0702.minecraft.betterportals.impl.mekanism.common.initMekanism(
                init = { commonInitCallbacks.add(it) },
                postInit = { commonPostInitCallbacks.add(it) },
                clientPostInit = { clientPostInitCallbacks.add(it) },
                configMekanismPortals = BPConfig.mekanismPortals.toConfiguration(),
            )
        }

        if (BPConfig.aetherPortals.enabled && Loader.isModLoaded("aether_legacy")) {
            de.johni0702.minecraft.betterportals.impl.aether.common.initAether(
                mod = this,
                init = { commonInitCallbacks.add(it) },
                clientPreInit = { clientPreInitCallbacks.add(it) },
                registerBlocks = { registerBlockCallbacks.add(it) },
                configAetherPortals = BPConfig.aetherPortals.toConfiguration(),
            )
        }

        if (BPConfig.abyssalcraftPortals.enabled && Loader.isModLoaded("abyssalcraft")) {
            de.johni0702.minecraft.betterportals.impl.abyssalcraft.common.initAbyssalcraft(
                mod = this,
                init = { commonInitCallbacks.add(it) },
                clientPreInit = { clientPreInitCallbacks.add(it) },
                registerBlocks = { registerBlockCallbacks.add(it) },
                configAbyssalcraftPortals = BPConfig.abyssalcraftPortals.toConfiguration(),
            )
        }

        if (BPConfig.travelHutsPortals.enabled && Loader.isModLoaded("travelhut")) {
            de.johni0702.minecraft.betterportals.impl.travelhuts.common.initTravelHuts(
                mod = this,
                init = { commonInitCallbacks.add(it) },
                clientPreInit = { clientPreInitCallbacks.add(it) },
                registerBlocks = { registerBlockCallbacks.add(it) },
                configTravelHutsPortals = BPConfig.travelHutsPortals.toConfiguration(),
            )
        }
    }

    @Mod.EventHandler
    fun preInit(event: FMLPreInitializationEvent) {
        logger = event.modLog
        proxy.preInit(this)

        MinecraftForge.EVENT_BUS.register(this)
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    fun registerBlocks(event: RegistryEvent.Register<Block>) {
        with(event.registry) {
            registerBlockCallbacks.forEach { it() }
        }
    }

    @Mod.EventHandler
    fun init(event: FMLInitializationEvent) {
        proxy.init(this)
    }

    @Mod.EventHandler
    fun preInit(event: FMLPostInitializationEvent) {
        proxy.postInit(this)
    }

    interface Proxy {
        fun preInit(mod: BetterPortalsMod)

        fun init(mod: BetterPortalsMod)

        fun postInit(mod: BetterPortalsMod)
    }

    internal abstract class CommonProxy : Proxy {
        override fun preInit(mod: BetterPortalsMod) {}

        override fun init(mod: BetterPortalsMod) {
            commonInitCallbacks.forEach { it() }
        }

        override fun postInit(mod: BetterPortalsMod) {
            commonPostInitCallbacks.forEach { it() }
        }
    }

    @Suppress("unused")
    internal class ServerProxy : CommonProxy()

    @Suppress("unused")
    internal class ClientProxy : CommonProxy() {
        // Note: Even pre-init is too late
        init {
            // Forge appears to not be able to handle multiple source sets
            try {
                val field = Minecraft::class.java.getDeclaredField("defaultResourcePacks")
                field.isAccessible = true

                var root: File? = File(".").absoluteFile
                while (root != null && !File(root, "src").exists()) {
                    root = root.parentFile
                }

                if (root != null) {
                    val mc = Minecraft.getMinecraft()
                    @Suppress("UNCHECKED_CAST")
                    (field.get(mc) as MutableList<IResourcePack>).addAll(
                        listOf(
                            "portal",
                            "transition",
                        ).map { FolderResourcePack(File(root, "src/$it/resources")) },
                    )
                }
            } catch (ignored: NoSuchFieldException) {
            }
        }

        override fun preInit(mod: BetterPortalsMod) {
            clientPreInitCallbacks.forEach { it() }
        }

        override fun init(mod: BetterPortalsMod) {
            clientInitCallbacks.forEach { it() }
            super.init(mod)
        }

        override fun postInit(mod: BetterPortalsMod) {
            super.postInit(mod)
            clientPostInitCallbacks.forEach { it() }
        }
    }

    @SidedProxy(
        modId = MOD_ID,
        serverSide = $$"de.johni0702.minecraft.betterportals.impl.BetterPortalsMod$ServerProxy",
        clientSide = $$"de.johni0702.minecraft.betterportals.impl.BetterPortalsMod$ClientProxy",
    )
    lateinit var proxy: Proxy
}
