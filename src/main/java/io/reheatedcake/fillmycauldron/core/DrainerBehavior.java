package io.reheatedcake.fillmycauldron.core;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;

/**
 * The `DrainerBehavior` class represents behaviors for converting an empty
 * container into a filled container by interacting with a cauldron block.
 */
public interface DrainerBehavior {
  /** Check if a given cauldron block can be drained */
  public Boolean canDrainCauldron(Block block);

  /** Get the bucket item for the drainable */
  public Item getFilledItem();

  /** Get the sound to play when filling the cauldron */
  public SoundEvent getFillSound();

  /** Attempt to drain the bucket into the cauldron */
  public DispenseResult tryDrainCauldron(ServerWorld world, BlockPos pos);
}
