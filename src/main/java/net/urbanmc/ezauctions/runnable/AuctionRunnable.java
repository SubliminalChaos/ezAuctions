package net.urbanmc.ezauctions.runnable;

import net.md_5.bungee.api.chat.BaseComponent;
import net.milkbowl.vault.economy.Economy;
import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.event.AuctionEndEvent;
import net.urbanmc.ezauctions.manager.ConfigManager;
import net.urbanmc.ezauctions.object.Auction;
import net.urbanmc.ezauctions.object.Bidder;
import net.urbanmc.ezauctions.util.MessageUtil;
import net.urbanmc.ezauctions.util.RewardUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.UUID;

public class AuctionRunnable extends BukkitRunnable {

    private Auction auction;
    private UUID auctioneer;
    private int timeLeft;
    private List<Integer> broadcastTimes = ConfigManager.getConfig().getIntegerList("auctions.broadcast-times");
    private int antiSnipeTimesRun = 0;

    public AuctionRunnable(Auction auction, EzAuctions plugin) {
        this.auction = auction;
        this.auctioneer = auction.getAuctioneer().getUniqueId();
        this.timeLeft = auction.getAuctionTime();

        broadcastStart();
        runTaskTimer(plugin, 0, 20);
    }

    @Override
    public void run() {
        if (broadcastTimes.contains(timeLeft)) {
            MessageUtil.broadcastSpammy(auctioneer, "auction.time_left", timeLeft);
        }

        if (timeLeft == 0) {
            endAuction();
            return;
        }

        timeLeft--;
        getAuction().setAuctionTime(timeLeft);
    }

    private void broadcastStart() {
        BaseComponent comp = getAuction().getStartingMessage();

        MessageUtil.broadcastRegular(auctioneer, comp);
    }

    public Auction getAuction() {
        return auction;
    }

    public int getAntiSnipeTimesRun() {
        return antiSnipeTimesRun;
    }

    public void antiSnipe() {
        FileConfiguration data = ConfigManager.getConfig();

        if (!(getAntiSnipeTimesRun() < data.getInt("antisnipe.run-times")))
            return;

        int addTime = data.getInt("antisnipe.time");

        MessageUtil.broadcastSpammy(auctioneer, "auction.antisnipe", addTime);
        timeLeft += addTime;

        antiSnipeTimesRun++;
    }

    public void endAuction() {
        cancel();

        AuctionEndEvent event = new AuctionEndEvent(getAuction());
        Bukkit.getPluginManager().callEvent(event);

        EzAuctions.getAuctionManager().next();

        if (getAuction().anyBids()) {
            Auction auc = getAuction();
            Economy econ = EzAuctions.getEcon();

            Bidder lastBidder = auc.getLastBidder();

            String lastBidderName = lastBidder.getBidder().getOfflinePlayer().getName();
            double lastBidAmount = lastBidder.getAmount();

            MessageUtil.broadcastRegular(auctioneer, "auction.finish", lastBidderName, lastBidAmount);

            RewardUtil.rewardAuction(auc, econ);
        } else {
            MessageUtil.broadcastRegular(auctioneer, "auction.finish.no_bids");
            RewardUtil.rewardCancel(getAuction());
        }
    }

    public void cancelAuction() {
        cancel();

        MessageUtil.broadcastRegular(auctioneer, "auction.cancelled");
        EzAuctions.getAuctionManager().next();

        RewardUtil.rewardCancel(getAuction());
    }

    public void impoundAuction(Player impounder) {
        cancel();

        MessageUtil.broadcastRegular(auctioneer, "auction.impounded");
        EzAuctions.getAuctionManager().next();

        RewardUtil.rewardImpound(getAuction(), impounder);
    }
}
