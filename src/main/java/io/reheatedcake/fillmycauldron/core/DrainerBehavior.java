package io.reheatedcake.fillmycauldron.core;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.BlockPos;

/**
 * The `DrainerBehavior` class represents behaviors for converting an empty
 * container into a filled container by interacting with a cauldron block.
 */
public abstract class DrainerBehavior {
  /** Check if a given cauldron block can be drained */
  public abstract boolean canDrainCauldron(Block block);

  /** Get the fileld item for the drainable */
  public abstract Item getFilledItem();

  /**
   * Get an item stack for the drainable (may contain more information than just
   * the item)
   */
  public ItemStack getFilledStack() {
    return getFilledItem().getDefaultInstance();
  }

  /** Check if the item stack is applicable to this drainer */
  protected boolean checkItem(ItemStack stack) {
    return true;
  }

  /** Get the sound to play when filling the cauldron */
  public abstract SoundEvent getFillSound();

  /** Attempt to drain the bucket into the cauldron */
  public abstract DispenseResult tryDrainCauldron(ServerLevel world, BlockPos pos, BlockState state);
}
