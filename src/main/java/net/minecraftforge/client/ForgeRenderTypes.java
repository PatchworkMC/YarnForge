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

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.util.Identifier;
import net.minecraftforge.common.util.NonNullLazy;
import net.minecraftforge.common.util.NonNullSupplier;
import org.lwjgl.opengl.GL11;

@SuppressWarnings("deprecation")
public enum ForgeRenderTypes
{
    ITEM_LAYERED_SOLID(()-> getItemLayeredSolid(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)),
    ITEM_LAYERED_CUTOUT(()-> getItemLayeredCutout(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)),
    ITEM_LAYERED_CUTOUT_MIPPED(()-> getItemLayeredCutoutMipped(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)),
    ITEM_LAYERED_TRANSLUCENT(()-> getItemLayeredTranslucent(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)),
    ITEM_UNSORTED_TRANSLUCENT(()-> getUnsortedTranslucent(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)),
    ITEM_UNLIT_TRANSLUCENT(()-> getUnlitTranslucent(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)),
    ITEM_UNSORTED_UNLIT_TRANSLUCENT(()-> getUnlitTranslucent(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, false));

    /**
     * @return A RenderType fit for multi-layer solid item rendering.
     */
    public static RenderLayer getItemLayeredSolid(Identifier textureLocation)
    {
        return Internal.layeredItemSolid(textureLocation);
    }

    /**
     * @return A RenderType fit for multi-layer cutout item item rendering.
     */
    public static RenderLayer getItemLayeredCutout(Identifier textureLocation)
    {
        return Internal.layeredItemCutout(textureLocation);
    }

    /**
     * @return A RenderType fit for multi-layer cutout-mipped item rendering.
     */
    public static RenderLayer getItemLayeredCutoutMipped(Identifier textureLocation)
    {
        return Internal.layeredItemCutoutMipped(textureLocation);
    }

    /**
     * @return A RenderType fit for multi-layer translucent item rendering.
     */
    public static RenderLayer getItemLayeredTranslucent(Identifier textureLocation)
    {
        return Internal.layeredItemTranslucent(textureLocation);
    }

    /**
     * @return A RenderType fit for translucent item/entity rendering, but with depth sorting disabled.
     */
    public static RenderLayer getUnsortedTranslucent(Identifier textureLocation)
    {
        return Internal.unsortedTranslucent(textureLocation);
    }

    /**
     * @return A RenderType fit for translucent item/entity rendering, but with diffuse lighting disabled
     * so that fullbright quads look correct.
     */
    public static RenderLayer getUnlitTranslucent(Identifier textureLocation)
    {
        return getUnlitTranslucent(textureLocation, true);
    }

    /**
     * @return A RenderType fit for translucent item/entity rendering, but with diffuse lighting disabled
     * so that fullbright quads look correct.
     * @param sortingEnabled If false, depth sorting will not be performed.
     */
    public static RenderLayer getUnlitTranslucent(Identifier textureLocation, boolean sortingEnabled)
    {
        return Internal.unlitTranslucent(textureLocation, sortingEnabled);
    }

    /**
     * @return Same as {@link RenderType#getEntityCutout(ResourceLocation)}, but with mipmapping enabled.
     */
    public static RenderLayer getEntityCutoutMipped(Identifier textureLocation)
    {
        return Internal.layeredItemCutoutMipped(textureLocation);
    }

    // ----------------------------------------
    //  Implementation details below this line
    // ----------------------------------------

    private final NonNullSupplier<RenderLayer> renderTypeSupplier;

    ForgeRenderTypes(NonNullSupplier<RenderLayer> renderTypeSupplier)
    {
        // Wrap in a Lazy<> to avoid running the supplier more than once.
        this.renderTypeSupplier = NonNullLazy.of(renderTypeSupplier);
    }

    public RenderLayer get()
    {
        return renderTypeSupplier.get();
    }

    private static class Internal extends RenderLayer
    {
        private Internal(String name, VertexFormat fmt, int glMode, int size, boolean doCrumbling, boolean depthSorting, Runnable onEnable, Runnable onDisable)
        {
            super(name, fmt, glMode, size, doCrumbling, depthSorting, onEnable, onDisable);
            throw new IllegalStateException("This class must not be instantiated");
        }

