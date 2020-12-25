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

package net.minecraftforge.client.event.sound;

import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.client.sound.Source;
import net.minecraftforge.eventbus.api.Event;

public class SoundEvent extends net.minecraftforge.eventbus.api.Event
{
    private final SoundSystem manager;
    public SoundEvent(SoundSystem manager)
    {
        this.manager = manager;
    }

    public SoundSystem getManager()
    {
        return manager;
    }

    public static class SoundSourceEvent extends SoundEvent
    {
        private final SoundInstance sound;
        private final Source source;
        private final String name;

        public SoundSourceEvent(SoundSystem manager, SoundInstance sound, Source source)
        {
            super(manager);
            this.name = sound.getId().getPath();
            this.sound = sound;
            this.source = source;
        }

        public SoundInstance getSound()
        {
            return sound;
        }

        public Source getSource()
        {
            return source;
        }

        public String getName()
        {
            return name;
        }
    }
}
