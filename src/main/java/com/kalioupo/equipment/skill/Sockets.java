package com.kalioupo.equipment.skill;

import com.kalioupo.equipment.KalioupoEquipment;
import com.kalioupo.equipment.registry.ModComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Socketed gear ("副位").
 *
 * <p>What fits in a socket depends on the host:</p>
 *
 * <ul>
 *   <li><b>weapons, tools and shields</b> take <b>ingots / alloys</b> - plating the blade - or a
 *       <b>sword</b>, which lends its active skill to the host</li>
 *   <li><b>armour</b> takes <b>one armour piece of the same slot</b> for the bigger version of the
 *       trait, and <b>ingots</b> afterwards</li>
 * </ul>
 *
 * <p>A socketed item contributes <b>only its trait</b>: its armour, attack damage and set bonus do
 * nothing. Sockets remember the item and its wear, but not its enchantments - gear carrying
 * enchantments or a custom name is refused rather than stripped.</p>
 */
public final class Sockets {
	public static final int MAX_SLOTS = 5;

	public static final String COPPER = KalioupoEquipment.MOD_ID + ":copper_alloy";
	public static final String ROSE_GOLD = KalioupoEquipment.MOD_ID + ":rose_gold";
	public static final String PRISMARINE = KalioupoEquipment.MOD_ID + ":prismarine";
	public static final String PHANTOM = KalioupoEquipment.MOD_ID + ":phantom";
	public static final String QUARTZ = KalioupoEquipment.MOD_ID + ":quartz_steel";

	/** charge speed per socket; an armour piece or sword is worth half again as much as an ingot */
	public static final int COPPER_INGOT_CHARGE = 10;
	public static final int COPPER_ARMOR_CHARGE = 15;
	/** self repair in durability per {@link SocketEffects} interval */
	public static final int ROSE_INGOT_REPAIR = 1;
	public static final int ROSE_ARMOR_REPAIR = 2;
	/** attack damage per quartz socket, and how many sockets count */
	public static final int QUARTZ_DAMAGE = 2;
	public static final int QUARTZ_MAX_STACK = 3;
	/**
	 * The charge trait stops here for the whole player. With a full copper set (60) plus two
	 * weapon sockets (20) the cap is reached exactly, so both halves of the build matter.
	 */
	public static final int CHARGE_CAP = 80;

	/** what a piece of gear is, and therefore what its sockets accept */
	public enum Slot {
		NONE,
		WEAPON,
		ARMOR
	}

	/** the active skills a socketed sword can lend to another weapon, best tier wins */
	public enum Skill {
		NONE,
		AMETHYST,
		ECHO,
		STARLIGHT
	}

	/** why a socket attempt did or did not work, so the table can say something useful */
	public enum Result {
		OK,
		NOT_SOCKETABLE,
		NO_SPACE,
		WRONG_KIND,
		NO_TRAIT,
		CAP_REACHED,
		ARMOR_SOCKET_USED,
		NESTED,
		/** enchanted or renamed gear cannot be stored, a socket only keeps item and wear */
		CANNOT_PRESERVE
	}

	private Sockets() {
	}

	// ------------------------------------------------------------------ component access

	public static List<SocketedItem> get(ItemStack stack) {
		if (stack.isEmpty()) {
			return List.of();
		}

		return stack.getOrDefault(ModComponents.SOCKETS, List.of());
	}

	/** The item ids of the sockets, which is all the trait maths needs. */
	public static List<String> ids(ItemStack stack) {
		List<String> ids = new ArrayList<>(MAX_SLOTS);

		for (SocketedItem socket : get(stack)) {
			ids.add(socket.id());
		}

		return ids;
	}

	public static int slots(ItemStack stack) {
		return stack.isEmpty() ? 0 : slots(stack.getItem());
	}

	public static int slots(Item item) {
		String path = ourPath(item);

		if (path == null || slotKind(path) == Slot.NONE) {
			return 0;
		}

		if (path.startsWith("starlight_")) {
			return 5;
		}
		if (path.startsWith("echo_")) {
			return 4;
		}
		if (path.startsWith("bedrock_")) {
			return 3;
		}
		if (path.startsWith("obsidian_")) {
			return 2;
		}

		return 1;
	}

	public static Slot slotKind(ItemStack stack) {
		if (stack.isEmpty()) {
			return Slot.NONE;
		}

		String path = ourPath(stack.getItem());
		return path == null ? Slot.NONE : slotKind(path);
	}

	private static Slot slotKind(String path) {
		if (armorSlotOf(path) != null) {
			return Slot.ARMOR;
		}

		return isWeaponPath(path) ? Slot.WEAPON : Slot.NONE;
	}

