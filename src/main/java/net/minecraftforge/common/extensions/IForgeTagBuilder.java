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

package net.minecraftforge.common.extensions;

import net.minecraft.data.server.AbstractTagProvider;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.RegistryKey;

//TODO, Tag removal support.
public interface IForgeTagBuilder<T>
{

    default AbstractTagProvider.ObjectBuilder<T> getBuilder() {
        return (AbstractTagProvider.ObjectBuilder<T>) this;
    }

    @SuppressWarnings("unchecked")
    default AbstractTagProvider.ObjectBuilder<T> addTags(Tag.Identified<T>... values) {
        AbstractTagProvider.ObjectBuilder<T> builder = getBuilder();
        for (Tag.Identified<T> value : values) {
            builder.addTag(value);
        }
        return builder;
    }

    default AbstractTagProvider.ObjectBuilder<T> add(RegistryKey<T>... keys) {
        AbstractTagProvider.ObjectBuilder<T> builder = getBuilder();
        for (RegistryKey<T> key : keys) {
            builder.getInternalBuilder().add(key.getValue(), getBuilder().getModID());
        }
        return builder;
    }

    default AbstractTagProvider.ObjectBuilder<T> replace() {
        return replace(true);
    }

    default AbstractTagProvider.ObjectBuilder<T> replace(boolean value) {
        getBuilder().getInternalBuilder().replace(value);
        return getBuilder();
    }

    default AbstractTagProvider.ObjectBuilder<T> addOptional(final Identifier location)
    {
        return getBuilder().add(new Tag.OptionalObjectEntry(location));
    }

    default AbstractTagProvider.ObjectBuilder<T> addOptionalTag(final Identifier location)
    {
        return getBuilder().add(new Tag.OptionalTagEntry(location));
    }
}
