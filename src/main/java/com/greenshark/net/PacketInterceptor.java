package com.greenshark.net;

import com.greenshark.GreenSharkClient;
import com.greenshark.model.CapturedPacket;
import com.greenshark.model.Direction;
import com.greenshark.model.PacketLog;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;

/**
 * Handler duplex inserido no pipeline Netty do cliente.
 * Vê os pacotes já decodificados (objetos {@link Packet}) nos dois sentidos,
 * antes do handler principal do Minecraft.
 *
 * REGRA DE OURO: nada aqui pode lançar exceção ou bloquear a thread de rede.
 * Toda captura é embrulhada em try/catch e o pacote SEMPRE segue seu caminho
 * (a não ser que o usuário tenha ligado o modo intercept de propósito).
 */
public class PacketInterceptor extends ChannelDuplexHandler {

    /** Segurar pacotes recebidos (servidor -> cliente). */
    public static volatile boolean interceptInbound = false;
    /** Segurar pacotes enviados (cliente -> servidor). */
    public static volatile boolean interceptOutbound = false;

    /** Não capturar (nem segurar) pacotes de alto volume (movimento, chunk, tempo…). */
    public static volatile boolean dropNoise = true;

    /** DIAGNÓSTICO TEMPORÁRIO: histograma de todos os pacotes vistos (antes de filtrar). */
    private static final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicLong> HIST =
            new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.concurrent.atomic.AtomicLong TOTAL = new java.util.concurrent.atomic.AtomicLong();

    private static void tally(Packet<?> p) {
        String friendly = null;
        try {
            friendly = com.greenshark.inspect.PacketNames.friendly(p);
        } catch (Throwable ignored) {
        }
        String key = (friendly != null ? friendly + "  " : "") + p.getClass().getName();
        HIST.computeIfAbsent(key, k -> new java.util.concurrent.atomic.AtomicLong()).incrementAndGet();
        long t = TOTAL.incrementAndGet();
        if (t % 3000 == 0) {
            String top = HIST.entrySet().stream()
                    .sorted((a, b) -> Long.compare(b.getValue().get(), a.getValue().get()))
                    .limit(18)
                    .map(e -> String.format("%7d  %s", e.getValue().get(), e.getKey()))
                    .reduce((a, b) -> a + "\n  " + b).orElse("(vazio)");
            GreenSharkClient.LOGGER.info("[GreenShark][hist] TOP pacotes (total visto={}):\n  {}", t, top);
        }
    }

    /**
     * Pacotes que NUNCA podem ser segurados: são a "batida de coração" da
     * conexão. Segurar um KeepAlive/Ping faz o servidor te derrubar por timeout.
     * Usamos instanceof (o Loom remapeia para o nome intermediary em produção).
     */
    private static boolean isCritical(Packet<?> p) {
        return p instanceof KeepAliveC2SPacket
                || p instanceof KeepAliveS2CPacket
                || p instanceof CommonPingS2CPacket
                || p instanceof CommonPongC2SPacket;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Packet<?> pk) {
            tally(pk);
        }
        if (msg instanceof Packet<?> packet && !(dropNoise && com.greenshark.inspect.PacketNames.isNoise(packet))) {
            try {
                long id = PacketLog.INSTANCE.nextId();
                PacketLog.INSTANCE.add(new CapturedPacket(id, Direction.CLIENTBOUND, packet));
                if (interceptInbound && !isCritical(packet)) {
                    InterceptQueue.INSTANCE.hold(new HeldPacket(id, Direction.CLIENTBOUND, packet, ctx, null));
                    return; // segurado de propósito; encaminhado depois pela GUI
                }
            } catch (Throwable t) {
                GreenSharkClient.LOGGER.error("[GreenShark] erro ao capturar (S→C); pacote encaminhado mesmo assim", t);
            }
        }
        super.channelRead(ctx, msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof Packet<?> pk) {
            tally(pk);
        }
        if (msg instanceof Packet<?> packet && !(dropNoise && com.greenshark.inspect.PacketNames.isNoise(packet))) {
            try {
                long id = PacketLog.INSTANCE.nextId();
                PacketLog.INSTANCE.add(new CapturedPacket(id, Direction.SERVERBOUND, packet));
                if (interceptOutbound && !isCritical(packet)) {
                    InterceptQueue.INSTANCE.hold(new HeldPacket(id, Direction.SERVERBOUND, packet, ctx, promise));
                    return; // segurado de propósito; encaminhado depois pela GUI
                }
            } catch (Throwable t) {
                GreenSharkClient.LOGGER.error("[GreenShark] erro ao capturar (C→S); pacote enviado mesmo assim", t);
            }
        }
        super.write(ctx, msg, promise);
    }
}
