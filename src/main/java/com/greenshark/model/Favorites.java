package com.greenshark.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Pacotes favoritados pelo usuário. Guardados à parte do {@link PacketLog},
 * então sobrevivem ao "Limpar" e ao limite de 3000 — o pacote que você marcou
 * fica salvo até você removê-lo.
 */
public class Favorites {
    public static final Favorites INSTANCE = new Favorites();

    private final LinkedHashMap<Long, CapturedPacket> map = new LinkedHashMap<>();

    public synchronized void toggle(CapturedPacket p) {
        if (map.containsKey(p.id)) {
            map.remove(p.id);
        } else {
            map.put(p.id, p);
        }
    }

    public synchronized boolean contains(long id) {
        return map.containsKey(id);
    }

    public synchronized List<CapturedPacket> snapshot() {
        return new ArrayList<>(map.values());
    }

    public synchronized void clear() {
        map.clear();
    }

    public synchronized int size() {
        return map.size();
    }
}
