package top.mrxiaom.sweet.actions.func.entry;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.sweet.actions.SweetActions;

import java.util.List;

public class Script {
    public List<IAction> actions;

    public Script(List<IAction> actions) {
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
