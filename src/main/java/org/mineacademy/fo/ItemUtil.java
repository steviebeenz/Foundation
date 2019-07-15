package org.mineacademy.fo;

import org.apache.commons.lang.WordUtils;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.nbt.NBTItem;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

/**
 * Utility class for managing items.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ItemUtil {

	// Is Minecraft older than 1.13? Storing here for best performance.
	private static final boolean LEGACY_MATERIALS = MinecraftVersion.olderThan(V.v1_13);

	// ----------------------------------------------------------------------------------------------------
	// Converting strings into Bukkit item-related classes
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Looks up a {@link PotionEffect} from the given name,
	 * failing if not found
	 *
	 * @param name
	 * @return
	 */
	public static PotionEffectType findPotion(String name) {
		name = PotionWrapper.getBukkitName(name);

		final PotionEffectType potion = PotionEffectType.getByName(name);
		Valid.checkNotNull(potion, "Invalid potion '" + name + "'! For valid names, see: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/potion/PotionEffectType.html");

		return potion;
	}

	/**
	 * Looks up an {@link Enchantment} from the given name,
	 * failing if not found
	 *
	 * @param name
	 * @return
	 */
	public static Enchantment findEnchantment(String name) {
		name = EnchantmentWrapper.toBukkit(name);

		final Enchantment e = Enchantment.getByName(name);
		Valid.checkNotNull(e, "Invalid enchantment '" + name + "'! For valid names, see: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/enchantments/Enchantment.html");

		return e;
	}

	// ----------------------------------------------------------------------------------------------------
	// Enumeration - fancy names
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Removes _ from the enum, lowercases everything and finally capitalizes it
	 *
	 * @param enumeration
	 * @return
	 */
	public static String bountifyCapitalized(Enum<?> enumeration) {
		return bountifyCapitalized(enumeration.toString());
	}

	/**
	 * Removes _ from the name, lowercases everything and finally capitalizes it
	 *
	 * @param name
	 * @return
	 */
	public static String bountifyCapitalized(String name) {
		return WordUtils.capitalizeFully(bountify(name));
	}

	/**
	 * Returns a human readable fancy potion effect type
	 *
	 * @param enumeration
	 * @return
	 */
	public static String bountify(PotionEffectType enumeration) {
		return PotionWrapper.getLocalizedName(enumeration.getName());
	}

	/**
	 * Returns a fancy enchantment name
	 *
	 * @param enchant
	 * @return
	 */
	public static String bountify(Enchantment enchant) {
		return EnchantmentWrapper.toMinecraft(enchant.getName());
	}

	/**
	 * Lowercases the given enum and replaces _ with spaces
	 *
	 * @param enumeration
	 * @return
	 */
	public static String bountify(Enum<?> enumeration) {
		return bountify(enumeration.toString());
	}

	/**
	 * Lowercases the given name and replaces _ with spaces
	 *
	 * @param name
	 * @return
	 */
	public static String bountify(String name) {
		return name.toLowerCase().replace("_", " ");
	}

	// ----------------------------------------------------------------------------------------------------
	// Comparing items
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Compares two items. Returns true if they are similar.
	 *
	 * Two items are similar if both are not null and if their type, data, name and lore equals.
	 * The damage, quantity, item flags enchants and other properties are ignored.
	 *
	 * @param first
	 * @param second
	 * @return true if items are similar (see above)
	 */
	public static boolean isSimilar(ItemStack first, ItemStack second) {
		if (first == null || second == null)
			return false;

		final boolean idMatch = first.getType() == second.getType();
		boolean dataMatch = LEGACY_MATERIALS ? first.getData().getData() == second.getData().getData() : true;
		final boolean metaMatch = first.hasItemMeta() == second.hasItemMeta();

		if (!idMatch || !metaMatch || !(dataMatch || (dataMatch = first.getType() == Material.BOW))) {
			return false;
		}

		// ItemMeta
		{
			final ItemMeta f = first.getItemMeta();
			final ItemMeta s = second.getItemMeta();

			final String fName = Common.stripColors(Common.getOrEmpty(f.getDisplayName()).toLowerCase());
			final String sName = Common.stripColors(Common.getOrEmpty(s.getDisplayName()).toLowerCase());

			if (!fName.equals(sName) || !Valid.listEquals(f.getLore(), s.getLore()))
				return false;
		}

		final NBTItem firstNbt = new NBTItem(first);
		final NBTItem secondNbt = new NBTItem(second);

		return matchNbt(SimplePlugin.getNamed(), firstNbt, secondNbt) && matchNbt(SimplePlugin.getNamed() + "_Item", firstNbt, secondNbt);
	}

	// Compares the NBT string tag of two items
	private static boolean matchNbt(String key, NBTItem firstNbt, NBTItem secondNbt) {
		final boolean firstHas = firstNbt.hasKey(key);
		final boolean secondHas = secondNbt.hasKey(key);

		if (!firstHas && !secondHas)
			return true; // nothing has, essentially same

		else if (firstHas && !secondHas || !firstHas && secondHas)
			return false; // one has but another hasn't, cannot be same

		return firstNbt.getString(key).equals(secondNbt.getString(key));
	}
}

