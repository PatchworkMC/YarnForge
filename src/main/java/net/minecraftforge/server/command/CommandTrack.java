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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.command.CommandException;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.server.timings.ForgeTimings;
import net.minecraftforge.server.timings.TimeTracker;

class CommandTrack
{
    private static final DecimalFormat TIME_FORMAT = new DecimalFormat("#####0.00");

    static ArgumentBuilder<ServerCommandSource, ?> register()
    {
        return CommandManager.literal("track")
            .then(StartTrackingCommand.register())
            .then(ResetTrackingCommand.register())
            .then(TrackResultsEntity.register())
            .then(TrackResultsTileEntity.register())
            .then(StartTrackingCommand.register());
    }

    private static class StartTrackingCommand
    {
        static ArgumentBuilder<ServerCommandSource, ?> register()
        {
            return CommandManager.literal("start")
                .requires(cs->cs.hasPermissionLevel(2)) //permission
                .then(CommandManager.literal("te")
                    .then(CommandManager.argument("duration", IntegerArgumentType.integer(1))
                        .executes(ctx -> {
                            int duration = IntegerArgumentType.getInteger(ctx, "duration");
                            TimeTracker.TILE_ENTITY_UPDATE.reset();
                            TimeTracker.TILE_ENTITY_UPDATE.enable(duration);
                            ctx.getSource().sendFeedback(new TranslatableText("commands.forge.tracking.te.enabled", duration), true);
                            return 0;
                        })
                    )
                )
                .then(CommandManager.literal("entity")
                    .then(CommandManager.argument("duration", IntegerArgumentType.integer(1))
                        .executes(ctx -> {
                            int duration = IntegerArgumentType.getInteger(ctx, "duration");
                            TimeTracker.ENTITY_UPDATE.reset();
                            TimeTracker.ENTITY_UPDATE.enable(duration);
                            ctx.getSource().sendFeedback(new TranslatableText("commands.forge.tracking.entity.enabled", duration), true);
                            return 0;
                        })
                    )
                );
        }
    }

    private static class ResetTrackingCommand
    {
        static ArgumentBuilder<ServerCommandSource, ?> register()
        {
            return CommandManager.literal("reset")
                .requires(cs->cs.hasPermissionLevel(2)) //permission
                .then(CommandManager.literal("te")
                    .executes(ctx -> {
                        TimeTracker.TILE_ENTITY_UPDATE.reset();
                        ctx.getSource().sendFeedback(new TranslatableText("commands.forge.tracking.te.reset"), true);
                        return 0;
                    })
                )
                .then(CommandManager.literal("entity")
                    .executes(ctx -> {
                        TimeTracker.ENTITY_UPDATE.reset();
                        ctx.getSource().sendFeedback(new TranslatableText("commands.forge.tracking.entity.reset"), true);
                        return 0;
                    })
                );
        }
    }

    private static class TrackResults
    {
        /**
         * Returns the time objects recorded by the time tracker sorted by average time
         *
         * @return A list of time objects
         */
        private static <T> List<ForgeTimings<T>> getSortedTimings(TimeTracker<T> tracker)
        {
            ArrayList<ForgeTimings<T>> list = new ArrayList<>();

            list.addAll(tracker.getTimingData());
            list.sort(Comparator.comparingDouble(ForgeTimings::getAverageTimings));
            Collections.reverse(list);

            return list;
        }

        private static <T> int execute(ServerCommandSource source, TimeTracker<T> tracker, Function<ForgeTimings<T>, Text> toString) throws CommandException
        {
            List<ForgeTimings<T>> timingsList = getSortedTimings(tracker);
            if (timingsList.isEmpty())
            {
                source.sendFeedback(new TranslatableText("commands.forge.tracking.no_data"), true);
            }
            else
            {
                timingsList.stream()
                        .filter(timings -> timings.getObject().get() != null)
                        .limit(10)
                        .forEach(timings -> source.sendFeedback(toString.apply(timings), true));
            }
            return 0;
        }
    }

    private static class TrackResultsEntity
    {
        static ArgumentBuilder<ServerCommandSource, ?> register()
        {
            return CommandManager.literal("entity").executes(ctx -> TrackResults.execute(ctx.getSource(), TimeTracker.ENTITY_UPDATE, data ->
                {
                    Entity entity = data.getObject().get();
                    if (entity == null)
                        return new TranslatableText("commands.forge.tracking.invalid");

                    BlockPos pos = entity.getBlockPos();
                    double averageTimings = data.getAverageTimings();
                    String tickTime = (averageTimings > 1000 ? TIME_FORMAT.format(averageTimings / 1000) : TIME_FORMAT.format(averageTimings)) + (averageTimings < 1000 ? "\u03bcs" : "ms");

                    return new TranslatableText("commands.forge.tracking.timing_entry", entity.getType().getRegistryName(), entity.world.getRegistryKey().getValue().toString(), pos.getX(), pos.getY(), pos.getZ(), tickTime);
                })
            );
        }
    }

    private static class TrackResultsTileEntity
    {
        static ArgumentBuilder<ServerCommandSource, ?> register()
        {
            return CommandManager.literal("te").executes(ctx -> TrackResults.execute(ctx.getSource(), TimeTracker.TILE_ENTITY_UPDATE, data ->
                {
                    BlockEntity te = data.getObject().get();
                    if (te == null)
                        return new TranslatableText("commands.forge.tracking.invalid");

                    BlockPos pos = te.getPos();

                    double averageTimings = data.getAverageTimings();
                    String tickTime = (averageTimings > 1000 ? TIME_FORMAT.format(averageTimings / 1000) : TIME_FORMAT.format(averageTimings)) + (averageTimings < 1000 ? "\u03bcs" : "ms");
                    return new TranslatableText("commands.forge.tracking.timing_entry", te.getType().getRegistryName(), te.getWorld().getRegistryKey().getValue().toString(), pos.getX(), pos.getY(), pos.getZ(), tickTime);
                })
            );
        }
    }
}
