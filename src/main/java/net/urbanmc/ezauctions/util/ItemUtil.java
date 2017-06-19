package net.urbanmc.ezauctions.util;

import net.milkbowl.vault.item.ItemInfo;
import net.milkbowl.vault.item.Items;
import net.urbanmc.ezauctions.object.Auction;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemUtil {

	public static void removeItemsFromInv(Auction auc, Player p) {
		int amount = auc.getAmount();
		ItemStack auctionItem = auc.getItem();

		for (int i = 0; i < p.getInventory().getSize(); i++) {
			if (p.getInventory().getItem(i) == null)
				continue;

			ItemStack is = p.getInventory().getItem(i);

			if (!auctionItem.isSimilar(is))
				continue;

			if (is.getAmount() > amount) {
				is.setAmount(is.getAmount() - amount);
				p.getInventory().setItem(i, is);

				break;
			}

			amount -= is.getAmount();
			p.getInventory().setItem(i, null);

			if (amount == 0)
				break;
		}
	}

	@SuppressWarnings("deprecation")
	static Material getMaterial(String type) {
		Material material = Material.getMaterial(type.toUpperCase());

		if (material != null)
			return material;

		if (isInt(type)) {
			int id = Integer.parseInt(type);
			material = Material.getMaterial(id);

			if (material != null)
				return material;
		}

		ItemInfo item = Items.itemByName(type);

		if (item != null)
			return item.getType();

		return null;
	}

	/**
	 * @return true if there is overflow, false if not
	 */
	static boolean addItemToInventory(Player p, ItemStack is, int amount, boolean message) {
		List<ItemStack> items = new ArrayList<>();

		int maxStackSize = ReflectionUtil.getMaxStackSize(is);

		while (amount > maxStackSize) {
			ItemStack cloned = is.clone();
			cloned.setAmount(maxStackSize);

			items.add(cloned);
			amount -= maxStackSize;
		}

		if (amount != 0) {
			ItemStack cloned = is.clone();
			cloned.setAmount(amount);

			items.add(cloned);
		}

		ItemStack[] array = new ItemStack[items.size()];
		array = items.toArray(array);

		Map<Integer, ItemStack> leftover = p.getInventory().addItem(array);

		if (!leftover.isEmpty()) {
			leftover.values().forEach(item -> p.getWorld().dropItemNaturally(p.getLocation(), item));

			if (message) {
				MessageUtil.privateMessage(p, "reward.full_inventory");
			}
		}

		return !leftover.isEmpty();
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	private static boolean isInt(String s) {
		try {
			Integer.parseInt(s);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
}