	/** "helmet" / "chestplate" / "leggings" / "boots", or null. */
	private static String armorSlotOf(String path) {
		for (String slot : new String[]{"helmet", "chestplate", "leggings", "boots"}) {
			if (path.endsWith("_" + slot)) {
				return slot;
			}
		}

		return null;
	}

	private static boolean isWeaponPath(String path) {
		return path.endsWith("_sword") || path.endsWith("_pickaxe") || path.endsWith("_axe")
			|| path.endsWith("_shovel") || path.endsWith("_hoe") || path.endsWith("_spear")
			|| path.endsWith("_shield") || path.endsWith("_shield_glow");
	}

	private static String ourPath(Item item) {
		Identifier id = BuiltInRegistries.ITEM.getKey(item);

		if (!KalioupoEquipment.MOD_ID.equals(id.getNamespace())) {
			return null;
		}

		return id.getPath();
	}

	// ------------------------------------------------------------------ what counts as a gem

	/** Ingots and alloys are the "gems" of weapon sockets. */
	public static boolean isMaterial(ItemStack stack) {
		String path = stack.isEmpty() ? null : ourPath(stack.getItem());

		if (path == null) {
			return false;
		}

		return path.endsWith("_ingot") || path.equals("compressed_obsidian_alloy");
	}

	public static boolean isArmorPiece(ItemStack stack) {
		String path = stack.isEmpty() ? null : ourPath(stack.getItem());
		return path != null && armorSlotOf(path) != null;
	}

	public static boolean isSword(ItemStack stack) {
		String path = stack.isEmpty() ? null : ourPath(stack.getItem());
		return path != null && path.endsWith("_sword");
	}

	public static String idOf(ItemStack stack) {
		return stack.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
	}

	/** True when the item carries something a socket could not keep (enchantments, a name). */
	public static boolean cannotPreserve(ItemStack gem) {
		if (gem.isEmpty()) {
			return false;
		}

		if (EnchantmentHelper.hasAnyEnchantments(gem)) {
			return true;
		}

		return gem.has(DataComponents.CUSTOM_NAME);
	}

	public static boolean hasTrait(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}

