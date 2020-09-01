package flaxbeard.immersivepetroleum;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import flaxbeard.immersivepetroleum.api.crafting.PumpjackHandler;
import flaxbeard.immersivepetroleum.client.ClientProxy;
import flaxbeard.immersivepetroleum.common.CommonEventHandler;
import flaxbeard.immersivepetroleum.common.CommonProxy;
import flaxbeard.immersivepetroleum.common.IPConfig;
import flaxbeard.immersivepetroleum.common.IPContent;
import flaxbeard.immersivepetroleum.common.IPContent.Fluids;
import flaxbeard.immersivepetroleum.common.IPSaveData;
import flaxbeard.immersivepetroleum.common.crafting.Serializers;
import flaxbeard.immersivepetroleum.common.network.IPPacketHandler;
import flaxbeard.immersivepetroleum.common.util.commands.ReservoirCommand;
import net.minecraft.command.Commands;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ImmersivePetroleum.MODID)
public class ImmersivePetroleum{
	public static final String MODID = "immersivepetroleum";

	public static final Logger log=LogManager.getLogger(MODID);
	
	public static final ItemGroup creativeTab = new ItemGroup(MODID){
		@Override
		public ItemStack createIcon(){
			return new ItemStack(Fluids.crudeOil.getFilledBucket());
		}
	};

	public static CommonProxy proxy=DistExecutor.runForDist(()->ClientProxy::new, ()->CommonProxy::new);

	public static ImmersivePetroleum INSTANCE;

	public ImmersivePetroleum(){
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, IPConfig.ALL);
		
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::loadComplete);
		
		MinecraftForge.EVENT_BUS.addListener(this::serverStarting);
		MinecraftForge.EVENT_BUS.addListener(this::serverAboutToStart);
		MinecraftForge.EVENT_BUS.addListener(this::serverStarted);
		
		Serializers.RECIPE_SERIALIZERS.register(FMLJavaModLoadingContext.get().getModEventBus());
		
		IPContent.populate();
		
		proxy.construct();
		proxy.registerContainersAndScreens();
	}
	
	public void setup(FMLCommonSetupEvent event){
		proxy.setup();
		
		// ---------------------------------------------------------------------------------------------------------------------------------------------
		
		proxy.preInit();
		
		IPContent.preInit();
		IPPacketHandler.preInit();
		
		proxy.preInitEnd();
		
		// ---------------------------------------------------------------------------------------------------------------------------------------------
		
		IPContent.init();
		
		MinecraftForge.EVENT_BUS.register(new CommonEventHandler());
		proxy.init();
		
		// ---------------------------------------------------------------------------------------------------------------------------------------------
		
		proxy.postInit();
		
		PumpjackHandler.recalculateChances(true);
	}
	
	public void loadComplete(FMLLoadCompleteEvent event){
		proxy.completed();
	}
	
	public void serverAboutToStart(FMLServerAboutToStartEvent event){
		proxy.serverAboutToStart();
	}

	public void serverStarting(FMLServerStartingEvent event){
		proxy.serverStarting();
		
		event.getCommandDispatcher().register(Commands.literal("ip").then(ReservoirCommand.create()));
	}
	
	public void serverStarted(FMLServerStartedEvent event){
		proxy.serverStarted();
		
		ServerWorld world = event.getServer().getWorld(DimensionType.OVERWORLD);
		if(!world.isRemote){
			IPSaveData worldData = world.getSavedData().getOrCreate(IPSaveData::new, IPSaveData.dataName);
			IPSaveData.setInstance(worldData);
		}
		
		PumpjackHandler.recalculateChances(true);
	}
}
