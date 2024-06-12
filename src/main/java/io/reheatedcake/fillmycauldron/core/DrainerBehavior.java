package io.reheatedcake.fillmycauldron.core;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;

/**
 * The `DrainerBehavior` class represents behaviors for converting an empty
 * container into a filled container by interacting with a cauldron block.
 */
public abstract class DrainerBehavior {
  /** Check if a given cauldron block can be drained */
  public abstract Boolean canDrainCauldron(Block block);

  /** Get the fileld item for the drainable */
  public abstract Item getFilledItem();

  /**
   * Get an item stack for the drainable (may contain more information than just
   * the item)
   */
  public ItemStack getFilledStack() {
    return getFilledItem().getDefaultStack();
  }

  /** Check if the item stack is applicable to this drainer */
  protected Boolean checkItem(ItemStack stack) {
    return true;
  }

  /** Get the sound to play when filling the cauldron */
  public abstract SoundEvent getFillSound();

  /** Attempt to drain the bucket into the cauldron */
  public abstract DispenseResult tryDrainCauldron(ServerWorld world, BlockPos pos, BlockState state);
}
