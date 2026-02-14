package top.mrxiaom.sweet.actions.func.block;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import top.mrxiaom.sweet.actions.SweetActions;
import top.mrxiaom.sweet.actions.func.script.Script;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScriptBlock {
    private final String world;
    private final BlockLoc loc;
    private EnumBlockTriggerType type;
    private Script script;
    private int cooldownGlobal;
    private int cooldownPerPlayer;

    private final Map<UUID, Long> cooldownMap = new HashMap<>();
    private long cooldownNext;

    public ScriptBlock(
            @NotNull String world,
            @NotNull BlockLoc loc,
            @NotNull EnumBlockTriggerType type,
            @NotNull Script script,
            int cooldownGlobal,
            int cooldownPerPlayer
    ) {
        this.world = world;
        this.loc = loc;
        this.type = type;
        this.script = script;
        this.cooldownGlobal = cooldownGlobal;
        this.cooldownPerPlayer = cooldownPerPlayer;
    }

    @NotNull
    public String world() {
        return world;
    }

    @NotNull
    public BlockLoc loc() {
        return loc;
    }

    @NotNull
    public ScriptBlock withLoc(@NotNull BlockLoc loc) {
        return new ScriptBlock(world, loc, type, script, cooldownGlobal, cooldownPerPlayer);
    }

    @NotNull
    public ScriptBlock withLoc(@NotNull String world, @NotNull BlockLoc loc) {
        return new ScriptBlock(world, loc, type, script, cooldownGlobal, cooldownPerPlayer);
    }

    @NotNull
    public EnumBlockTriggerType type() {
        return type;
    }

    public void type(@NotNull EnumBlockTriggerType type) {
        this.type = type;
    }

    @NotNull
    public Script script() {
        return script;
    }

    public void script(@NotNull Script script) {
        this.script = script;
    }

    public int cooldownGlobal() {
        return cooldownGlobal;
    }

    public void cooldownGlobal(int cooldownGlobal) {
        this.cooldownGlobal = cooldownGlobal;
    }

    public int cooldownPerPlayer() {
        return cooldownPerPlayer;
    }

    public void cooldownPerPlayer(int cooldownPerPlayer) {
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
