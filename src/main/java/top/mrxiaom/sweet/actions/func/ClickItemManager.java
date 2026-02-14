package top.mrxiaom.sweet.actions.func;

import org.bukkit.Material;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.actions.SweetActions;
import top.mrxiaom.sweet.actions.func.item.ClickItem;
import top.mrxiaom.sweet.actions.func.item.EnumClickType;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@AutoRegister
public class ClickItemManager extends AbstractModule implements Listener {
    private final Map<String, ClickItem> clickItemMap = new HashMap<>();
    public ClickItemManager(SweetActions plugin) {
        super(plugin);
        registerEvents(this);
    }

    @Override
    public void reloadConfig(MemoryConfiguration cfg) {
        clickItemMap.clear();
        for (String path : cfg.getStringList("click-items-folder")) {
            File folder = plugin.resolve(path);
            if (!folder.exists()) {
                Util.mkdirs(folder);
                if (path.equals("./click-items")) {
                    plugin.saveResource("click-items/example.yml", new File(folder, "example.yml"));
                }
            }
            Util.reloadFolder(folder, false, (rawId, file) -> {
                if (!file.getName().endsWith(".yml")) return;
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                String id = rawId.replace("\\", "/");
                try {
                    ClickItem clickItem = new ClickItem(plugin, id, config);
                    clickItemMap.put(clickItem.id(), clickItem);
                } catch (Throwable t) {
                    warn("[click-items] 加载配置 " + id + " 时出现错误:" + t.getMessage());
                }
            });
        }
        info("[click-items] 加载了 " + clickItemMap.size() + " 个物品点击配置");
    }

    private EnumClickType parseType(Action action) {
        switch (action) {
            case LEFT_CLICK_AIR:
            case LEFT_CLICK_BLOCK:
                return EnumClickType.LEFT;
            case RIGHT_CLICK_AIR:
            case RIGHT_CLICK_BLOCK:
                return EnumClickType.RIGHT;
        }
        return null;
    }

    private ClickItem match(Player player, EnumClickType clickType, ItemStack item) {
        if (item == null || item.getAmount() < 1 || item.getType().equals(Material.AIR)) return null;
        if (clickType == null) return null;
        for (ClickItem clickItem : clickItemMap.values()) {
            if (clickItem.isClickTypeMatch(clickType) && clickItem.isItemMatch(item)) {
                if (clickItem.checkSneakingNotMatch(player)) continue;
                if (clickItem.checkPermissionNotMatch(player)) continue;
                if (clickItem.checkCooldownNotMatch(player)) continue;
                if (clickItem.checkRequirementNotMatch(player)) continue;
                return clickItem;
            }
        }
        return null;
    }

    private boolean isOffHand(PlayerInteractEvent e) {
        try {
            return EquipmentSlot.OFF_HAND.equals(e.getHand());
        } catch (LinkageError ignored) {
            return false;
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getItem() == null) return;
        Player player = e.getPlayer();
        if (isOffHand(e)) {
            onInteractOffHand(e, player);
        } else {
            onInteractMainHand(e, player);
        }
    }

    private void onInteractMainHand(PlayerInteractEvent e, Player player) {
        // noinspection deprecation
        ItemStack item = player.getItemInHand();
        ClickItem clickItem = match(player, parseType(e.getAction()), item);
        if (clickItem == null) return;

        int amount = item.getAmount();
        int finalAmount = amount - clickItem.getCostItemAmount();
        if (finalAmount < 0) {
            clickItem.runCostItemDenyActions(player);
        }

        e.setCancelled(true);
        clickItem.setCooldown(player);
        if (finalAmount == 0) {
            item.setAmount(0);
            item.setType(Material.AIR);
            // noinspection deprecation
            player.setItemInHand(null);
        } else {
            item.setAmount(finalAmount);
            // noinspection deprecation
            player.setItemInHand(item);
        }

        plugin.getScheduler().runTask(() -> clickItem.executeActions(player));
    }

    private void onInteractOffHand(PlayerInteractEvent e, Player player) {
        PlayerInventory inv = player.getInventory();
        ItemStack item = inv.getItemInOffHand();
        ClickItem clickItem = match(player, parseType(e.getAction()), item);
        if (clickItem == null) return;

        int amount = item.getAmount();
        int finalAmount = amount - clickItem.getCostItemAmount();
        if (finalAmount < 0) {
            clickItem.runCostItemDenyActions(player);
        }

        e.setCancelled(true);
        clickItem.setCooldown(player);
        if (finalAmount == 0) {
            item.setAmount(0);
            item.setType(Material.AIR);
            inv.setItemInOffHand(null);
        } else {
            item.setAmount(finalAmount);
            inv.setItemInOffHand(item);
        }

        plugin.getScheduler().runTask(() -> clickItem.executeActions(player));
    }
}
