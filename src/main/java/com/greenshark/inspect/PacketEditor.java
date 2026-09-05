package com.greenshark.inspect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.util.Map;

import net.minecraft.network.packet.Packet;

/**
 * Edição "best-effort" de pacotes. Para records, constrói uma nova instância
 * pelo construtor canônico; para classes comuns, muta os campos no lugar.
 * Só campos de tipo simples (primitivos/String/enum) são alterados; os demais
 * são mantidos como estavam.
 */
public final class PacketEditor {

    private PacketEditor() {
    }

    public static Packet<?> applyEdits(Packet<?> packet, Map<String, String> edits) throws Exception {
        Class<?> cls = packet.getClass();

        if (cls.isRecord()) {
            RecordComponent[] comps = cls.getRecordComponents();
            Class<?>[] types = new Class<?>[comps.length];
            Object[] args = new Object[comps.length];
            for (int i = 0; i < comps.length; i++) {
                types[i] = comps[i].getType();
                Object current = comps[i].getAccessor().invoke(packet);
                if (edits.containsKey(comps[i].getName()) && PacketInspector.isEditable(types[i])) {
                    args[i] = parse(types[i], edits.get(comps[i].getName()), current);
                } else {
                    args[i] = current;
                }
            }
            Constructor<?> ctor = cls.getDeclaredConstructor(types);
            ctor.setAccessible(true);
            return (Packet<?>) ctor.newInstance(args);
        }

        // Classe comum: muta no lugar.
        for (Map.Entry<String, String> e : edits.entrySet()) {
            Field f = findField(cls, e.getKey());
            if (f == null || !PacketInspector.isEditable(f.getType())) {
                continue;
            }
            f.setAccessible(true);
            f.set(packet, parse(f.getType(), e.getValue(), f.get(packet)));
        }
        return packet;
    }

    private static Field findField(Class<?> c, String name) {
        while (c != null && c != Object.class) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                c = c.getSuperclass();
            }
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object parse(Class<?> t, String s, Object current) {
        s = s.trim();
        if (t == String.class) {
            return s;
        }
        if (t == int.class || t == Integer.class) {
            return Integer.parseInt(s);
        }
        if (t == long.class || t == Long.class) {
            return Long.parseLong(s);
        }
        if (t == short.class || t == Short.class) {
            return Short.parseShort(s);
        }
        if (t == byte.class || t == Byte.class) {
            return Byte.parseByte(s);
        }
        if (t == boolean.class || t == Boolean.class) {
            return Boolean.parseBoolean(s);
        }
        if (t == float.class || t == Float.class) {
            return Float.parseFloat(s);
        }
        if (t == double.class || t == Double.class) {
            return Double.parseDouble(s);
        }
        if (t == char.class || t == Character.class) {
            return s.isEmpty() ? current : s.charAt(0);
        }
        if (t.isEnum()) {
            return Enum.valueOf((Class<Enum>) t, s);
        }
        return current;
    }
}
