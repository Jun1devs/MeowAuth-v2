package org.jun1devs.meowauth.server;

/**
 * Minecraft formatting codes used across the mod.
 * Using constants instead of magic strings like "§c".
 *
 * @see <a href="https://minecraft.wiki/w/Formatting_codes">Formatting codes</a>
 */
public final class MeowColors {

    private MeowColors() {}

    public static final String RED    = "§c";
    public static final String GREEN  = "§a";
    public static final String YELLOW = "§e";
    public static final String GOLD   = "§6";
    public static final String WHITE  = "§f";

    /** Prefix for all MeowAuth messages. */
    public static final String PREFIX = "[MeowAuth] ";
}
