package top.mrxiaom.sweet.actions.func.item;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.actions.ActionProviders;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.pluginbase.utils.ConfigUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WeightedActionList {
    public static class Weighted {
        private final int weight;
        private final List<IAction> actions;
        public Weighted(int weight, List<IAction> actions) {
            this.weight = weight;
            this.actions = actions;
        }

        public int weight() {
            return weight;
        }

        public List<IAction> actions() {
            return actions;
        }
    }
    public static final WeightedActionList EMPTY = new WeightedActionList(new MemoryConfiguration());
    private final Random random = new Random();
    private final boolean enable;
    private final boolean runAfterFixedActions;
    private final List<Weighted> actionList;
    private final List<Weighted> weightedList;
    public WeightedActionList(ConfigurationSection config) {
        this.enable = config.getBoolean("enable", false);
        this.runAfterFixedActions = config.getBoolean("run-after-fixed-actions", true);
        this.actionList = new ArrayList<>();
        this.weightedList = new ArrayList<>();
        for (ConfigurationSection section : ConfigUtils.getSectionList(config, "action-list")) {
            int weight = section.getInt("weight");
            if (weight < 1) {
                throw new IllegalArgumentException("无效的 weight: " + weight);
            }
            List<IAction> actions = ActionProviders.loadActions(section, "actions");
            Weighted weighted = new Weighted(weight, actions);
            this.actionList.add(weighted);
            for (int i = 0; i < weight; i++) {
                this.weightedList.add(weighted);
            }
        }
    }

    public boolean isEnable() {
        return enable;
    }

    public boolean isRunAfterFixedActions() {
        return runAfterFixedActions;
    }

    @Nullable
    public Weighted random() {
        if (weightedList.isEmpty()) return null;
        return weightedList.get(random.nextInt(weightedList.size()));
    }

    public List<Weighted> actionList() {
        return actionList;
    }
}