		String id = idOf(stack);
		return chargeOf(id) > 0 || repairOf(id) > 0 || waterOf(id) || fallOf(id)
			|| quartzOf(id) || skillOf(id) != Skill.NONE;
	}

	// ------------------------------------------------------------------ traits by id

	/** Traits by id; 0 charge and 0 repair means "nothing happens, do not accept it". */
	public static int chargeOf(String id) {
		if (!id.startsWith(COPPER)) {
			return 0;
		}

		return isBigGem(id) ? COPPER_ARMOR_CHARGE : COPPER_INGOT_CHARGE;
	}

	public static int repairOf(String id) {
		if (!id.startsWith(ROSE_GOLD)) {
			return 0;
		}

		return isBigGem(id) ? ROSE_ARMOR_REPAIR : ROSE_INGOT_REPAIR;
	}

	public static boolean waterOf(String id) {
		return id.startsWith(PRISMARINE);
	}

	public static boolean fallOf(String id) {
		return id.startsWith(PHANTOM);
	}

	public static boolean quartzOf(String id) {
		return id.startsWith(QUARTZ);
	}

	/** Which active skill a socketed sword lends; swords without a skill return NONE. */
	public static Skill skillOf(String id) {
		if (!KalioupoEquipment.MOD_ID.equals(identifierNamespace(id))) {
			return Skill.NONE;
		}

		return switch (identifierPath(id)) {
			case "amethyst_sword" -> Skill.AMETHYST;
			case "echo_sword" -> Skill.ECHO;
			case "starlight_sword" -> Skill.STARLIGHT;
			default -> Skill.NONE;
		};
	}

	/** The best skill inherited by this weapon from its sockets. */
	public static Skill inherited(ItemStack host) {
		Skill best = Skill.NONE;

		for (SocketedItem socket : get(host)) {
			Skill skill = skillOf(socket.id());

			if (skill.ordinal() > best.ordinal()) {
				best = skill;
			}
		}

		return best;
	}

	/**
	 * The skill the weapon actually uses: an inherited one always wins over the weapon's own, so
	 * socketing a starlight sword into an echo sword really does give you the starlight modes
	 * instead of the echo sword quietly keeping its sonic boom.
	 */
	public static Skill effective(ItemStack held) {
		Skill inherited = inherited(held);

		if (inherited != Skill.NONE) {
			return inherited;
		}

		if (held.is(com.kalioupo.equipment.registry.ModItems.AMETHYST_SWORD)) {
			return Skill.AMETHYST;
		}

		if (held.is(com.kalioupo.equipment.registry.ModItems.ECHO_SWORD)) {
			return Skill.ECHO;
		}

		if (held.is(com.kalioupo.equipment.registry.ModItems.STARLIGHT_SWORD)) {
			return Skill.STARLIGHT;
		}

		return Skill.NONE;
	}

	/** A socket holding armour or a whole sword is worth more than a plain ingot. */
	private static boolean isBigGem(String id) {
		return isArmorId(id) || id.endsWith("_sword");
	}

	private static boolean isArmorId(String id) {
		return id.endsWith("_helmet") || id.endsWith("_chestplate")
			|| id.endsWith("_leggings") || id.endsWith("_boots");
	}

	private static String identifierNamespace(String id) {
		int split = id.indexOf(':');
		return split < 0 ? "" : id.substring(0, split);
	}

	private static String identifierPath(String id) {
		int split = id.indexOf(':');
		return split < 0 ? id : id.substring(split + 1);
	}

	// ------------------------------------------------------------------ socketing

	public static Result check(Player player, ItemStack host, ItemStack gem) {
		if (host.isEmpty() || gem.isEmpty()) {
			return Result.NOT_SOCKETABLE;
		}

		Slot hostKind = slotKind(host);

		if (hostKind == Slot.NONE) {
			return Result.NOT_SOCKETABLE;
		}

		if (get(host).size() >= slots(host)) {
			return Result.NO_SPACE;
		}

		// an already socketed item cannot go in: it would nest a whole socket list
		if (!get(gem).isEmpty()) {
			return Result.NESTED;
		}

		String gemId = idOf(gem);
		boolean gemIsArmor = isArmorPiece(gem);
		boolean gemIsMaterial = isMaterial(gem);
		boolean gemIsSword = isSword(gem);

		if (!gemIsArmor && !gemIsMaterial && !gemIsSword) {
			return Result.WRONG_KIND;
		}

		// weapons take materials and swords (the sword lends its skill), never armour
		if (hostKind == Slot.WEAPON && !gemIsMaterial && !gemIsSword) {
			return Result.WRONG_KIND;
		}

		// armour takes armour pieces and materials, never a sword
		if (hostKind == Slot.ARMOR && gemIsSword) {
			return Result.WRONG_KIND;
		}

		if (hostKind == Slot.ARMOR && gemIsArmor) {
			// only the same slot, and only one of them
			if (!armorSlotOf(ourPath(host.getItem())).equals(armorSlotOf(ourPath(gem.getItem())))) {
				return Result.WRONG_KIND;
			}

			if (hasArmorSocket(host)) {
				return Result.ARMOR_SOCKET_USED;
			}
		}

		if (!hasTrait(gem)) {
			return Result.NO_TRAIT;
		}

		if (cannotPreserve(gem)) {
			return Result.CANNOT_PRESERVE;
		}

		int charge = chargeOf(gemId);

		if (charge > 0 && player != null && totalCharge(player, host) + charge > CHARGE_CAP) {
			return Result.CAP_REACHED;
		}

		return Result.OK;
	}

	/** Appends the gem to the host and returns true when it fit. The gem itself is not consumed. */
	public static boolean insert(Player player, ItemStack host, ItemStack gem) {
		if (check(player, host, gem) != Result.OK) {
			return false;
		}

		add(host, gem);
		return true;
	}

	/** Stores the item and its wear, no checks (also used to undo a failed swap). */
	public static void add(ItemStack host, ItemStack gem) {
		add(host, new SocketedItem(idOf(gem), gem.getDamageValue()));
	}

	public static void add(ItemStack host, SocketedItem socket) {
		List<SocketedItem> sockets = new ArrayList<>(get(host));
		sockets.add(socket);
		host.set(ModComponents.SOCKETS, List.copyOf(sockets));
	}

	/** Removes and returns the last socket as an item with its wear restored. */
	public static ItemStack extractLast(ItemStack host) {
		List<SocketedItem> sockets = new ArrayList<>(get(host));

		if (sockets.isEmpty()) {
			return ItemStack.EMPTY;
		}

		SocketedItem removed = sockets.remove(sockets.size() - 1);
		set(host, sockets);
		return stackOf(removed);
	}

	/** Pulls every socket out, used when the host is about to break. */
	public static List<ItemStack> ejectAll(ItemStack host) {
		List<SocketedItem> sockets = new ArrayList<>(get(host));
		set(host, new ArrayList<>());

		List<ItemStack> out = new ArrayList<>(sockets.size());

		for (SocketedItem socket : sockets) {
			out.add(stackOf(socket));
		}

		return out;
	}

	private static void set(ItemStack host, List<SocketedItem> sockets) {
		if (sockets.isEmpty()) {
			host.remove(ModComponents.SOCKETS);
		} else {
			host.set(ModComponents.SOCKETS, List.copyOf(sockets));
		}
	}

	private static ItemStack stackOf(SocketedItem socket) {
		Identifier key = Identifier.tryParse(socket.id());

		if (key == null) {
			return ItemStack.EMPTY;
		}

		Item item = BuiltInRegistries.ITEM.get(key).map(holder -> holder.value()).orElse(null);

		if (item == null || item == Items.AIR) {
			return ItemStack.EMPTY;
		}

		ItemStack stack = new ItemStack(item);

		if (socket.damage() > 0 && stack.isDamageableItem()) {
			stack.setDamageValue(Math.min(socket.damage(), stack.getMaxDamage() - 1));
		}

		return stack;
	}

	// ------------------------------------------------------------------ trait totals

	public static int chargePercent(List<SocketedItem> sockets) {
		int total = 0;

		for (SocketedItem socket : sockets) {
			total += chargeOf(socket.id());
		}

		return total;
	}

	public static int chargePercent(ItemStack stack) {
		return chargePercent(get(stack));
	}

	public static int repairPoints(ItemStack stack) {
		int total = 0;

		for (SocketedItem socket : get(stack)) {
			total += repairOf(socket.id());
		}

		return total;
	}

	public static boolean hasArmorSocket(List<SocketedItem> sockets) {
		for (SocketedItem socket : sockets) {
			if (isArmorId(socket.id())) {
				return true;
			}
		}

		return false;
	}

	public static boolean hasArmorSocket(ItemStack stack) {
		return hasArmorSocket(get(stack));
	}

	private static boolean anyTrait(ItemStack stack, Trait trait) {
		for (SocketedItem socket : get(stack)) {
			String id = socket.id();
			boolean hit = switch (trait) {
				case WATER -> waterOf(id);
				case FALL -> fallOf(id);
				case CHARGE -> chargeOf(id) > 0;
				case REPAIR -> repairOf(id) > 0;
			};

			if (hit) {
				return true;
			}
		}

		return false;
	}

	private enum Trait {
		CHARGE,
		REPAIR,
		WATER,
		FALL
	}

	/** every worn or held item, the only places sockets are read from */
	private static List<ItemStack> equipped(Player player) {
		List<ItemStack> stacks = new ArrayList<>(6);
		stacks.add(player.getMainHandItem());
		stacks.add(player.getOffhandItem());
		stacks.add(player.getItemBySlot(EquipmentSlot.HEAD));
		stacks.add(player.getItemBySlot(EquipmentSlot.CHEST));
		stacks.add(player.getItemBySlot(EquipmentSlot.LEGS));
		stacks.add(player.getItemBySlot(EquipmentSlot.FEET));
		return stacks;
	}

	/** The charge bonus the player would have if one more gem went into {@code host}. */
	public static int totalCharge(Player player, ItemStack host) {
		if (player == null) {
			return chargePercent(host);
		}

		int total = 0;

		for (ItemStack stack : equipped(player)) {
			if (stack == host) {
				continue;
			}

			total += chargePercent(stack);
		}

		return total + chargePercent(host);
	}

	/** The charge bonus the player has right now, capped. */
	public static int totalCharge(Player player) {
		return Math.min(CHARGE_CAP, player == null ? 0 : totalCharge(player, ItemStack.EMPTY));
	}

	/** Socketed copper anywhere on the wearer raises the charge speed of the swords. */
	public static double chargeMultiplier(LivingEntity entity) {
		if (!(entity instanceof Player player)) {
			return 1.0D;
		}

		return 1.0D + totalCharge(player) / 100.0D;
	}

	public static boolean hasWaterTrait(Player player) {
		if (player == null) {
			return false;
		}

		for (ItemStack stack : equipped(player)) {
			if (anyTrait(stack, Trait.WATER)) {
				return true;
			}
		}

		return false;
	}

	public static boolean hasFallTrait(Player player) {
		return fallPercent(player) > 0;
	}

	/**
	 * How much fall damage the wearer ignores: the first phantom socket already does most of the
	 * work (-50%), every further one adds another -10%, capped at -80%. So one socket is worth
	 * having and a full set of them is still better, but stacking is never full immunity.
	 */
	public static int fallPercent(Player player) {
		if (player == null) {
			return 0;
		}

		int sockets = 0;

		for (ItemStack stack : equipped(player)) {
			for (SocketedItem socket : get(stack)) {
				if (fallOf(socket.id())) {
					sockets++;
				}
			}
		}

		if (sockets <= 0) {
			return 0;
		}

		return Math.min(80, 50 + 10 * (sockets - 1));
	}

	/** How many quartz sockets the given item carries (already capped). */
	public static int quartzSockets(ItemStack stack) {
		int count = 0;

		for (SocketedItem socket : get(stack)) {
			if (quartzOf(socket.id())) {
				count++;
			}
		}

		return Math.min(QUARTZ_MAX_STACK, count);
	}
}
