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

package net.minecraftforge.debug.chat;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.EntitySelectorOptions;
import net.minecraft.command.EntitySelectorReader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.predicate.NumberRange;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraftforge.common.command.EntitySelectorManager;
import net.minecraftforge.common.command.IEntitySelectorType;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("entity_selector_test")
public class EntitySelectorTest
{
    public EntitySelectorTest()
    {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
    }

    public void setup(FMLCommonSetupEvent event)
    {
        EntitySelectorOptions.putOption("health", this::healthArgument, parser -> true, new LiteralText("Selects entities based on their current health."));
        EntitySelectorManager.register("er", new ExampleCustomSelector());
    }

    /**
     * Example for a custom selector argument, checks for the health of the entity
     */
    private void healthArgument(EntitySelectorReader parser) throws CommandSyntaxException
    {
        NumberRange.FloatRange bound = NumberRange.FloatRange.parse(parser.getReader());
        parser.setPredicate(entity -> entity instanceof LivingEntity && bound.test(((LivingEntity) entity).getHealth()));
    }

    /**
     * Example for a custom selector type, works like @r but for entities.
     * Basically does exactly what @e[sorter=random, limit=1, ...] does.
     */
    private class ExampleCustomSelector implements IEntitySelectorType
    {
        @Override
        public EntitySelector build(EntitySelectorReader parser) throws CommandSyntaxException
        {
            parser.setSorter(EntitySelectorReader.RANDOM);
            parser.setLimit(1);
            parser.setIncludesNonPlayers(true);
            parser.setPredicate(Entity::isAlive);
            parser.setSuggestionProvider((builder, consumer) -> builder.suggest(String.valueOf('[')).buildFuture());
            if (parser.getReader().canRead() && parser.getReader().peek() == '[')
            {
                parser.getReader().skip();
                parser.setSuggestionProvider((builder, consumer) -> {
                    builder.suggest(String.valueOf(']'));
                    EntitySelectorOptions.suggestOptions(parser, builder);
                    return builder.buildFuture();
                });

                parser.readArguments();
            }

            parser.buildPredicate();
            return parser.build();
        }

        @Override
        public Text getSuggestionTooltip()
        {
            return new LiteralText("Example: Selects a random entity");
        }
    }
}
