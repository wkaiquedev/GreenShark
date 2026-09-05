package com.greenshark.action;

import com.greenshark.mixin.ClientConnectionAccessor;
import com.greenshark.model.CapturedPacket;
import com.greenshark.model.Direction;
import com.greenshark.net.PipelineInjector;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;

/** Reenvio (replay) de pacotes capturados. */
public final class PacketActions {

    private PacketActions() {
    }

    public static ClientConnection connection() {
        var nh = MinecraftClient.getInstance().getNetworkHandler();
        return nh == null ? null : nh.getConnection();
    }

    /** Reenvia um pacote cliente -> servidor para o servidor. */
    public static boolean replayServerbound(Packet<?> packet) {
        ClientConnection c = connection();
        if (c == null) {
            return false;
        }
        c.send(packet);
        return true;
    }

    /** Reinjeta um pacote servidor -> cliente para o cliente processar de novo. */
    public static boolean replayClientbound(Packet<?> packet) {
        ClientConnection c = connection();
        if (c == null) {
            return false;
        }
        Channel ch = ((ClientConnectionAccessor) c).getChannel();
        if (ch == null) {
            return false;
        }
        ch.eventLoop().execute(() -> {
            // Dispara a partir do nosso handler para entregar ao "packet_handler"
            // seguinte, sem re-capturar nem passar pelos decoders.
            ChannelHandlerContext ourCtx = ch.pipeline().context(PipelineInjector.HANDLER_NAME);
            if (ourCtx != null) {
                ourCtx.fireChannelRead(packet);
                return;
            }
            ChannelHandlerContext mcCtx = ch.pipeline().context(PipelineInjector.MC_HANDLER);
            if (mcCtx != null) {
                mcCtx.fireChannelRead(packet);
            }
        });
        return true;
    }

    public static boolean replay(CapturedPacket cp) {
        return cp.direction == Direction.SERVERBOUND
                ? replayServerbound(cp.packet)
                : replayClientbound(cp.packet);
    }
}
