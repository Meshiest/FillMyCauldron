package io.reheatedcake.fillmycauldron.behaviors;

import io.reheatedcake.fillmycauldron.core.DispenseResult;
import io.reheatedcake.fillmycauldron.core.FillCauldronBehavior;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.impl.transfer.fluid.CauldronStorage;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class FluidFillBehavior extends FillCauldronBehavior {
  private Fluid fluid;
  private FluidVariant fluidResource;
  private long fluidAmount;

  public FluidFillBehavior(Fluid fluid, Item item, long amount) {
    super(item, FluidVariantAttributes.getEmptySound(FluidVariant.of(fluid)));
    this.fluid = fluid;
    this.fluidResource = FluidVariant.of(fluid);
    this.fluidAmount = amount;
  }

  @Override
  protected DispenseResult tryFillCauldron(ServerWorld world, BlockPos pos) {
    // get the cauldron's fluid storage
    CauldronStorage storage = CauldronStorage.get(world, pos);

    // dispenser is attempting to fill a cauldron with a different fluid
    if (!storage.isResourceBlank() && !storage.getResource().getFluid().equals(fluid)) {
      return DispenseResult.NOOP;
    }

    // dispenser is attempting to overfill the cauldron
    if (storage.getAmount() + fluidAmount > storage.getCapacity()) {
      return DispenseResult.NOOP;
    }

    // attempt to insert the fluid into the cauldron
    try (var transaction = Transaction.openOuter()) {
      // check if the amount inserted into the cauldron is not exactly as configured
      if (storage.insert(fluidResource, fluidAmount, transaction) != fluidAmount) {
        return DispenseResult.NOOP;
      }

      // save the fluid transfer
      transaction.commit();

      return DispenseResult.CONTINUE;
    }
  }
}
