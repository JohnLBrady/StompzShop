package me.diamond.stompzstore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Transaction {

    Store store = null;
    Player player = null;

    Inventory itemnumber = Bukkit.createInventory(null,9,"Change Amount");
    Inventory storeUI = Bukkit.createInventory(null, InventoryType.DISPENSER,"Stompz Store");

    ItemStack main = null;

    ItemStack buyitem = new ItemStack(Material.PURPLE_SHULKER_BOX);
    ItemStack sellitem = new ItemStack(Material.EMERALD);
    ItemStack stompzitem = new ItemStack(Material.GOLD_INGOT);
    ItemStack cancelitem = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short)14);
    ItemStack acceptitem = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short)5);
    ItemStack capacityitem = new ItemStack(Material.CHEST);
    ItemStack notavailable = new ItemStack(Material.BARRIER);

    ItemStack add1item = new ItemStack(Material.IRON_NUGGET);
    ItemStack add8item = new ItemStack(Material.IRON_INGOT);
    ItemStack add64item = new ItemStack(Material.IRON_BLOCK);
    ItemStack remove1item = new ItemStack(Material.IRON_NUGGET);
    ItemStack remove8item = new ItemStack(Material.IRON_INGOT);
    ItemStack remove64item = new ItemStack(Material.IRON_BLOCK);

    ItemStack fullinv = new ItemStack(Material.BOOK);

    double buy = 0;
    double sell = 0;
    int quantity = 1;

    boolean buying = true;

    public Transaction(Player p, Store s){
        store = s;
        player = p;
        buy = store.buy;
        sell = store.sell;

        setMainItem();
        setItems();
        setInventories();
    }

    public void setItems(){
        ItemMeta buyitemm =  buyitem.getItemMeta();
        ItemMeta sellitemm = sellitem.getItemMeta();
        ItemMeta stompzitemm = stompzitem.getItemMeta();
        ItemMeta cancelitemm = cancelitem.getItemMeta();
        ItemMeta acceptitemm = acceptitem.getItemMeta();
        ItemMeta capacityitemm = capacityitem.getItemMeta();
        ItemMeta naitemm = notavailable.getItemMeta();

        ItemMeta add1itemm = add1item.getItemMeta();
        ItemMeta add8itemm = add8item.getItemMeta();
        ItemMeta add64itemm = add64item.getItemMeta();
        ItemMeta remove1itemm = remove1item.getItemMeta();
        ItemMeta remove8itemm = remove8item.getItemMeta();
        ItemMeta remove64itemm = remove64item.getItemMeta();

        ItemMeta fullinvmeta = fullinv.getItemMeta();

        buyitemm.setDisplayName("Buy");
        sellitemm.setDisplayName("Sell");
        stompzitemm.setDisplayName(buy + " Stompz");
        cancelitemm.setDisplayName("Cancel");
        acceptitemm.setDisplayName("Accept");
        capacityitemm.setDisplayName("Capacity " + store.getStock(player) + "/" + store.capacity);
        add1itemm.setDisplayName("+1");
        add8itemm.setDisplayName("+8");
        add64itemm.setDisplayName("+64");
        remove1itemm.setDisplayName("-1");
        remove8itemm.setDisplayName("-8");
        remove64itemm.setDisplayName("-64");

        fullinvmeta.setDisplayName("Need More Space");

        buyitem.setItemMeta(buyitemm);
        sellitem.setItemMeta(sellitemm);
        stompzitem.setItemMeta(stompzitemm);
        cancelitem.setItemMeta(cancelitemm);
        acceptitem.setItemMeta(acceptitemm);
        capacityitem.setItemMeta(capacityitemm);
        notavailable.setItemMeta(naitemm);

        add1item.setItemMeta(add1itemm);
        add8item.setItemMeta(add8itemm);
        add64item.setItemMeta(add64itemm);
        remove1item.setItemMeta(remove1itemm);
        remove8item.setItemMeta(remove8itemm);
        remove64item.setItemMeta(remove64itemm);

        fullinv.setItemMeta(fullinvmeta);
    }

    public void setInventories(){
        if(buying) {
            storeUI.setItem(0, buyitem);
        }else storeUI.setItem(0,sellitem);
        storeUI.setItem(1,main);
        storeUI.setItem(2,stompzitem);
        storeUI.setItem(6,cancelitem);
        storeUI.setItem(7,capacityitem);
        if(notAvailable()){
            storeUI.setItem(8,notavailable);
        }else if(checkInventory()){
            storeUI.setItem(8,fullinv);
        }else storeUI.setItem(8,acceptitem);

        itemnumber.setItem(0,cancelitem);
        itemnumber.setItem(1,remove64item);
        itemnumber.setItem(2,remove8item);
        itemnumber.setItem(3,remove1item);
        itemnumber.setItem(4,main);
        itemnumber.setItem(5,add1item);
        itemnumber.setItem(6,add8item);
        itemnumber.setItem(7,add64item);
        itemnumber.setItem(8,acceptitem);
    }

    public void setMainItem(){
        main = store.item.clone();
        ItemMeta mainmeta = main.getItemMeta();
        mainmeta.setDisplayName(quantity + " x " + store.name.replace("_"," "));
        main.setItemMeta(mainmeta);
    }

    public void add1(){
        quantity += 1;
        updateAll();
    }

    public void add8(){
        quantity += 8;
        updateAll();
    }

    public void add64(){
        quantity += 64;
        updateAll();
    }

    public void remove1(){
        quantity -= 1;
        updateAll();
    }

    public void remove8(){
        quantity -= 8;
        updateAll();
    }

    public void remove64(){
        quantity -= 64;
        updateAll();
    }

    public boolean notAvailable(){
        if(buying && (store.getStock(player) <= 0)){
            return true;
        }else if(!buying && (store.getStock(player)== store.capacity)){
            return true;
        }else if(buying && (store.buy > Main.econ.getBalance(player))){
            return true;
        }else if(!buying && !player.getInventory().containsAtLeast(store.item, quantity)) {
            return true;
        }else return false;
    }

    public boolean checkInventory(){
        if(buying) {
            Material item = store.item.getType();
            int left = quantity;
            HashMap<Integer, ItemStack> current = new HashMap<>();
            if (player.getInventory().contains(item)) {
                for (int i : player.getInventory().all(item).keySet()) {
                    if (left > 0) {
                        if (player.getInventory().getItem(i).getAmount() < item.getMaxStackSize()) {
                            int fill = item.getMaxStackSize() - player.getInventory().getItem(i).getAmount();
                            if (left > fill) {
                                left -= fill;
                            } else left = 0;
                        }
                    }
                }
            }
            if (left > 0) {
                int empty = 0;
                for (int i = 0; i < player.getInventory().getStorageContents().length; i++) {
                    if (player.getInventory().getItem(i) == null) {
                        empty++;
                    }
                }

                for (int i = 0; i < empty; i++) {
                    if (left > item.getMaxStackSize()) {
                        left -= item.getMaxStackSize();
                    } else left = 0;
                }
            }

            if (left > 0) return true;
        }
        return false;
    }

    public boolean checkMath(int value, Player p){
        int sum = quantity + value;
        if(sum <= 0){
            return false;
        }else if(buying && (sum > store.getStock(player))){
            return false;
        }else if(!buying && (sum + store.getStock(player) > store.capacity)){
            return false;
        }else if(buying && (sum * store.buy) > Main.econ.getBalance(p)){
            return false;
        }else if(!buying && player.getInventory().containsAtLeast(main, sum)){
            return true;
        }else return true;
    }

    public void toggleBS(){
        if(buying){
            buying = false;
            quantity = 1;
            updateAll();
        }else{
            buying = true;
            quantity = 1;
            updateAll();
        }
    }

    public void setStompz(double value){
        ItemMeta stompzmeta = stompzitem.getItemMeta();
        stompzmeta.setDisplayName(value + " Stompz");
        stompzitem.setItemMeta(stompzmeta);
    }

    public void updateItemQuantity(){
        ItemMeta mainmeta = main.getItemMeta();
        mainmeta.setDisplayName(quantity + " x " + store.name.replace("_"," "));
        main.setItemMeta(mainmeta);
    }

    public void updatePrice(){
        buy = store.buy * quantity;
        sell = store.sell * quantity;

        if(buying){
            setStompz(buy);
        }else{
            setStompz(sell);
        }
    }

    public void updateAll(){
        updateItemQuantity();
        updatePrice();
        setInventories();
    }

    public void acceptTransaction(){
        ItemStack paidfor = store.item.clone();
        paidfor.setAmount(quantity);

        if(buying){
            HashMap<Integer, ItemStack> items = player.getInventory().addItem(paidfor);
            if(items != null) {
                for(int i : items.keySet()){
                    if (items.get(i) != null) {
                        player.getWorld().dropItem(player.getLocation(), items.get(i));
                    }
                }
            }

            Main.econ.withdrawPlayer(player, buy);
            player.sendMessage(ChatColor.GREEN + "You have paid " + ChatColor.GOLD + "$"+buy + ChatColor.GREEN + " for " + ChatColor.BLUE + quantity + " x " + store.name.replace("_"," "));
            store.stockChange(player, -quantity);
            store.buystat += quantity;
        }else{
            player.getInventory().removeItem(paidfor);
            Main.econ.depositPlayer(player, sell);
            player.sendMessage(ChatColor.GREEN + "You have been paid " + ChatColor.GOLD + "$"+ sell + ChatColor.GREEN + " for " + ChatColor.BLUE + quantity + " x " + store.name.replace("_"," "));
            store.stockChange(player, quantity);
            store.sellstat += quantity;
        }
    }

}
