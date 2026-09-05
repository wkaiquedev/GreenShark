package com.greenshark.model;

import net.minecraft.network.packet.Packet;

/** Um pacote capturado no pipeline, com metadados para exibição. */
public class CapturedPacket {
    public final long id;
    public final long time;
    public final Direction direction;
    public final Packet<?> packet;
    /** Nome amigável para exibir (inclui a classe externa em pacotes aninhados). */
    public final String name;
    /** Nome completo da classe, usado para filtragem de ruído. */
    public final String className;

    public CapturedPacket(long id, Direction direction, Packet<?> packet) {
        this.id = id;
        this.time = System.currentTimeMillis();
        this.direction = direction;
        this.packet = packet;

        Class<?> c = packet.getClass();
        String cn;
        try {
            cn = c.getName();
        } catch (Throwable t) {
            cn = "unknown";
        }
        this.className = cn;

        String friendly = null;
        try {
            friendly = com.greenshark.inspect.PacketNames.friendly(packet);
        } catch (Throwable ignored) {
            // cai no nome da classe
        }

        String simple;
        try {
            simple = c.getSimpleName();
            if (simple.isEmpty()) {
                simple = cn;
            } else if (c.getEnclosingClass() != null) {
                simple = c.getEnclosingClass().getSimpleName() + "$" + simple;
            }
        } catch (Throwable t) {
            simple = cn;
        }
        this.name = friendly != null ? friendly : simple;
    }
}
