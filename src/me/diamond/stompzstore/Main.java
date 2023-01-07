package me.diamond.stompzstore;

import net.milkbowl.vault.economy.Economy;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class Main extends JavaPlugin {

    public static Economy econ = null;

    public static HashMap<Player,Store> placingstores = new HashMap<>();
    public static List<Store> storelist = new ArrayList<>();
    public static HashMap<Player,Transaction> customers = new HashMap<>();

    public long timer = 0L;
    public long resetHours = TimeUnit.HOURS.toMillis(12L);
    public long lastRunTime = -1L;

    public List<String> warnings = new ArrayList<>();

    @Override
    public void onEnable(){

        createConfig();

        if(getConfig().getConfigurationSection("storelist").getKeys(false) != null || getConfig().getConfigurationSection("storelist").getKeys(false).size()>0) {
            getLogger().log(Level.INFO,"Store List Size: " + getConfig().getConfigurationSection("storelist").getKeys(false).size());
            for (String s : getConfig().getConfigurationSection("storelist").getKeys(false)) {
                createConfigStore(s);
            }
        }

        if (!setupEconomy()) {
            getServer().getLogger().log(Level.SEVERE, String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        new StoreListener(this);

        timer = getConfig().getLong("timeleft", 300000);
        resetHours = TimeUnit.HOURS.toMillis(getConfig().getLong("resetHours", 12L));

        Timer4Reset();

    }

    @Override
    public void onDisable(){
        getConfig().set("timeleft", timer);
        saveStores();
        saveConfig();
    }


    public void Timer4Reset(){

        this.getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (this.lastRunTime == -1L) {
                this.lastRunTime = System.currentTimeMillis();
            }
            timer -= 5000L;
            //sendMessage("the time is now " + timer); Debugging Stuff :D
            if (timer <= 0L) {
                sendMessage(ChatColor.GOLD + "The admin stores have been refreshed.");
                for(Store s : storelist) {
                    resetStock(s);
                }
                timer = resetHours;
            }
        }, 600L, 100L);
    }

    public void sendMessage(String mess){
        this.getServer().getOnlinePlayers().forEach((player) -> {
            player.sendMessage(mess);
        });
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if(cmd.getName().equalsIgnoreCase("stompzstore")){
            Player p = (Player)sender;
            if(!p.hasPermission("stompzstore.all")){
                p.sendMessage(ChatColor.RED + "You don't have permissions to command this plugin.");
                return true;
            }
            if(args.length == 0){
                help(p);
                return true;
            }
            if(args[0].equalsIgnoreCase("set")){
                if(args.length == 5){
                    if(isStore(args[1])){
                        editStore(getStore(args[1]),args[2],args[3],args[4]);
                        p.sendMessage(ChatColor.BLUE + "The " + args[1] + " store has been updated.");
                        saveStores();
                    }else{
                        createStore(args[1],args[2],args[3],args[4], p.getInventory().getItemInMainHand());
                        p.sendMessage(ChatColor.BLUE + "The " + args[1] + " store has been created.");
                        saveStores();
                    }
                }else{
                    p.sendMessage(ChatColor.RED + "Please do full command: /stompzstore set (storename) (buy) (sell) (capacity)");
                }
            }else if(args[0].equalsIgnoreCase("remove")){
                if(args.length == 2){
                    if(isStore(args[1])){
                        storelist.remove(getStore(args[1]));
                        deleteStore(args[1]);
                        saveConfig();
                        p.sendMessage(ChatColor.BLUE + args[1] + " store has been removed");
                        saveStores();
                    }else{
                        p.sendMessage(ChatColor.RED + "There is no store for that item.");
                    }
                }else{
                    p.sendMessage(ChatColor.RED + "Please use /stompzstore remove (storename)");
                }
            }else if(args[0].equalsIgnoreCase("placestore")){
                if(args.length == 2){
                    if(isStore(args[1])) {
                        placingstores.put(p,getStore(args[1]));
                        p.sendMessage(ChatColor.BLUE + "Now click a block to link that store to a block.");
                        saveStores();
                    }else{
                        p.sendMessage(ChatColor.RED + "That is not a store yet. Make it with \"/stompzstore set\" command");
                    }
                }else{
                    p.sendMessage(ChatColor.RED + "Please use /stompzstore placestore (storename)");
                }
            }else if(args[0].equalsIgnoreCase("removestore")){
                if(args.length == 2){
                    if(isStore(args[1])) {
                        getStore(args[1]).removeBlock();
                        p.sendMessage(ChatColor.BLUE + "Store has been removed from the block it was linked to.");
                    }else{
                        p.sendMessage(ChatColor.RED + "You cannot remove a store that doesn't exist.");
                    }
                }else{
                    p.sendMessage(ChatColor.RED + "Please use /stompzstore removestore (storename)");
                }
            }else if(args[0].equalsIgnoreCase("list")){
                p.sendMessage(ChatColor.DARK_GREEN + "List of Stores:");
                String dalist = "[";
                for(Store s : storelist){
                    if(storelist.get(storelist.size()-1).equals(s)){
                        dalist = dalist + s.getName() + "]";
                    }else{
                        dalist = dalist + s.getName() + ", ";
                    }
                }
                if(storelist.size()==0){
                    dalist = dalist + "No Stores Available]";
                }
                p.sendMessage(ChatColor.GREEN + dalist);
            }else if(args[0].equalsIgnoreCase("info")){
                if(args.length == 2){
                    if(isStore(args[1])){
                        giveInfo(p, getStore(args[1]));
                    }else{
                        p.sendMessage(ChatColor.RED + "That is not a store.");
                    }
                }else {
                    p.sendMessage(ChatColor.RED + "Please use /stompzstore info (storename)");
                }
            }else if(args[0].equalsIgnoreCase("clearstats")){
                for(Store s : storelist){
                    s.clearstats();
                    saveStats(s.name, s.buystat, s.sellstat);
                    saveStores();
                    saveConfig();
                }
                p.sendMessage(ChatColor.BLUE + "Stores' stats have been cleared.");
            }else if(args[0].equalsIgnoreCase("savestores")) {
                saveStores();
                saveConfig();
                p.sendMessage(ChatColor.BLUE + "Stores config was saved.");
            }else if(args[0].equalsIgnoreCase("seewarns")) {
                if(warnings.size() > 0) {
                    for (String s : warnings) {
                        p.sendMessage(ChatColor.RED + "[*]" + s);
                    }
                }else{
                    p.sendMessage(ChatColor.BLUE + "There are no warnings from StompzStore atm.");
                }
            }else if(args[0].equalsIgnoreCase("fullstats")){
                giveFullStats(p);
            }else if(args[0].equalsIgnoreCase("overview")){
                giveOverview(p);
            }else if(args[0].equalsIgnoreCase("settimer")){
                if(args.length == 2){
                    int temp = 12;
                    try{
                        temp = Integer.parseInt(args[1]);
                    }catch(Exception e){
                        p.sendMessage(ChatColor.RED + "That is not an integer.");
                        return true;
                    }
                    if(Integer.parseInt(args[1]) > 0){
                        resetHours = TimeUnit.HOURS.toMillis((long)Integer.parseInt(args[1]));
                        getConfig().set("resetHours", temp);
                        saveConfig();
                        p.sendMessage(ChatColor.GREEN + "The reset timer has been set to " + ChatColor.BLUE + args[1] + ChatColor.GREEN + " hours.");
                    }else{
                        p.sendMessage(ChatColor.RED + "Can't set the restock hours to less than 1.");
                    }
                }else{
                    p.sendMessage(ChatColor.RED + "Arguments have not the right number of them.");
                }
            }else{
                p.sendMessage(ChatColor.RED + "Please use /stompzstore for a list of possible commands.");
            }
            return true;
        }
        return false;
    }

    public void help(Player p){
        p.sendMessage(ChatColor.DARK_GREEN + "StompzStore Help: (/stompzstore [args])");
        p.sendMessage(ChatColor.GREEN + "set (name) (buy) (sell) (capacity)" + ChatColor.BLUE + " - " + ChatColor.DARK_GRAY + "Sets store info");
        p.sendMessage(ChatColor.GREEN + "remove (storename)" + ChatColor.BLUE + " - " + ChatColor.DARK_GRAY +"Removes store's existance");
        p.sendMessage(ChatColor.GREEN + "placestore (storename)" + ChatColor.BLUE + " - " + ChatColor.DARK_GRAY + "Be able to link store to a block");
        p.sendMessage(ChatColor.GREEN + "removestore (storename)" + ChatColor.BLUE + " - " + ChatColor.DARK_GRAY + "Un-links store block");
        p.sendMessage(ChatColor.GREEN + "list" + ChatColor.BLUE + " - " + ChatColor.DARK_GRAY + "Gives a list of the stores available");
        p.sendMessage(ChatColor.GREEN + "info (storename)" + ChatColor.BLUE + " - " + ChatColor.DARK_GRAY + "Gives info about the store.");
        p.sendMessage(ChatColor.GREEN + "clearstats" + ChatColor.BLUE + " - " + ChatColor.DARK_GRAY + "Clears the stats of every Store.");
        p.sendMessage(ChatColor.GREEN + "savestores" + ChatColor.BLUE + " - " + ChatColor.DARK_GRAY + "Saves stores in StompzStore Config.");
        p.sendMessage(ChatColor.GREEN + "seewarns" + ChatColor.BLUE + " - " + ChatColor.DARK_GRAY + "Gives the list of current warnings.");
        p.sendMessage(ChatColor.GREEN + "fullstats" + ChatColor.BLUE + " - " + ChatColor.DARK_GRAY + "Gives info for all of the stores.");
        p.sendMessage(ChatColor.GREEN + "overview" + ChatColor.BLUE + " - " + ChatColor.DARK_GRAY + "Gives an overview of all stores.");
        p.sendMessage(ChatColor.GREEN + "settimer (hours)" + ChatColor.BLUE + " - " + ChatColor.DARK_GRAY + "Sets how often the stores refresh.");
    }

    public void giveInfo(Player p, Store s){
        p.sendMessage(ChatColor.DARK_GREEN + s.name + " Store:");
        p.sendMessage(ChatColor.GREEN + "* Buy Price: " + s.buy);
        p.sendMessage(ChatColor.GREEN + "* Sell Price: " + s.sell);
        p.sendMessage(ChatColor.GREEN + "* Capactiy: " + s.capacity);
        p.sendMessage(ChatColor.GREEN + "* Buy Stat: " + s.buystat);
        p.sendMessage(ChatColor.GREEN + "* Sell Stat: " + s.sellstat);
        if(s.market == null){
            p.sendMessage(ChatColor.GREEN + "* Block is Linked: false");
        }else{
            p.sendMessage(ChatColor.GREEN + "* Block is Linked: true");
        }
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public boolean isStore(String name){
        for(Store s : storelist){
            if(s.getName().equalsIgnoreCase(name)){
                return true;
            }
        }
        return false;
    }

    public Store getStore(String name){
        for(Store s : storelist){
            if(s.getName().equalsIgnoreCase(name)){
                return s;
            }
        }
        return null;
    }

    public void createStore(String item, String b, String s, String c, ItemStack i){
        Store newstore = new Store(item, Double.parseDouble(b), Double.parseDouble(s), Integer.parseInt(c), i);
        storelist.add(newstore);
    }

    public void createConfigStore(String store){

        double b = getConfig().getDouble("storelist." + store + ".buy");
        double s = getConfig().getDouble("storelist." + store + ".sell");
        int c = getConfig().getInt("storelist." + store + ".capacity");
        ItemStack i = getConfig().getItemStack("storelist." + store + ".item");
        Block bl = null;
        if(!StringUtils.isBlank(getConfig().getString("storelist."+ store + ".world"))) {
            if(getServer().getWorld(getConfig().getString("storelist." + store + ".world")) != null) {
                bl = getServer().getWorld(getConfig().getString("storelist." + store + ".world")).getBlockAt(getConfig().getInt("storelist." + store + ".block.x"), getConfig().getInt("storelist." + store + ".block.y"), getConfig().getInt("storelist." + store + ".block.z"));
            }
        }

        int bs = getConfig().getInt("storelist." + store + ".buystat");

        int ss = getConfig().getInt("storelist." + store + ".sellstat");


        Store newstore = new Store(store, b, s, c, i);
        newstore.market = bl;
        newstore.buystat = bs;
        newstore.sellstat = ss;

        ConfigurationSection configRec = getConfig().getConfigurationSection("storelist." + store + ".receipts");
        HashMap<UUID, Integer> reciepts = new HashMap<>();
        if(configRec != null) {
            if (configRec.getKeys(false) != null || configRec.getKeys(false).size() > 0) {
                for (String rec : configRec.getKeys(false)) {
                    reciepts.put(UUID.fromString(rec), configRec.getInt(rec));
                }
            }
        }
        newstore.reciepts = reciepts;

        storelist.add(newstore);
    }

    public void saveStores(){
        for(Store s : storelist){
            getConfig().set("storelist." + s.name, null);

            getConfig().set("storelist."+s.name+".buy",s.buy);
            getConfig().set("storelist."+s.name+".sell",s.sell);
            getConfig().set("storelist."+s.name+".capacity",s.capacity);
            getConfig().set("storelist."+s.name+".item",s.item);

            getConfig().set("storelist."+s.name+".buystat",s.buystat);
            getConfig().set("storelist."+s.name+".sellstat",s.sellstat);
            if(s.market != null) {
                getConfig().set("storelist." + s.name + ".world", s.market.getWorld().getName());
                getConfig().set("storelist." + s.name + ".block.x", s.market.getX());
                getConfig().set("storelist." + s.name + ".block.y", s.market.getY());
                getConfig().set("storelist." + s.name + ".block.z", s.market.getZ());
            }
            if(!s.reciepts.isEmpty()){
                for(UUID id : s.reciepts.keySet()) {
                    getConfig().set("storelist." + s.name + ".receipts." + id.toString(), s.reciepts.get(id));
                }
            }
        }
    }

    public void deleteStore(String s){
        getConfig().set("storelist."+s,null);
    }

    public void saveStats(String store, int b, int s){
        getConfig().set("storelist."+store+".buystat", b);
        getConfig().set("storelist."+store+".sellstat", s);
        saveConfig();
    }

    public void resetStock(Store store){
        store.reciepts.clear();
    }

    public void editStore(Store store, String b, String s, String c){
        store.resetValues(Double.parseDouble(b), Double.parseDouble(s), Integer.parseInt(c));
    }

    public boolean isStoreBlock(Block b){
        for(Store s : storelist){
            if(s.market != null) {
                if (s.market.equals(b)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Store getbStore(Block b){
        for(Store s : storelist){
            if(s.market != null) {
                if (s.market.equals(b)) {
                    return s;
                }
            }
        }
        return null;
    }

    public boolean isPlacingStore(Player player){
        for(Player p : placingstores.keySet()){
            if(player.equals(p)){
                return true;
            }
        }
        return false;
    }

    private void createConfig() {
        try {
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }
            File file = new File(getDataFolder(), "config.yml");
            if (!file.exists()) {
                getLogger().info("Config.yml not found, creating!");
                saveDefaultConfig();
            } else {
                getLogger().info("Config.yml found, loading!");
            }
        } catch (Exception e) {
            e.printStackTrace();

        }

    }

    public void checkForWarnings(){

        for(Store s : storelist){
            if(s.market == null){
                warnings.add("The " + s.getName() + " store is not linked to a block.");
            }
            if(s.item == null) {
                warnings.add("The " + s.getName() + " store was not set up right and is missing it's item.");
            }
        }
    }

    public void giveFullStats(Player p){
        String stats = "";
        for(Store s : storelist){
            String add = "[" + s.getName() + " | B: " + s.buystat + " | S: " + s.sellstat + "] ";
            stats = stats + add;
        }
        p.sendMessage(ChatColor.GREEN + "StompzStore Statistics: (bought/sold)");
        p.sendMessage(ChatColor.BLUE + stats);
    }

    public void giveOverview(Player p){
        p.sendMessage(ChatColor.GOLD + "STOMPZSTORE OVERVIEW:");
        p.sendMessage(ChatColor.GREEN + "Restock Timer: " + ChatColor.BLUE + getConfig().getInt("resetHours") +
        ChatColor.DARK_GREEN + " || " + ChatColor.GREEN + "Time Left: " + ChatColor.BLUE + TimeUnit.MILLISECONDS.toMinutes(timer)/60 + " hours " + TimeUnit.MILLISECONDS.toMinutes(timer)%60 + " minutes");
        p.sendMessage(ChatColor.GREEN + "Top Selling: " + ChatColor.BLUE + topS() + ChatColor.DARK_GREEN + " || " + ChatColor.GREEN + "Top Buying: " + ChatColor.BLUE + topB());
    }

    public String topB(){
        int b = 0;
        List<String> topstores = new ArrayList<>();
        String send = "";

        if(storelist.size()==0){
            return "None";
        }
        for(Store s : storelist){
            if(s.buystat > b){
                b = s.buystat;
            }
        }
        for(Store s : storelist){
            if(s.buystat == b){
                topstores.add(s.getName());
            }
        }
        if(topstores.size()>1){
            send = send + "[";
            for(String st : topstores){
                if(topstores.get(topstores.size()-1).equals(st)){
                    send = send + st + "]";
                }else send = send + st + ", ";
            }
            return send;
        }else{
            return topstores.get(0);
        }
    }

    public String topS(){
        int b = 0;
        List<String> topstores = new ArrayList<>();
        String send = "";

        if(storelist.size()==0){
            return "None";
        }
        for(Store s : storelist){
            if(s.sellstat > b){
                b = s.sellstat;
            }
        }
        for(Store s : storelist){
            if(s.sellstat == b){
                topstores.add(s.getName());
            }
        }
        if(topstores.size()>1){
            send = send + "[";
            for(String st : topstores){
                if(topstores.get(topstores.size()-1).equals(st)){
                    send = send + st + "]";
                }else send = send + st + ", ";
            }
            return send;
        }else{
            return topstores.get(0);
        }
    }

}
