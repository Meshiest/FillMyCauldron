package io.reheatedcake.fillmycauldron.core;

import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * A generic dispenser behavior for a bucket item to replace the contents of a
 * cauldron and yield an empty bucket
 */
public class FillCauldronBehavior extends DefaultDispenseItemBehavior {
  private DefaultDispenseItemBehavior fallbackBehavior = new DefaultDispenseItemBehavior();
  private SoundEvent fillSound;
  private Item emptyItem;

  public FillCauldronBehavior(Item emptyItem, Item fullItem, SoundEvent fillSound) {
    this.fillSound = fillSound;
    this.emptyItem = emptyItem;

    if (DispenserBlock.DISPENSER_REGISTRY.containsKey(fullItem)) {
      DefaultDispenseItemBehavior foundBehavior = (DefaultDispenseItemBehavior) DispenserBlock.DISPENSER_REGISTRY
          .get(fullItem);
      if (foundBehavior != null) {
        this.fallbackBehavior = foundBehavior;
      }
    }
  }

  private ItemStack replace(BlockSource pointer, ItemStack oldStack, ItemStack newStack) {
    pointer.level().gameEvent(null, GameEvent.FLUID_PLACE, pointer.pos());
    return this.consumeWithRemainder(pointer, oldStack, newStack);
  }

  protected DispenseResult tryFillCauldron(ServerLevel world, BlockPos pos, BlockState state, ItemStack stack) {
    return DispenseResult.NOOP;
  }

  @Override
  public ItemStack execute(BlockSource pointer, ItemStack stack) {
    BlockPos blockPos;
    ServerLevel worldAccess = pointer.level();
    BlockState blockState = worldAccess
        .getBlockState(blockPos = pointer.pos().relative(pointer.state().getValue(DispenserBlock.FACING)));
    Block block = blockState.getBlock();

    // default behavior for non-cauldron blocks
    if (!(block instanceof AbstractCauldronBlock)) {
      return fallbackBehavior.dispense(pointer, stack);
    }

    try {
      switch (tryFillCauldron(worldAccess, blockPos, blockState, stack)) {
        case NOOP:
          return stack;
        case FALLBACK:
          return fallbackBehavior.dispense(pointer, stack);
        case CONTINUE:
          break;
      }
    } catch (Exception e) {
      FillCauldronBehavior.LOGGER.error("Failed to fill cauldron", e);
      return fallbackBehavior.dispense(pointer, stack);
    }

    // play the fill sound
    worldAccess.playSound(null, blockPos, fillSound, SoundSource.BLOCKS, 1.0f, 1.0f);

    // replace the full bucket with an empty one
    return this.replace(pointer, stack, this.emptyItem.getDefaultInstance());
  }

}
