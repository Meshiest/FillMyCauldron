package io.reheatedcake.fillmycauldron;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.impl.transfer.fluid.CauldronStorage;
import net.fabricmc.fabric.mixin.transfer.BucketItemAccessor;
import net.minecraft.block.AbstractCauldronBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.ItemDispenserBehavior;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.event.GameEvent;

public class FillMyCauldron implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("fill-my-cauldron");

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing FillMyCauldron");

		var fallbackDrainBehavior = (ItemDispenserBehavior) DispenserBlock.BEHAVIORS.get(Items.WATER_BUCKET);
		var newDrainBehavior = new ItemDispenserBehavior() {
			private ItemStack replace(BlockPointer pointer, ItemStack oldStack, ItemStack newStack) {
				pointer.world().emitGameEvent(null, GameEvent.FLUID_PLACE, pointer.pos());
				return this.decrementStackWithRemainder(pointer, oldStack, newStack);
			}

			@Override
			public ItemStack dispenseSilently(BlockPointer pointer, ItemStack stack) {
				BlockPos blockPos;
				ServerWorld worldAccess = pointer.world();
				BlockState blockState = worldAccess
						.getBlockState(blockPos = pointer.pos().offset(pointer.state().get(DispenserBlock.FACING)));
				Block block = blockState.getBlock();

				// default behavior for non-cauldron blocks
				if (!(block instanceof AbstractCauldronBlock)) {
					return fallbackDrainBehavior.dispense(pointer, stack);
				}

				// get the cauldron's fluid storage
				CauldronStorage storage = CauldronStorage.get(worldAccess, blockPos);

				// cauldron is not empty
				if (!storage.isResourceBlank() || storage.getAmount() != 0)
					return stack;

				// empty cauldron behavior
				try (var transaction = Transaction.openOuter()) {
					var bucketFluid = ((BucketItemAccessor) stack.getItem()).fabric_getFluid();
					var resource = FluidVariant.of(bucketFluid);

					// amount the cauldron filled is not exactly 1 bucket
					if (storage.insert(resource, FluidConstants.BUCKET, transaction) != FluidConstants.BUCKET) {
						return stack;
					}

					// save the fluid transfer
					transaction.commit();

					// play the fill sound
					worldAccess.playSound(null, blockPos, FluidVariantAttributes.getEmptySound(resource),
							SoundCategory.BLOCKS, 1.0f, 1.0f);

					// replace the full bucket with an empty one
					return this.replace(pointer, stack, Items.BUCKET.getDefaultStack());
				}
			}
		};

		// register the new behavior for all fluid buckets
		for (var fluid : Registries.FLUID) {
			var bucket = fluid.getBucketItem();
			if (bucket == null)
				continue;
			DispenserBlock.registerBehavior(bucket, newDrainBehavior);
		}

		var fallbackFillBehavior = (ItemDispenserBehavior) DispenserBlock.BEHAVIORS.get(Items.BUCKET);
		DispenserBlock.registerBehavior(Items.BUCKET, new ItemDispenserBehavior() {
			private ItemStack replace(BlockPointer pointer, ItemStack oldStack, ItemStack newStack) {
				pointer.world().emitGameEvent(null, GameEvent.FLUID_PICKUP, pointer.pos());
				return this.decrementStackWithRemainder(pointer, oldStack, newStack);
			}

			@Override
			public ItemStack dispenseSilently(BlockPointer pointer, ItemStack stack) {
				BlockPos blockPos;
				ServerWorld worldAccess = pointer.world();
				BlockState blockState = worldAccess
						.getBlockState(blockPos = pointer.pos().offset(pointer.state().get(DispenserBlock.FACING)));
				Block block = blockState.getBlock();

				// default behavior for non-cauldron blocks
				if (!(block instanceof AbstractCauldronBlock)) {
					return fallbackFillBehavior.dispense(pointer, stack);
				}

				// get the cauldron's fluid storage
				CauldronStorage storage = CauldronStorage.get(worldAccess, blockPos);

				// cauldron is empty
				if (storage.isResourceBlank() || storage.getAmount() == 0)
					return stack;

				var resource = storage.getResource();
				var bucket = resource.getFluid().getBucketItem();

				// no bucket for the fluid
				if (bucket == null)
					return stack;

				try (var transaction = Transaction.openOuter()) {

					// not enough fluid to fill the bucket
					if (storage.extract(resource, FluidConstants.BUCKET, transaction) != FluidConstants.BUCKET) {
						return stack;
					}

					// save the fluid transfer
					transaction.commit();

					// play the fill sound
					worldAccess.playSound(null, blockPos, FluidVariantAttributes.getFillSound(resource),
							SoundCategory.BLOCKS, 1.0f, 1.0f);

					// replace the bucket with the filled one
					return this.replace(pointer, stack, bucket.getDefaultStack());
				}
			}
		});

	}
}