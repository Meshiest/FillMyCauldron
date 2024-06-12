package io.reheatedcake.fillmycauldron.behaviors;

import io.reheatedcake.fillmycauldron.core.DispenseResult;
import io.reheatedcake.fillmycauldron.core.FillCauldronBehavior;
import net.minecraft.block.AbstractCauldronBlock;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;

/** A simple behavior that replaces an empty cauldron with a desired cauldron */
public class SwapFillBehavior extends FillCauldronBehavior {
  private BlockState fillBlockState;

  public SwapFillBehavior(Item emptyItem, BlockState fillBlockState, Item item, SoundEvent fillSound) {
    super(emptyItem, item, fillSound);
    this.fillBlockState = fillBlockState;
  }

  @Override
  protected DispenseResult tryFillCauldron(ServerWorld world, BlockPos pos, BlockState state, ItemStack stack) {
    var cauldron = (AbstractCauldronBlock) state.getBlock();

    // cauldron is full - cannot keep filling
    if (cauldron.isFull(state)) {
      return DispenseResult.NOOP;
    }

    world.setBlockState(pos, fillBlockState);

    return DispenseResult.CONTINUE;
  }
}
