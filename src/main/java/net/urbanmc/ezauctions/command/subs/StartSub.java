package net.urbanmc.ezauctions.command.subs;

import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.event.AuctionQueueEvent;
import net.urbanmc.ezauctions.manager.AuctionManager;
import net.urbanmc.ezauctions.manager.AuctionsPlayerManager;
import net.urbanmc.ezauctions.manager.ConfigManager;
import net.urbanmc.ezauctions.object.Auction;
import net.urbanmc.ezauctions.object.AuctionsPlayer;
import net.urbanmc.ezauctions.object.Permission;
import net.urbanmc.ezauctions.util.AuctionUtil;
import net.urbanmc.ezauctions.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StartSub extends SubCommand {

	public StartSub() {
		super("start", Permission.COMMAND_START, true, "s");
	}

	public void run(CommandSender sender, String[] args) {

		Player p = (Player) sender;

		if (args.length < 3 || args.length > 6) {
			sendPropMessage(p, "command.auction.start.help");
			return;
		}

		AuctionManager manager = EzAuctions.getAuctionManager();

		if (!manager.isAuctionsEnabled()) {
			sendPropMessage(p, "command.auction.start.disabled");
			return;
		}

		if (manager.getQueueSize() == ConfigManager.getConfig().getInt("general.auction-queue-limit")) {
			sendPropMessage(p, "command.auction.start.queue_full");
			return;
		}

		if (manager.inQueueOrCurrent(p.getUniqueId())) {
			sendPropMessage(p, "command.auction.start.in_queue");
			return;
		}

		if (!hasFee(p)) {
			sendPropMessage(p, "command.auction.start.lacking_fee");
			return;
		}

		AuctionsPlayer ap = AuctionsPlayerManager.getInstance().getPlayer(p.getUniqueId());

		Auction auction = AuctionUtil.parseAuction(
				ap,
				args[1],
				args[2],
				args.length < 4 ? String
						.valueOf(ConfigManager.getInstance().get("auctions.default.increment")) : args[3],
				args.length < 5 ? String.valueOf(ConfigManager.getInstance().get("auctions.default.autobuy")) :
						args[4],
				args.length < 6 ? String
						.valueOf(ConfigManager.getConfig().getInt("auctions.default.auction-time")) : args[5],
				false);

		if (auction == null)
			return;

		AuctionQueueEvent event = new AuctionQueueEvent(auction);
		Bukkit.getPluginManager().callEvent(event);

		if (event.isCancelled())
			return;

		removeFee(p);

		ItemUtil.removeItemsFromInv(auction, p);

		if (EzAuctions.getAuctionManager().addToQueue(auction)) {
			int position = EzAuctions.getAuctionManager().getPositionInQueue(ap);
			sendPropMessage(p, "command.auction.start.added_to_queue", position);
		}
	}

	private boolean hasFee(Player p) {
		double fee = ConfigManager.getConfig().getDouble("auctions.fees.start-price");

		return EzAuctions.getEcon().has(p, fee);
	}

	private void removeFee(Player p) {
		double fee = ConfigManager.getConfig().getDouble("auctions.fees.start-price");

		if (fee > 0) {
			EzAuctions.getEcon().withdrawPlayer(p, fee);
		}
	}
}
