package io.github.eggohito.contex;

import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Contex implements ModInitializer {

    public static final String MOD_NAMESPACE = "contex";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAMESPACE);

    @Override
    public void onInitialize() {
        LOGGER.info("Loaded. The game can now (de)serialize custom text components!");
    }

    public static Identifier identifier(String path) {
        return new Identifier(MOD_NAMESPACE, path);
    }

}
