package squeek.spiceoflife.network;

import net.minecraft.entity.player.EntityPlayer;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class NetworkHelper {

    public static EntityPlayer getSidedPlayer(MessageContext ctx) {
        return ctx.side == Side.SERVER ? ctx.getServerHandler().playerEntity : getClientPlayer();
    }

    @SideOnly(Side.CLIENT)
    public static EntityPlayer getClientPlayer() {
        return FMLClientHandler.instance()
            .getClientPlayerEntity();
    }
}
