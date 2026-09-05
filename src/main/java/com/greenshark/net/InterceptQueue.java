package com.greenshark.net;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Fila de pacotes segurados pelo modo intercept, aguardando ação do usuário. */
public class InterceptQueue {
    public static final InterceptQueue INSTANCE = new InterceptQueue();

    private final Deque<HeldPacket> held = new ArrayDeque<>();

    public synchronized void hold(HeldPacket h) {
        held.addLast(h);
    }

    public synchronized HeldPacket peek() {
        return held.peekFirst();
    }

    public synchronized HeldPacket poll() {
        return held.pollFirst();
    }

    public synchronized List<HeldPacket> snapshot() {
        return new ArrayList<>(held);
    }

    public synchronized int size() {
        return held.size();
    }

    /** Encaminha todos os pacotes segurados de uma vez. */
    public synchronized void forwardAll() {
        HeldPacket h;
        while ((h = held.pollFirst()) != null) {
            h.forward();
        }
    }
}
