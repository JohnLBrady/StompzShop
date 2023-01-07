package me.diamond.stompzstore;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;

public class StoreListener implements Listener{

    Main pl;
    PluginManager pm;

    public StoreListener(Main plugin){
        pl = plugin;
        pm = pl.getServer().getPluginManager();
        pm.registerEvents(this, pl);
    }

    @EventHandler
    public void interactStore(PlayerInteractEvent e){
        Player p = e.getPlayer();
        Block b = e.getClickedBlock();
        if(pl.isPlacingStore(p)){
            if(!pl.isStoreBlock(b)){
                Main.placingstores.get(p).setBlock(b);
                Main.placingstores.remove(p);
                p.sendMessage(ChatColor.BLUE + "You have linked a store with a block!");
            }else{
                p.sendMessage(ChatColor.RED + "That is already a store block for an item.");
            }
            return;
        }
        if(pl.isStoreBlock(b)){
            Transaction tr = new Transaction(p, pl.getbStore(b));
            pl.customers.put(p,tr);
            updateStore(p,tr);
        }
    }

    @EventHandler
    public void StoreOptions(InventoryClickEvent e){
        Player p = (Player) e.getWhoClicked();
        if(Main.customers.keySet().contains(p)){
            Transaction t = Main.customers.get(p);
            ItemStack clicked = e.getCurrentItem();
            if(clicked == null){
                return;
            }
            if(clicked.equals(t.buyitem)){
                t.toggleBS();
                updateStore(p,t);
            }else if(clicked.equals(t.sellitem)){
                t.toggleBS();
                updateStore(p,t);
            }else if(clicked.equals(t.acceptitem)){
                if(e.getClickedInventory().equals(t.itemnumber)){
                    updateStore(p,t);
                }else{
                    closeStore(p);

                    t.acceptTransaction();

                    pl.saveStats(t.store.name, t.store.buystat, t.store.sellstat);

                    pl.customers.remove(p);
                }
            }else if(clicked.equals(t.cancelitem)){
                if(e.getClickedInventory().equals(t.storeUI)){
                    closeStore(p);
                    pl.customers.remove(p);
                }else{
                    updateStore(p,t);
                }
            }else if(clicked.equals(t.add1item)){
                if(t.checkMath(1, p)){
                    t.add1();
                    updateItemChange(p,t);
                }
            }else if(clicked.equals(t.add8item)){
                if(t.checkMath(8, p)){
                    t.add8();
                    updateItemChange(p,t);
                }
            }else if(clicked.equals(t.add64item)){
                if(t.checkMath(64, p)){
                    t.add64();
                    updateItemChange(p,t);
                }
            }else if(clicked.equals(t.remove1item)){
                if(t.checkMath(-1, p)){
                    t.remove1();
                    updateItemChange(p,t);
                }
            }else if(clicked.equals(t.remove8item)){
                if(t.checkMath(-8, p)){
                    t.remove8();
                    updateItemChange(p,t);
                }
            }else if(clicked.equals(t.remove64item)){
                if(t.checkMath(-64, p)){
                    t.remove64();
                    updateItemChange(p,t);
                }
            }else if(clicked.equals(t.main)){
                if(e.getClickedInventory().equals(t.storeUI)) {
                    updateItemChange(p, t);
                }
            }

            e.setCancelled(true);
        }
    }

    @EventHandler
    public void BreakStore(BlockBreakEvent e){
        Player p = e.getPlayer();
        if(pl.isStoreBlock(e.getBlock())) {
            if (p.hasPermission("stompzstore.break")) {
                pl.getbStore(e.getBlock()).removeBlock();
                p.sendMessage(ChatColor.BLUE + "You have unlinked the store block.");
            } else {
                p.sendMessage(ChatColor.RED + "You cannot break that store.");
            }
        }
    }


    @EventHandler
    public void onExitStore(InventoryCloseEvent e){

        Player p = (Player) e.getPlayer();
        if(pl.customers.keySet().contains(p)) {
            BukkitScheduler scheduler = pl.getServer().getScheduler();
            scheduler.runTask(pl, new Runnable() {
                @Override
                public void run() {
                    if (p.getOpenInventory().getType().equals(InventoryType.CRAFTING) || p.getOpenInventory().getType().equals(InventoryType.CRAFTING)) {
                        pl.customers.remove(p);
                    }
                }
            });
        }
    }

    @EventHandler
    public void onOpJoin(PlayerJoinEvent e){
        Player p = e.getPlayer();
        pl.checkForWarnings();
        if(pl.warnings.size() > 0) {
            if (p.hasPermission("stompzstore.inform")) {
                p.sendMessage(ChatColor.RED + "There are warnings from StompzStore that need resolving.");
            }
        }
    }


    public void updateStore(Player p, Transaction t){
        BukkitScheduler scheduler = pl.getServer().getScheduler();
        scheduler.runTask(pl, new Runnable() {
            @Override
            public void run() {
                p.openInventory(t.storeUI);
            }
        });
    }

    public void updateItemChange(Player p, Transaction t){
        BukkitScheduler scheduler = pl.getServer().getScheduler();
        scheduler.runTask(pl, new Runnable() {
            @Override
            public void run() {
                p.openInventory(t.itemnumber);
            }
        });
    }

    public void closeStore(Player p){
        BukkitScheduler scheduler = pl.getServer().getScheduler();
        scheduler.runTask(pl, new Runnable() {
            @Override
            public void run() {
                p.closeInventory();
            }
        });
    }


}
