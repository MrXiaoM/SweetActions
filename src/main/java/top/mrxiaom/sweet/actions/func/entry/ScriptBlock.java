package top.mrxiaom.sweet.actions.func.entry;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import top.mrxiaom.sweet.actions.SweetActions;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScriptBlock {
    public String world;
    public BlockLoc loc;
    public EnumBlockTriggerType type;
    public String scriptId;
    public Script script;
    public int cooldownGlobal;
    public int cooldownPerPlayer;

    private final Map<UUID, Long> cooldownMap = new HashMap<>();
    private long cooldownNext;

    public ScriptBlock(String world, BlockLoc loc, EnumBlockTriggerType type, String scriptId, Script script, int cooldownGlobal, int cooldownPerPlayer) {
        this.world = world;
        this.loc = loc;
        this.type = type;
        this.scriptId = scriptId;
        this.script = script;
        this.cooldownGlobal = cooldownGlobal;
        this.cooldownPerPlayer = cooldownPerPlayer;
    }

    public void handleExecute(SweetActions plugin, Player player, Block block) {
        // 检查冷却时间
        long now = System.currentTimeMillis();
        boolean enableGlobal = cooldownGlobal > 0;
        boolean enablePerPlayer = cooldownPerPlayer > 0;
        if (enableGlobal && now < cooldownNext) return;
        if (enablePerPlayer) {
            long next = cooldownMap.getOrDefault(player.getUniqueId(), now);
            if (now < next) return;
        }
        if (enableGlobal) cooldownNext = now + cooldownGlobal;
        if (enablePerPlayer) cooldownMap.put(player.getUniqueId(), now + cooldownPerPlayer);
        // 正式执行脚本
        script.execute(plugin, player, block);
    }
}
