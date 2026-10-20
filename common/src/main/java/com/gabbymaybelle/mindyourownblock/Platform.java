package com.gabbymaybelle.mindyourownblock;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * The few things the mod asks of the loader on the player's game. Each loader's client entrypoint
 * sets this first thing; a dedicated server never needs it.
 */
public final class Platform {
	/** What each loader answers on the player's game. */
	public interface Client {
		/** Whether the server understands this message (it has the mod too). */
		boolean canSendToServer(CustomPacketPayload.Type<?> type);

		void sendToServer(CustomPacketPayload payload);
	}

	private static Client client;

	private Platform() {
	}

	public static void setClient(Client hooks) {
		client = hooks;
	}

	public static Client client() {
		return client;
	}
}
