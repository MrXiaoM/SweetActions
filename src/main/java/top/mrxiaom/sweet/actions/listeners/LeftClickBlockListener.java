package top.mrxiaom.sweet.actions.listeners;

import org.bukkit.block.Block;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEvent;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.sweet.actions.SweetActions;
import top.mrxiaom.sweet.actions.func.entry.EnumBlockTriggerType;
import top.mrxiaom.sweet.actions.func.entry.ScriptBlock;

@AutoRegister
public class LeftClickBlockListener extends AbstractBlockListener {
    public LeftClickBlockListener(SweetActions plugin) {
        super(plugin);
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        map.clear();
        reloadMapWithType(EnumBlockTriggerType.LEFT_CLICK);
        reloadMapWithType(EnumBlockTriggerType.SHIFT_LEFT_CLICK);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(PlayerInteractEvent e) {
        if (!e.hasBlock() || e.useInteractedBlock().equals(Event.Result.DENY)) return;
        Block block = e.getClickedBlock();
        if (block == null) return;
        ScriptBlock scriptBlock = getScriptBlock(block.getLocation());
        Player player = e.getPlayer();
        if (scriptBlock != null && scriptBlock.type.check(player)) {
            scriptBlock.handleExecute(plugin, player, block);
        }
    }
}
