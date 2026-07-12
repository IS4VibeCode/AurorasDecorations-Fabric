/*
 * Copyright (c) 2021 - 2023 LambdAurora <email@lambdaurora.dev>
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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import com.mojang.logging.LogUtils;
import dev.lambdaurora.aurorasdeco.AurorasDeco;
import dev.lambdaurora.aurorasdeco.block.*;
import dev.lambdaurora.aurorasdeco.registry.LanternRegistry;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.registry.Registries;
import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.metadata.ResourceMetadataReader;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * A dynamically-generated, in-memory resource pack -- its contents depend on which wood types /
 * blocks were actually discovered at runtime, so it can't be file-backed.
 * <p>
 * QSL's {@code InMemoryResourcePack} provided this as a convenience base class; Fabric has no
 * equivalent (confirmed by checking Fabric API's {@code ModResourcePack}, which is a bare marker
 * interface extending vanilla {@link ResourcePack} with zero storage convenience of its own -- see
 * java/CLAUDE.md §3/Task #18). This class implements {@link ResourcePack} directly instead, backed
 * by plain in-memory maps.
 * <p>
 * Getting this pack actually treated as an always-active default pack (QSL's
 * {@code getRegisterDefaultResourcePackEvent()}, which Fabric also has no equivalent for) is handled
 * separately -- see {@link #registerAsDefaultPack} and {@code ResourcePackManagerMixin}. That part is
 * unverified against a real build/run; everything else in this class is a direct, checked port of
 * vanilla's real {@link ResourcePack} interface (net.minecraft.resource, Yarn 1.20.1+build.10).
 */
public class AurorasDecoPack implements ResourcePack {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final List<AurorasDecoPack> DEFAULT_PACKS = new ArrayList<>();

	private final ResourceType type;
	private final Map<Identifier, byte[]> assets = new ConcurrentHashMap<>();
	private final Map<Identifier, byte[]> data = new ConcurrentHashMap<>();
	private final Map<String, byte[]> root = new ConcurrentHashMap<>();

	private boolean hasRegisteredOneTimeResources = false;

	public AurorasDecoPack(ResourceType type) {
		this.type = type;
	}

	/**
	 * Registers {@code pack} to always be part of the active resource pack list, regardless of the
	 * user's own pack selection -- see {@code ResourcePackManagerMixin}/{@code AurorasDecoResourcePackProvider}.
	 */
	public static void registerAsDefaultPack(AurorasDecoPack pack) {
		DEFAULT_PACKS.add(pack);
	}

	public static List<AurorasDecoPack> getDefaultPacks() {
		return DEFAULT_PACKS;
	}

	public ResourceType getType() {
		return this.type;
	}

	public AurorasDecoPack rebuild(ResourceType type, @Nullable ResourceManager resourceManager) {
		this.registerTag(new String[]{"blocks"}, Identifier.of("flower_pots"), HangingFlowerPotBlock.stream()
				.map(Registries.BLOCK::getId));

		this.registerTag(new String[]{"blocks", "items"}, AurorasDeco.id("benches"), BenchBlock.streamBenches()
				.map(Registries.BLOCK::getId));
		this.registerTag(new String[]{"blocks", "items"}, AurorasDeco.id("shelves"), ShelfBlock.streamShelves()
				.map(Registries.BLOCK::getId));
		this.registerTag(new String[]{"blocks"}, Identifier.of("mineable/axe"), SignPostBlock.stream() // @TODO: FIX THIS SO IT DOESN'T FUCK THE STONE FENCES
				.map(Registries.BLOCK::getId));
		this.registerTag(new String[]{"blocks", "items"}, AurorasDeco.id("small_log_piles"), SmallLogPileBlock.stream()
				.map(Registries.BLOCK::getId));
		this.registerTag(new String[]{"blocks", "items"}, AurorasDeco.id("stumps"), StumpBlock.streamLogStumps()
				.map(Registries.BLOCK::getId));
		this.registerTag(new String[]{"blocks"}, AurorasDeco.id("wall_lanterns"), LanternRegistry.streamIds());

		return type == ResourceType.CLIENT_RESOURCES ? this.rebuildClient(resourceManager) : this.rebuildData();
	}

	public AurorasDecoPack rebuildClient(ResourceManager resourceManager) {
		Datagen.generateClientData(resourceManager);

		return this;
	}

	private void registerTag(String[] types, Identifier id, Stream<Identifier> entries) {
		var root = new JsonObject();
		root.addProperty("replace", false);
		var values = new JsonArray();

		entries.forEach(value -> values.add(value.toString()));

		root.add("values", values);

		for (var type : types) {
			this.putJson(ResourceType.SERVER_DATA, Identifier.of(id.getNamespace(), "tags/" + type + "/" + id.getPath()), root);
		}
	}

	public AurorasDecoPack rebuildData() {
		if (!this.hasRegisteredOneTimeResources) {
			Datagen.registerDefaultRecipes();
			Datagen.registerDefaultWoodcuttingRecipes();
			this.hasRegisteredOneTimeResources = true;
		}

		BenchBlock.streamBenches().forEach(Datagen::registerBenchBlockLootTable);
		ExtendedCandleBlock.stream().forEach(Datagen::registerCandleLikeBlockLootTable);
		ShelfBlock.streamShelves().forEach(Datagen::registerDoubleBlockLootTable);
		SmallLogPileBlock.stream().forEach(Datagen::registerDoubleBlockLootTable);
		StumpBlock.streamLogStumps().forEach(Datagen::dropsSelf);

		return this;
	}

