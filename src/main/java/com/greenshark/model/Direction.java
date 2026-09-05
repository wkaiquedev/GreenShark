package com.greenshark.model;

/** Direção de um pacote em relação ao cliente. */
public enum Direction {
    /** Servidor -> Cliente (recebido). */
    CLIENTBOUND("S→C"),
    /** Cliente -> Servidor (enviado). */
    SERVERBOUND("C→S");

    public final String arrow;

    Direction(String arrow) {
        this.arrow = arrow;
    }
}
