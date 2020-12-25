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

package net.minecraftforge.client.model;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.minecraftforge.logging.ModelLoaderErrorMessage;

import java.util.function.Function;

import com.google.common.collect.Maps;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;

import static net.minecraftforge.fml.Logging.MODELLOADING;

public final class ModelLoader extends net.minecraft.client.render.model.ModelLoader
{
    private static final Logger LOGGER = LogManager.getLogger();
    private final Map<Identifier, Exception> loadingExceptions = Maps.newHashMap();
    private UnbakedModel missingModel = null;

    private boolean isLoading = false;

    private static ModelLoader instance;

    @Nullable
    public static ModelLoader instance()
    {
        return instance;
    }

    public boolean isLoading()
    {
        return isLoading;
    }

    public ModelLoader(ResourceManager manager, BlockColors colours, Profiler profiler, int maxMipmapLevel)
    {
        super(manager, colours, false);
        instance = this;
        processLoading(profiler, maxMipmapLevel);
    }

    private static Set<Identifier> specialModels = new HashSet<>();

    /**
     * Indicate to vanilla that it should load and bake the given model, even if no blocks or
     * items use it. This is useful if e.g. you have baked models only for entity renderers.
     * Call during {@link net.minecraftforge.client.event.ModelRegistryEvent}
     * @param rl The model, either {@link ModelResourceLocation} to point to a blockstate variant,
     *           or plain {@link ResourceLocation} to point directly to a json in the models folder.
     */
    public static void addSpecialModel(Identifier rl) {
        specialModels.add(rl);
    }

    @Override
    public Set<Identifier> getSpecialModels() {
        return specialModels;
    }

    /**
     * Hooked from ModelBakery, allows using MRLs that don't end with "inventory" for items.
     */
    public static ModelIdentifier getInventoryVariant(String s)
    {
        if(s.contains("#"))
        {
            return new ModelIdentifier(s);
        }
        return new ModelIdentifier(s, "inventory");
    }

    protected Identifier getModelLocation(Identifier model)
    {
        return new Identifier(model.getNamespace(), model.getPath() + ".json");
    }

    protected UnbakedModel getMissingModel()
    {
        if (missingModel == null)
        {
            try
            {
                missingModel = getOrLoadModel(MISSING);
            }
            catch(Exception e)
            {
                throw new RuntimeException("Missing the missing model, this should never happen");
            }
        }
        return missingModel;
    }

    /**
     * Use this if you don't care about the exception and want some model anyway.
     */
    public UnbakedModel getModelOrMissing(Identifier location)
    {
        try
        {
            return getOrLoadModel(location);
        }
        catch(Exception e)
        {
            return getMissingModel();
        }
    }

    /**
     * Use this if you want the model, but need to log the error.
     */
    public UnbakedModel getModelOrLogError(Identifier location, String error)
    {
        try
        {
            return getOrLoadModel(location);
        }
        catch(Exception e)
        {
            LOGGER.error(error, e);
            return getMissingModel();
        }
    }

    // Temporary to compile things
    public static final class White {
        public static final Identifier LOCATION = new Identifier("white");
        private static Sprite instance = null;
        @SuppressWarnings("deprecation")
        public static final Sprite instance()
        {
            if (instance == null)
            {
                instance = new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, LOCATION).getSprite();
            }
            return instance;
        }
    }

    /**
     * 16x16 pure white sprite.
     */
    /*/ TODO Custom TAS
    public static final class White extends TextureAtlasSprite
    {
        public static final ResourceLocation LOCATION = new ResourceLocation("white");
        public static final White INSTANCE = new White();

        private White()
        {
            super(LOCATION.toString());
            this.width = this.height = 16;
        }

        @Override
        public boolean hasCustomLoader(IResourceManager manager, ResourceLocation location)
        {
            return true;
        }

        @Override
        public boolean load(IResourceManager manager, ResourceLocation location, Function<ResourceLocation, TextureAtlasSprite> textureGetter)
        {
            BufferedImage image = new BufferedImage(this.getIconWidth(), this.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setBackground(Color.WHITE);
            graphics.clearRect(0, 0, this.getIconWidth(), this.getIconHeight());
            int[][] pixels = new int[Minecraft.getMinecraft().gameSettings.mipmapLevels + 1][];
            pixels[0] = new int[image.getWidth() * image.getHeight()];
            image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels[0], 0, image.getWidth());
            this.clearFramesTextureData();
            this.framesTextureData.add(pixels);
            return false;
        }

        public void register(TextureMap map)
        {
            map.setTextureEntry(White.INSTANCE);
        }
    }
*/
    @SuppressWarnings("serial")
    public static class ItemLoadingException extends ModelLoadingException
    {
        private final Exception normalException;
        private final Exception blockstateException;

        public ItemLoadingException(String message, Exception normalException, Exception blockstateException)
        {
            super(message);
            this.normalException = normalException;
            this.blockstateException = blockstateException;
        }
    }

    /**
     * Internal, do not use.
     */
    public void onPostBakeEvent(Map<Identifier, BakedModel> modelRegistry)
    {
        BakedModel missingModel = modelRegistry.get(MISSING);
        for(Map.Entry<Identifier, Exception> entry : loadingExceptions.entrySet())
        {
            // ignoring pure ResourceLocation arguments, all things we care about pass ModelResourceLocation
            if(entry.getKey() instanceof ModelIdentifier)
            {
                LOGGER.debug(MODELLOADING, ()-> new ModelLoaderErrorMessage((ModelIdentifier)entry.getKey(), entry.getValue()));
                final ModelIdentifier location = (ModelIdentifier)entry.getKey();
                final BakedModel model = modelRegistry.get(location);
                if(model == null)
                {
                    modelRegistry.put(location, missingModel);
                }
            }
        }
        loadingExceptions.clear();
        isLoading = false;
    }

    /**
     * Helper method for registering all itemstacks for given item to map to universal bucket model.
     *//*
    public static void setBucketModelDefinition(Item item) {
        ModelLoader.setCustomMeshDefinition(item, stack -> ModelDynBucket.LOCATION);
        ModelBakery.registerItemVariants(item, ModelDynBucket.LOCATION);
    }
*/

    private static final Function<Identifier, UnbakedModel> DEFAULT_MODEL_GETTER = (rl) -> ModelLoader.instance().getModelOrMissing(rl);

    /**
     * Get the default texture getter the models will be baked with.
     */
    public static Function<SpriteIdentifier, Sprite> defaultTextureGetter()
    {
        return SpriteIdentifier::getSprite;
    }

    public static Function<Identifier, UnbakedModel> defaultModelGetter()
    {
        return DEFAULT_MODEL_GETTER;
    }
}
