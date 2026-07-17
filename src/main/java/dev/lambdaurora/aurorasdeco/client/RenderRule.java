/*
 * Copyright (c) 2021 LambdAurora <email@lambdaurora.dev>
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

package dev.lambdaurora.aurorasdeco.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Represents a render rule.
 * <p>
 * Render rules can be used to change the default rendering of an item in a specific context like shelves.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
@Environment(EnvType.CLIENT)
public record RenderRule(List<Model> models) {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Map<Identifier, RenderRule> ITEM_RULES = new Object2ObjectOpenHashMap<>();
	private static final Map<TagKey<Item>, RenderRule> TAG_RULES = new Object2ObjectOpenHashMap<>();

	public @Nullable Model getModelId(ItemStack stack, BlockState state, long seed) {
		if (this.models.size() == 1) {
			Model model = this.models.get(0);
			return model.test(stack, state) ? model : null;
		} else {
			final int i = Math.abs(Objects.hash(stack.getCount(), stack.getName().getString(), seed) % this.models.size());
			int actualI = i;

			Model model;
			do {
				model = this.models.get(actualI);

				actualI++;
				if (actualI >= this.models.size())
					actualI = 0;

				if (actualI == i)
					return null;
			} while (!model.test(stack, state));
			return model;
		}
	}

	public @Nullable BakedModel getModel(ItemStack stack, BlockState state, long seed) {
		var model = this.getModelId(stack, state, seed);
		return model == null ? null : model.getModel();
	}

	public static @Nullable RenderRule getRenderRule(ItemStack stack) {
		var itemId = Registries.ITEM.getId(stack.getItem());

		var rule = ITEM_RULES.get(itemId);
		if (rule != null) {
			return rule;
		}

		for (var entry : TAG_RULES.entrySet()) {
			if (stack.isIn(entry.getKey())) {
				return entry.getValue();
			}
		}

		return null;
	}

	public static BakedModel getModel(ItemStack stack, BlockState state, World world, long seed) {
		BakedModel model = null;

		var rule = RenderRule.getRenderRule(stack);
		if (rule != null)
			model = rule.getModel(stack, state, seed);

		if (model == null)
			return MinecraftClient.getInstance().getItemRenderer().getModel(stack, world, null, 0);
		return model;
	}

	public static void addModels(ModelLoadingPlugin.Context context) {
		ITEM_RULES.values().stream().flatMap(rule -> rule.models().stream()).map(Model::modelId).map(RenderRule::toRequestedModelPath).forEach(context::addModels);
		TAG_RULES.values().stream().flatMap(rule -> rule.models().stream()).map(Model::modelId).map(RenderRule::toRequestedModelPath).forEach(context::addModels);
	}

	/**
	 * Confirmed real via a live-test log ({@code FileNotFoundException} at
	 * {@code aurorasdeco:models/blackboard_base.json}, and the same for every {@code special/book/*}
	 * model): {@code ModelLoadingPlugin.Context#addModels} only ever accepts a bare {@code Identifier}
	 * in 1.21.1 and resolves it straight to {@code models/<path>.json}, with no notion of an
	 * "inventory"/item-model variant at all. The pre-port (Quilt, 1.20.1) code passed a
	 * {@code ModelIdentifier} directly to the equivalent call -- back then {@code ModelIdentifier}
	 * itself extended {@code Identifier}, and the model loader resolved an "inventory"-variant one
	 * through the real {@code models/item/} convention these assets have always shipped under
	 * (confirmed via git history: {@code models/item/blackboard_base.json} etc. have never moved). In
	 * 1.21, {@code ModelIdentifier} became a plain record (id + variant, no longer an {@code Identifier}
	 * subclass) -- porting this call site to compile against the new API (extracting {@code
	 * ModelIdentifier#id()}) was necessary, but it silently dropped the variant that the item-model
	 * convention depended on, since the new {@code addModels} has no way to receive it at all. Baking
	 * {@code item/} into the path ourselves for the "inventory" case restores the real resolved location.
	 */
	public static Identifier toRequestedModelPath(ModelIdentifier modelId) {
		if (modelId.getVariant().equals(ModelIdentifier.INVENTORY_VARIANT)) {
			return modelId.id().withPrefixedPath("item/");
		}
		return modelId.id();
	}

	/**
	 * Parses every {@code aurorasdeco/render_rules/*.json} resource into a snapshot, without touching
	 * {@link #ITEM_RULES}/{@link #TAG_RULES} yet.
	 * <p>
	 * Previously this parsing (as {@code reload(ResourceManager)}) ran inside a plain
	 * {@code SimpleSynchronousResourceReloadListener}, entirely independent of the {@link
	 * ModelLoadingPlugin#register} callback that reads {@link #ITEM_RULES}/{@link #TAG_RULES} in {@link
	 * #addModels}. Nothing tied those two together: on any reload where the model-loading pass happened
	 * to run before that listener (Fabric's own reload-listener graph has no dependency between them,
	 * since neither side declared one), {@code addModels} would request baking for whatever the *previous*
	 * reload's rules were -- or nothing at all, on the very first load -- silently leaving every
	 * render-rule book/etc. model unbaked and rendering as the missing-model placeholder. Moved to
	 * {@link net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin}, which exists
	 * specifically for this "load external data, then use it while registering models" sequencing: its
	 * {@code DataLoader} (this method) is guaranteed to complete before {@link #apply} runs, which is in
	 * turn guaranteed to run before model baking -- no reload-listener ordering race possible.
	 */
	public static CompletableFuture<RuleSet> load(ResourceManager manager, Executor executor) {
		return CompletableFuture.supplyAsync(() -> {
			Map<Identifier, RenderRule> itemRules = new Object2ObjectOpenHashMap<>();
			Map<TagKey<Item>, RenderRule> tagRules = new Object2ObjectOpenHashMap<>();

			manager.findResources("aurorasdeco/render_rules", path -> path.getPath().endsWith(".json")).forEach((id, resource) -> {
				try (var reader = new InputStreamReader(resource.getInputStream())) {
					var element = JsonParser.parseReader(reader);
					if (element.isJsonObject()) {
						var root = element.getAsJsonObject();

						var models = new ArrayList<Model>();
						var modelsJson = root.getAsJsonArray("models");
						modelsJson.forEach(modelElement -> {
							var model = Model.readModelPredicate(id, modelElement);
							if (model != null)
								models.add(model);
						});

						if (models.isEmpty())
							return;

						var renderRule = new RenderRule(models);

						var match = root.getAsJsonObject("match");
						if (match.has("item")) {
							itemRules.put(Identifier.tryParse(match.get("item").getAsString()), renderRule);
						} else if (match.has("items")) {
							var array = match.getAsJsonArray("items");
							for (var item : array) {
								itemRules.put(Identifier.tryParse(item.getAsString()), renderRule);
							}
						} else if (match.has("tag")) {
							var tagId = Identifier.tryParse(match.get("tag").getAsString());
							tagRules.put(TagKey.of(RegistryKeys.ITEM, tagId), renderRule);
						}
					}
				} catch (Exception e) {
					LOGGER.error("Failed to read render rule {}.", id, e);
				}
			});

			return new RuleSet(itemRules, tagRules);
		}, executor);
	}

	/**
	 * Installs a {@link RuleSet} produced by {@link #load} and requests baking for every model it
	 * references, in one atomic step -- see {@link #load}'s doc comment for why both need to happen
	 * together rather than as two independently-ordered steps.
	 */
	public static void apply(RuleSet ruleSet, ModelLoadingPlugin.Context context) {
		ITEM_RULES.clear();
		ITEM_RULES.putAll(ruleSet.itemRules());
		TAG_RULES.clear();
		TAG_RULES.putAll(ruleSet.tagRules());
		addModels(context);
	}

	public record RuleSet(Map<Identifier, RenderRule> itemRules, Map<TagKey<Item>, RenderRule> tagRules) {}

	public record Model(ModelIdentifier modelId, @Nullable Block restrictedBlock, @Nullable TagKey<Block> restrictedBlockTag) {
		public boolean test(ItemStack stack, BlockState state) {
			if (this.restrictedBlock != null) {
				return state.isOf(this.restrictedBlock);
			} else if (this.restrictedBlockTag != null) {
				return state.isIn(this.restrictedBlockTag);
			}
			return true;
		}

		public BakedModel getModel() {
			// A model registered via Context#addModels has no corresponding ModelIdentifier at all
			// (confirmed via FabricBakedModelManager's own doc comment) -- it must be retrieved through
			// the Identifier-keyed overload using the exact same path addModels was given, not vanilla's
			// ModelIdentifier-keyed getModel (which queries a completely different, never-baked entry).
			// This was the actual reason books/blackboards still rendered as missing-model even after
			// toRequestedModelPath fixed the *registration* path -- the *retrieval* side needed the
			// identical fix.
			return MinecraftClient.getInstance().getBakedModelManager().getModel(toRequestedModelPath(this.modelId));
		}

		public static @Nullable Model readModelPredicate(Identifier manifest, JsonElement json) {
			if (json.isJsonPrimitive()) {
				var modelId = Identifier.tryParse(json.getAsString());
				if (modelId == null) {
					LOGGER.error("Failed to parse model identifier {} in render rule {}.", json.getAsString(), manifest);
					return null;
				}

				return new Model(new ModelIdentifier(modelId, "inventory"), null, null);
			}

			var object = json.getAsJsonObject();

			if (!object.has("model")) {
				LOGGER.error("Failed to parse model entry in render rule {}, missing model field.", manifest);
				return null;
			}

			var modelId = Identifier.tryParse(object.get("model").getAsString());
			if (modelId == null) {
				LOGGER.error("Failed to parse model identifier {} in render rule {}.", json.getAsString(), manifest);
				return null;
			}

			Block restrictedBlock = null;
			TagKey<Block> restrictedBlockTag = null;
			if (object.has("restrict_to")) {
				var restrict = object.getAsJsonObject("restrict_to");
				if (restrict.has("block")) {
					var blockId = Identifier.tryParse(restrict.get("block").getAsString());
					if (blockId == null) {
						LOGGER.error("Failed to parse block identifier in render rule {}.", manifest);
					} else {
						restrictedBlock = Registries.BLOCK.get(blockId);
					}
				} else if (restrict.has("tag")) {
					var blockId = Identifier.tryParse(restrict.get("tag").getAsString());
					if (blockId == null) {
						LOGGER.error("Failed to parse tag identifier in render rule {}.", manifest);
					} else {
						restrictedBlockTag = TagKey.of(RegistryKeys.BLOCK, blockId);
					}
				}
			}
			return new Model(new ModelIdentifier(modelId, "inventory"), restrictedBlock, restrictedBlockTag);
		}
	}
}
