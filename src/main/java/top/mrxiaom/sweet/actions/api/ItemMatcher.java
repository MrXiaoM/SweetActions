package top.mrxiaom.sweet.actions.api;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface ItemMatcher {
    boolean isMatch(@NotNull ItemStack item);

    interface Provider {
        @NotNull ItemMatcher load(ConfigurationSection section) throws RuntimeException;
    }
}
