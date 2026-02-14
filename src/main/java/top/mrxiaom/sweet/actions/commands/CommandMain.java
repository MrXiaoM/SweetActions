package top.mrxiaom.sweet.actions.commands;
        
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.actions.SweetActions;
import top.mrxiaom.sweet.actions.func.AbstractModule;
import top.mrxiaom.sweet.actions.func.ScriptBlockManager;
import top.mrxiaom.sweet.actions.func.block.BlockLoc;
import top.mrxiaom.sweet.actions.func.block.EnumBlockTriggerType;
import top.mrxiaom.sweet.actions.func.block.ScriptBlock;
import top.mrxiaom.sweet.actions.func.script.Script;

import java.util.*;

@AutoRegister
public class CommandMain extends AbstractModule implements CommandExecutor, TabCompleter, Listener {
    private final Map<UUID, ScriptBlock> tempCreate = new HashMap<>();
    private final Set<UUID> tempRemove = new HashSet<>();
    public CommandMain(SweetActions plugin) {
        super(plugin);
        registerCommand("sweetactions", this);
        registerEvents();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        tempCreate.remove(e.getPlayer().getUniqueId());
    }
    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        tempCreate.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.useInteractedBlock() == Event.Result.DENY) return;
        Block block = e.getClickedBlock();
        if (block == null) return;
        if (e.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
            Player player = e.getPlayer();
            ScriptBlockManager manager = ScriptBlockManager.inst();
            UUID uuid = player.getUniqueId();
            ScriptBlock create = tempCreate.remove(uuid);
            if (create != null) {
                e.setCancelled(true);
                if (manager.get(block) != null) {
                    // TODO: 支持一个方块添加多个脚本
                    t(player, "&e这里已经有一个脚本方块了");
                    return;
                }
                ScriptBlock scriptBlock = create.withLoc(block.getWorld().getName(), BlockLoc.of(block));
                manager.put(scriptBlock);
                manager.saveScriptBlocks(block.getWorld().getName());
                t(player, "&a已添加脚本方块");
                return;
            }
            if (tempRemove.remove(uuid)) {
                ScriptBlock scriptBlock = manager.get(block);
                if (scriptBlock == null) {
                    t(player, "&e该位置不存在脚本方块");
                    return;
                }
                manager.remove(scriptBlock);
                manager.saveScriptBlocks(block.getWorld().getName());
                t(player, "&a已移除脚本方块");
                return;
            }
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length >= 1 && "block".equalsIgnoreCase(args[0]) && sender.hasPermission("sweet.actions.block")) {
            if (args.length > 3 && "create".equalsIgnoreCase(args[1])) {
                if (!(sender instanceof Player)) {
                    return t(sender, "&e该命令只能由玩家执行");
                }
                Player player = (Player) sender;
                UUID uuid = player.getUniqueId();
                if (tempRemove.contains(uuid) || tempCreate.containsKey(uuid)) {
                    return t(player, "&e你有未处理的请求，在完成请求之前，无法创建脚本方块");
                }
                EnumBlockTriggerType type = Util.valueOr(EnumBlockTriggerType.class, args[2], null);
                if (type == null) {
                    return t(player, "&e指定的脚本方块触发类型无效");
                }
                ScriptBlockManager manager = ScriptBlockManager.inst();
                Script script = manager.getScript(args[3]);
                if (script == null) {
                    return t(player, "&e找不到指定脚本 " + args[3]);
                }
                ScriptBlock scriptBlock = new ScriptBlock("", new BlockLoc(0, 0, 0), type, script, 0, 1);
                tempCreate.put(uuid, scriptBlock);
                return t(player, "&a请左键点击一个方块以创建");
            }
            if (args.length > 1 && "remove".equalsIgnoreCase(args[1])) {
                if (!(sender instanceof Player)) {
                    return t(sender, "&e该命令只能由玩家执行");
                }
                Player player = (Player) sender;
                UUID uuid = player.getUniqueId();
                if (tempRemove.contains(uuid) || tempCreate.containsKey(uuid)) {
                    return t(player, "&e你有未处理的请求，在完成请求之前，无法移除脚本方块");
                }
                tempRemove.add(uuid);
                return t(player, "&a请左键点击一个方块以移除");
            }
            if (args.length > 1 && "edit".equalsIgnoreCase(args[1])) {
                if (!(sender instanceof Player)) {
                    return t(sender, "&e该命令只能由玩家执行");
                }
                Player player = (Player) sender;
                ScriptBlockManager manager = ScriptBlockManager.inst();
                RayTraceResult result = player.rayTraceBlocks(4.5);
                Block block = result == null ? null : result.getHitBlock();
                ScriptBlock scriptBlock = manager.get(block);
                if (scriptBlock == null) {
                    return t(sender, "&e这个位置不存在脚本方块");
                }
                // TODO: 编辑脚本方块配置
                // type, script, cooldownGlobal, cooldownPerPlayer
                manager.saveScriptBlocks(block.getWorld().getName());
                return true;
            }
        }
        if (args.length == 1 && "reload".equalsIgnoreCase(args[0]) && sender.isOp()) {
            plugin.reloadConfig();
            return t(sender, "&a配置文件已重载");
        }
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            if (sender instanceof Player) {
                if (sender.hasPermission("sweet.actions.block")) {
                    list.add("block");
                }
            }
            if (sender.isOp()) {
                list.add("reload");
            }
            return startsWith(list, args[0]);
        }
        if (args.length == 2) {
            if (sender instanceof Player) {
                if ("block".equalsIgnoreCase(args[0]) && sender.hasPermission("sweet.actions.block")) {
                    List<String> list = new ArrayList<>();
                    list.add("create");
                    list.add("edit");
                    list.add("remove");
                    return startsWith(list, args[1]);
                }
            }
        }
        if (args.length == 3) {
            if (sender instanceof Player) {
                if ("block".equalsIgnoreCase(args[0]) && sender.hasPermission("sweet.actions.block")) {
                    if ("edit".equalsIgnoreCase(args[1])) {
                        Player player = (Player) sender;
                        RayTraceResult result = player.rayTraceBlocks(4.5);
                        Block block = result == null ? null : result.getHitBlock();
                        ScriptBlock scriptBlock = ScriptBlockManager.inst().get(block);
                        if (scriptBlock != null) {
                            List<String> list = new ArrayList<>();
                            // TODO: 为编辑命令添加命令补全
                            return startsWith(list, args[2]);
                        }
                    }
                }
            }
        }
        return Collections.emptyList();
    }

    public List<String> startsWith(Collection<String> list, String s) {
        String s1 = s.toLowerCase();
        List<String> stringList = new ArrayList<>(list);
        stringList.removeIf(it -> !it.toLowerCase().startsWith(s1));
        return stringList;
    }
}
