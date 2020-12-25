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

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.potion.Potion;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public interface IForgeEffectInstance {

    default StatusEffectInstance getEffectInstance() {
        return (StatusEffectInstance)this;
    }

    /**
     * If the Potion effect should be displayed in the players inventory
     * @return true to display it (default), false to hide it.
     */
    default boolean shouldRender() {
        return getEffectInstance().getEffectType().shouldRender(getEffectInstance());
    }

    /**
     * If the standard PotionEffect text (name and duration) should be drawn when this potion is active.
     * @return true to draw the standard text
     */
    default boolean shouldRenderInvText() {
        return getEffectInstance().getEffectType().shouldRenderInvText(getEffectInstance());
    }

    /**
     * If the Potion effect should be displayed in the player's ingame HUD
     * @return true to display it (default), false to hide it.
     */
    default boolean shouldRenderHUD() {
        return getEffectInstance().getEffectType().shouldRenderHUD(getEffectInstance());
    }

    /**
     * Called to draw the this Potion onto the player's inventory when it's active.
     * This can be used to e.g. render Potion icons from your own texture.
     *
     * @param gui the gui instance
     * @param mStack The MatrixStack
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z level
     */
    @Environment(EnvType.CLIENT)
    default void renderInventoryEffect(AbstractInventoryScreen<?> gui, MatrixStack mStack, int x, int y, float z) {
        getEffectInstance().getEffectType().renderInventoryEffect(getEffectInstance(), gui, mStack, x, y, z);
    }

    /**
     * Called to draw the this Potion onto the player's ingame HUD when it's active.
     * This can be used to e.g. render Potion icons from your own texture.
     *
     * @param gui the gui instance
     * @param mStack The MatrixStack
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z level
     * @param alpha the alpha value, blinks when the potion is about to run out
     */
    @Environment(EnvType.CLIENT)
    default void renderHUDEffect(DrawableHelper gui, MatrixStack mStack, int x, int y, float z, float alpha) {
        getEffectInstance().getEffectType().renderHUDEffect(getEffectInstance(), gui, mStack, x, y, z, alpha);
    }

    /***
     * Returns a list of curative items for the potion effect
     * By default, this list is initialized using {@link Potion#getCurativeItems}
     *
     * @return The list (ItemStack) of curative items for the potion effect
     */
    List<ItemStack> getCurativeItems();

    /***
     * Checks the given ItemStack to see if it is in the list of curative items for the potion effect
     * @param stack The ItemStack being checked against the list of curative items for this PotionEffect
     * @return true if the given ItemStack is in the list of curative items for this PotionEffect, false otherwise
     */
    default boolean isCurativeItem(ItemStack stack) {
       return this.getCurativeItems().stream().anyMatch(e -> e.isItemEqualIgnoreDamage(stack));
    }

    /***
     * Sets the list of curative items for this potion effect, overwriting any already present
     * @param curativeItems The list of ItemStacks being set to the potion effect
     */
    void setCurativeItems(List<ItemStack> curativeItems);

    /***
     * Adds the given stack to the list of curative items for this PotionEffect
     * @param stack The ItemStack being added to the curative item list
     */
    default void addCurativeItem(ItemStack stack) {
       if (!this.isCurativeItem(stack))
          this.getCurativeItems().add(stack);
    }

    default void writeCurativeItems(CompoundTag nbt) {
       ListTag list = new ListTag();
       getCurativeItems().forEach(s -> list.add(s.toTag(new CompoundTag())));
       nbt.put("CurativeItems", list);
    }
}
