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

package net.minecraftforge.fml.packs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.resource.AbstractFileResourcePack;
import net.minecraft.resource.ResourceNotFoundException;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.metadata.PackResourceMetadata;
import net.minecraft.resource.metadata.ResourceMetadataReader;
import net.minecraft.util.Identifier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class DelegatingResourcePack extends AbstractFileResourcePack {

	private final List<ResourcePack> delegates;
	private final Map<String, List<ResourcePack>> namespacesAssets;
	private final Map<String, List<ResourcePack>> namespacesData;

	private final String name;
	private final PackResourceMetadata packInfo;

	public DelegatingResourcePack(String id, String name, PackResourceMetadata packInfo, List<? extends ResourcePack> packs) {
		super(new File(id));
		this.name = name;
		this.packInfo = packInfo;
		this.delegates = ImmutableList.copyOf(packs);
		this.namespacesAssets = this.buildNamespaceMap(ResourceType.CLIENT_RESOURCES, delegates);
		this.namespacesData = this.buildNamespaceMap(ResourceType.SERVER_DATA, delegates);
	}

	private Map<String, List<ResourcePack>> buildNamespaceMap(ResourceType type, List<ResourcePack> packList) {
		Map<String, List<ResourcePack>> map = new HashMap<>();
		for (ResourcePack pack : packList) {
			for (String namespace : pack.getNamespaces(type)) {
				map.computeIfAbsent(namespace, k -> new ArrayList<>()).add(pack);
			}
		}
		map.replaceAll((k, list) -> ImmutableList.copyOf(list));
		return ImmutableMap.copyOf(map);
	}

	@Override
	public String getName() {
		return name;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T parseMetadata(ResourceMetadataReader<T> deserializer) throws IOException {
		if (deserializer.getKey().equals("pack")) {
			return (T) packInfo;
		}
		return null;
	}

	@Override
	public Collection<Identifier> findResources(ResourceType type, String pathIn, String pathIn2, int maxDepth, Predicate<String> filter) {
		return delegates.stream()
			.flatMap(r -> r.findResources(type, pathIn, pathIn2, maxDepth, filter).stream())
			.collect(Collectors.toList());
	}

	@Override
	public Set<String> getNamespaces(ResourceType type) {
		return type == ResourceType.CLIENT_RESOURCES ? namespacesAssets.keySet() : namespacesData.keySet();
	}

	@Override
	public void close() {
		for (ResourcePack pack : delegates) {
			pack.close();
		}
	}

	@Override
	public InputStream openRoot(String fileName) throws IOException {
		// root resources do not make sense here
		throw new ResourceNotFoundException(this.base, fileName);
	}

	@Override
	protected InputStream openFile(String resourcePath) throws IOException {
		// never called, we override all methods that call this
		throw new ResourceNotFoundException(this.base, resourcePath);
	}

	@Override
	protected boolean containsFile(String resourcePath) {
		// never called, we override all methods that call this
		return false;
	}

	@Override
	public InputStream open(ResourceType type, Identifier location) throws IOException {
		for (ResourcePack pack : getCandidatePacks(type, location)) {
			if (pack.contains(type, location)) {
				return pack.open(type, location);
			}
		}
		throw new ResourceNotFoundException(this.base, getFilename(type, location));
	}

	@Override
	public boolean contains(ResourceType type, Identifier location) {
		for (ResourcePack pack : getCandidatePacks(type, location)) {
			if (pack.contains(type, location)) {
				return true;
			}
		}
		return false;
	}

	private List<ResourcePack> getCandidatePacks(ResourceType type, Identifier location) {
		Map<String, List<ResourcePack>> map = type == ResourceType.CLIENT_RESOURCES ? namespacesAssets : namespacesData;
		List<ResourcePack> packsWithNamespace = map.get(location.getNamespace());
		return packsWithNamespace == null ? Collections.emptyList() : packsWithNamespace;
	}

	private static String getFilename(ResourceType type, Identifier location) {
		// stolen from ResourcePack
		return String.format("%s/%s/%s", type.getDirectory(), location.getNamespace(), location.getPath());
	}

}