        public static RenderLayer unsortedTranslucent(Identifier textureLocation)
        {
            final boolean sortingEnabled = false;
            MultiPhaseParameters renderState = MultiPhaseParameters.builder()
                    .texture(new Texture(textureLocation, false, false))
                    .transparency(TRANSLUCENT_TRANSPARENCY)
                    .diffuseLighting(ENABLE_DIFFUSE_LIGHTING)
                    .alpha(ONE_TENTH_ALPHA)
                    .cull(DISABLE_CULLING)
                    .lightmap(ENABLE_LIGHTMAP)
                    .overlay(ENABLE_OVERLAY_COLOR)
                    .build(true);
            return of("forge_entity_unsorted_translucent", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, GL11.GL_QUADS, 256, true, sortingEnabled, renderState);
        }

        public static RenderLayer unlitTranslucent(Identifier textureLocation, boolean sortingEnabled)
        {
            MultiPhaseParameters renderState = MultiPhaseParameters.builder()
                    .texture(new Texture(textureLocation, false, false))
                    .transparency(TRANSLUCENT_TRANSPARENCY)
                    .alpha(ONE_TENTH_ALPHA)
                    .cull(DISABLE_CULLING)
                    .lightmap(ENABLE_LIGHTMAP)
                    .overlay(ENABLE_OVERLAY_COLOR)
                    .build(true);
            return of("forge_entity_unlit_translucent", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, GL11.GL_QUADS, 256, true, sortingEnabled, renderState);
        }

        public static RenderLayer layeredItemSolid(Identifier locationIn) {
            RenderLayer.MultiPhaseParameters rendertype$state = RenderLayer.MultiPhaseParameters.builder()
                    .texture(new RenderPhase.Texture(locationIn, false, false))
                    .transparency(NO_TRANSPARENCY)
                    .diffuseLighting(ENABLE_DIFFUSE_LIGHTING)
                    .lightmap(ENABLE_LIGHTMAP)
                    .overlay(ENABLE_OVERLAY_COLOR)
                    .build(true);
            return of("forge_item_entity_solid", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, 7, 256, true, false, rendertype$state);
        }

        public static RenderLayer layeredItemCutout(Identifier locationIn) {
            RenderLayer.MultiPhaseParameters rendertype$state = RenderLayer.MultiPhaseParameters.builder()
                    .texture(new RenderPhase.Texture(locationIn, false, false))
                    .transparency(NO_TRANSPARENCY)
                    .diffuseLighting(ENABLE_DIFFUSE_LIGHTING)
                    .alpha(ONE_TENTH_ALPHA)
                    .lightmap(ENABLE_LIGHTMAP)
                    .overlay(ENABLE_OVERLAY_COLOR)
                    .build(true);
            return of("forge_item_entity_cutout", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, 7, 256, true, false, rendertype$state);
        }

        public static RenderLayer layeredItemCutoutMipped(Identifier locationIn) {
            RenderLayer.MultiPhaseParameters rendertype$state = RenderLayer.MultiPhaseParameters.builder()
                    .texture(new RenderPhase.Texture(locationIn, false, true))
                    .transparency(NO_TRANSPARENCY)
                    .diffuseLighting(ENABLE_DIFFUSE_LIGHTING)
                    .alpha(ONE_TENTH_ALPHA)
                    .lightmap(ENABLE_LIGHTMAP)
                    .overlay(ENABLE_OVERLAY_COLOR)
                    .build(true);
            return of("forge_item_entity_cutout_mipped", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, 7, 256, true, false, rendertype$state);
        }

        public static RenderLayer layeredItemTranslucent(Identifier locationIn) {
            RenderLayer.MultiPhaseParameters rendertype$state = RenderLayer.MultiPhaseParameters.builder()
                    .texture(new RenderPhase.Texture(locationIn, false, false))
                    .transparency(TRANSLUCENT_TRANSPARENCY)
                    .diffuseLighting(ENABLE_DIFFUSE_LIGHTING)
                    .alpha(ONE_TENTH_ALPHA)
                    .lightmap(ENABLE_LIGHTMAP)
                    .overlay(ENABLE_OVERLAY_COLOR)
                    .build(true);
            return of("forge_item_entity_translucent_cull", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, 7, 256, true, true, rendertype$state);
        }
    }
}
