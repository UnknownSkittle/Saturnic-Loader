package com.saturnic;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import java.nio.file.Path;

public class SaturnicLoaderMod implements ClientModInitializer {

    public static ScriptManager SCRIPT_MANAGER;

    @Override
    public void onInitializeClient() {
        Path gameDir = FabricLoader.getInstance().getGameDir();
        Path saturnicDir = gameDir.resolve("saturnic");

        SCRIPT_MANAGER = new ScriptManager(saturnicDir);
        SCRIPT_MANAGER.loadClient();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                SCRIPT_MANAGER.onTick();
            }
        });

        System.out.println("[SaturnicLoader] Initialized.");
    }
}

