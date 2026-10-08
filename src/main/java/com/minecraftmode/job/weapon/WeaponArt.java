package com.minecraftmode.job.weapon;

/**
 * Colors for the generated weapon texture: {@code metal} (blade/head/body), {@code grip}
 * (handle/wood/string) and {@code accent} (gem, trim, glow). Tier decides how ornate it is.
 */
public record WeaponArt(int metal, int grip, int accent) {
}
