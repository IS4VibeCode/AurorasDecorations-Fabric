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

import dev.lambdaurora.aurorasdeco.resource.AurorasDecoPack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resource.LifecycledResourceManagerImpl;
import net.minecraft.resource.ReloadableResourceManagerImpl;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.ArrayList;
import java.util.List;

/**
 * Ports the original pre-Fabric-port mod's 2022 design (java/CLAUDE.md §3n) for getting
 * {@code AurorasDecoPack} included in an actual reload, replacing the earlier three-piece
 * {@code BakedModelManagerMixin} + {@code ResourcePackManagerMixin} force-append + hardened
 * {@code AurorasDecoPack#getNamespaces} workaround.
 * <p>
 * The idea: intercept the {@code packs} argument right where {@link ReloadableResourceManagerImpl#reload}
 * constructs its {@link LifecycledResourceManagerImpl}, and reuse this same interception point to
 * rebuild the pack's content synchronously <em>before</em> insertion. Because rebuild-then-insert
 * happens as one atomic step here, this structurally cannot hit the namespace-timing bug the old
 * three-piece fix needed a workaround for -- the manager under construction never sees the pack before
 * its content exists, so {@code getNamespaces()} always derives a correct answer. (The old hardened
 * {@code getNamespaces()} override is left in place regardless -- a fixed, timing-independent answer
 * is strictly simpler and doesn't depend on this mixin's correctness either.)
 * <p>
 * A "mirror" {@code LifecycledResourceManagerImpl} is built from {@code packs} <em>before</em>
 * {@code AurorasDecoPack} is added to it, and passed as the {@code ResourceManager} datagen reads
 * other mods' textures from -- avoiding a self-referential read of the not-yet-generated pack while
 * generating it. Constructing this mirror does not re-trigger this same mixin recursively, since the
 * mixin targets {@link ReloadableResourceManagerImpl} (the wrapper), not
 * {@link LifecycledResourceManagerImpl} (the wrapped class actually being constructed) -- mixing into
 * the wrapped class directly was tried and rejected specifically because building a mirror of it from
 * within its own mixin recurses forever.
 * <p>
 * {@link ReloadableResourceManagerImpl} is confirmed client-only: scanning every class in the game jar
 * for a reference to it found only {@code MinecraftClient} besides itself. The server's own datapack
 * loading (world/server startup, {@code /reload}) constructs {@link LifecycledResourceManagerImpl}
 * directly via {@code SaveLoading.DataPacks}/{@code MinecraftServer}, without this wrapper -- see
 * {@code ResourcePackManagerMixin} for how the server side ({@code SERVER_DATA}, which needs no live
 * {@code ResourceManager} to rebuild) is covered instead.
 */
@Environment(EnvType.CLIENT)
@Mixin(ReloadableResourceManagerImpl.class)
public abstract class ReloadableResourceManagerMixin {
	@Shadow
	@Final
	private ResourceType type;

	@ModifyArg(
			method = "reload",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/resource/LifecycledResourceManagerImpl;<init>(Lnet/minecraft/resource/ResourceType;Ljava/util/List;)V"
			),
			index = 1
	)
	private List<ResourcePack> aurorasdeco$includeDynamicPacks(List<ResourcePack> packs) {
		if (this.type != ResourceType.CLIENT_RESOURCES) return packs;

		var toRebuild = AurorasDecoPack.getDefaultPacks().stream()
				.filter(pack -> pack.getType() == ResourceType.CLIENT_RESOURCES)
				.toList();
		if (toRebuild.isEmpty()) return packs;

		packs = new ArrayList<>(packs);
		var mirror = new LifecycledResourceManagerImpl(ResourceType.CLIENT_RESOURCES, packs);
		for (var pack : toRebuild) {
			packs.remove(pack);
			packs.add(0, pack.rebuild(ResourceType.CLIENT_RESOURCES, mirror));
		}

		return packs;
	}
}
