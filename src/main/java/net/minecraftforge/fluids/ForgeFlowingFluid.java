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

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import javax.annotation.Nullable;
import java.util.function.Supplier;

public abstract class ForgeFlowingFluid extends FlowableFluid
{
    private final Supplier<? extends Fluid> flowing;
    private final Supplier<? extends Fluid> still;
    @Nullable
    private final Supplier<? extends Item> bucket;
    @Nullable
    private final Supplier<? extends FluidBlock> block;
    private final FluidAttributes.Builder builder;
    private final boolean canMultiply;
    private final int slopeFindDistance;
    private final int levelDecreasePerBlock;
    private final float explosionResistance;
    private final int tickRate;

    protected ForgeFlowingFluid(Properties properties)
    {
        this.flowing = properties.flowing;
        this.still = properties.still;
        this.builder = properties.attributes;
        this.canMultiply = properties.canMultiply;
        this.bucket = properties.bucket;
        this.block = properties.block;
        this.slopeFindDistance = properties.slopeFindDistance;
        this.levelDecreasePerBlock = properties.levelDecreasePerBlock;
        this.explosionResistance = properties.explosionResistance;
        this.tickRate = properties.tickRate;
    }

    @Override
    public Fluid getFlowing()
    {
        return flowing.get();
    }

    @Override
    public Fluid getStill()
    {
        return still.get();
    }

    @Override
    protected boolean isInfinite()
    {
        return canMultiply;
    }

    @Override
    protected void beforeBreakingBlock(WorldAccess worldIn, BlockPos pos, BlockState state)
    {
        BlockEntity tileentity = state.getBlock().hasTileEntity(state) ? worldIn.getBlockEntity(pos) : null;
        Block.dropStacks(state, worldIn, pos, tileentity);
    }

    @Override
    protected int getFlowSpeed(WorldView worldIn)
    {
        return slopeFindDistance;
    }

    @Override
    protected int getLevelDecreasePerBlock(WorldView worldIn)
    {
        return levelDecreasePerBlock;
    }

    @Override
    public Item getBucketItem()
    {
        return bucket != null ? bucket.get() : Items.AIR;
    }

    @Override
    protected boolean canBeReplacedWith(FluidState state, BlockView world, BlockPos pos, Fluid fluidIn, Direction direction)
    {
        // Based on the water implementation, may need to be overriden for mod fluids that shouldn't behave like water.
        return direction == Direction.DOWN && !matchesType(fluidIn);
    }

    @Override
    public int getTickRate(WorldView world)
    {
        return tickRate;
    }

    @Override
    protected float getBlastResistance()
    {
        return explosionResistance;
    }

    @Override
    protected BlockState toBlockState(FluidState state)
    {
        if (block != null)
            return block.get().getDefaultState().with(FluidBlock.LEVEL, method_15741(state));
        return Blocks.AIR.getDefaultState();
    }

    @Override
    public boolean matchesType(Fluid fluidIn) {
        return fluidIn == still.get() || fluidIn == flowing.get();
    }

    @Override
    protected FluidAttributes createAttributes()
    {
        return builder.build(this);
    }

    public static class Flowing extends ForgeFlowingFluid
    {
        public Flowing(Properties properties)
        {
            super(properties);
            setDefaultState(getStateManager().getDefaultState().with(LEVEL, 7));
        }

        protected void appendProperties(StateManager.Builder<Fluid, FluidState> builder) {
            super.appendProperties(builder);
            builder.add(LEVEL);
        }

        public int getLevel(FluidState state) {
            return state.get(LEVEL);
        }

        public boolean isStill(FluidState state) {
            return false;
        }
    }

    public static class Source extends ForgeFlowingFluid
    {
        public Source(Properties properties)
        {
            super(properties);
        }

        public int getLevel(FluidState state) {
            return 8;
        }

        public boolean isStill(FluidState state) {
            return true;
        }
    }

    public static class Properties
    {
        private Supplier<? extends Fluid> still;
        private Supplier<? extends Fluid> flowing;
        private FluidAttributes.Builder attributes;
        private boolean canMultiply;
        private Supplier<? extends Item> bucket;
        private Supplier<? extends FluidBlock> block;
        private int slopeFindDistance = 4;
        private int levelDecreasePerBlock = 1;
        private float explosionResistance = 1;
        private int tickRate = 5;

        public Properties(Supplier<? extends Fluid> still, Supplier<? extends Fluid> flowing, FluidAttributes.Builder attributes)
        {
            this.still = still;
            this.flowing = flowing;
            this.attributes = attributes;
        }

        public Properties canMultiply()
        {
            canMultiply = true;
            return this;
        }

        public Properties bucket(Supplier<? extends Item> bucket)
        {
            this.bucket = bucket;
            return this;
        }

        public Properties block(Supplier<? extends FluidBlock> block)
        {
            this.block = block;
            return this;
        }

        public Properties slopeFindDistance(int slopeFindDistance)
        {
            this.slopeFindDistance = slopeFindDistance;
            return this;
        }

        public Properties levelDecreasePerBlock(int levelDecreasePerBlock)
        {
            this.levelDecreasePerBlock = levelDecreasePerBlock;
            return this;
        }

        public Properties explosionResistance(float explosionResistance)
        {
            this.explosionResistance = explosionResistance;
            return this;
        }

        public Properties tickRate(int tickRate)
        {
            this.tickRate = tickRate;
            return this;
        }
    }
}
