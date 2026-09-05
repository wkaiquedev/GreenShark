package com.greenshark.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Buffer circular dos pacotes capturados, 100% lock-free.
 *
 * A thread de rede (Netty) escreve com {@link #add}; a thread de render lê com
 * {@link #snapshot}. Nada bloqueia a thread de rede — isso é crítico: um lock
 * segurado pela render poderia atrasar o processamento de pacotes e causar
 * timeout na conexão.
 */
public class PacketLog {
    public static final PacketLog INSTANCE = new PacketLog();

    private static final int MAX = 3000;

    private final ConcurrentLinkedDeque<CapturedPacket> packets = new ConcurrentLinkedDeque<>();
    private final AtomicInteger count = new AtomicInteger();
    private final AtomicLong counter = new AtomicLong();
    private volatile boolean capturing = true;

    public long nextId() {
        return counter.incrementAndGet();
    }

    public void add(CapturedPacket p) {
        if (!capturing) {
            return;
        }
        packets.addLast(p);
        if (count.incrementAndGet() > MAX) {
            if (packets.pollFirst() != null) {
                count.decrementAndGet();
            }
        }
    }

    /** Cópia consistente-fraca para a GUI; não bloqueia escritores. */
    public List<CapturedPacket> snapshot() {
        return new ArrayList<>(packets);
    }

    public void clear() {
        packets.clear();
        count.set(0);
    }

    public int size() {
        return count.get();
    }

    public boolean isCapturing() {
        return capturing;
    }

    public void setCapturing(boolean capturing) {
        this.capturing = capturing;
    }
}
