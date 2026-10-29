package com.kalioupo.equipment.skill;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * One entry in a socket.
 *
 * <p>Deliberately <b>not</b> an {@code ItemStack}: nesting a full stack inside a data component
 * means nesting every component (including enchantments, which need registry lookups), and a
 * socketed enchanted sword was enough to make a world fail to load. This record keeps the two
 * things that matter - the item and how worn it is - and nothing that needs a registry.</p>
 *
 * <p>Enchantments are therefore not preserved: gear that is enchanted or renamed is refused at the
 * socket table instead of silently losing them.</p>
 */
public record SocketedItem(String id, int damage) {
	public static final Codec<SocketedItem> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.STRING.fieldOf("id").forGetter(SocketedItem::id),
		Codec.INT.optionalFieldOf("damage", 0).forGetter(SocketedItem::damage)
	).apply(instance, SocketedItem::new));

	/**
	 * Reads the current shape <b>and</b> the older plain-string ids, so worlds written by earlier
	 * builds still load instead of throwing on an unexpected entry.
	 */
	public static final Codec<SocketedItem> LENIENT = Codec.either(Codec.STRING, CODEC)
		.xmap(
			either -> either.map(id -> new SocketedItem(id, 0), item -> item),
			item -> Either.right(item)
		);
}
