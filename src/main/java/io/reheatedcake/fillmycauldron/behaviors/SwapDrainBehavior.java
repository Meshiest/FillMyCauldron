package io.reheatedcake.fillmycauldron.behaviors;

import io.reheatedcake.fillmycauldron.core.DispenseResult;
import io.reheatedcake.fillmycauldron.core.DrainerBehavior;
import net.minecraft.block.AbstractCauldronBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;

/**
 * A simple behavior that replaces the target block, when full, with an empty
 * cauldron
 */
public class SwapDrainBehavior extends DrainerBehavior {
  private SoundEvent fillSound;
  private Item item;
  private Block cauldronType;

  public SwapDrainBehavior(Item item, Block cauldronType, SoundEvent fillSound) {
    this.fillSound = fillSound;
    this.cauldronType = cauldronType;
    this.item = item;
  }

  public Boolean canDrainCauldron(Block block) {
    return block.equals(cauldronType);
  }

  public Item getFilledItem() {
    return item;
  }

  public SoundEvent getFillSound() {
    return fillSound;
  }

  public DispenseResult tryDrainCauldron(ServerWorld world, BlockPos pos, BlockState state) {
    var cauldron = (AbstractCauldronBlock) state.getBlock();

    // cauldron is empty
    if (!cauldron.isFull(state)) {
      return DispenseResult.NOOP;
    }

    world.setBlockState(pos, Blocks.CAULDRON.getDefaultState());

    return DispenseResult.CONTINUE;
  }

}
