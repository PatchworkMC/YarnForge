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

package net.minecraftforge.debug.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraftforge.common.ForgeMod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Multimap;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Rarity;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome.Category;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("gravity_attribute_test")
public class GravityAttributeTest
{
    public static final boolean ENABLE = false;
    private static Logger logger = LogManager.getLogger();
    private int ticks;
    private static final UUID REDUCED_GRAVITY_ID = UUID.fromString("DEB06000-7979-4242-8888-00000DEB0600");
    private static final EntityAttributeModifier REDUCED_GRAVITY = (new EntityAttributeModifier(REDUCED_GRAVITY_ID, "Reduced gravity", (double)-0.80, Operation.MULTIPLY_TOTAL));


    public GravityAttributeTest()
    {
        if (ENABLE)
        {
            MinecraftForge.EVENT_BUS.register(this);
            FMLJavaModLoadingContext.get().getModEventBus().register(this);
        }
    }

    @SubscribeEvent
    public void worldTick(TickEvent.WorldTickEvent event)
    {
        if (!event.world.isClient)
        {
            if (ticks++ > 60)
            {
                ticks = 0;
                World w = event.world;
                List<LivingEntity> list;
                if(w.isClient)
                {
                    ClientWorld cw = (ClientWorld)w;
                    list = new ArrayList<>(100);
                    for(Entity e : cw.getEntities())
                    {
                        if(e.isAlive() && e instanceof LivingEntity)
                            list.add((LivingEntity)e);
                    }
                }
                else
                {
                    ServerWorld sw = (ServerWorld)w;
                    Stream<LivingEntity> s = sw.getEntities().filter(Entity::isAlive).filter(e -> e instanceof LivingEntity).map(e -> (LivingEntity)e);
                    list = s.collect(Collectors.toList());
                }

                for(LivingEntity liv : list)
                {
                    EntityAttributeInstance grav = liv.getAttributeInstance(ForgeMod.ENTITY_GRAVITY.get());
                    boolean inPlains = liv.world.getBiome(liv.getBlockPos()).getCategory() == Category.PLAINS;
                    if (inPlains && !grav.hasModifier(REDUCED_GRAVITY))
                    {
                        logger.info("Granted low gravity to Entity: {}", liv);
                        grav.addTemporaryModifier(REDUCED_GRAVITY);
                    }
                    else if (!inPlains && grav.hasModifier(REDUCED_GRAVITY))
                    {
                        logger.info("Removed low gravity from Entity: {}", liv);
                        grav.removeModifier(REDUCED_GRAVITY);
                    }
                }
            }

        }
    }

    @SubscribeEvent
    public void registerItems(RegistryEvent.Register<Item> event)
    {
        event.getRegistry().register(new ItemGravityStick(new Item.Settings().group(ItemGroup.TOOLS).rarity(Rarity.RARE)).setRegistryName("gravity_attribute_test:gravity_stick"));
    }

    public static class ItemGravityStick extends Item
    {
        private static final UUID GRAVITY_MODIFIER = UUID.fromString("DEB06001-7979-4242-8888-10000DEB0601");

        public ItemGravityStick(Settings properties)
        {
            super(properties);
        }

        @Override
        public Multimap<EntityAttribute, EntityAttributeModifier> getAttributeModifiers(EquipmentSlot slot)
        {
            @SuppressWarnings("deprecation")
            Multimap<EntityAttribute, EntityAttributeModifier> multimap = super.getAttributeModifiers(slot);
            if (slot == EquipmentSlot.MAINHAND)
                multimap.put(ForgeMod.ENTITY_GRAVITY.get(), new EntityAttributeModifier(GRAVITY_MODIFIER, "More Gravity", 1.0D, Operation.ADDITION));

            return multimap;
        }
    }
}
