package io.reheatedcake.fillmycauldron.core;

import java.util.Map;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.AbstractCauldronBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.ItemDispenserBehavior;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.event.GameEvent;

public class DrainCauldronBehavior extends ItemDispenserBehavior {
  private ItemDispenserBehavior fallbackBehavior = new ItemDispenserBehavior();
  public final Map<Item, DrainerBehavior> BEHAVIORS = new Object2ObjectOpenHashMap<>();

  public DrainCauldronBehavior(Item container) {
    if (DispenserBlock.BEHAVIORS.containsKey(container)) {
      ItemDispenserBehavior foundBehavior = (ItemDispenserBehavior) DispenserBlock.BEHAVIORS.get(container);
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

  private ItemStack replace(BlockPointer pointer, ItemStack oldStack, ItemStack newStack) {
    pointer.world().emitGameEvent(null, GameEvent.FLUID_PLACE, pointer.pos());
    return this.decrementStackWithRemainder(pointer, oldStack, newStack);
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
    worldAccess.playSound(null, blockPos, drainer.getFillSound(), SoundCategory.BLOCKS, 1.0f, 1.0f);

    // replace the empty item with the filled one
    return this.replace(pointer, stack, drainer.getFilledStack());
  }
}
