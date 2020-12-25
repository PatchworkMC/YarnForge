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

package net.minecraftforge.client.extensions;

import java.util.Collection;
import java.util.function.Function;
import net.minecraft.client.texture.Sprite;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import com.google.common.collect.ImmutableList;

public interface IForgeTextureAtlasSprite
{
    /**
     * The result of this function determines is the below 'load' function is called, and the
     * default vanilla loading code is bypassed completely.
     * @param manager Main resource manager
     * @param location File resource location
     * @return True to use your own custom load code and bypass vanilla loading.
     */
    default boolean hasCustomLoader(ResourceManager manager, Identifier location)
    {
        return false;
    }

    /**
     * Load the specified resource as this sprite's data.
     * Returning false from this function will prevent this icon from being stitched onto the master texture.
     * @param manager Main resource manager
     * @param location File resource location
     * @param textureGetter accessor for dependencies. All of them will be loaded before this one
     * @return False to prevent this Icon from being stitched
     */
    default boolean load(ResourceManager manager, Identifier location, Function<Identifier, Sprite> textureGetter)
    {
        return true;
    }

    /**
     * @return all textures that should be loaded before this texture.
     */
    default Collection<Identifier> getDependencies() 
    {
        return ImmutableList.of();
    }
}
