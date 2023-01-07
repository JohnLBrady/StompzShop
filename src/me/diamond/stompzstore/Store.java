package me.diamond.stompzstore;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.UUID;

public class Store {

    public String name = "";
    public double buy = 0;
    public double sell = 0;
    public int capacity = 0;
    public Block market = null;

    public ItemStack item;

    public int buystat = 0;
    public int sellstat = 0;

    public HashMap<UUID, Integer> reciepts = new HashMap<>();


    public Store(String n,double b,double s,int c, ItemStack i){
        name = n;
        buy = b;
        sell = s;
        capacity = c;
        item = i;
    }

    public void resetValues(double b, double s, int c){
        buy = b;
        sell = s;
        capacity = c;
    }

    public void setBlock(Block b){
        market = b;
    }

    public void removeBlock(){
        market = null;
    }

    public String getName(){
        return name;
    }

    public void clearstats(){
        buystat = 0;
        sellstat = 0;
    }

    public int getStock(Player p){
        if(reciepts.containsKey(p.getUniqueId())){
            return reciepts.get(p.getUniqueId());
        }
        return capacity/2;
    }

    public void stockChange(Player p, int amount){
        if(reciepts.containsKey(p.getUniqueId())){
            reciepts.put(p.getUniqueId(), reciepts.get(p.getUniqueId()) + amount);
        }
        else reciepts.put(p.getUniqueId(), capacity/2 + amount);
    }

}
