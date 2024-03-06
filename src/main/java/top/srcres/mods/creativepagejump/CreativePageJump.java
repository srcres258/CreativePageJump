package top.srcres.mods.creativepagejump;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(CreativePageJump.MODID)
public class CreativePageJump {
    public static final String MODID = "creativepagejump";

    public CreativePageJump(IEventBus modEventBus) {
        // This mod is only client-sided.
        if (!FMLEnvironment.dist.equals(Dist.CLIENT))
            throw new RuntimeException(String.format("The mod %s is only client-sided.", MODID));
    }
}
