package com.gabbymaybelle.mindyourownblock;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mind Your Own Block: hold a key while placing and the block keeps the shape you placed it in.
 * The loader entrypoints ({@code fabric} and {@code neoforge}) call {@link #init()} once;
 * everything that doesn't care which loader it runs on lives here in {@code common}.
 */
public final class MindYourOwnBlock {
	public static final String MOD_ID = "mind_your_own_block";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private MindYourOwnBlock() {
	}

	public static void init() {
		LOGGER.info("Mind Your Own Block: ready");
	}

	/** An id in the mod's own namespace. */
	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
