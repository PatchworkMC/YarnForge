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

package net.minecraftforge.fml.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.collect.Maps;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.RegistryManager;

public class FMLHandshakeMessages {
	static class LoginIndexedMessage implements IntSupplier {
		private int loginIndex;

		void setLoginIndex(final int loginIndex) {
			this.loginIndex = loginIndex;
		}

		int getLoginIndex() {
			return loginIndex;
		}

		@Override
		public int getAsInt() {
			return getLoginIndex();
		}
	}

	/**
	 * Server to client "list of mods". Always first handshake message.
	 */
	public static class S2CModList extends LoginIndexedMessage {
		private final List<String> mods;
		private final Map<Identifier, String> channels;
		private final List<Identifier> registries;

		public S2CModList() {
			this.mods = ModList.get().getMods().stream().map(ModInfo::getModId).collect(Collectors.toList());
			this.channels = NetworkRegistry.buildChannelVersions();
			this.registries = RegistryManager.getRegistryNamesForSyncToClient();
		}

		private S2CModList(List<String> mods, Map<Identifier, String> channels, List<Identifier> registries) {
			this.mods = mods;
			this.channels = channels;
			this.registries = registries;
		}

		public static S2CModList decode(PacketByteBuf input) {
			List<String> mods = new ArrayList<>();
			int len = input.readVarInt();
			for (int x = 0; x < len; x++) {
				mods.add(input.readString(0x100));
			}

			Map<Identifier, String> channels = new HashMap<>();
			len = input.readVarInt();
			for (int x = 0; x < len; x++) {
				channels.put(input.readIdentifier(), input.readString(0x100));
			}

			List<Identifier> registries = new ArrayList<>();
			len = input.readVarInt();
			for (int x = 0; x < len; x++) {
				registries.add(input.readIdentifier());
			}

			return new S2CModList(mods, channels, registries);
		}

		public void encode(PacketByteBuf output) {
			output.writeVarInt(mods.size());
			mods.forEach(m -> output.writeString(m, 0x100));

			output.writeVarInt(channels.size());
			channels.forEach((k, v) -> {
				output.writeIdentifier(k);
				output.writeString(v, 0x100);
			});

			output.writeVarInt(registries.size());
			registries.forEach(output::writeIdentifier);
		}

		public List<String> getModList() {
			return mods;
		}

		public List<Identifier> getRegistries() {
			return this.registries;
		}

		public Map<Identifier, String> getChannels() {
			return this.channels;
		}
	}

	public static class C2SModListReply extends LoginIndexedMessage {
		private final List<String> mods;
		private final Map<Identifier, String> channels;
		private final Map<Identifier, String> registries;

		public C2SModListReply() {
			this.mods = ModList.get().getMods().stream().map(ModInfo::getModId).collect(Collectors.toList());
			this.channels = NetworkRegistry.buildChannelVersions();
			this.registries = Maps.newHashMap(); //TODO: Fill with known hashes, which requires keeping a file cache
		}

		private C2SModListReply(List<String> mods, Map<Identifier, String> channels, Map<Identifier, String> registries) {
			this.mods = mods;
			this.channels = channels;
			this.registries = registries;
		}

		public static C2SModListReply decode(PacketByteBuf input) {
			List<String> mods = new ArrayList<>();
			int len = input.readVarInt();
			for (int x = 0; x < len; x++) {
				mods.add(input.readString(0x100));
			}

			Map<Identifier, String> channels = new HashMap<>();
			len = input.readVarInt();
			for (int x = 0; x < len; x++) {
				channels.put(input.readIdentifier(), input.readString(0x100));
			}

			Map<Identifier, String> registries = new HashMap<>();
			len = input.readVarInt();
			for (int x = 0; x < len; x++) {
				registries.put(input.readIdentifier(), input.readString(0x100));
			}

			return new C2SModListReply(mods, channels, registries);
		}

		public void encode(PacketByteBuf output) {
			output.writeVarInt(mods.size());
			mods.forEach(m -> output.writeString(m, 0x100));

			output.writeVarInt(channels.size());
			channels.forEach((k, v) -> {
				output.writeIdentifier(k);
				output.writeString(v, 0x100);
			});

			output.writeVarInt(registries.size());
			registries.forEach((k, v) -> {
				output.writeIdentifier(k);
				output.writeString(v, 0x100);
			});
		}

		public List<String> getModList() {
			return mods;
		}

		public Map<Identifier, String> getRegistries() {
			return this.registries;
		}

		public Map<Identifier, String> getChannels() {
			return this.channels;
		}
	}

	public static class C2SAcknowledge extends LoginIndexedMessage {
		public void encode(PacketByteBuf buf) {

		}

		public static C2SAcknowledge decode(PacketByteBuf buf) {
			return new C2SAcknowledge();
		}
	}

	public static class S2CRegistry extends LoginIndexedMessage {
		private final Identifier registryName;
		@Nullable
		private final ForgeRegistry.Snapshot snapshot;

		public S2CRegistry(final Identifier name,
		                   @Nullable
			                   ForgeRegistry.Snapshot snapshot) {
			this.registryName = name;
			this.snapshot = snapshot;
		}

		void encode(final PacketByteBuf buffer) {
			buffer.writeIdentifier(registryName);
			buffer.writeBoolean(hasSnapshot());
			if (hasSnapshot()) {
				buffer.writeBytes(snapshot.getPacketData());
			}
		}

		public static S2CRegistry decode(final PacketByteBuf buffer) {
			Identifier name = buffer.readIdentifier();
			ForgeRegistry.Snapshot snapshot = null;
			if (buffer.readBoolean()) {
				snapshot = ForgeRegistry.Snapshot.read(buffer);
			}
			return new S2CRegistry(name, snapshot);
		}

		public Identifier getRegistryName() {
			return registryName;
		}

		public boolean hasSnapshot() {
			return snapshot != null;
		}

		@Nullable
		public ForgeRegistry.Snapshot getSnapshot() {
			return snapshot;
		}
	}

	public static class S2CConfigData extends LoginIndexedMessage {
		private final String fileName;
		private final byte[] fileData;

		public S2CConfigData(final String configFileName, final byte[] configFileData) {
			this.fileName = configFileName;
			this.fileData = configFileData;
		}

		void encode(final PacketByteBuf buffer) {
			buffer.writeString(this.fileName);
			buffer.writeByteArray(this.fileData);
		}

		public static S2CConfigData decode(final PacketByteBuf buffer) {
			return new S2CConfigData(buffer.readString(128), buffer.readByteArray());
		}

		public String getFileName() {
			return fileName;
		}

		public byte[] getBytes() {
			return fileData;
		}
	}
}
