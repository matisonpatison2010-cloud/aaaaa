package net.example.mace;

import net.fabricmc.api.ModInitializer;

public class MaceMod implements ModInitializer {
    public static final String MODID = "mace";

    @Override
    public void onInitialize() {
        System.out.println("[MaceMod] Loaded successfully");
    }
}
