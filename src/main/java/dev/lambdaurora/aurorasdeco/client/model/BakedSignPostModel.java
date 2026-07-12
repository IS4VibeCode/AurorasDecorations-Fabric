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

package dev.lambdaurora.aurorasdeco.client.model;

import com.mojang.logging.LogUtils;
import dev.lambdaurora.aurorasdeco.block.SignPostBlock;
import net.fabricmc.fabric.api.client.model.loading.v1.BlockStateResolver;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.render.model.json.ModelVariantMap;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.function.Supplier;

/**
 * Represents the baked model of the sign post block.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
@Environment(EnvType.CLIENT)
public class BakedSignPostModel extends ForwardingBakedModel {
	public BakedSignPostModel(BakedModel fenceModel) {
		this.wrapped = fenceModel;
	}

	@Override
	public boolean isVanillaAdapter() {
		return false;
	}

	@Override
	public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier,
			RenderContext context) {
		if (state.getBlock() instanceof SignPostBlock signPostBlock) {
			this.wrapped.emitBlockQuads(blockView, signPostBlock.getFenceState(state), pos, randomSupplier, context);
		}
	}

	public record Provider(SignPostBlock signPostBlock) implements BlockStateResolver {
		private static final Logger LOGGER = LogUtils.getLogger();

		@Override
		public void resolveBlockStates(Context context) {
			// Context#getOrLoadModel(Identifier) only ever consults ModelLoader's file-backed
			// `unbakedModels` map (loads `models/<path>.json` verbatim) -- confirmed via bytecode,
			// same class of bug already found once for UnbakedGlassboardModel. Fences are declared
			// with a "multipart" blockstate definition (post/side parts picked per connection), so
			// there is no flat `models/block/<fence>.json` file matching the bare block id -- looking
			// one up here always missed and rendered as a missing-model (purple-black) cube.
			//
			// The fix mirrors RestModelManager's bench-rest loading: parse the fence block's own
			// blockstates/*.json directly via ModelVariantMap and take its multipart model. That
			// model's MultipartBakedModel re-evaluates each part's predicate against whatever
			// BlockState is passed to emitBlockQuads at render time -- exactly what
			// BakedSignPostModel#emitBlockQuads already does by handing it the live fenceState -- so a
			// single shared multipart model correctly covers every connection combination without
			// needing to be reloaded per sign post state.
			var fenceBlock = this.signPostBlock.getFenceBlock();
			var fenceModel = loadFenceMultipartModel(fenceBlock);

			if (fenceModel == null) {
				return;
			}

			var states = this.signPostBlock.getStateManager().getStates();
			for (var state : states) {
				context.setModel(state, new UnbakedForwardingModel(fenceModel, BakedSignPostModel::new));
			}
		}

		// getModelDependencies()/setParents() are forwarded by UnbakedForwardingModel (see that class),
		// so the standard model-loading pipeline resolves this model's own sub-part references (e.g.
		// aurorasdeco:block/jacaranda_fence_post) the same way it already does for every other custom
		// model registered through Context#setModel -- no manual dependency preloading needed here,
		// mirroring RestModelManager's bench-rest loading, which relies on the same pipeline behavior.
		private static UnbakedModel loadFenceMultipartModel(Block fenceBlock) {
			var fenceId = Registries.BLOCK.getId(fenceBlock);
			var blockStatesId = fenceId.withPrefixedPath("blockstates/").withSuffixedPath(".json");
			var resourceManager = MinecraftClient.getInstance().getResourceManager();
			var resource = resourceManager.getResource(blockStatesId);

			if (resource.isEmpty()) {
				LOGGER.warn("Could not load the fence model for sign post ({}): could not locate its blockstate file.", fenceId);
				return null;
			}

			try (var reader = new InputStreamReader(resource.get().getInputStream())) {
				var deserializationContext = new ModelVariantMap.DeserializationContext();
				deserializationContext.setStateFactory(fenceBlock.getStateManager());
				var map = ModelVariantMap.fromJson(deserializationContext, reader);
				var multipartModel = map.getMultipartModel();

				if (multipartModel == null) {
					LOGGER.warn("Could not load the fence model for sign post ({}): blockstate file has no multipart definition.", fenceId);
				}

				return multipartModel;
			} catch (IOException e) {
				LOGGER.warn("Could not load the fence model for sign post ({}):", fenceId, e);
				return null;
			}
		}
	}
}
