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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceArrayMap;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.login.LoginQueryResponseC2SPacket;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.network.packet.s2c.login.LoginQueryRequestS2CPacket;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.util.Identifier;
import net.minecraftforge.fml.unsafe.UnsafeHacks;

public interface ICustomPacket<T extends Packet<?>> {
	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	enum Fields {
		CPACKETCUSTOMPAYLOAD(CustomPayloadC2SPacket.class),
		SPACKETCUSTOMPAYLOAD(CustomPayloadS2CPacket.class),
		CPACKETCUSTOMLOGIN(LoginQueryResponseC2SPacket.class),
		SPACKETCUSTOMLOGIN(LoginQueryRequestS2CPacket.class),
		;

		static final Reference2ReferenceArrayMap<Class<?>, Fields> lookup;

		static {
			lookup = Stream.of(values()).
				collect(Collectors.toMap(Fields::getClazz, Function.identity(), (m1, m2) -> m1, Reference2ReferenceArrayMap::new));
		}

		private final Class<?> clazz;

		final Optional<Field> data;
		final Optional<Field> channel;
		final Optional<Field> index;

		Fields(Class<?> customPacketClass) {
			this.clazz = customPacketClass;
			Field[] fields = customPacketClass.getDeclaredFields();
			data = Arrays.stream(fields).filter(f -> !Modifier.isStatic(f.getModifiers()) && f.getType() == PacketByteBuf.class).findFirst();
			channel = Arrays.stream(fields).filter(f -> !Modifier.isStatic(f.getModifiers()) && f.getType() == Identifier.class).findFirst();
			index = Arrays.stream(fields).filter(f -> !Modifier.isStatic(f.getModifiers()) && f.getType() == int.class).findFirst();
		}

		private Class<?> getClazz() {
			return clazz;
		}
	}

	default PacketByteBuf getInternalData() {
		return Fields.lookup.get(this.getClass()).data.map(f -> UnsafeHacks.<PacketByteBuf>getField(f, this)).orElse(null);
	}

	default Identifier getName() {
		return Fields.lookup.get(this.getClass()).channel.map(f -> UnsafeHacks.<Identifier>getField(f, this)).orElse(FMLLoginWrapper.WRAPPER);
	}

	default int getIndex() {
		return Fields.lookup.get(this.getClass()).index.map(f -> UnsafeHacks.getIntField(f, this)).orElse(Integer.MIN_VALUE);
	}

	default void setData(PacketByteBuf buffer) {
		Fields.lookup.get(this.getClass()).data.ifPresent(f -> UnsafeHacks.setField(f, this, buffer));
	}

	default void setName(Identifier channelName) {
		Fields.lookup.get(this.getClass()).channel.ifPresent(f -> UnsafeHacks.setField(f, this, channelName));
	}

	default void setIndex(int index) {
		Fields.lookup.get(this.getClass()).index.ifPresent(f -> UnsafeHacks.setIntField(f, this, index));
	}

	default NetworkDirection getDirection() {
		return NetworkDirection.directionFor(this.getClass());
	}

	@SuppressWarnings("unchecked")
	default T getThis() {
		return (T) this;
	}
}
