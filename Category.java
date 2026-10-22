package dev.nova.module;

public enum Category {
    HUD("HUD"), PVP("PvP"), WORLD("Мир"), COSMETIC("Косметика"), QOL("QoL"), CLIENT("Клиент");

    public final String title;
    Category(String title) { this.title = title; }
}
