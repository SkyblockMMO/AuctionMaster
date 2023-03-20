package me.intel.AuctionMaster.Menus;

import me.intel.AuctionMaster.AuctionMaster;
import me.intel.AuctionMaster.AuctionObjects.AuctionBIN;
import me.intel.AuctionMaster.AuctionObjects.AuctionClassic;
import me.intel.AuctionMaster.Utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import java.util.ArrayList;

public class CreateAuctionConfirmMenu {
    private Inventory inventory;
    private final ClickListen listener = new ClickListen();
    private double fee;

    public CreateAuctionConfirmMenu(Player player, double fee){
        Bukkit.getScheduler().runTaskAsynchronously(AuctionMaster.plugin, () -> {
            this.fee = fee;
            inventory = Bukkit.createInventory(player, AuctionMaster.configLoad.createAuctionConfirmMenuSize, AuctionMaster.utilsAPI.chat(player, AuctionMaster.configLoad.createAuctionConfirmMenuName));

            if (AuctionMaster.configLoad.useBackgoundGlass)
                for (int i = 0; i < AuctionMaster.configLoad.createAuctionConfirmMenuSize; i++)
                    inventory.setItem(i, AuctionMaster.configLoad.backgroundGlass.clone());

            ArrayList<String> lore = new ArrayList<>();
            for (String line : AuctionMaster.configLoad.confirmItemLore)
                lore.add(AuctionMaster.utilsAPI.chat(player, line.replace("%cost%", AuctionMaster.numberFormatHelper.formatNumber(fee))));
            inventory.setItem(AuctionMaster.menusCfg.getInt("create-auction-confirm-menu.confirm-item-slot"), AuctionMaster.itemConstructor.getItem(AuctionMaster.configLoad.confirmItemMaterial, AuctionMaster.utilsAPI.chat(player, AuctionMaster.configLoad.confirmItemName.replace("%cost%", AuctionMaster.numberFormatHelper.formatNumber(fee))), lore));

            lore = new ArrayList<>();
            for (String line : AuctionMaster.configLoad.cancelItemLore)
                lore.add(AuctionMaster.utilsAPI.chat(player, line));
            inventory.setItem(AuctionMaster.menusCfg.getInt("create-auction-confirm-menu.cancel-item-slot"), AuctionMaster.itemConstructor.getItem(AuctionMaster.configLoad.cancelItemMaterial, AuctionMaster.utilsAPI.chat(player, AuctionMaster.configLoad.cancelItemName), lore));

            Bukkit.getScheduler().runTask(AuctionMaster.plugin, () -> {
                Bukkit.getPluginManager().registerEvents(listener, AuctionMaster.plugin);
                player.openInventory(inventory);
            });
        });
    }

    public class ClickListen implements Listener {
        private boolean singleClick=false;

        @EventHandler
        public void onClick(InventoryClickEvent e){
            if (!(e.getWhoClicked() instanceof Player))
                return;
            if (!e.getInventory().equals(inventory))
                return;
            if (e.getClickedInventory() == null)
                return;
            if (!e.getClickedInventory().equals(inventory))
                return;

            Player player = (Player) e.getWhoClicked();
            e.setCancelled(true);
            if (e.getCurrentItem()==null || e.getCurrentItem().getType().equals(Material.AIR))
                return;

            if (e.getClickedInventory().equals(inventory)) {
                if (e.getSlot() == AuctionMaster.menusCfg.getInt("create-auction-confirm-menu.cancel-item-slot")) {
                    Utils.playSound(player, "auction-cancel");
                    new CreateAuctionMainMenu(player);
                }
                else if (e.getSlot() == AuctionMaster.menusCfg.getInt("create-auction-confirm-menu.confirm-item-slot")){
                    if(singleClick)
                        return;
                    singleClick=true;
                    player.closeInventory();

                    String uuid = player.getUniqueId().toString();
                    double startingBid = AuctionMaster.auctionsHandler.startingBid.getOrDefault(uuid, AuctionMaster.configLoad.defaultStartingBid);
                    long duration;
                    if (AuctionMaster.auctionsHandler.startingDuration.containsKey(uuid))
                        duration= AuctionMaster.auctionsHandler.startingDuration.get(uuid);
                    else
                        duration= AuctionMaster.configLoad.defaultDuration;

                    Boolean status;
                    if (AuctionMaster.auctionsHandler.buyItNowSelected != null && (AuctionMaster.configLoad.onlyBuyItNow || ((AuctionMaster.configLoad.defaultBuyItNow && !AuctionMaster.auctionsHandler.buyItNowSelected.contains(player.getUniqueId().toString())) || (!AuctionMaster.configLoad.defaultBuyItNow && AuctionMaster.auctionsHandler.buyItNowSelected.contains(player.getUniqueId().toString())))))
                        status = AuctionMaster.auctionsHandler.createAuction(new AuctionBIN(player, startingBid, duration, AuctionMaster.auctionsHandler.previewItems.get(uuid)));
                    else
                        status = AuctionMaster.auctionsHandler.createAuction(new AuctionClassic(player, startingBid, duration, AuctionMaster.auctionsHandler.previewItems.get(uuid)));
                    if (!status)
                        return;
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "husksynclock " + player.getName());
                    Utils.playSound(player, "auction-confirm");
                    AuctionMaster.economy.removeMoney(player, fee);
                    AuctionMaster.auctionsDatabase.deletePreviewItems(uuid);
                    AuctionMaster.auctionsHandler.previewItems.remove(uuid);
                    AuctionMaster.auctionsDatabase.removePreviewItem(uuid);
                    AuctionMaster.auctionsHandler.startingDuration.remove(uuid);
                    AuctionMaster.auctionsHandler.startingBid.remove(uuid);

                    String text = AuctionMaster.utilsAPI.chat(player, AuctionMaster.auctionsManagerCfg.getString("auction-created-message"));
                    if (text != null && !text.equals(""))
                        player.sendMessage(text);
                }
            }
        }

        @EventHandler
        public void onClose(InventoryCloseEvent e){
            if(inventory.equals(e.getInventory())) {
                HandlerList.unregisterAll(this);
                inventory = null;
            }
        }
    }
}