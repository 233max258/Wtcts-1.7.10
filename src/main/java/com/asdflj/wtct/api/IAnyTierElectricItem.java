package com.asdflj.wtct.api;

/**
 * Marks a powered item whose IC2 tier is "any": GregTech's charging helpers key their voltage gates
 * off {@code IElectricItem.getTier} and {@code GTModHandler.isElectricItem(ItemStack, byte)} compares
 * that tier against the machine's own tier <em>exactly</em>, so a normally-tiered item can only be
 * charged by the one battery-buffer tier that equals it. Items carrying this marker are exempt from
 * that exact match (see {@code MixinGTModHandler}), and the item itself answers {@code getTier} with
 * {@code -1}, the value GT's charge path already treats as "no tier limit" - together the item reads
 * as a plain rechargeable that any voltage of GT machine can charge, which is how its IC2 charging
 * already behaved.
 */
public interface IAnyTierElectricItem {
}
