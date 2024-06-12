package io.reheatedcake.fillmycauldron.core;

import net.minecraft.block.AbstractCauldronBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.ItemDispenserBehavior;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.event.GameEvent;

/**
 * A generic dispenser behavior for a bucket item to replace the contents of a
 * cauldron and yield an empty bucket
 */
public class FillCauldronBehavior extends ItemDispenserBehavior {
  private ItemDispenserBehavior fallbackBehavior = new ItemDispenserBehavior();
  private SoundEvent fillSound;
  private Item emptyItem;

  public FillCauldronBehavior(Item emptyItem, Item fullItem, SoundEvent fillSound) {
    this.fillSound = fillSound;
    this.emptyItem = emptyItem;

    if (DispenserBlock.BEHAVIORS.containsKey(fullItem)) {
      ItemDispenserBehavior foundBehavior = (ItemDispenserBehavior) DispenserBlock.BEHAVIORS.get(fullItem);
      if (foundBehavior != null) {
        this.fallbackBehavior = foundBehavior;
      }
    }
  }

  private ItemStack replace(BlockPointer pointer, ItemStack oldStack, ItemStack newStack) {
    pointer.world().emitGameEvent(null, GameEvent.FLUID_PLACE, pointer.pos());
    return this.decrementStackWithRemainder(pointer, oldStack, newStack);
  }

  protected DispenseResult tryFillCauldron(ServerWorld world, BlockPos pos, BlockState state, ItemStack stack) {
    return DispenseResult.NOOP;
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
    worldAccess.playSound(null, blockPos, fillSound, SoundCategory.BLOCKS, 1.0f, 1.0f);

    // replace the full bucket with an empty one
    return this.replace(pointer, stack, this.emptyItem.getDefaultStack());
  }

}
