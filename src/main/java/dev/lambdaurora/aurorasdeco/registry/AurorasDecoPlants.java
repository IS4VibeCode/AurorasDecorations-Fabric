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

package dev.lambdaurora.aurorasdeco.registry;

import dev.lambdaurora.aurorasdeco.block.BurntVineBlock;
import dev.lambdaurora.aurorasdeco.block.DirectionalFlowerPotBlock;
import dev.lambdaurora.aurorasdeco.block.plant.DaffodilBlock;
import dev.lambdaurora.aurorasdeco.block.plant.DuckweedBlock;
import dev.lambdaurora.aurorasdeco.block.plant.LavenderBlock;
import dev.lambdaurora.aurorasdeco.block.sapling.JacarandaSaplingGenerator;
import dev.lambdaurora.aurorasdeco.item.DuckweedItem;
import net.minecraft.block.*;
import net.minecraft.item.BlockItem;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;

import java.util.List;

import static dev.lambdaurora.aurorasdeco.registry.AurorasDecoRegistry.registerBlock;
import static dev.lambdaurora.aurorasdeco.registry.AurorasDecoRegistry.registerWithItem;

/**
 * Contains the different plants definitions added in Aurora's Decorations.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public final class AurorasDecoPlants {
	private AurorasDecoPlants() {
		throw new UnsupportedOperationException("AurorasDecoPlants only contains static definitions.");
	}

	static void init() {}

	public static final List<BlockState> FLOWER_FOREST_PLANTS;

	/* Plants */

	public static final Registrar.BlockEntry<DaffodilBlock> DAFFODIL = Registrar.register("daffodil", new DaffodilBlock())
			.withItem(new FabricItemSettings(), BlockItem::new)
			.finish();

	public static final Registrar.BlockEntry<LavenderBlock> LAVENDER = Registrar.register("lavender", new LavenderBlock())
			.withItem(new FabricItemSettings(), BlockItem::new)
			.finish();

	public static final Registrar.BlockEntry<DuckweedBlock> DUCKWEED = Registrar.register("duckweed", new DuckweedBlock())
			.withItem(new FabricItemSettings(), DuckweedItem::new)
			.finish();

	/* Burnt Plants */

	public static final BurntVineBlock BURNT_VINE_BLOCK = registerBlock("burnt_vine", new BurntVineBlock());

	/* Potted Plants */

	public static final FlowerPotBlock POTTED_DAFFODIL = registerBlock("potted/daffodil",
			new DirectionalFlowerPotBlock(DAFFODIL.block(), FabricBlockSettings.copyOf(Blocks.FLOWER_POT)));

	public static final FlowerPotBlock POTTED_LAVENDER = registerBlock("potted/lavender",
			new FlowerPotBlock(LAVENDER.block(), FabricBlockSettings.copyOf(Blocks.FLOWER_POT)));

	/* Saplings */

	public static final SaplingBlock JACARANDA_SAPLING = registerWithItem("jacaranda_sapling",
			new SaplingBlock(new JacarandaSaplingGenerator(), FabricBlockSettings.copyOf(Blocks.OAK_SAPLING)),
			new FabricItemSettings()
	);

	public static final FlowerPotBlock POTTED_JACARANDA_SAPLING = registerBlock("potted/jacaranda_sapling",
			new FlowerPotBlock(JACARANDA_SAPLING, FabricBlockSettings.copyOf(Blocks.FLOWER_POT)));

	/* Leaves */

	public static final LeavesBlock JACARANDA_LEAVES = registerWithItem("jacaranda_leaves",
			new LeavesBlock(FabricBlockSettings.copyOf(Blocks.BIRCH_LEAVES)),
			new FabricItemSettings());
	public static final LeavesBlock BUDDING_JACARANDA_LEAVES = registerWithItem("budding_jacaranda_leaves",
			new LeavesBlock(FabricBlockSettings.copyOf(Blocks.FLOWERING_AZALEA_LEAVES)),
			new FabricItemSettings());
	public static final LeavesBlock FLOWERING_JACARANDA_LEAVES = registerWithItem("flowering_jacaranda_leaves",
			new LeavesBlock(FabricBlockSettings.copyOf(BUDDING_JACARANDA_LEAVES)),
			new FabricItemSettings());

	static {
		FLOWER_FOREST_PLANTS = List.of(DAFFODIL.getDefaultState(), LAVENDER.getDefaultState());
	}
}
