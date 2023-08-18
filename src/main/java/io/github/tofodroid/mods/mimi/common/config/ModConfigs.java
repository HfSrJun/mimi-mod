package io.github.tofodroid.mods.mimi.common.config;

import io.github.tofodroid.mods.mimi.common.MIMIMod;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.config.ModConfig.Type;

@Mod.EventBusSubscriber(modid = MIMIMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModConfigs {
    public static ClientConfig CLIENT;
    private static ForgeConfigSpec CLIENTSPEC;
    public static CommonConfig COMMON;
    private static ForgeConfigSpec COMMONSPEC;

    static {
        final Pair<ClientConfig, ForgeConfigSpec> clientPair = new ForgeConfigSpec.Builder().configure(ClientConfig::new);
        CLIENT = clientPair.getLeft();
        CLIENTSPEC = clientPair.getRight();

        final Pair<CommonConfig, ForgeConfigSpec> commonPair = new ForgeConfigSpec.Builder().configure(CommonConfig::new);
        COMMON = commonPair.getLeft();
        COMMONSPEC = commonPair.getRight();
    }

    public static void preInit(ModLoadingContext context) { 
        context.registerConfig(Type.CLIENT, ModConfigs.CLIENTSPEC);
        context.registerConfig(Type.COMMON, ModConfigs.COMMONSPEC);
    }
}
