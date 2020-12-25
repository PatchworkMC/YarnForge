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

package net.minecraftforge.common.util;

import java.lang.ref.WeakReference;
import java.util.Objects;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import javax.annotation.Nullable;

/**
 * Represents a captured snapshot of a block which will not change
 * automatically.
 * <p>
 * Unlike Block, which only one object can exist per coordinate, BlockSnapshot
 * can exist multiple times for any given Block.
 */
public class BlockSnapshot
{
    private static final boolean DEBUG = Boolean.parseBoolean(System.getProperty("forge.debugBlockSnapshot", "false"));

    private final RegistryKey<World> dim;
    private final BlockPos pos;
    private final int flags;
    private final BlockState block;
    @Nullable
    private final CompoundTag nbt;

    @Nullable
    private WeakReference<WorldAccess> world;
    private String toString = null;

    private BlockSnapshot(RegistryKey<World> dim, WorldAccess world, BlockPos pos, BlockState state, @Nullable CompoundTag nbt, int flags)
    {
        this.dim = dim;
        this.pos = pos.toImmutable();
        this.block = state;
        this.flags = flags;
        this.nbt = nbt;

        this.world = new WeakReference<>(world);

        if (DEBUG)
            System.out.println("Created " + this.toString());
    }

    public static BlockSnapshot create(RegistryKey<World> dim, WorldAccess world, BlockPos pos)
    {
        return create(dim, world, pos, 3);
    }

    public static BlockSnapshot create(RegistryKey<World> dim, WorldAccess world, BlockPos pos, int flag)
    {
        return new BlockSnapshot(dim, world, pos, world.getBlockState(pos), getTileNBT(world.getBlockEntity(pos)), flag);
    }

    @Nullable
    private static CompoundTag getTileNBT(@Nullable BlockEntity te)
    {
        return te == null ? null : te.toTag(new CompoundTag());
    }

    public BlockState getCurrentBlock()
    {
        WorldAccess world = getWorld();
        return world == null ? Blocks.AIR.getDefaultState() : world.getBlockState(this.pos);
    }

    @Nullable
    public WorldAccess getWorld()
    {
        WorldAccess world = this.world != null ? this.world.get() : null;
        if (world == null)
        {
            world = ServerLifecycleHooks.getCurrentServer().getWorld(this.dim);
            this.world = new WeakReference<WorldAccess>(world);
        }
        return world;
    }

    public BlockState getReplacedBlock()
    {
        return this.block;
    }

    @Nullable
    public BlockEntity getTileEntity()
    {
        return getNbt() != null ? BlockEntity.createFromTag(getReplacedBlock(), getNbt()) : null;
    }

    public boolean restore()
    {
        return restore(false);
    }

    public boolean restore(boolean force)
    {
        return restore(force, true);
    }

    public boolean restore(boolean force, boolean notifyNeighbors)
    {
        return restoreToLocation(getWorld(), getPos(), force, notifyNeighbors);
    }

    public boolean restoreToLocation(WorldAccess world, BlockPos pos, boolean force, boolean notifyNeighbors)
    {
        BlockState current = getCurrentBlock();
        BlockState replaced = getReplacedBlock();

        int flags = notifyNeighbors ? Constants.BlockFlags.DEFAULT : Constants.BlockFlags.BLOCK_UPDATE;

        if (current != replaced)
        {
            if (force)
                world.setBlockState(pos, replaced, flags);
            else
                return false;
        }

        world.setBlockState(pos, replaced, flags);
        if (world instanceof World)
            ((World)world).updateListeners(pos, current, replaced, flags);

        BlockEntity te = null;
        if (getNbt() != null)
        {
            te = world.getBlockEntity(pos);
            if (te != null)
            {
                te.fromTag(getReplacedBlock(), getNbt());
                te.markDirty();
            }
        }

        if (DEBUG)
            System.out.println("Restored " + this.toString());
        return true;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        final BlockSnapshot other = (BlockSnapshot) obj;
        return this.dim.equals(other.dim) &&
            this.pos.equals(other.pos) &&
            this.block == other.block &&
            this.flags == other.flags &&
            Objects.equals(this.nbt, other.nbt);
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 73 * hash + this.dim.hashCode();
        hash = 73 * hash + this.pos.hashCode();
        hash = 73 * hash + this.block.hashCode();
        hash = 73 * hash + this.flags;
        hash = 73 * hash + Objects.hashCode(this.getNbt());
        return hash;
    }

    @Override
    public String toString()
    {
        if (toString == null)
        {
            this.toString =
                "BlockSnapshot[" +
                "World:" + this.dim.getValue() + ',' +
                "Pos: " + this.pos + ',' +
                "State: " + this.block + ',' +
                "Flags: " + this.flags + ',' +
                "NBT: " + (this.nbt == null ? "null" : this.nbt.toString()) +
                ']';
        }
        return this.toString;
    }

    public BlockPos getPos() { return pos; }


    public int getFlag() { return flags; }

    @Nullable
    public CompoundTag getNbt() { return nbt; }

}
