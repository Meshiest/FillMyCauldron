package io.reheatedcake.fillmycauldron.behaviors;

import io.reheatedcake.fillmycauldron.core.DrainerBehavior;
import io.reheatedcake.fillmycauldron.core.DispenseResult;
import net.fabricmc.fabric.api.transfer.v1.fluid.CauldronFluidContent;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.impl.transfer.fluid.CauldronStorage;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;

public class FluidDrainBehavior extends DrainerBehavior {
  private Fluid fluid;
  private SoundEvent fillSound;
  private Item item;
  private long fluidAmount;

  public FluidDrainBehavior(Fluid fluid, Item item, long amount, SoundEvent fillSound) {
    this.fillSound = fillSound;
    this.item = item;
    this.fluid = fluid;
    this.fluidAmount = amount;
  }

  public FluidDrainBehavior(Fluid fluid, Item item, long amount) {
    this(fluid, item, amount, FluidVariantAttributes.getFillSound(FluidVariant.of(fluid)));
  }

  public Boolean canDrainCauldron(Block block) {
    var content = CauldronFluidContent.getForBlock(block);
    return content != null && content.fluid.equals(fluid);
  }

  public Item getFilledItem() {
    return item;
  }

  public SoundEvent getFillSound() {
    return fillSound;
  }

  public DispenseResult tryDrainCauldron(ServerWorld world, BlockPos pos, BlockState state) {
    CauldronStorage storage = CauldronStorage.get(world, pos);

    // cauldron is empty or does not have enough fluid
    if (storage.isResourceBlank() || storage.getAmount() < fluidAmount) {
      return DispenseResult.NOOP;
    }

    // unexpected fluid
    if (!storage.getResource().getFluid().equals(fluid)) {
      return DispenseResult.NOOP;
    }

    // fill cauldron behavior
    try (var transaction = Transaction.openOuter()) {

      // not enough fluid to fill the item
      if (storage.extract(storage.getResource(), fluidAmount, transaction) != fluidAmount) {
        return DispenseResult.NOOP;
      }

      // save the fluid transfer
      transaction.commit();

      return DispenseResult.CONTINUE;
    }
  }

}