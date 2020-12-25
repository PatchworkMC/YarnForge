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

package net.minecraftforge.client;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.Maps;
import net.minecraft.client.render.item.ItemModels;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.registries.IRegistryDelegate;

/**
 * Wrapper around ItemModeMesher that cleans up the internal maps to respect ID remapping.
 */
public class ItemModelMesherForge extends ItemModels
{
    final Map<IRegistryDelegate<Item>, ModelIdentifier> locations = Maps.newHashMap();
    final Map<IRegistryDelegate<Item>, BakedModel> models = Maps.newHashMap();

    public ItemModelMesherForge(BakedModelManager manager)
    {
        super(manager);
    }

    @Override
    @Nullable
    public BakedModel getModel(Item item)
    {
        return models.get(item.delegate);
    }

    @Override
    public void putModel(Item item, ModelIdentifier location)
    {
        IRegistryDelegate<Item> key = item.delegate;
        locations.put(key, location);
        models.put(key, getModelManager().getModel(location));
    }

    @Override
    public void reloadModels()
    {
        final BakedModelManager manager = this.getModelManager();
        for (Map.Entry<IRegistryDelegate<Item>, ModelIdentifier> e : locations.entrySet())
        {
        	models.put(e.getKey(), manager.getModel(e.getValue()));
        }
    }

    public ModelIdentifier getLocation(@Nonnull ItemStack stack)
    {
        ModelIdentifier location = locations.get(stack.getItem().delegate);

        if (location == null)
        {
            location = ModelLoader.MISSING;
        }

        return location;
    }
}
