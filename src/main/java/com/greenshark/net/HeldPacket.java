package com.greenshark.net;

import com.greenshark.model.Direction;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.packet.Packet;

/**
 * Um pacote "segurado" pelo modo intercept (como o Intercept do Burp).
 * Guarda o contexto do Netty para poder encaminhar ou descartar depois,
 * a partir da thread da GUI.
 */
public class HeldPacket {
    public final long id;
    public final Direction direction;
    public volatile Packet<?> packet;

    private final ChannelHandlerContext ctx;
    private final ChannelPromise promise; // apenas para pacotes SERVERBOUND
    private boolean resolved = false;

    public HeldPacket(long id, Direction direction, Packet<?> packet,
                      ChannelHandlerContext ctx, ChannelPromise promise) {
        this.id = id;
        this.direction = direction;
        this.packet = packet;
        this.ctx = ctx;
        this.promise = promise;
    }

    /** Deixa o pacote seguir (possivelmente editado). */
    public synchronized void forward() {
        if (resolved) {
            return;
        }
        resolved = true;
        final Packet<?> p = packet;
        ctx.channel().eventLoop().execute(() -> {
            if (direction == Direction.CLIENTBOUND) {
                ctx.fireChannelRead(p);
            } else {
                ctx.write(p, promise);
                ctx.flush();
            }
        });
    }

    /** Descarta o pacote silenciosamente. */
    public synchronized void drop() {
        if (resolved) {
            return;
        }
        resolved = true;
        if (direction == Direction.SERVERBOUND && promise != null) {
            ctx.channel().eventLoop().execute(promise::setSuccess);
        }
    }

    public boolean isResolved() {
        return resolved;
    }
}
