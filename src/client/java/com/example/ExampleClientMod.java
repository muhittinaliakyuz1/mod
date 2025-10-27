package com.example;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleClientMod implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(ExampleMod.MOD_ID + "-client");

    @Override
    public void onInitializeClient() {
        LOGGER.info("İstemci modu başlatılıyor...");
        
        // Client-side network dinleyicilerini kaydet
        ClientNetworking.registerReceivers();
        
        // Client-side komutları kaydet
        ClientCommands.register();

        LOGGER.info("İstemci modu başlatıldı!");
    }
}
