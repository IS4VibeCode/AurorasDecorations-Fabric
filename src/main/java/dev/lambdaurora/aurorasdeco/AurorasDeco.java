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

package dev.lambdaurora.aurorasdeco;

import com.mojang.logging.LogUtils;
import dev.lambdaurora.aurorasdeco.blackboard.BlackboardColor;
import dev.lambdaurora.aurorasdeco.block.big_flower_pot.BigPottedCactusBlock;
import dev.lambdaurora.aurorasdeco.block.big_flower_pot.PottedPlantType;
import dev.lambdaurora.aurorasdeco.item.group.ItemTree;
import dev.lambdaurora.aurorasdeco.registry.AurorasDecoPackets;
import dev.lambdaurora.aurorasdeco.registry.AurorasDecoRegistry;
import dev.lambdaurora.aurorasdeco.resource.AurorasDecoPack;
import dev.lambdaurora.aurorasdeco.util.AuroraUtil;
import dev.lambdaurora.aurorasdeco.world.gen.DynamicWorldGen;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import org.slf4j.Logger;

/**
 * Represents the Aurora's Decorations mod.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public class AurorasDeco implements ModInitializer {
	public static final String NAMESPACE = "aurorasdeco";
	public static final Logger LOGGER = LogUtils.getLogger();
	public static final AurorasDecoPack RESOURCE_PACK = new AurorasDecoPack(ResourceType.SERVER_DATA);

	@Override
	public void onInitialize() {
		AurorasDecoRegistry.init();

		// QSL's RegistryMonitor.forAll() both sweeps entries already registered AND subscribes to
		// future ones in a single call. Fabric's RegistryEntryAddedCallback only covers future ones,
		// so an explicit initial sweep is needed alongside it -- see java/CLAUDE.md §3c, this exact
		// pattern was already confirmed to work (including reactively, against real NeoForge-native
		// mods observed through Connector) by the aurorasdeco-registry-spike experiment.
		for (var id : Registries.ITEM.getIds()) {
			onItemRegistered(id, Registries.ITEM.get(id));
		}
		RegistryEntryAddedCallback.event(Registries.ITEM).register((rawId, id, item) -> onItemRegistered(id, item));

		ItemTree.init();

		AurorasDecoPackets.init();
		ServerPlayNetworking.registerGlobalReceiver(AurorasDecoPackets.SignPostOpenGuiFailPayload.ID, AurorasDecoPackets::handleSignPostOpenGuiFailPacket);
		ServerPlayNetworking.registerGlobalReceiver(AurorasDecoPackets.SignPostSetTextPayload.ID, AurorasDecoPackets::handleSignPostSetTextPacket);
		ServerPlayNetworking.registerGlobalReceiver(AurorasDecoPackets.PainterPaletteScrollPayload.ID, AurorasDecoPackets::handlePainterPaletteScroll);

		DynamicWorldGen.init();

		var modContainer = FabricLoader.getInstance().getModContainer(NAMESPACE).orElseThrow();
		ResourceManagerHelper.registerBuiltinResourcePack(id("azalea_tree"), modContainer,
				Text.literal("Aurora's Deco").formatted(Formatting.GOLD)
						.append(Text.literal(" - ").formatted(Formatting.GRAY))
						.append(Text.translatable("resourcepack.aurorasdeco.azalea_tree.name").formatted(Formatting.LIGHT_PURPLE)),
				ResourcePackActivationType.DEFAULT_ENABLED
		);
		ResourceManagerHelper.registerBuiltinResourcePack(id("swamp_worldgen"), modContainer,
				Text.literal("Aurora's Deco").formatted(Formatting.GOLD)
						.append(Text.literal(" - ").formatted(Formatting.GRAY))
						.append(Text.translatable("resourcepack.aurorasdeco.swamp_tweaks.name").formatted(Formatting.DARK_GREEN)),
				ResourcePackActivationType.NORMAL
		);
		// RESOURCE_PACK (the dynamically-generated, always-active pack) is wired up in
		// AurorasDecoPack itself -- see that class for how it gets injected, since Fabric has no
		// registerDefaultResourcePackEvent equivalent (java/CLAUDE.md §3a/§3c Task #18).
		AurorasDecoPack.registerAsDefaultPack(RESOURCE_PACK);
	}

	private static void onItemRegistered(Identifier id, Item item) {
		if (AuroraUtil.idEqual(id, "pockettools", "pocket_cactus")) {
			Registry.register(Registries.BLOCK, id("big_flower_pot/pocket_cactus"),
					PottedPlantType.register("pocket_cactus", Blocks.POTTED_CACTUS, item,
							type -> new BigPottedCactusBlock(type, BigPottedCactusBlock.POCKET_CACTUS_SHAPE)));
		} else if (PottedPlantType.isValidPlant(item)) {
			var potBlock = PottedPlantType.registerFromItem(item);
			if (potBlock != null)
				Registry.register(Registries.BLOCK, id("big_flower_pot/" + potBlock.getPlantType().getId()), potBlock);
		}

		BlackboardColor.tryRegisterColorFromItem(id, item);
	}

	public static boolean isDevMode() {
		return FabricLoader.getInstance().isDevelopmentEnvironment() || Boolean.getBoolean("aurorasdeco.debug");
	}

	public static void log(String message) {
		if (isDevMode())
			LOGGER.info("\033[32m" + message + "\033[0m");
		else
			LOGGER.info("[AurorasDeco] " + message);
	}

	public static void warn(String message, Object... params) {
		if (isDevMode())
			LOGGER.warn("\033[33m" + message + "\033[0m", params);
		else
			LOGGER.warn("[AurorasDeco] " + message, params);
	}

	public static void error(String message, Object... params) {
		if (isDevMode())
			LOGGER.error("\033[31;1m" + message + "\033[0m", params);
		else
			LOGGER.error("[AurorasDeco] " + message, params);
	}

	public static void debug(String message, Object... params) {
		if (isDevMode()) {
			LOGGER.info("\033[38;5;214m[Debug] \033[32;1m" + message + "\033[0m", params);
		}
	}

	public static void debugWarn(String message, Object... params) {
		if (isDevMode()) {
			LOGGER.info("\033[38;5;214m[Debug] \033[31;1m" + message + "\033[0m", params);
		}
	}

	public static Identifier id(String path) {
		return Identifier.of(NAMESPACE, path);
	}
}
