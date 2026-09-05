package com.greenshark.net;

import com.greenshark.GreenSharkClient;
import com.greenshark.mixin.ClientConnectionAccessor;
import io.netty.channel.Channel;
import net.minecraft.network.ClientConnection;

/** Insere o {@link PacketInterceptor} no pipeline Netty da conexão do cliente. */
public final class PipelineInjector {

    /** Nome do nosso handler no pipeline. */
    public static final String HANDLER_NAME = "greenshark_interceptor";
    /** Nome do handler principal do Minecraft (a própria ClientConnection). */
    public static final String MC_HANDLER = "packet_handler";

    private PipelineInjector() {
    }

    public static void inject(ClientConnection connection) {
        if (connection == null) {
            return;
        }
        final Channel channel = ((ClientConnectionAccessor) connection).getChannel();
        if (channel == null) {
            GreenSharkClient.LOGGER.warn("[GreenShark] Canal Netty nulo; interceptor não injetado.");
            return;
        }
        channel.eventLoop().execute(() -> {
            try {
                if (channel.pipeline().get(HANDLER_NAME) != null) {
                    return; // já injetado
                }
                if (channel.pipeline().get(MC_HANDLER) != null) {
                    channel.pipeline().addBefore(MC_HANDLER, HANDLER_NAME, new PacketInterceptor());
                } else {
                    channel.pipeline().addLast(HANDLER_NAME, new PacketInterceptor());
                }
                GreenSharkClient.LOGGER.info("[GreenShark] Interceptor injetado no pipeline.");
            } catch (Throwable t) {
                GreenSharkClient.LOGGER.error("[GreenShark] Falha ao injetar interceptor", t);
            }
        });
    }
}
