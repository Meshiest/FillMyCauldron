package io.reheatedcake.fillmycauldron;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reheatedcake.fillmycauldron.behaviors.FluidDrainBehavior;
import io.reheatedcake.fillmycauldron.behaviors.FluidFillBehavior;
import io.reheatedcake.fillmycauldron.core.DrainCauldronBehavior;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.transfer.v1.fluid.CauldronFluidContent;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.block.DispenserBlock;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;

public class FillMyCauldron implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("fill-my-cauldron");

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing FillMyCauldron");

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
			DispenserBlock.registerBehavior(bucket, new FluidFillBehavior(fluid, bucket, FluidConstants.BUCKET));

			// register empty bucket + full cauldron = full bucket behavior
			drainWithBucket.registerBehavior(new FluidDrainBehavior(fluid, bucket, FluidConstants.BUCKET));
		}

		// Add a behavior to the dispenser for emptying cauldrons using an empty bucket
		DispenserBlock.registerBehavior(Items.BUCKET, drainWithBucket);
	}
}