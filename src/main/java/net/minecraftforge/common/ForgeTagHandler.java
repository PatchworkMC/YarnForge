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

package net.minecraftforge.common;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.resource.ResourceManager;
import net.minecraft.tag.RequiredTagList;
import net.minecraft.tag.RequiredTagListRegistry;
import net.minecraft.tag.Tag;
import net.minecraft.tag.Tag.Identified;
import net.minecraft.tag.TagGroup;
import net.minecraft.tag.TagGroupLoader;
import net.minecraft.tag.TagManager;
import net.minecraft.util.Identifier;
import net.minecraftforge.common.Tags.IOptionalNamedTag;
import net.minecraftforge.fml.network.FMLPlayMessages.SyncCustomTagTypes;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.RegistryManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ForgeTagHandler
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static Map<Identifier, TagGroup<?>> customTagTypes = Collections.emptyMap();
    private static Set<Identifier> customTagTypeNames = Collections.emptySet();
    private static boolean tagTypesSet = false;

    @Nullable
    private static <T extends IForgeRegistryEntry<T>> RequiredTagList<T> getTagRegistry(IForgeRegistry<T> registry)
    {
        return (RequiredTagList<T>) RequiredTagListRegistry.get(registry.getRegistryName());
    }

    private static void validateRegistrySupportsTags(IForgeRegistry<?> registry)
    {
        //Note: We also check against getTagRegistry in case someone decides to use the helpers for tag creation for types supported by vanilla
        if (getTagRegistry(registry) == null && (!(registry instanceof ForgeRegistry) || ((ForgeRegistry<?>) registry).getTagFolder() == null))
        {
            throw new IllegalArgumentException("Registry " + registry.getRegistryName() + " does not support tag types.");
        }
    }

    /**
     * Helper method that creates a named tag for a forge registry, erroring if the registry doesn't support custom tag types. If the custom tag types
     * have not been set yet, this method falls back and creates the tag reference delaying adding it to the tag registry to allow for statically
     * initializing and referencing the tag.
     * @param registry Registry the tag is for
     * @param name     Name of the tag
     * @param <T>      Type of the registry
     * @return A named tag
     */
    public static <T extends IForgeRegistryEntry<T>> Tag.Identified<T> makeWrapperTag(IForgeRegistry<T> registry, Identifier name)
    {
        validateRegistrySupportsTags(registry);
        if (tagTypesSet)
        {
            RequiredTagList<T> tagRegistry = getTagRegistry(registry);
            if (tagRegistry == null) throw new IllegalArgumentException("Registry " + registry.getRegistryName() + " does not support tag types.");
            return tagRegistry.add(name.toString());
        }
        return RequiredTagList.createDelayedTag(registry.getRegistryName(), name);
    }

    /**
     * Helper method that creates an optional tag for a forge registry, erroring if the registry doesn't support custom tag types. If the custom tag types
     * have not been set yet, this method falls back and creates the tag reference delaying adding it to the tag registry to allow for statically
     * initializing and referencing the tag.
     * @param registry Registry the tag is for
     * @param name     Name of the tag
     * @param <T>      Type of the registry
     * @return An optional tag
     */
    public static <T extends IForgeRegistryEntry<T>> IOptionalNamedTag<T> createOptionalTag(IForgeRegistry<T> registry, Identifier name)
    {
        return createOptionalTag(registry, name, null);
    }

    /**
     * Helper method that creates an optional tag for a forge registry, erroring if the registry doesn't support custom tag types. If the custom tag types
     * have not been set yet, this method falls back and creates the tag reference delaying adding it to the tag registry to allow for statically
     * initializing and referencing the tag.
     * @param registry Registry the tag is for
     * @param name     Name of the tag
     * @param defaults Default values for the optional tag
     * @param <T>      Type of the registry
     * @return An optional tag
     */
    public static <T extends IForgeRegistryEntry<T>> IOptionalNamedTag<T> createOptionalTag(IForgeRegistry<T> registry, Identifier name, @Nullable Set<Supplier<T>> defaults)
    {
        validateRegistrySupportsTags(registry);
        if (tagTypesSet)
        {
            RequiredTagList<T> tagRegistry = getTagRegistry(registry);
            if (tagRegistry == null) throw new IllegalArgumentException("Registry " + registry.getRegistryName() + " does not support tag types.");
            return tagRegistry.createOptional(name, defaults);
        }
        return RequiredTagList.createDelayedOptional(registry.getRegistryName(), name, defaults);
    }

    /**
     * Helper method for creating named tags for custom forge registries. If the custom tag types have not been set yet, this method falls back and creates
     * the tag reference delaying adding it to the tag registry to allow for statically initializing and referencing the tag.
     * @param registryName Name of the registry the tag is for
     * @param name         Name of the tag
     * @param <T>          Type of the registry
     * @return A named tag
     * @implNote This method only errors instantly if tag types have already been set, otherwise the error is delayed until after registries finish initializing
     * and we can validate if the custom registry really does support custom tags.
     */
    public static <T extends IForgeRegistryEntry<T>> Tag.Identified<T> makeWrapperTag(Identifier registryName, Identifier name)
    {
        if (tagTypesSet)
        {
            IForgeRegistry<T> registry = RegistryManager.ACTIVE.getRegistry(registryName);
            if (registry == null) throw new IllegalArgumentException("Could not find registry named: " + registryName);
            return makeWrapperTag(registry, name);
        }
        return RequiredTagList.createDelayedTag(registryName, name);
    }

    /**
     * Helper method for creating optional tags for custom forge registries. If the custom tag types have not been set yet, this method falls back and creates
     * the tag reference delaying adding it to the tag registry to allow for statically initializing and referencing the tag.
     * @param registryName Name of the registry the tag is for
     * @param name         Name of the tag
     * @param <T>          Type of the registry
     * @return An optional tag
     * @implNote This method only errors instantly if tag types have already been set, otherwise the error is delayed until after registries finish initializing
     * and we can validate if the custom registry really does support custom tags.
     */
    public static <T extends IForgeRegistryEntry<T>> IOptionalNamedTag<T> createOptionalTag(Identifier registryName, Identifier name)
    {
        return createOptionalTag(registryName, name, null);
    }

    /**
     * Helper method for creating optional tags for custom forge registries. If the custom tag types have not been set yet, this method falls back and creates
     * the tag reference delaying adding it to the tag registry to allow for statically initializing and referencing the tag.
     * @param registryName Name of the registry the tag is for
     * @param name         Name of the tag
     * @param defaults     Default values for the optional tag
     * @param <T>          Type of the registry
     * @return An optional tag
     * @implNote This method only errors instantly if tag types have already been set, otherwise the error is delayed until after registries finish initializing
     * and we can validate if the custom registry really does support custom tags.
     */
    public static <T extends IForgeRegistryEntry<T>> IOptionalNamedTag<T> createOptionalTag(Identifier registryName, Identifier name, @Nullable Set<Supplier<T>> defaults)
    {
        if (tagTypesSet)
        {
            IForgeRegistry<T> registry = RegistryManager.ACTIVE.getRegistry(registryName);
            if (registry == null) throw new IllegalArgumentException("Could not find registry named: " + registryName);
            return createOptionalTag(registry, name, defaults);
        }
        return RequiredTagList.createDelayedOptional(registryName, name, defaults);
    }

    /**
     * Gets the all the registry names of registries that support custom tag types.
     */
    public static Set<Identifier> getCustomTagTypeNames()
    {
        return customTagTypeNames;
    }

    /**
     * Gets a map of registry name to tag collection for all custom tag types.
     *
     * @apiNote Prefer interacting with this via the current {@link ITagCollectionSupplier} and using one of the forge extension getCustomTypeCollection methods
     */
    public static Map<Identifier, TagGroup<?>> getCustomTagTypes()
    {
        return customTagTypes;
    }

    /**
     * Sets the set containing the resource locations representing the registry name of each forge registry that supports custom tag types.
     *
     * @apiNote Internal: Calling this manually <strong>WILL</strong> cause a crash to occur as it can only be called once, and is done so by
     * forge after all registries have been initialized.
     */
    public static void setCustomTagTypes(Set<Identifier> customTagTypes)
    {
        if (tagTypesSet) throw new RuntimeException("Custom tag types have already been set, this method should only be called by forge, and after registries are initialized");
        tagTypesSet = true;
        customTagTypeNames = ImmutableSet.copyOf(customTagTypes);
        //Add the static references for custom tag types to the proper tag registries
        // Note: If this ends up being a hotspot due to lots of mods having lots of statically registered tags
        // that get loaded/registered before the new registry event is fired/processed everywhere then this
        // potentially should end up being moved into an async processor.
        RequiredTagList.performDelayedAdd();
    }

    /**
     * Creates a map for custom tag type to tag reader
     *
     * @apiNote Internal: For use by NetworkTagManager
     */
    public static Map<Identifier, TagGroupLoader<?>> createCustomTagTypeReaders()
    {
        LOGGER.debug("Gathering custom tag collection reader from types.");
        ImmutableMap.Builder<Identifier, TagGroupLoader<?>> builder = ImmutableMap.builder();
        for (Identifier registryName : customTagTypeNames)
        {
            ForgeRegistry<?> registry = RegistryManager.ACTIVE.getRegistry(registryName);
            if (registry != null && registry.getTagFolder() != null)
            {
                builder.put(registryName, new TagGroupLoader<>(rl -> Optional.ofNullable(registry.getValue(rl)), "tags/" + registry.getTagFolder(), registryName.getPath()));
            }
        }
        return builder.build();
    }

    /**
     * Resets the cached collections for the various custom tag types.
     *
     * @apiNote Internal
     */
    public static void resetCachedTagCollections(boolean makeEmpty, boolean withOptional)
    {
        ImmutableMap.Builder<Identifier, TagGroup<?>> builder = ImmutableMap.builder();
        for (Identifier registryName : customTagTypeNames)
        {
            RequiredTagList<?> tagRegistry = RequiredTagListRegistry.get(registryName);
            if (tagRegistry != null)
            {
                if (makeEmpty)
                {
                    if (withOptional)
                        builder.put(registryName, tagRegistry.reinjectOptionalTags(TagGroup.create(Collections.emptyMap())));
                    else
                        builder.put(registryName, TagGroup.create(Collections.emptyMap()));
                }
                else
                {
                    builder.put(registryName, TagGroup.create(tagRegistry.getTags().stream().distinct().collect(Collectors.toMap(Identified::getId, namedTag -> namedTag))));
                }
            }
        }
        customTagTypes = builder.build();
    }

    /**
     * Used to ensure that all custom tag types have a defaulted collection when vanilla is initializing a defaulted TagCollectionManager
     *
     * @apiNote Internal: For use by TagCollectionManager
     */
    public static TagManager populateTagCollectionManager(TagGroup<Block> blockTags, TagGroup<Item> itemTags, TagGroup<Fluid> fluidTags, TagGroup<EntityType<?>> entityTypeTags)
    {
        //Default the tag collections
        resetCachedTagCollections(false, false);
        if (!customTagTypes.isEmpty())
        {
            LOGGER.debug("Populated the TagCollectionManager with {} extra types", customTagTypes.size());
        }
        return TagManager.create(blockTags, itemTags, fluidTags, entityTypeTags);
    }

    /**
     * Updates the custom tag types' tags from reloading via NetworkTagManager
     *
     * @apiNote Internal: For use by NetworkTagManager
     */
    public static void updateCustomTagTypes(List<TagCollectionReaderInfo> tagCollectionReaders)
    {
        ImmutableMap.Builder<Identifier, TagGroup<?>> builder = ImmutableMap.builder();
        for (TagCollectionReaderInfo info : tagCollectionReaders)
        {
            builder.put(info.tagType, info.reader.applyReload(info.tagBuilders));
        }
        customTagTypes = builder.build();
    }

    /**
     * Updates the custom tag types' tags from packet
     *
     * @apiNote Internal
     */
    public static void updateCustomTagTypes(SyncCustomTagTypes packet)
    {
        customTagTypes = packet.getCustomTagTypes();
        reinjectOptionalTagsCustomTypes();
    }

    /**
     * Gets the completable future containing the reload results for all custom tag types.
     *
     * @apiNote Internal: For use by NetworkTagManager
     */
    public static CompletableFuture<List<TagCollectionReaderInfo>> getCustomTagTypeReloadResults(ResourceManager resourceManager, Executor backgroundExecutor, Map<Identifier, TagGroupLoader<?>> readers)
    {
        CompletableFuture<List<TagCollectionReaderInfo>> customResults = CompletableFuture.completedFuture(new ArrayList<>());
        for (Map.Entry<Identifier, TagGroupLoader<?>> entry : readers.entrySet())
        {
            customResults = customResults.thenCombine(entry.getValue().prepareReload(resourceManager, backgroundExecutor), (results, result) -> {
                results.add(new TagCollectionReaderInfo(entry.getKey(), entry.getValue(), result));
                return results;
            });
        }
        return customResults;
    }

    /**
     * Add all the missing optional tags back into the custom tag types tag collections
     *
     * @apiNote Internal
     */
    public static void reinjectOptionalTagsCustomTypes()
    {
        ImmutableMap.Builder<Identifier, TagGroup<?>> builder = ImmutableMap.builder();
        for (Entry<Identifier, TagGroup<?>> entry : customTagTypes.entrySet())
        {
            Identifier registry = entry.getKey();
            RequiredTagList<?> tagRegistry = RequiredTagListRegistry.get(registry);
            TagGroup<?> tagCollection = entry.getValue();
            builder.put(registry, tagRegistry == null ? tagCollection : tagRegistry.reinjectOptionalTags((TagGroup) tagCollection));
        }
        customTagTypes = builder.build();
    }

    /**
     * Gets an {@link ITagCollectionSupplier} with empty custom tag type collections to allow for checking if the client is requiring any tags of custom tag types.
     *
     * @apiNote Internal: For use with validating missing tags when connecting to a vanilla server
     */
    public static TagManager withNoCustom(TagManager tagCollectionSupplier)
    {
        ImmutableMap.Builder<Identifier, TagGroup<?>> builder = ImmutableMap.builder();
        for (Identifier registryName : customTagTypeNames)
        {
            RequiredTagList<?> tagRegistry = RequiredTagListRegistry.get(registryName);
            if (tagRegistry != null)
            {
                builder.put(registryName, TagGroup.create(Collections.emptyMap()));
            }
        }
        return withSpecificCustom(tagCollectionSupplier, builder.build());
    }

    /**
     * Gets an {@link ITagCollectionSupplier} with specific custom tag types for testing if any tags are missing.
     *
     * @apiNote Internal
     */
    public static TagManager withSpecificCustom(TagManager tagCollectionSupplier, Map<Identifier, TagGroup<?>> customTagTypes)
    {
        return new TagManager()
        {
            @Override
            public TagGroup<Block> getBlocks()
            {
                return tagCollectionSupplier.getBlocks();
            }

            @Override
            public TagGroup<Item> getItems()
            {
                return tagCollectionSupplier.getItems();
            }

            @Override
            public TagGroup<Fluid> getFluids()
            {
                return tagCollectionSupplier.getFluids();
            }

            @Override
            public TagGroup<EntityType<?>> getEntityTypes()
            {
                return tagCollectionSupplier.getEntityTypes();
            }

            @Override
            public Map<Identifier, TagGroup<?>> getCustomTagTypes()
            {
                return customTagTypes;
            }
        };
    }

    /**
     * Helper storage class for keeping track of various data for all custom tag types in the NetworkTagReader to make the code easier to read.
     *
     * @apiNote Internal: For use by NetworkTagManager
     */
    public static class TagCollectionReaderInfo
    {

        private final Identifier tagType;
        private final TagGroupLoader<?> reader;
        private final Map<Identifier, Tag.Builder> tagBuilders;

        private TagCollectionReaderInfo(Identifier tagType, TagGroupLoader<?> reader, Map<Identifier, Tag.Builder> tagBuilders)
        {
            this.tagType = tagType;
            this.reader = reader;
            this.tagBuilders = tagBuilders;
        }
    }
}