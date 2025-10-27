package com.example;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleMod implements ModInitializer {
    public static final String MOD_ID = "modid";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("ExampleMod (modid) başlatılıyor...");

        // Komut registrasyonları CommandRegistrationCallback ile zaten kayıt olunur,
        // ancak sınıfın yüklenmesini sağlamak için registerCommands çağırıyoruz.
        CommandRegistry.registerCommands();

        // Heart item kullanımını yöneten listener'ı kaydet
        HeartItemHandler.register();

        // Kalp sistemi (server tarafı) init
        HeartRegistry.init();

        // Sunucu başlatıldığında player data dosyasını yükle/sakla
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            PlayerDataManager.initialize(server);
            CodeManager.register(server);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> PlayerDataManager.saveData());

        LOGGER.info("ExampleMod (modid) başlatıldı.");
    }
}
