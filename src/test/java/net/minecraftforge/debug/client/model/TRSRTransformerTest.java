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

package net.minecraftforge.debug.client.model;

import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.AffineTransformation;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Quaternion;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.data.IDynamicBakedModel;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.pipeline.BakedQuadBuilder;
import net.minecraftforge.client.model.pipeline.TRSRTransformer;
import net.minecraftforge.common.model.TransformationHelper;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@Mod(TRSRTransformerTest.MODID)
public class TRSRTransformerTest {
    public static final String MODID = "trsr_transformer_test";
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    private static final RegistryObject<Block> TEST_BLOCK = BLOCKS.register("test", () -> new Block(Block.Properties.of(Material.STONE)));
    @SuppressWarnings("unused")
    private static final RegistryObject<Item> TEST_ITEM = ITEMS.register("test", () -> new BlockItem(TEST_BLOCK.get(), new Item.Settings().group(ItemGroup.MISC)));

    public TRSRTransformerTest() {
        final IEventBus mod = FMLJavaModLoadingContext.get().getModEventBus();
        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> mod.addListener(this::onModelBake));
        BLOCKS.register(mod);
        ITEMS.register(mod);
    }

    public void onModelBake(ModelBakeEvent e) {
        for (Identifier id : e.getModelRegistry().keySet()) {
            if (MODID.equals(id.getNamespace()) && "test".equals(id.getPath())) {
                e.getModelRegistry().put(id, new MyBakedModel(e.getModelRegistry().get(id)));
            }
        }
    }

    public class MyBakedModel implements IDynamicBakedModel {
        private final BakedModel base;

        public MyBakedModel(BakedModel base) {
            this.base = base;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random rand, IModelData data) {
            ImmutableList.Builder<BakedQuad> quads = new ImmutableList.Builder<>();

            Quaternion rot = TransformationHelper.quatFromXYZ(new Vector3f(0, 45, 0), true);
            Vector3f translation = new Vector3f(0, 0.33f, 0);

            AffineTransformation trans = new AffineTransformation(translation, rot, null, null).blockCenterToCorner();

            for (BakedQuad quad : base.getQuads(state, side, rand, data)) {

                if (true)
                {
                    BakedQuadBuilder builder = new BakedQuadBuilder();

                    TRSRTransformer transformer = new TRSRTransformer(builder, trans);

                    quad.pipe(transformer);

                    quads.add(builder.build());
                } /* else {
                    QuadTransformer qt = new QuadTransformer(trans);
                    quads.add(qt.processOne(quad));
                }*/
            }

            return quads.build();
        }

        @Override
        public boolean useAmbientOcclusion() {
            return base.useAmbientOcclusion();
        }

        @Override
        public boolean hasDepth() {
            return base.hasDepth();
        }

        @Override
        public boolean isSideLit() {
            return base.isSideLit();
        }

        @Override
        public boolean isBuiltin() {
            return base.isBuiltin();
        }

        @Override
        public Sprite getSprite() {
            return base.getSprite();
        }

        @Override
        public ModelOverrideList getOverrides() {
            return base.getOverrides();
        }
    }
}
