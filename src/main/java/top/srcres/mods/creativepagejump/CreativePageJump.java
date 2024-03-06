package top.srcres.mods.creativepagejump;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(CreativePageJump.MODID)
public class CreativePageJump {
    public static final String MODID = "creativepagejump";

    public CreativePageJump() {
        // This mod is only client-sided.
        if (!FMLEnvironment.dist.equals(Dist.CLIENT))
            throw new RuntimeException(String.format("The mod %s is only client-sided.", MODID));
    }
}
