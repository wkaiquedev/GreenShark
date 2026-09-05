package com.greenshark.inspect;

import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.network.packet.Packet;

/**
 * Lê, via reflexão, os campos de um pacote para exibição didática.
 * Pacotes modernos do Minecraft são majoritariamente {@code record}s, então
 * conseguimos nomes de campo legíveis (no ambiente de dev com mappings yarn).
 */
public final class PacketInspector {

    /** Um campo descrito de um pacote. */
    public record Field(String name, String type, String value, boolean editable) {
    }

    private PacketInspector() {
    }

    public static List<Field> describe(Packet<?> packet) {
        List<Field> out = new ArrayList<>();
        Class<?> cls = packet.getClass();

        if (cls.isRecord()) {
            for (RecordComponent rc : cls.getRecordComponents()) {
                String value;
                try {
                    Object v = rc.getAccessor().invoke(packet);
                    value = str(v);
                } catch (Throwable t) {
                    value = "<erro: " + t.getMessage() + ">";
                }
                out.add(new Field(rc.getName(), rc.getType().getSimpleName(), value, isEditable(rc.getType())));
            }
        } else {
            for (java.lang.reflect.Field f : allFields(cls)) {
                if (Modifier.isStatic(f.getModifiers()) || f.isSynthetic()) {
                    continue;
                }
                try {
                    f.setAccessible(true);
                    Object v = f.get(packet);
                    out.add(new Field(f.getName(), f.getType().getSimpleName(), str(v), isEditable(f.getType())));
                } catch (Throwable t) {
                    out.add(new Field(f.getName(), f.getType().getSimpleName(), "<inacessível>", false));
                }
            }
        }
        return out;
    }

    private static List<java.lang.reflect.Field> allFields(Class<?> c) {
        List<java.lang.reflect.Field> list = new ArrayList<>();
        while (c != null && c != Object.class) {
            Collections.addAll(list, c.getDeclaredFields());
            c = c.getSuperclass();
        }
        return list;
    }

    /** Tipos que sabemos parsear de volta a partir de texto no editor. */
    public static boolean isEditable(Class<?> t) {
        return t.isPrimitive()
                || t == String.class
                || t == Integer.class || t == Long.class || t == Short.class || t == Byte.class
                || t == Boolean.class || t == Float.class || t == Double.class || t == Character.class
                || t.isEnum();
    }

    private static String str(Object v) {
        if (v == null) {
            return "null";
        }
        if (v instanceof byte[] b) {
            return "byte[" + b.length + "]";
        }
        String s = String.valueOf(v);
        if (s.length() > 400) {
            s = s.substring(0, 400) + "…";
        }
        return s;
    }
}
