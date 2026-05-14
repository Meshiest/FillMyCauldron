package io.reheatedcake.fillmycauldron.behaviors;

import io.reheatedcake.fillmycauldron.core.DispenseResult;
import io.reheatedcake.fillmycauldron.core.DrainerBehavior;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Item;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.BlockPos;

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

  public boolean canDrainCauldron(Block block) {
    return block.equals(cauldronType);
  }

  public Item getFilledItem() {
    return item;
  }

  public SoundEvent getFillSound() {
    return fillSound;
  }

  public DispenseResult tryDrainCauldron(ServerLevel world, BlockPos pos, BlockState state) {
    var cauldron = (AbstractCauldronBlock) state.getBlock();

    // cauldron is empty
    if (!cauldron.isFull(state)) {
      return DispenseResult.NOOP;
    }

    world.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());

    return DispenseResult.CONTINUE;
  }

}
