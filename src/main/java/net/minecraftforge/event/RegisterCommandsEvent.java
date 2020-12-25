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

package net.minecraftforge.event;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.*;
import net.minecraft.resources.*;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;


/**
 * Commands are rebuilt whenever {@link DataPackRegistries} is recreated.
 * You can use this event to register your commands whenever the {@link net.minecraft.command.Commands} class in constructed.
 *
 * The event is fired on the {@link MinecraftForge#EVENT_BUS}
 */
public class RegisterCommandsEvent extends Event
{
    private final CommandDispatcher<ServerCommandSource> dispatcher;
    private final CommandManager.RegistrationEnvironment environment;
    
    public RegisterCommandsEvent(CommandDispatcher<ServerCommandSource> dispatcher, CommandManager.RegistrationEnvironment environment)
    {
        this.dispatcher = dispatcher;
        this.environment = environment;
    }
    
    public CommandDispatcher<ServerCommandSource> getDispatcher()
    {
        return dispatcher;
    }
    
    public CommandManager.RegistrationEnvironment getEnvironment()
    {
        return environment;
    }
}
