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

package net.minecraftforge.registries;

import net.minecraft.Bootstrap;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.Schedule;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.decoration.painting.PaintingMotive;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.fluid.Fluid;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.village.VillagerProfession;
import net.minecraft.item.Item;
import net.minecraft.particle.ParticleType;
import net.minecraft.potion.Potion;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.sound.SoundEvent;
import net.minecraft.stat.StatType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.carver.Carver;
import net.minecraft.world.gen.decorator.Decorator;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraft.world.gen.foliage.FoliagePlacerType;
import net.minecraft.world.gen.placer.BlockPlacerType;
import net.minecraft.world.gen.stateprovider.BlockStateProviderType;
import net.minecraft.world.gen.surfacebuilder.SurfaceBuilder;
import net.minecraft.world.gen.tree.TreeDecoratorType;
import net.minecraft.world.poi.PointOfInterestType;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.common.world.ForgeWorldType;
import net.minecraftforge.fml.common.registry.GameRegistry;

/**
 * A class that exposes static references to all vanilla and Forge registries.
 * Created to have a central place to access the registries directly if modders need.
 * It is still advised that if you are registering things to go through {@link GameRegistry} register methods, but queries and iterations can use this.
 */
public class ForgeRegistries
{
    static { init(); } // This must be above the fields so we guarantee it's run before findRegistry is called. Yay static inializers

    // Game objects
    public static final IForgeRegistry<Block> BLOCKS = RegistryManager.ACTIVE.getRegistry(Block.class);
    public static final IForgeRegistry<Fluid> FLUIDS = RegistryManager.ACTIVE.getRegistry(Fluid.class);
    public static final IForgeRegistry<Item> ITEMS = RegistryManager.ACTIVE.getRegistry(Item.class);
    public static final IForgeRegistry<StatusEffect> POTIONS = RegistryManager.ACTIVE.getRegistry(StatusEffect.class);
    public static final IForgeRegistry<SoundEvent> SOUND_EVENTS = RegistryManager.ACTIVE.getRegistry(SoundEvent.class);
    public static final IForgeRegistry<Potion> POTION_TYPES = RegistryManager.ACTIVE.getRegistry(Potion.class);
    public static final IForgeRegistry<Enchantment> ENCHANTMENTS = RegistryManager.ACTIVE.getRegistry(Enchantment.class);
    public static final IForgeRegistry<EntityType<?>> ENTITIES = RegistryManager.ACTIVE.getRegistry(EntityType.class);
    public static final IForgeRegistry<BlockEntityType<?>> TILE_ENTITIES = RegistryManager.ACTIVE.getRegistry(BlockEntityType.class);
    public static final IForgeRegistry<ParticleType<?>> PARTICLE_TYPES = RegistryManager.ACTIVE.getRegistry(ParticleType.class);
    public static final IForgeRegistry<ScreenHandlerType<?>> CONTAINERS = RegistryManager.ACTIVE.getRegistry(ScreenHandlerType.class);
    public static final IForgeRegistry<PaintingMotive> PAINTING_TYPES = RegistryManager.ACTIVE.getRegistry(PaintingMotive.class);
    public static final IForgeRegistry<RecipeSerializer<?>> RECIPE_SERIALIZERS = RegistryManager.ACTIVE.getRegistry(RecipeSerializer.class);
    public static final IForgeRegistry<EntityAttribute> ATTRIBUTES = RegistryManager.ACTIVE.getRegistry(EntityAttribute.class);
    public static final IForgeRegistry<StatType<?>> STAT_TYPES = RegistryManager.ACTIVE.getRegistry(StatType.class);

    // Villages
    public static final IForgeRegistry<VillagerProfession> PROFESSIONS = RegistryManager.ACTIVE.getRegistry(VillagerProfession.class);
    public static final IForgeRegistry<PointOfInterestType> POI_TYPES = RegistryManager.ACTIVE.getRegistry(PointOfInterestType.class);
    public static final IForgeRegistry<MemoryModuleType<?>> MEMORY_MODULE_TYPES = RegistryManager.ACTIVE.getRegistry(MemoryModuleType.class);
    public static final IForgeRegistry<SensorType<?>> SENSOR_TYPES = RegistryManager.ACTIVE.getRegistry(SensorType.class);
    public static final IForgeRegistry<Schedule> SCHEDULES = RegistryManager.ACTIVE.getRegistry(Schedule.class);
    public static final IForgeRegistry<Activity> ACTIVITIES = RegistryManager.ACTIVE.getRegistry(Activity.class);

    // Worldgen
    public static final IForgeRegistry<Carver<?>> WORLD_CARVERS = RegistryManager.ACTIVE.getRegistry(Carver.class);
    public static final IForgeRegistry<SurfaceBuilder<?>> SURFACE_BUILDERS = RegistryManager.ACTIVE.getRegistry(SurfaceBuilder.class);
    public static final IForgeRegistry<Feature<?>> FEATURES = RegistryManager.ACTIVE.getRegistry(Feature.class);
    public static final IForgeRegistry<Decorator<?>> DECORATORS = RegistryManager.ACTIVE.getRegistry(Decorator.class);
    public static final IForgeRegistry<ChunkStatus> CHUNK_STATUS = RegistryManager.ACTIVE.getRegistry(ChunkStatus.class);
    public static final IForgeRegistry<StructureFeature<?>> STRUCTURE_FEATURES = RegistryManager.ACTIVE.getRegistry(StructureFeature.class);
    public static final IForgeRegistry<BlockStateProviderType<?>> BLOCK_STATE_PROVIDER_TYPES = RegistryManager.ACTIVE.getRegistry(BlockStateProviderType.class);
    public static final IForgeRegistry<BlockPlacerType<?>> BLOCK_PLACER_TYPES = RegistryManager.ACTIVE.getRegistry(BlockPlacerType.class);
    public static final IForgeRegistry<FoliagePlacerType<?>> FOLIAGE_PLACER_TYPES = RegistryManager.ACTIVE.getRegistry(FoliagePlacerType.class);
    public static final IForgeRegistry<TreeDecoratorType<?>> TREE_DECORATOR_TYPES = RegistryManager.ACTIVE.getRegistry(TreeDecoratorType.class);

