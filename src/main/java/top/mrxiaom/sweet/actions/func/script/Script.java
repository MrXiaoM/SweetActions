package top.mrxiaom.sweet.actions.func.script;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.sweet.actions.SweetActions;

import java.util.List;

public class Script {
    private final String id;
    private List<IAction> actions;
    @ApiStatus.Internal
    public Script(String id, List<IAction> actions) {
        this.id = id;
        this.actions = actions;
    }

    public String id() {
        return id;
    }

    public List<IAction> actions() {
        return actions;
    }

    public void actions(List<IAction> actions) {
        this.actions = actions;
    }

    public void execute(SweetActions plugin, Player player, Block block) {
        // 计划到下一tick执行，避免阻塞事件
        plugin.getScheduler().runTask(() -> {
            for (IAction action : actions) {
                action.run(player);
            }
        });
    }
}
