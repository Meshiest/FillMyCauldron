package io.reheatedcake.fillmycauldron.behaviors;

import io.reheatedcake.fillmycauldron.core.DispenseResult;
import io.reheatedcake.fillmycauldron.core.FillCauldronBehavior;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.BlockPos;

/** A simple behavior that replaces an empty cauldron with a desired cauldron */
public class SwapFillBehavior extends FillCauldronBehavior {
  private BlockState fillBlockState;

  public SwapFillBehavior(Item emptyItem, BlockState fillBlockState, Item item, SoundEvent fillSound) {
    super(emptyItem, item, fillSound);
    this.fillBlockState = fillBlockState;
  }

  @Override
  protected DispenseResult tryFillCauldron(ServerLevel world, BlockPos pos, BlockState state, ItemStack stack) {
    var cauldron = (AbstractCauldronBlock) state.getBlock();

    // cauldron is full - cannot keep filling
    if (cauldron.isFull(state)) {
      return DispenseResult.NOOP;
    }

    world.setBlockAndUpdate(pos, fillBlockState);

    return DispenseResult.CONTINUE;
  }
}
