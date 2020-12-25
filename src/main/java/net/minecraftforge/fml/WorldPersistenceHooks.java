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

package net.minecraftforge.fml;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.level.storage.LevelStorage;

public class WorldPersistenceHooks {
	private static final List<WorldPersistenceHook> worldPersistenceHooks = new ArrayList<>();

	public static void addHook(WorldPersistenceHook hook) {
		worldPersistenceHooks.add(hook);
	}

	public static void handleWorldDataSave(final LevelStorage.Session levelSave, final SaveProperties serverInfo, final CompoundTag tagCompound) {
		worldPersistenceHooks.forEach(wac -> tagCompound.put(wac.getModId(), wac.getDataForWriting(levelSave, serverInfo)));
	}

	public static void handleWorldDataLoad(LevelStorage.Session levelSave, SaveProperties serverInfo, CompoundTag tagCompound) {
		worldPersistenceHooks.forEach(wac -> wac.readData(levelSave, serverInfo, tagCompound.getCompound(wac.getModId())));
	}

	public interface WorldPersistenceHook {
		String getModId();

		CompoundTag getDataForWriting(LevelStorage.Session levelSave, SaveProperties serverInfo);

		void readData(LevelStorage.Session levelSave, SaveProperties serverInfo, CompoundTag tag);
	}
}
