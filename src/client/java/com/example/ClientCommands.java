package com.example;

import net.minecraft.client.MinecraftClient;

public class ClientCommands {
    
    public static void register() {
        // Client-side numpad komutu - basitleştirildi
        System.out.println("ClientCommands kaydedildi!");
    }
    
    public static void openNumpad() {
        MinecraftClient.getInstance().setScreen(new NumpadScreen());
    }
}
