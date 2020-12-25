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

package net.minecraftforge.debug.item;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.DispenserBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.*;
import net.minecraft.util.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

@Mod(CustomElytraTest.MOD_ID)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = CustomElytraTest.MOD_ID)
public class CustomElytraTest
{
    public static final String MOD_ID = "custom_elytra_test";
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    private static final RegistryObject<Item> TEST_ELYTRA = ITEMS.register("test_elytra",() -> new CustomElytra(new Item.Settings().maxDamage(100).group(ItemGroup.MISC)));

    public CustomElytraTest()
    {
        final IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modBus);
        modBus.addListener(this::onClientSetup);
    }

    private void onClientSetup(FMLClientSetupEvent event)
    {
        registerElytraLayer();
    }

    @Environment(EnvType.CLIENT)
    private void registerElytraLayer()
    {
        MinecraftClient.getInstance().getEntityRenderDispatcher().getSkinMap().values().forEach(player -> player.addFeature(new CustomElytraLayer(player)));
    }

    public static class CustomElytra extends Item
    {

        public CustomElytra(Settings properties)
        {
            super(properties);
            DispenserBlock.registerBehavior(this, ArmorItem.DISPENSER_BEHAVIOR);
        }

        @Nullable
        @Override
        public EquipmentSlot getEquipmentSlot(ItemStack stack)
        {
            return EquipmentSlot.CHEST; //Or you could just extend ItemArmor
        }

        @Override
        public boolean canElytraFly(ItemStack stack, LivingEntity entity)
        {
            return true;
        }

        @Override
        public boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks)
        {
            //Adding 1 to ticksElytraFlying prevents damage on the very first tick.
            if (!entity.world.isClient && (flightTicks + 1) % 20 == 0)
            {
                stack.damage(1, entity, e -> e.sendEquipmentBreakStatus(EquipmentSlot.CHEST));
            }
            return true;
        }
    }

    @Environment(EnvType.CLIENT)
    public static class CustomElytraLayer extends ElytraFeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>>
    {
        private static final Identifier TEXTURE_ELYTRA = new Identifier(MOD_ID, "textures/entity/custom_elytra.png");

        public CustomElytraLayer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> rendererIn)
        {
            super(rendererIn);
        }

        @Override
        public boolean shouldRender(ItemStack stack, AbstractClientPlayerEntity entity)
        {
            return stack.getItem() == TEST_ELYTRA.get();
        }

        @Override
        public Identifier getElytraTexture(ItemStack stack, AbstractClientPlayerEntity entity)
        {
            return TEXTURE_ELYTRA;
        }
    }
}
