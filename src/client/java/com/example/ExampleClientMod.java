package com.example;

import net.fabricmc.api.ClientModInitializer;

public class ExampleClientMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Client-side packet listener'ları register et
        ClientNetworking.registerReceivers();
        
        // Client-side komutları kaydet
        ClientCommands.register();
        

        // Buraya keybind veya başka client-only init ekleyebilirsin
        System.out.println("ExampleClientMod başlatıldı!");
    }
}
