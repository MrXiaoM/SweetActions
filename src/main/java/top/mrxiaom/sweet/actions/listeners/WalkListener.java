package top.mrxiaom.sweet.actions.listeners;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.sweet.actions.SweetActions;
import top.mrxiaom.sweet.actions.func.entry.BlockLoc;
import top.mrxiaom.sweet.actions.func.entry.EnumBlockTriggerType;
import top.mrxiaom.sweet.actions.func.entry.ScriptBlock;

import java.util.Map;

@AutoRegister
public class WalkListener extends AbstractBlockListener {
    public WalkListener(SweetActions plugin) {
        super(plugin);
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        map.clear();
        reloadMapWithType(EnumBlockTriggerType.WALK);
    }

    private boolean isNotMoved(Location loc1, Location loc2) {
        if (loc1 == null || loc2 == null) return true;
        World world1 = loc1.getWorld();
        World world2 = loc2.getWorld();
        if (world1 == null || world2 == null || world1.getName().equals(world2.getName())) {
            return true;
        }
        return loc1.getBlockY() == loc2.getBlockY()
                && loc1.getBlockX() == loc2.getBlockX()
                && loc1.getBlockZ() == loc2.getBlockZ();
    }

    @Nullable
    @Override
    public ScriptBlock getScriptBlock(Location loc) {
        Map<BlockLoc, ScriptBlock> blockMap = getBlockMap(loc);
        if (blockMap == null) return null;
        return blockMap.get(BlockLoc.of(loc, -1));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent e) {
        if (e.isCancelled()) return;
        Location from = e.getFrom();
        Location to = e.getTo();
        if (isNotMoved(from, to)) return;
        ScriptBlock scriptBlock = getScriptBlock(to);
        if (scriptBlock != null) {
            scriptBlock.handleExecute(plugin, e.getPlayer(), to.getBlock());
        }
    }
}