    // Dynamic/Data driven.
    public static final IForgeRegistry<Biome> BIOMES = RegistryManager.ACTIVE.getRegistry(Keys.BIOMES);

    // Custom forge registries
    public static final IForgeRegistry<DataSerializerEntry> DATA_SERIALIZERS = RegistryManager.ACTIVE.getRegistry(DataSerializerEntry.class);
    public static final IForgeRegistry<GlobalLootModifierSerializer<?>> LOOT_MODIFIER_SERIALIZERS = RegistryManager.ACTIVE.getRegistry(GlobalLootModifierSerializer.class);
    public static final IForgeRegistry<ForgeWorldType> WORLD_TYPES = RegistryManager.ACTIVE.getRegistry(ForgeWorldType.class);

    public static final class Keys {
        //Vanilla
        public static final RegistryKey<Registry<Block>>  BLOCKS  = key("block");
        public static final RegistryKey<Registry<Fluid>>  FLUIDS  = key("fluid");
        public static final RegistryKey<Registry<Item>>   ITEMS   = key("item");
        public static final RegistryKey<Registry<StatusEffect>> EFFECTS = key("mob_effect");
        public static final RegistryKey<Registry<Potion>> POTIONS = key("potion");
        public static final RegistryKey<Registry<EntityAttribute>> ATTRIBUTES = key("attribute");
        public static final RegistryKey<Registry<StatType<?>>> STAT_TYPES = key("stat_type");
        public static final RegistryKey<Registry<SoundEvent>> SOUND_EVENTS = key("sound_event");
        public static final RegistryKey<Registry<Enchantment>> ENCHANTMENTS = key("enchantment");
        public static final RegistryKey<Registry<EntityType<?>>> ENTITY_TYPES = key("entity_type");
        public static final RegistryKey<Registry<PaintingMotive>> PAINTING_TYPES = key("motive");
        public static final RegistryKey<Registry<ParticleType<?>>> PARTICLE_TYPES = key("particle_type");
        public static final RegistryKey<Registry<ScreenHandlerType<?>>> CONTAINER_TYPES = key("menu");
        public static final RegistryKey<Registry<BlockEntityType<?>>> TILE_ENTITY_TYPES = key("block_entity_type");
        public static final RegistryKey<Registry<RecipeSerializer<?>>> RECIPE_SERIALIZERS = key("recipe_serializer");
        public static final RegistryKey<Registry<VillagerProfession>> VILLAGER_PROFESSIONS = key("villager_profession");
        public static final RegistryKey<Registry<PointOfInterestType>> POI_TYPES = key("point_of_interest_type");
        public static final RegistryKey<Registry<MemoryModuleType<?>>> MEMORY_MODULE_TYPES = key("memory_module_type");
        public static final RegistryKey<Registry<SensorType<?>>> SENSOR_TYPES = key("sensor_type");
        public static final RegistryKey<Registry<Schedule>> SCHEDULES = key("schedule");
        public static final RegistryKey<Registry<Activity>> ACTIVITIES = key("activity");
        public static final RegistryKey<Registry<Carver<?>>> WORLD_CARVERS = key("worldgen/carver");
        public static final RegistryKey<Registry<SurfaceBuilder<?>>> SURFACE_BUILDERS = key("worldgen/surface_builder");
        public static final RegistryKey<Registry<Feature<?>>> FEATURES = key("worldgen/feature");
        public static final RegistryKey<Registry<Decorator<?>>> DECORATORS = key("worldgen/decorator");
        public static final RegistryKey<Registry<ChunkStatus>> CHUNK_STATUS = key("chunk_status");
        public static final RegistryKey<Registry<StructureFeature<?>>> STRUCTURE_FEATURES = key("worldgen/structure_feature");
        public static final RegistryKey<Registry<BlockStateProviderType<?>>> BLOCK_STATE_PROVIDER_TYPES = key("worldgen/block_state_provider_type");
        public static final RegistryKey<Registry<BlockPlacerType<?>>> BLOCK_PLACER_TYPES = key("worldgen/block_placer_type");
        public static final RegistryKey<Registry<FoliagePlacerType<?>>> FOLIAGE_PLACER_TYPES = key("worldgen/foliage_placer_type");
        public static final RegistryKey<Registry<TreeDecoratorType<?>>> TREE_DECORATOR_TYPES = key("worldgen/tree_decorator_type");

        // Vanilla Dynamic
        public static final RegistryKey<Registry<Biome>> BIOMES = key("worldgen/biome");

        //Forge
        public static final RegistryKey<Registry<DataSerializerEntry>> DATA_SERIALIZERS = key("data_serializers");
        public static final RegistryKey<Registry<GlobalLootModifierSerializer<?>>> LOOT_MODIFIER_SERIALIZERS = key("forge:loot_modifier_serializers");
        public static final RegistryKey<Registry<ForgeWorldType>> WORLD_TYPES = key("forge:world_types");

        private static <T> RegistryKey<Registry<T>> key(String name)
        {
            return RegistryKey.ofRegistry(new Identifier(name));
        }
        private static void init() {}
    }

    /**
     * This function is just to make sure static inializers in other classes have run and setup their registries before we query them.
     */
    private static void init()
    {
        Keys.init();
        GameData.init();
        Bootstrap.initialize();
        Tags.init();
    }
}
