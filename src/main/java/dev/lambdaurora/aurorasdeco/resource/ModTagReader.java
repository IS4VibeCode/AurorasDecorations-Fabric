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
import net.minecraft.registry.tag.TagManagerLoader;
import net.minecraft.resource.DirectoryResourcePack;
import net.minecraft.resource.ReloadableResourceManagerImpl;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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
		var groupLoader = new TagGroupLoader<>(Optional::of, TagManagerLoader.getPath(registryKey));
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
	 * equivalent -- {@code ReloadableResourceManagerImpl} takes only a {@code ResourceType} and needs
	 * an explicit, otherwise-async {@code reload(...)} call to attach packs. No {@code ResourceReloader}s
	 * are registered here (only raw pack-backed resource lookup is needed, not any data-processing
	 * reload listener), and {@code Runnable::run} as both executors makes the reload run synchronously
	 * on the calling thread, so blocking on {@code whenComplete()} is safe and immediate.
	 */
	private ResourceManager createResourceManager() {
		var resourcePacks = new ArrayList<ResourcePack>();

		for (var mod : FabricLoader.getInstance().getAllMods()) {
			for (var root : mod.getRootPaths()) {
				resourcePacks.add(new DirectoryResourcePack(mod.getMetadata().getId(), root, false));
			}
		}

		var manager = new ReloadableResourceManagerImpl(ResourceType.SERVER_DATA);
		manager.reload(Runnable::run, Runnable::run, CompletableFuture.completedFuture(Unit.INSTANCE), resourcePacks)
				.whenComplete().join();
		return manager;
	}

	private ResourceManager getResourceManager() {
		if (this.resourceManager == null) {
			this.resourceManager = this.createResourceManager();
		}

		return this.resourceManager;
	}
}