/**
 * A simple class holding some of the potion names
 */
@RequiredArgsConstructor
enum PotionWrapper {

	SLOW("SLOW", "Slowness"),
	STRENGTH("INCREASE_DAMAGE"),
	JUMP_BOOST("JUMP"),
	INSTANT_HEAL("INSTANT_HEALTH"),
	REGEN("REGENERATION");

	private final String bukkitName;
	private final String minecraftName;

	private PotionWrapper(String bukkitName) {
		this(bukkitName, null);
	}

	protected static String getLocalizedName(String name) {
		String localizedName = name;

		for (final PotionWrapper e : values())
			if (name.toUpperCase().replace(" ", "_").equals(e.bukkitName)) {
				localizedName = e.getMinecraftName();

				break;
			}

		return WordUtils.capitalizeFully(localizedName.replace("_", " "));
	}

	protected static String getBukkitName(String name) {
		name = name.toUpperCase().replace(" ", "_");

		for (final PotionWrapper e : values())
			if (e.toString().equalsIgnoreCase(name) || e.minecraftName != null && e.minecraftName.equalsIgnoreCase(name))
				return e.bukkitName;

		return name;
	}

	public String getMinecraftName() {
		return Common.getOrDefault(minecraftName, bukkitName);
	}
}

/**
 * A simple class holding some of the enchantments names
 */
@RequiredArgsConstructor
enum EnchantmentWrapper {
	PROTECTION("PROTECTION_ENVIRONMENTAL"),
	FIRE_PROTECTION("PROTECTION_FIRE"),
	FEATHER_FALLING("PROTECTION_FALL"),
	BLAST_PROTECTION("PROTECTION_EXPLOSIONS"),
	PROJECTILE_PROTECTION("PROTECTION_PROJECTILE"),
	RESPIRATION("OXYGEN"),
	AQUA_AFFINITY("WATER_WORKER"),
	THORN("THORNS"),
	CURSE_OF_VANISHING("VANISHING_CURSE"),
	CURSE_OF_BINDING("BINDING_CURSE"),
	SHARPNESS("DAMAGE_ALL"),
	SMITE("DAMAGE_UNDEAD"),
	BANE_OF_ARTHROPODS("DAMAGE_ARTHROPODS"),
	LOOTING("LOOT_BONUS_MOBS"),
	SWEEPING_EDGE("SWEEPING"),
	EFFICIENCY("DIG_SPEED"),
	UNBREAKING("DURABILITY"),
	FORTUNE("LOOT_BONUS_BLOCKS"),
	POWER("ARROW_DAMAGE"),
	PUNCH("ARROW_KNOCKBACK"),
	FLAME("ARROW_FIRE"),
	INFINITY("ARROW_INFINITE"),
	LUCK_OF_THE_SEA("LUCK");

	private final String bukkitName;

	protected static String toBukkit(String name) {
		name = name.toUpperCase().replace(" ", "_");

		for (final EnchantmentWrapper e : values())
			if (e.toString().equals(name))
				return e.bukkitName;

		return name;
	}

	protected static String toMinecraft(String name) {
		name = name.toUpperCase().replace(" ", "_");

		for (final EnchantmentWrapper e : values())
			if (name.equals(e.bukkitName))
				return ItemUtil.bountifyCapitalized(e);

		return WordUtils.capitalizeFully(name);
	}

	public String getBukkitName() {
		return bukkitName != null ? bukkitName : name();
	}
}