package top.mrxiaom.sweet.actions.func.item;

import com.ezylang.evalex.EvaluationException;
import com.ezylang.evalex.Expression;
import com.ezylang.evalex.parser.ParseException;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.actions.ActionProviders;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.pluginbase.utils.ListPair;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.pluginbase.utils.depend.PAPI;
import top.mrxiaom.sweet.actions.SweetActions;
import top.mrxiaom.sweet.actions.api.ItemMatcher;

import java.util.*;

public class ClickItem {
    private final SweetActions plugin;
    private final String id;
    private final ItemMatcher itemMatcher;
    // action-matcher
    private final EnumClickType clickType;
    private final boolean sneakEnable;
    private final boolean sneakToggle;
    private final List<IAction> sneakDenyActions;

    private final @Nullable String permissionNode;
    private final List<IAction> permissionDenyActions;

    private final double cooldownGlobalSeconds, cooldownPlayerSeconds;
    private final List<IAction> cooldownDenyActions;

    private final @Nullable String requirementEval;
    private final List<IAction> requirementDenyActions;

    private final int costItemAmount;
    private final List<IAction> costItemDenyActions;

    // action-executor
    private final List<IAction> executeActions;
    private final WeightedActionList executeWeightedActions;

    private long cooldownGlobalEndTime = 0L;
    private Map<UUID, Long> cooldownPlayerEndTimeMap = new HashMap<>();

    public ClickItem(@NotNull SweetActions plugin, @NotNull String id, @NotNull ConfigurationSection config) {
        this.plugin = plugin;
        this.id = id;
        ConfigurationSection itemMatcherSection = config.getConfigurationSection("item-matcher");
        if (itemMatcherSection == null) {
            throw new IllegalArgumentException("未设置 item-matcher");
        }
        this.itemMatcher = plugin.parseItemMatcher(itemMatcherSection);

        EnumClickType clickType = Util.valueOr(EnumClickType.class, config.getString("action-matcher.click"), null);
        if (clickType == null) {
            throw new IllegalArgumentException("action-matcher.click 的值无效");
        }
        this.clickType = clickType;

        this.sneakEnable = config.getBoolean("action-matcher.need-sneaking.enable", false);
        this.sneakToggle = config.getBoolean("action-matcher.need-sneaking.toggle", true);
        this.sneakDenyActions = ActionProviders.loadActions(config, "action-matcher.need-sneaking.deny-actions");

        String permissionNode = config.getString("action-matcher.permission.node", "none");
        if (permissionNode.equals("none") || permissionNode.isEmpty()) {
            this.permissionNode = null;
        } else {
            this.permissionNode = permissionNode;
        }
        this.permissionDenyActions = ActionProviders.loadActions(config, "action-matcher.permission.deny-actions");

        this.cooldownGlobalSeconds = config.getDouble("action-matcher.cooldown.global-seconds", 0.0);
        this.cooldownPlayerSeconds = config.getDouble("action-matcher.cooldown.player-seconds", 0.0);
        this.cooldownDenyActions = ActionProviders.loadActions(config, "action-matcher.cooldown.deny-actions");

        String permissionEval = config.getString("action-matcher.requirement.eval", "").trim();
        if (permissionEval.isEmpty()) {
            this.requirementEval = null;
        } else {
            this.requirementEval = permissionEval;
        }
        this.requirementDenyActions = ActionProviders.loadActions(config, "action-matcher.requirement.deny-actions");

        this.costItemAmount = Math.max(0, config.getInt("action-matcher.cost-item.amount"));
        this.costItemDenyActions = ActionProviders.loadActions(config, "action-matcher.cost-item.deny-actions");

        this.executeActions = ActionProviders.loadActions(config, "action-executor.actions");
        ConfigurationSection weightActionsSection = config.getConfigurationSection("action-executor.weighted-actions");
        if (weightActionsSection == null) {
            this.executeWeightedActions = WeightedActionList.EMPTY;
        } else {
            this.executeWeightedActions = new WeightedActionList(itemMatcherSection);
        }
    }

    public String id() {
        return id;
    }

    public double getCooldownGlobalSeconds() {
        return cooldownGlobalSeconds;
    }

    public double getCooldownPlayerSeconds() {
        return cooldownPlayerSeconds;
    }

    public List<IAction> getCooldownDenyActions() {
        return cooldownDenyActions;
    }

    public void runCooldownDenyActions(Player player, double seconds) {
        ListPair<String, Object> r = new ListPair<>();
        r.add("%cooldown%", String.format("%.1f", seconds));
        ActionProviders.run(plugin, player, getCooldownDenyActions(), r);
    }

