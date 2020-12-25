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

package net.minecraftforge.common.crafting;

import java.util.stream.Stream;

import com.google.gson.JsonObject;

import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;

public class VanillaIngredientSerializer implements IIngredientSerializer<Ingredient>
{
    public static final VanillaIngredientSerializer INSTANCE  = new VanillaIngredientSerializer();

    public Ingredient parse(PacketByteBuf buffer)
    {
        return Ingredient.ofEntries(Stream.generate(() -> new Ingredient.StackEntry(buffer.readItemStack())).limit(buffer.readVarInt()));
    }

    public Ingredient parse(JsonObject json)
    {
       return Ingredient.ofEntries(Stream.of(Ingredient.entryFromJson(json)));
    }

    public void write(PacketByteBuf buffer, Ingredient ingredient)
    {
        ItemStack[] items = ingredient.getMatchingStacksClient();
        buffer.writeVarInt(items.length);

        for (ItemStack stack : items)
            buffer.writeItemStack(stack);
    }
}
