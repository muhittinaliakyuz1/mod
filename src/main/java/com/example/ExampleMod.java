package com.example;

import net.fabricmc.api.ModInitializer;

public class ExampleMod implements ModInitializer {

    @Override
    public void onInitialize() {
        // Burada komutları kaydediyoruz
        CommandRegistry.registerCommands();

        // Başka başlatma işleri de buraya eklenebilir
        System.out.println("ExampleMod yüklendi!");
    }
}
