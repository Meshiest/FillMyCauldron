package io.reheatedcake.fillmycauldron.core;

import java.util.Map;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.gameevent.GameEvent;

public class DrainCauldronBehavior extends DefaultDispenseItemBehavior {
  private DefaultDispenseItemBehavior fallbackBehavior = new DefaultDispenseItemBehavior();
  public final Map<Item, DrainerBehavior> BEHAVIORS = new Object2ObjectOpenHashMap<>();

  public DrainCauldronBehavior(Item container) {
    if (DispenserBlock.DISPENSER_REGISTRY.containsKey(container)) {
      DefaultDispenseItemBehavior foundBehavior = (DefaultDispenseItemBehavior) DispenserBlock.DISPENSER_REGISTRY
          .get(container);
      if (foundBehavior != null) {
        this.fallbackBehavior = foundBehavior;
      }
    }
  }

  /** Register a new drainable to the set of available drain behaviors */
  public void registerBehavior(DrainerBehavior drainer) {
    BEHAVIORS.put(drainer.getFilledItem(), drainer);
  }

  /** Find the first drain behavior for the given block */
  private DrainerBehavior findDrainer(Block block, ItemStack stack) {
    for (DrainerBehavior drainer : BEHAVIORS.values()) {
      if (drainer.canDrainCauldron(block) && drainer.checkItem(stack)) {
        return drainer;
      }
    }
    return null;
  }

  private ItemStack replace(BlockSource pointer, ItemStack oldStack, ItemStack newStack) {
    pointer.level().gameEvent(null, GameEvent.FLUID_PLACE, pointer.pos());
    return this.consumeWithRemainder(pointer, oldStack, newStack);
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

    // find a drainable behavior for the cauldron
    var drainer = findDrainer(block, stack);
    if (drainer == null) {
      return fallbackBehavior.dispense(pointer, stack);
    }

    // attempt to drain the cauldron
    try {
      switch (drainer.tryDrainCauldron(worldAccess, blockPos, blockState)) {
        case NOOP:
          return stack;
        case FALLBACK:
          return fallbackBehavior.dispense(pointer, stack);
        case CONTINUE:
          break;
      }
    } catch (Exception e) {
      FillCauldronBehavior.LOGGER.error("Failed to drain cauldron", e);
      return fallbackBehavior.dispense(pointer, stack);
    }

    // play the filled container sound
    worldAccess.playSound(null, blockPos, drainer.getFillSound(), SoundSource.BLOCKS, 1.0f, 1.0f);

    // replace the empty item with the filled one
    return this.replace(pointer, stack, drainer.getFilledStack());
  }
}
