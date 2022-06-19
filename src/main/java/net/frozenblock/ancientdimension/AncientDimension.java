package net.frozenblock.ancientdimension;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.kyrptonaught.customportalapi.api.CustomPortalBuilder;
import net.minecraft.block.Blocks;
import net.minecraft.command.CommandBuildContext;
import net.minecraft.command.CommandException;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionOptions;

import net.minecraft.world.gen.chunk.FlatChunkGenerator;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.quiltmc.qsl.command.api.CommandRegistrationCallback;
import org.quiltmc.qsl.lifecycle.api.event.ServerLifecycleEvents;
import org.quiltmc.qsl.worldgen.dimension.api.QuiltDimensions;

import java.util.Objects;

public class AncientDimension implements ModInitializer, ServerLifecycleEvents.Ready, CommandRegistrationCallback {

	public static final String MOD_ID = "ancientdimension";

	public static final Identifier ANCIENT_DIMENSION = new Identifier(MOD_ID, "ancientdimension");

	private static final RegistryKey<DimensionOptions> DIMENSION_KEY = RegistryKey.of(Registry.DIMENSION_KEY,
			new Identifier("quiltdimension", "ancientdimension")
	);

	private static RegistryKey<World> WORLD_KEY = RegistryKey.of(Registry.WORLD_KEY, DIMENSION_KEY.getValue());

	@Override
	public void onInitialize(ModContainer mod) {
		Registry.register(Registry.CHUNK_GENERATOR, new Identifier("ancientdimension", "ancientdimension"), EmptyChunkGenerator.CODEC);

		WORLD_KEY = RegistryKey.of(Registry.WORLD_KEY, new Identifier("ancientdimension", "ancientdimension"));

		CustomPortalBuilder.beginPortal()
				.frameBlock(Blocks.REINFORCED_DEEPSLATE)
				.lightWithItem(Items.ECHO_SHARD)
				.destDimID(ANCIENT_DIMENSION)
				.tintColor(45,65,101)
				.forcedSize(20, 6)
				.returnDim(new Identifier("overworld"), true)
				.onlyLightInOverworld()
				.registerPortal();
	}

	@Override
	public void readyServer(MinecraftServer server) {
		ServerWorld overworld = server.getWorld(World.OVERWORLD);
		ServerWorld targetWorld = server.getWorld(WORLD_KEY);

		if (targetWorld == null) {
			throw new AssertionError("Test world somehow doesn't exist.");
		}

		CowEntity cow = EntityType.COW.create(overworld);

		assert cow != null;
		if (!cow.world.getRegistryKey().equals(World.OVERWORLD)) {
			throw new AssertionError("Cow was spawned but isn't in the overworld.");
		}

		var target = new TeleportTarget(Vec3d.ZERO, new Vec3d(1, 1, 1), 45f, 60f);
		CowEntity teleportedEntity = QuiltDimensions.teleport(cow, targetWorld, target);

		if (teleportedEntity == null || !teleportedEntity.world.getRegistryKey().equals(WORLD_KEY)) {
			throw new AssertionError("Cow was not teleported correctly.");
		}

		if (!teleportedEntity.getPos().equals(target.position)) {
			throw new AssertionError("Cow was moved to different world, but not to the correct location.");
		}
	}

	@Override
	public void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandBuildContext buildContext,
								 CommandManager.RegistrationEnvironment environment) {
		dispatcher.register(CommandManager.literal("quilt_dimension_test").executes(this::swapTargeted));
	}

	private int swapTargeted(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity player = context.getSource().getPlayer();
		ServerWorld serverWorld = player.getWorld();
		ServerWorld modWorld = context.getSource().getServer().getWorld(WORLD_KEY);

		if (serverWorld != modWorld) {
			var target = new TeleportTarget(new Vec3d(0.5, 101, 0.5), Vec3d.ZERO, 0, 0);
			assert modWorld != null;
			QuiltDimensions.teleport(player, modWorld, target);

			if (player.world != modWorld) {
				throw new CommandException(Text.literal("Teleportation failed!"));
			}


			modWorld.setBlockState(new BlockPos(0, 100, 0), Blocks.DIAMOND_BLOCK.getDefaultState());
			modWorld.setBlockState(new BlockPos(0, 101, 0), Blocks.TORCH.getDefaultState());
		} else {
			var target = new TeleportTarget(new Vec3d(0, 100, 0), Vec3d.ZERO,
					(float) Math.random() * 360 - 180, (float) Math.random() * 360 - 180);
			QuiltDimensions.teleport(player, Objects.requireNonNull(context.getSource().getServer().getWorld(World.END)), target);
		}

		return 1;
	}
}