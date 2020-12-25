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

import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.BaseText;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Language;
import net.minecraftforge.fml.network.ConnectionType;
import net.minecraftforge.fml.network.NetworkHooks;

public class TextComponentHelper
{
    private TextComponentHelper() {}

    /**
     * Detects when sending to a vanilla client and falls back to sending english,
     * since they don't have the lang data necessary to translate on the client.
     */
    public static BaseText createComponentTranslation(CommandOutput source, final String translation, final Object... args)
    {
        if (isVanillaClient(source))
        {
            return new LiteralText(String.format(Language.getInstance().get(translation), args));
        }
        return new TranslatableText(translation, args);
    }

    private static boolean isVanillaClient(CommandOutput sender)
    {
        if (sender instanceof ServerPlayerEntity)
        {
            ServerPlayerEntity playerMP = (ServerPlayerEntity) sender;
            ServerPlayNetworkHandler channel = playerMP.networkHandler;
            return NetworkHooks.getConnectionType(()->channel.connection) == ConnectionType.VANILLA;
        }
        return false;
    }
}
