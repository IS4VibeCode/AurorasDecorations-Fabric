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

package dev.lambdaurora.aurorasdeco.mixin;

import dev.lambdaurora.aurorasdeco.resource.AurorasDecoPack;
import dev.lambdaurora.aurorasdeco.resource.AurorasDecoResourcePackProvider;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProvider;
import net.minecraft.resource.ResourceType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Adds {@link AurorasDecoResourcePackProvider} to every {@link ResourcePackManager} as it's
 * constructed, regardless of who's constructing it (client or server) -- this only needs to hook the
 * manager's own constructor, not chase down every call site that builds one.
 * <p>
 * QSL's {@code getRegisterDefaultResourcePackEvent()} did this job on Quilt; Fabric has no equivalent
 * event (java/CLAUDE.md §3/Task #18).
 * <p>
 * <b>The provider-based registration above was confirmed NOT sufficient by a real load test</b> (the
 * dynamic pack's synthetic name never appeared in vanilla's own "Reloading ResourceManager: {}" log
 * line), so {@link ResourcePackManager#createResourcePacks()} is also force-appended to directly --
 * but only for the {@code SERVER_DATA}-typed default pack now (see {@code ReloadableResourceManagerMixin} for the
 * {@code CLIENT_RESOURCES} side, java/CLAUDE.md §3n). Splitting the two matters: unlike the client
 * pack, {@link AurorasDecoPack#rebuildData()} needs no live {@link net.minecraft.resource.ResourceManager}
 * (it only registers recipes/tags/loot tables from already-registered blocks), so it's safe -- and,
 * confirmed by direct inspection, <em>necessary</em> -- to rebuild it right here: nothing anywhere in
 * this codebase was ever calling {@code rebuildData()} at all, meaning every dynamically-generated
 * recipe/tag/loot-table for wood-type-derived content (benches, stumps, shelves, ...) was silently
 * absent server-side, entirely independently of whatever pack-inclusion bug this class also fixes.
 * {@link ResourcePackManager#createResourcePacks()} is the right place for this because, confirmed via
 * {@code SaveLoading.DataPacks.load()}'s own bytecode, it's called immediately before <em>every</em>
 * construction of a {@code LifecycledResourceManagerImpl} for {@code SERVER_DATA} -- both at server/
 * world startup and on {@code /reload} -- covering the server side unconditionally, the same way it
 * already covers pack inclusion for both types.
 */
@Mixin(ResourcePackManager.class)
public class ResourcePackManagerMixin {
	@Inject(method = "<init>", at = @At("RETURN"))
	private void aurorasdeco$addProvider(ResourcePackProvider[] providers, CallbackInfo ci) {
		var accessor = (ResourcePackManagerAccessor) this;
		var newProviders = new LinkedHashSet<ResourcePackProvider>(accessor.aurorasdeco$getProviders());
		newProviders.add(new AurorasDecoResourcePackProvider());
		accessor.aurorasdeco$setProviders(newProviders);
	}

	@Inject(method = "createResourcePacks", at = @At("RETURN"), cancellable = true)
	private void aurorasdeco$includeAndRebuildServerDataPacks(CallbackInfoReturnable<List<ResourcePack>> cir) {
		var toRebuild = AurorasDecoPack.getDefaultPacks().stream()
				.filter(pack -> pack.getType() == ResourceType.SERVER_DATA)
				.toList();
		if (toRebuild.isEmpty()) return;

		var packs = new ArrayList<ResourcePack>(cir.getReturnValue());
		for (var pack : toRebuild) {
			packs.remove(pack);
			packs.add(0, pack.rebuild(ResourceType.SERVER_DATA, null));
		}
		cir.setReturnValue(packs);
	}
}
