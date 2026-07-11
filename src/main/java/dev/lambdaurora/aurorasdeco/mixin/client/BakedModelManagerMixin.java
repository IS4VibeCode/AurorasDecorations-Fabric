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

package dev.lambdaurora.aurorasdeco.mixin.client;

import dev.lambdaurora.aurorasdeco.client.AurorasDecoClient;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.profiler.Profiler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * {@code AurorasDecoClient}'s {@code reload/render_rules} listener is what actually generates every
 * dynamically-derived blockstate/model JSON (stumps, benches, shelves, sign posts, etc. -- one set per
 * discovered wood type) and writes it into {@code AurorasDecoPack}. Fabric's
 * {@code IdentifiableResourceReloadListener#getFabricDependencies()} only lets a listener declare what
 * it must run <em>after</em> -- there is no way to make {@code BakedModelManager} (Fabric's own
 * {@code ResourceReloadListenerKeys.MODELS}) depend on a third-party listener, since its own dependency
 * list is hardcoded to {@code TEXTURES} only (confirmed by decompiling Fabric API's
 * {@code KeyedResourceReloadListenerClientMixin}). Without forcing the order some other way, the model
 * loader's reload can win the race and read an empty pack -- confirmed as the real cause of every
 * dynamically-generated block rendering as the missing-model placeholder cube on load. Injecting at the
 * very start of {@code BakedModelManager.reload(...)} and rebuilding {@code AurorasDecoPack} synchronously
 * before vanilla proceeds guarantees the content exists by the time this method's own resource reads
 * happen, regardless of Fabric's listener-ordering graph.
 */
@Environment(EnvType.CLIENT)
@Mixin(BakedModelManager.class)
public class BakedModelManagerMixin {
	@Inject(method = "reload", at = @At("HEAD"))
	private void aurorasdeco$rebuildResourcePackBeforeReload(ResourceReloader.Synchronizer synchronizer, ResourceManager manager,
			Profiler prepareProfiler, Profiler applyProfiler, Executor prepareExecutor, Executor applyExecutor,
			CallbackInfoReturnable<CompletableFuture<Void>> cir) {
		AurorasDecoClient.RESOURCE_PACK.rebuild(ResourceType.CLIENT_RESOURCES, manager);
	}
}
