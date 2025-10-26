package com.example;

import net.minecraft.client.MinecraftClient;

public class ClientNetworking {

    public static void registerReceivers() {
        // Client-side networking basitleştirildi
        // Numpad ekranı doğrudan açılabilir
        System.out.println("ClientNetworking başlatıldı!");
    }
    
    public static void openNumpadScreen() {
        MinecraftClient.getInstance().setScreen(new NumpadScreen());
    }
}
