package com.greenshark.inspect;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientTickEndC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.RenameItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.network.packet.s2c.play.BossBarS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkRenderDistanceCenterS2CPacket;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityAttributesS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionSyncS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySetHeadYawS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.LightUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListHeaderS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerPropertyUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;

/**
 * Identifica pacotes por {@code instanceof} — que o Loom remapeia para o nome
 * intermediary em produção. Assim tanto o filtro de ruído quanto os nomes
 * legíveis funcionam no servidor real (onde os nomes de classe são ofuscados).
 */
public final class PacketNames {

    private PacketNames() {
    }

    /** Pacotes de alto volume que poluem a captura (não interessam ao teste comum). */
    public static boolean isNoise(Packet<?> p) {
        return p instanceof BlockBreakingProgressS2CPacket  // rachadura de bloco (flood pesado em SMPs)
                || p instanceof EntityS2CPacket             // movimento relativo (base + subclasses)
                || p instanceof EntityPositionS2CPacket
                || p instanceof EntityPositionSyncS2CPacket
                || p instanceof EntitySetHeadYawS2CPacket
                || p instanceof EntityVelocityUpdateS2CPacket
                || p instanceof EntityTrackerUpdateS2CPacket
                || p instanceof EntityAttributesS2CPacket
                || p instanceof EntityStatusS2CPacket
                || p instanceof LightUpdateS2CPacket
                || p instanceof ChunkDataS2CPacket
                || p instanceof ChunkDeltaUpdateS2CPacket
                || p instanceof ChunkRenderDistanceCenterS2CPacket
                || p instanceof ParticleS2CPacket
                || p instanceof WorldTimeUpdateS2CPacket
                || p instanceof PlayerListS2CPacket
                || p instanceof PlayerListHeaderS2CPacket
                || p instanceof BossBarS2CPacket
                || p instanceof UnloadChunkS2CPacket
                || p instanceof EntityPassengersSetS2CPacket
                || p instanceof KeepAliveS2CPacket
                || p instanceof CommonPingS2CPacket
                || p instanceof PlayerMoveC2SPacket
                || p instanceof ClientTickEndC2SPacket
                || p instanceof KeepAliveC2SPacket
                || p instanceof CommonPongC2SPacket;
    }

    /**
     * Nome legível para os pacotes que importam num teste de segurança de GUI/leilão.
     * Retorna {@code null} se não for um dos conhecidos (aí cai no nome da classe).
     */
    public static String friendly(Packet<?> p) {
        // --- Containers / GUI (o que interessa pro leilão) ---
        if (p instanceof OpenScreenS2CPacket) return "OpenScreen (abre GUI)";
        if (p instanceof InventoryS2CPacket) return "Inventory (conteúdo da GUI)";
        if (p instanceof ScreenHandlerSlotUpdateS2CPacket) return "SlotUpdate (S→C)";
        if (p instanceof ScreenHandlerPropertyUpdateS2CPacket) return "PropertyUpdate (S→C)";
        if (p instanceof CloseScreenS2CPacket) return "CloseScreen (S→C)";
        if (p instanceof ClickSlotC2SPacket) return "★ ClickSlot (clique na GUI)";
        if (p instanceof ButtonClickC2SPacket) return "ButtonClick (C→S)";
        if (p instanceof CreativeInventoryActionC2SPacket) return "CreativeInvAction (C→S)";
        if (p instanceof CloseHandledScreenC2SPacket) return "CloseScreen (C→S)";
        if (p instanceof RenameItemC2SPacket) return "RenameItem (bigorna)";
        if (p instanceof UpdateSignC2SPacket) return "UpdateSign (placa)";

        // --- Interação / mundo ---
        if (p instanceof PlayerInteractEntityC2SPacket) return "InteractEntity (bater/usar)";
        if (p instanceof PlayerInteractBlockC2SPacket) return "InteractBlock";
        if (p instanceof PlayerInteractItemC2SPacket) return "InteractItem";
        if (p instanceof PlayerActionC2SPacket) return "PlayerAction (quebrar/soltar)";
        if (p instanceof EntityDamageS2CPacket) return "EntityDamage (S→C)";

        // --- Chat / comandos ---
        if (p instanceof CommandExecutionC2SPacket) return "CommandExecution (comando)";
        if (p instanceof ChatMessageC2SPacket) return "ChatMessage (C→S)";
        if (p instanceof GameMessageS2CPacket) return "GameMessage (chat S→C)";

        // --- Plugin channels ---
        if (p instanceof CustomPayloadS2CPacket) return "CustomPayload (plugin S→C)";
        if (p instanceof CustomPayloadC2SPacket) return "CustomPayload (plugin C→S)";

        return null;
    }
}
