package top.mrxiaom.sweet.actions.func.entry;

import org.bukkit.entity.Player;

public enum EnumBlockTriggerType {
    WALK,
    LEFT_CLICK,
    RIGHT_CLICK,
    SHIFT_LEFT_CLICK(true),
    SHIFT_RIGHT_CLICK(true)

    ;
    public final boolean needSneaking;
    EnumBlockTriggerType() {
        this(false);
    }
    EnumBlockTriggerType(boolean needSneaking) {
        this.needSneaking = needSneaking;
    }

    public boolean check(Player player) {
        return needSneaking == player.isSneaking();
    }
}
