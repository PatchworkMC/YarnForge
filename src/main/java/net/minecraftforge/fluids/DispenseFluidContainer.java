/*
 * Minecraft Forge
 * Copyright (c) 2016-2020.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.minecraftforge.fluids;

import javax.annotation.Nonnull;

import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.ItemDispenserBehavior;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

/**
 * Fills or drains a fluid container item using a Dispenser.
 */
public class DispenseFluidContainer extends ItemDispenserBehavior
{
    private static final DispenseFluidContainer INSTANCE = new DispenseFluidContainer();

    public static DispenseFluidContainer getInstance()
    {
        return INSTANCE;
    }

    private DispenseFluidContainer() {}

    private final ItemDispenserBehavior dispenseBehavior = new ItemDispenserBehavior();

    @Override
    @Nonnull
    public ItemStack dispenseSilently(@Nonnull BlockPointer source, @Nonnull ItemStack stack)
    {
        if (FluidUtil.getFluidContained(stack).isPresent())
        {
            return dumpContainer(source, stack);
        }
        else
        {
            return fillContainer(source, stack);
        }
    }

    /**
     * Picks up fluid in front of a Dispenser and fills a container with it.
     */
    @Nonnull
    private ItemStack fillContainer(@Nonnull BlockPointer source, @Nonnull ItemStack stack)
    {
        World world = source.getWorld();
        Direction dispenserFacing = source.getBlockState().get(DispenserBlock.FACING);
        BlockPos blockpos = source.getBlockPos().offset(dispenserFacing);

        FluidActionResult actionResult = FluidUtil.tryPickUpFluid(stack, null, world, blockpos, dispenserFacing.getOpposite());
        ItemStack resultStack = actionResult.getResult();

        if (!actionResult.isSuccess() || resultStack.isEmpty())
        {
            return super.dispenseSilently(source, stack);
        }

        if (stack.getCount() == 1)
        {
            return resultStack;
        }
        else if (((DispenserBlockEntity)source.getBlockEntity()).addToFirstFreeSlot(resultStack) < 0)
        {
            this.dispenseBehavior.dispense(source, resultStack);
        }

        ItemStack stackCopy = stack.copy();
        stackCopy.decrement(1);
        return stackCopy;
    }

    /**
     * Drains a filled container and places the fluid in front of the Dispenser.
     */
    @Nonnull
    private ItemStack dumpContainer(BlockPointer source, @Nonnull ItemStack stack)
    {
        ItemStack singleStack = stack.copy();
        singleStack.setCount(1);
        IFluidHandlerItem fluidHandler = FluidUtil.getFluidHandler(singleStack).orElse(null);
        if (fluidHandler == null)
        {
            return super.dispenseSilently(source, stack);
        }

        FluidStack fluidStack = fluidHandler.drain(FluidAttributes.BUCKET_VOLUME, IFluidHandler.FluidAction.EXECUTE);
        Direction dispenserFacing = source.getBlockState().get(DispenserBlock.FACING);
        BlockPos blockpos = source.getBlockPos().offset(dispenserFacing);
        FluidActionResult result = FluidUtil.tryPlaceFluid(null, source.getWorld(), Hand.MAIN_HAND, blockpos, stack, fluidStack);

        if (result.isSuccess())
        {
            ItemStack drainedStack = result.getResult();

            if (drainedStack.getCount() == 1)
            {
                return drainedStack;
            }
            else if (!drainedStack.isEmpty() && ((DispenserBlockEntity)source.getBlockEntity()).addToFirstFreeSlot(drainedStack) < 0)
            {
                this.dispenseBehavior.dispense(source, drainedStack);
            }

            ItemStack stackCopy = drainedStack.copy();
            stackCopy.decrement(1);
            return stackCopy;
        }
        else
        {
            return this.dispenseBehavior.dispense(source, stack);
        }
    }
}
