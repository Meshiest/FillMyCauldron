package io.reheatedcake.fillmycauldron;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reheatedcake.fillmycauldron.behaviors.FluidDrainBehavior;
import io.reheatedcake.fillmycauldron.behaviors.FluidFillBehavior;
import io.reheatedcake.fillmycauldron.behaviors.SwapDrainBehavior;
import io.reheatedcake.fillmycauldron.behaviors.SwapFillBehavior;
import io.reheatedcake.fillmycauldron.core.DrainCauldronBehavior;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.transfer.v1.fluid.CauldronFluidContent;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.block.Blocks;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potions;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;

public class FillMyCauldron implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("fill-my-cauldron");

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing FillMyCauldron");

		registerBucketBehaviors();
		registerBottleBehaviors();
	}

	private void registerBucketBehaviors() {
		var drainWithBucket = new DrainCauldronBehavior(Items.BUCKET);

		// register the new behavior for all fluid buckets
		for (var fluid : Registries.FLUID) {
			// an empty cauldron is already filled with air...
			if (fluid == Fluids.EMPTY) {
				continue;
			}

			var bucket = fluid.getBucketItem();
			var cauldron = CauldronFluidContent.getForFluid(fluid);
			// ensure the fluid has a bucket and every fluid is compatible with the cauldron
			if (bucket == null || cauldron == null)
				continue;

			LOGGER.info("Registering {} as the bucket for the {} block via {}", bucket,
					cauldron.block.asItem(),
					FluidVariant.of(fluid).getRegistryEntry().getIdAsString());

			// register full bucket + empty cauldron = full cauldron behavior
			DispenserBlock.registerBehavior(bucket,
					new FluidFillBehavior(Items.BUCKET, fluid, bucket, FluidConstants.BUCKET));

			// register empty bucket + full cauldron = full bucket behavior
			drainWithBucket.registerBehavior(new FluidDrainBehavior(fluid, bucket, FluidConstants.BUCKET));
		}

		// fill empty bucket with snow behavior
		drainWithBucket.registerBehavior(new SwapDrainBehavior(Items.POWDER_SNOW_BUCKET, Blocks.POWDER_SNOW_CAULDRON,
				SoundEvents.ITEM_BUCKET_FILL_POWDER_SNOW));
		// empty snow bucket into cauldron behavior
		DispenserBlock.registerBehavior(Items.POWDER_SNOW_BUCKET, new SwapFillBehavior(Items.BUCKET,
				Blocks.POWDER_SNOW_CAULDRON.getDefaultState().with(LeveledCauldronBlock.LEVEL, 3), Items.POWDER_SNOW_BUCKET,
				SoundEvents.ITEM_BUCKET_EMPTY_POWDER_SNOW));

		// Add a behavior to the dispenser for emptying cauldrons using an empty bucket
		DispenserBlock.registerBehavior(Items.BUCKET, drainWithBucket);
	}

	private void registerBottleBehaviors() {
		// Add a behavior to the dispenser for filling cauldrons using a glass bottle
		DispenserBlock.registerBehavior(Items.POTION,
				new FluidFillBehavior(Items.GLASS_BOTTLE, Fluids.WATER, Items.POTION, FluidConstants.BOTTLE,
						SoundEvents.ITEM_BOTTLE_EMPTY) {
					@Override
					protected Boolean checkItem(ItemStack stack) {
						PotionContentsComponent component = (PotionContentsComponent) stack.get(DataComponentTypes.POTION_CONTENTS);
						return component != null && component.matches(Potions.WATER);
					}
				});

		// Add a behavior to the dispenser for emptying cauldrons using a glass bottle
		var drainWithBottle = new DrainCauldronBehavior(Items.GLASS_BOTTLE);
		drainWithBottle.registerBehavior(
				new FluidDrainBehavior(Fluids.WATER, Items.POTION, FluidConstants.BOTTLE, SoundEvents.ITEM_BOTTLE_FILL) {
					@Override
					public ItemStack getFilledStack() {
						return PotionContentsComponent.createStack(Items.POTION, Potions.WATER);
					}
				});
		DispenserBlock.registerBehavior(Items.GLASS_BOTTLE, drainWithBottle);
	}
}