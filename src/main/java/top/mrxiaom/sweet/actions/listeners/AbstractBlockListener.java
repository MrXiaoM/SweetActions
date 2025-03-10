package top.mrxiaom.sweet.actions.listeners;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.sweet.actions.SweetActions;
import top.mrxiaom.sweet.actions.func.AbstractModule;
import top.mrxiaom.sweet.actions.func.ScriptBlockManager;
import top.mrxiaom.sweet.actions.func.entry.BlockLoc;
import top.mrxiaom.sweet.actions.func.entry.EnumBlockTriggerType;
import top.mrxiaom.sweet.actions.func.entry.ScriptBlock;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractBlockListener extends AbstractModule implements Listener {
    protected final Map<String, Map<BlockLoc, ScriptBlock>> map = new HashMap<>();
    public AbstractBlockListener(SweetActions plugin) {
        super(plugin);
        registerEvents();
    }
    @Override
    public int priority() {
        return 1001;
    }
    protected void reloadMapWithType(EnumBlockTriggerType type) {
        ScriptBlockManager manager = ScriptBlockManager.inst();
        for (ScriptBlock scriptBlock : manager.all()) {
            if (scriptBlock.type.equals(type)) {
                Map<BlockLoc, ScriptBlock> blockMap = getSubMap(scriptBlock.world);
                blockMap.put(scriptBlock.loc, scriptBlock);
            }
        }
    }
    @Nullable
    public ScriptBlock getScriptBlock(Location loc) {
        Map<BlockLoc, ScriptBlock> blockMap = getBlockMap(loc);
        if (blockMap == null) return null;
        return blockMap.get(BlockLoc.of(loc));
    }

    protected Map<BlockLoc, ScriptBlock> getSubMap(String key) {
        return ScriptBlockManager.getSubMap(map, key);
    }
    protected Map<BlockLoc, ScriptBlock> getBlockMap(Location loc) {
        World world = loc.getWorld();
        if (world == null) return null;
        return map.get(world.getName());
    }
}