    public int getCostItemAmount() {
        return costItemAmount;
    }

    public List<IAction> getCostItemDenyActions() {
        return costItemDenyActions;
    }

    public void runCostItemDenyActions(Player player) {
        ActionProviders.run(plugin, player, getCostItemDenyActions());
    }

    public boolean isItemMatch(@NotNull ItemStack item) {
        return itemMatcher.isMatch(item);
    }

    public boolean isClickTypeMatch(@NotNull EnumClickType clickType) {
        return this.clickType.equals(clickType);
    }

    public List<IAction> getSneakDenyActions() {
        return sneakDenyActions;
    }

    public void runSneakDenyActions(Player player) {
        ActionProviders.run(plugin, player, getSneakDenyActions());
    }

    public List<IAction> getPermissionDenyActions() {
        return permissionDenyActions;
    }

    public void runPermissionDenyActions(Player player) {
        ActionProviders.run(plugin, player, getPermissionDenyActions());
    }

    public List<IAction> getRequirementDenyActions() {
        return requirementDenyActions;
    }

    public void runRequirementDenyActions(Player player) {
        ActionProviders.run(plugin, player, getRequirementDenyActions());
    }

    /**
     * 检查玩家的潜行状态匹配情况，如果不匹配就执行拒绝命令
     * @param player 玩家
     * @return 是否不匹配
     */
    public boolean checkSneakingNotMatch(Player player) {
        if (!sneakEnable) return false;
        if (player.isSneaking() == sneakToggle) {
            return false;
        } else {
            runSneakDenyActions(player);
            return true;
        }
    }

    /**
     * 检查玩家的权限匹配情况，如果不匹配就执行拒绝命令
     * @param player 玩家
     * @return 是否不匹配
     */
    public boolean checkPermissionNotMatch(Player player) {
        if (permissionNode == null) return false;
        if (player.hasPermission(permissionNode)) {
            return false;
        } else {
            runPermissionDenyActions(player);
            return true;
        }
    }

    public boolean checkCooldownNotMatch(Player player) {
        long now = System.currentTimeMillis();
        double globalSeconds = getCooldownGlobalSeconds();
        double playerSeconds = getCooldownPlayerSeconds();
        if (globalSeconds > 0) {
            long endTime = cooldownGlobalEndTime;
            if (now < endTime) {
                runCooldownDenyActions(player, (endTime - now) / 1000.0);
                return true;
            }
        }
        if (playerSeconds > 0) {
            long endTime = cooldownPlayerEndTimeMap.getOrDefault(player.getUniqueId(), 0L);
            if (now < endTime) {
                runCooldownDenyActions(player, (endTime - now) / 1000.0);
                return true;
            }
        }
        return false;
    }

    public void setCooldown(Player player) {
        long now = System.currentTimeMillis();
        double globalSeconds = getCooldownGlobalSeconds();
        double playerSeconds = getCooldownPlayerSeconds();
        if (globalSeconds > 0) {
            cooldownGlobalEndTime = now + (long)(globalSeconds * 1000L);
        }
        if (playerSeconds > 0) {
            cooldownPlayerEndTimeMap.put(player.getUniqueId(), now + (long)(playerSeconds * 1000L));
        }
    }

    public boolean checkRequirementNotMatch(Player player) {
        if (requirementEval == null) return false;
        String str = PAPI.setPlaceholders(player, requirementEval);
        boolean result = false;
        try {
            result = new Expression(str).evaluate().getBooleanValue() == Boolean.TRUE;
        } catch (EvaluationException | ParseException e) {
            plugin.warn("为玩家 " + player.getName() + " 解析 " + id + " 的表达式 " + str + " 时出现异常", e);
        }
        if (result) {
            return false;
        } else {
            runRequirementDenyActions(player);
            return true;
        }
    }

    public void executeActions(Player player) {
        if (executeWeightedActions.isEnable()) {
            WeightedActionList.Weighted weighted = executeWeightedActions.random();
            if (executeWeightedActions.isRunAfterFixedActions()) {
                List<IAction> actions = new ArrayList<>(executeActions);
                if (weighted != null) {
                    actions.addAll(weighted.actions());
                }
                ActionProviders.run(plugin, player, actions);
            } else {
                ActionProviders.run(plugin, player, executeActions);
                if (weighted != null) {
                    ActionProviders.run(plugin, player, weighted.actions());
                }
            }
        } else {
            ActionProviders.run(plugin, player, executeActions);
        }
    }
}
