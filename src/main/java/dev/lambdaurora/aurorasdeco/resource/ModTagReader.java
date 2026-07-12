/*
 * Copyright (c) 2023 LambdAurora <email@lambdaurora.dev>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package dev.lambdaurora.aurorasdeco.resource;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagGroupLoader;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.DirectoryResourcePack;
import net.minecraft.resource.LifecycledResourceManagerImpl;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Mojang removed Material, so I will read tags, I am a menace.
 */
public class ModTagReader {
	public static final ModTagReader INSTANCE = new ModTagReader();
	private final Map<TagKey<?>, Collection<Identifier>> tags = new Object2ObjectOpenHashMap<>();
	private boolean loaded = false;
	private ResourceManager resourceManager;

	public void load() {
		if (!this.loaded) {
			this.loaded = true;
			this.loadTags(RegistryKeys.BLOCK);
		}
	}

	public <T> void loadTags(RegistryKey<Registry<T>> registryKey) {
		var groupLoader = new TagGroupLoader<>(Optional::of, RegistryKeys.getTagPath(registryKey));
		groupLoader.load(this.getResourceManager()).forEach((id, values) -> {
			this.tags.put(TagKey.of(registryKey, id), values);
		});
	}

	public Collection<Identifier> getValues(TagKey<?> tagKey) {
		this.load();
		return this.tags.get(tagKey);
	}

	/**
	 * QSL's {@code ResourceLoaderImpl.appendModResourcePacks} has no Fabric equivalent -- rebuilt here
	 * using {@code FabricLoader.getAllMods()}, which includes a synthetic "minecraft" container for the
	 * game itself, so this covers vanilla's own tag files the same way it covers every other mod's,
	 * with no separate "default pack" builder needed. Each mod's own root path(s) get wrapped as a
	 * {@link DirectoryResourcePack} -- works for both loose directories (dev environment) and real
	 * jars, since Fabric Loader's NIO paths transparently work either way.
	 * <p>
	 * Quilt Mappings' {@code MultiPackResourceManager(ResourceType, List)} one-liner has no real Yarn
	 * equivalent, but {@link LifecycledResourceManagerImpl}'s own constructor is exactly that: a static,
	 * ready-to-use {@code ResourceManager} view over a list of packs, no reload orchestration involved.
	 * Constructing it directly (rather than the higher-level {@code ReloadableResourceManagerImpl.reload(...)})
	 * is required, not just simpler: {@code fabric_resource_loader_v0}'s own mixin
	 * (SimpleResourceReloadMixin) hooks into *every* {@code reload(...)} call unconditionally and sorts
	 * resource reload listeners assuming a real {@code RecipeManager} listener is present -- true for the
	 * game's own full reload, never true for this throwaway, listener-less tag-reading manager, so calling
	 * {@code reload(...)} here threw {@code IllegalStateException: No RecipeManager found in listeners!}
	 * on a real 1.21.1/Connector load. {@link LifecycledResourceManagerImpl}'s constructor never touches
	 * that machinery at all, matching what this method actually needs: raw pack-backed resource lookup,
	 * no data-processing reload listener.
	 */
	private ResourceManager createResourceManager() {
		var resourcePacks = new ArrayList<ResourcePack>();

		for (var mod : FabricLoader.getInstance().getAllMods()) {
			for (var root : mod.getRootPaths()) {
				// Most of the ~300+ mods in a real pack have no "data" directory at all (client-only
				// visual/rendering mods, resource-pack-only mods, ...). Wrapping them as a
				// DirectoryResourcePack anyway is harmless in principle, but Quilt Loader's own mapped
				// jar filesystem (QuiltMapFileSystemProvider) throws NotDirectoryException rather than
				// NoSuchFileException when probing a path that simply doesn't exist -- vanilla catches
				// it and logs an ERROR with a full stack trace per mod instead of silently skipping,
				// confirmed as pure log noise (~540 occurrences in a real load, one per data-less mod)
				// with zero effect on tag loading correctness. Skip mods with no "data" directory
				// outright to avoid manufacturing this noise ourselves.
				if (!java.nio.file.Files.isDirectory(root.resolve("data"))) continue;

				var packInfo = new net.minecraft.resource.ResourcePackInfo(
						mod.getMetadata().getId(),
						net.minecraft.text.Text.literal(mod.getMetadata().getId()),
						net.minecraft.resource.ResourcePackSource.NONE,
						Optional.empty()
				);
				resourcePacks.add(new DirectoryResourcePack(packInfo, root));
			}
		}

		return new LifecycledResourceManagerImpl(ResourceType.SERVER_DATA, resourcePacks);
	}

	private ResourceManager getResourceManager() {
		if (this.resourceManager == null) {
			this.resourceManager = this.createResourceManager();
		}

		return this.resourceManager;
	}
}
