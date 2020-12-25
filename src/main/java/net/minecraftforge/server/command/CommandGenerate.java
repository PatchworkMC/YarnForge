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

package net.minecraftforge.server.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.command.CommandException;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.WorldWorkerManager;

class CommandGenerate
{
    static ArgumentBuilder<ServerCommandSource, ?> register()
    {
        return CommandManager.literal("generate")
            .requires(cs->cs.hasPermissionLevel(4)) //permission
            .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                .then(CommandManager.argument("count", IntegerArgumentType.integer(1))
                    .then(CommandManager.argument("dim", DimensionArgumentType.dimension())
                        .then(CommandManager.argument("interval", IntegerArgumentType.integer())
                            .executes(ctx -> execute(ctx.getSource(), BlockPosArgumentType.getBlockPos(ctx, "pos"), getInt(ctx, "count"), DimensionArgumentType.getDimensionArgument(ctx, "dim"), getInt(ctx, "interval")))
                        )
                        .executes(ctx -> execute(ctx.getSource(), BlockPosArgumentType.getBlockPos(ctx, "pos"), getInt(ctx, "count"), DimensionArgumentType.getDimensionArgument(ctx, "dim"), -1))
                    )
                    .executes(ctx -> execute(ctx.getSource(), BlockPosArgumentType.getBlockPos(ctx, "pos"), getInt(ctx, "count"), ctx.getSource().getWorld(), -1))
                )
            );
    }

    private static int getInt(CommandContext<ServerCommandSource> ctx, String name)
    {
        return IntegerArgumentType.getInteger(ctx, name);
    }

    private static int execute(ServerCommandSource source, BlockPos pos, int count, ServerWorld dim, int interval) throws CommandException
    {
        BlockPos chunkpos = new BlockPos(pos.getX() >> 4, 0, pos.getZ() >> 4);

        ChunkGenWorker worker = new ChunkGenWorker(source, chunkpos, count, dim, interval);
        source.sendFeedback(worker.getStartMessage(source), true);
        WorldWorkerManager.addWorker(worker);

        return 0;
    }
}