	/* Storage */

	public void putText(ResourceType type, Identifier id, String text) {
		this.getResourceMap(type).put(id, text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
	}

	public void putJsonText(ResourceType type, Identifier id, String json) {
		this.putText(type, Identifier.of(id.getNamespace(), id.getPath() + ".json"), json);
	}

	public void putJson(ResourceType type, Identifier id, JsonObject json) {
		if (!id.getPath().endsWith(".json")) id = Identifier.of(id.getNamespace(), id.getPath() + ".json");

		var stringWriter = new StringWriter();
		var jsonWriter = new JsonWriter(stringWriter);
		jsonWriter.setLenient(true);
		jsonWriter.setIndent("  ");
		try {
			Streams.write(json, jsonWriter);
		} catch (IOException e) {
			LOGGER.error("Failed to write JSON at {}.", id, e);
		}

		this.putText(type, id, stringWriter.toString());
	}

	public void putImage(Identifier id, NativeImage image) {
		if (!id.getPath().endsWith(".png")) id = Identifier.of(id.getNamespace(), "textures/" + id.getPath() + ".png");
		try {
			this.assets.put(id, image.getBytes());
		} catch (IOException e) {
			LOGGER.warn("Could not encode texture " + id + " to PNG.", e);
		}
	}

	private Map<Identifier, byte[]> getResourceMap(ResourceType type) {
		return switch (type) {
			case CLIENT_RESOURCES -> this.assets;
			case SERVER_DATA -> this.data;
		};
	}

	/* ResourcePack */

	@Override
	public @Nullable InputSupplier<InputStream> openRoot(String... path) {
		var bytes = this.root.get(String.join("/", path));
		if (bytes == null) return null;
		return () -> new ByteArrayInputStream(bytes);
	}

	@Override
	public @Nullable InputSupplier<InputStream> open(ResourceType type, Identifier id) {
		var bytes = this.getResourceMap(type).get(id);
		if (bytes == null) return null;
		return () -> new ByteArrayInputStream(bytes);
	}

	@Override
	public void findResources(ResourceType type, String namespace, String startingPath, ResultConsumer consumer) {
		this.getResourceMap(type).forEach((id, bytes) -> {
			if (id.getNamespace().equals(namespace) && id.getPath().startsWith(startingPath)) {
				consumer.accept(id, () -> new ByteArrayInputStream(bytes));
			}
		});
	}

	/**
	 * The full set of namespaces this pack can ever contribute content under, regardless of whether
	 * {@link #rebuild} has populated any content for them yet.
	 * <p>
	 * {@code getNamespaces} must NOT derive this from the current, possibly-still-empty contents of
	 * {@link #assets}/{@link #data} -- {@link net.minecraft.resource.LifecycledResourceManagerImpl}'s
	 * constructor calls it exactly once, synchronously, at the moment a reload's {@code ResourceManager}
	 * is assembled, which happens <em>before</em> any {@link net.minecraft.resource.ResourceReloader}
	 * (including the listener that actually calls {@link #rebuild}) has run. On a fresh client launch
	 * this pack's maps are still empty at that instant, so a derived answer would be the empty set --
	 * permanently, for that whole reload cycle, since namespace-to-pack routing is frozen at
	 * construction while only each pack's own content stays mutable afterward. Confirmed as the actual
	 * cause of every dynamically-generated block (benches, stumps, wall lanterns, ...) rendering as the
	 * missing-model placeholder on a real load test: the pack's content was correctly generated, and
	 * even included in the reload's pack list, but registered under zero namespaces, so nothing was ever
	 * routed to it. A fixed, namespace-timing-independent answer sidesteps the race entirely: this pack
	 * always writes under {@code aurorasdeco} (via {@link Datagen}) and vanilla-namespaced tag overrides
	 * (via {@link #registerTag}, e.g. {@code minecraft:tags/blocks/flower_pots.json}), so those two are
	 * always claimed regardless of generation timing.
	 */
	private static final Set<String> NAMESPACES = Set.of(AurorasDeco.NAMESPACE, "minecraft");

	@Override
	public Set<String> getNamespaces(ResourceType type) {
		return NAMESPACES;
	}

	@Override
	public @Nullable <T> T parseMetadata(ResourceMetadataReader<T> metaReader) throws IOException {
		// This virtual pack has no real pack.mcmeta -- synthesize just enough of one to satisfy the
		// "pack" key vanilla always checks when loading any resource pack. Any other metadata key
		// (e.g. mod-specific ones) is simply absent from this pack.
		if (!metaReader.getKey().equals("pack")) return null;

		var packJson = new JsonObject();
		packJson.addProperty("description", "Aurora's Decorations dynamically-generated data.");
		packJson.addProperty("pack_format", 15); // 1.20.1's data/resource pack_format.

		var json = new JsonObject();
		json.add("pack", packJson);

		return metaReader.fromJson(net.minecraft.util.JsonHelper.getObject(json, "pack"));
	}

	@Override
	public void close() {
	}

	@Override
	public String getId() {
		return "Aurora's Decorations Virtual Pack";
	}
}
