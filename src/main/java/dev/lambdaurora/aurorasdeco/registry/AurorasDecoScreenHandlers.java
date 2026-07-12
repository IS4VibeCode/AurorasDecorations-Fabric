/*
 * Copyright (c) 2022 LambdAurora <email@lambdaurora.dev>
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

package dev.lambdaurora.aurorasdeco.registry;

import dev.lambdaurora.aurorasdeco.block.PartType;
import dev.lambdaurora.aurorasdeco.screen.CopperHopperScreenHandler;
import dev.lambdaurora.aurorasdeco.screen.PainterPaletteScreenHandler;
import dev.lambdaurora.aurorasdeco.screen.SawmillScreenHandler;
import dev.lambdaurora.aurorasdeco.screen.ShelfScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;

import static dev.lambdaurora.aurorasdeco.AurorasDeco.id;

/**
 * Represents the registered screen handlers of Aurora's Decorations.
 *
 * @author LambdAurora
 * @version 1.0.0-beta.7
 * @since 1.0.0-beta.6
 */
public class AurorasDecoScreenHandlers {
	private AurorasDecoScreenHandlers() {
		throw new UnsupportedOperationException("AurorasDecoScreenHandlers only contains static definitions.");
	}

	public static final ScreenHandlerType<CopperHopperScreenHandler> COPPER_HOPPER_SCREEN_HANDLER_TYPE = register("copper_hopper",
			new ScreenHandlerType<>(CopperHopperScreenHandler::new, FeatureSet.empty()));

	public static final ScreenHandlerType<PainterPaletteScreenHandler> PAINTER_PALETTE_SCREEN_HANDLER_TYPE = register("painter_palette",
			new ExtendedScreenHandlerType<>(PainterPaletteScreenHandler::new, PainterPaletteScreenHandler.OpeningData.PACKET_CODEC));

	public static final ScreenHandlerType<SawmillScreenHandler> SAWMILL_SCREEN_HANDLER_TYPE = register("sawmill",
			new ScreenHandlerType<>(SawmillScreenHandler::new, FeatureSet.empty()));

	public static final ScreenHandlerType<ShelfScreenHandler> SHELF_SCREEN_HANDLER_TYPE = register("shelf",
			new ExtendedScreenHandlerType<>(ShelfScreenHandler::new,
					PacketCodecs.indexed(i -> PartType.values()[i], PartType::ordinal)));

	private static <SH extends ScreenHandler> ScreenHandlerType<SH> register(String name, ScreenHandlerType<SH> type) {
		return Registry.register(Registries.SCREEN_HANDLER, id(name), type);
	}

	static void init() {}
}
