package com.greenshark.mixin;

import io.netty.channel.Channel;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Expõe o {@link Channel} do Netty escondido dentro do {@link ClientConnection}.
 * Usamos um Accessor (remapeado pelo Loom) em vez de reflexão crua, para
 * funcionar tanto no ambiente de dev (yarn) quanto em produção (intermediary).
 */
@Mixin(ClientConnection.class)
public interface ClientConnectionAccessor {
    @Accessor("channel")
    Channel getChannel();
}
