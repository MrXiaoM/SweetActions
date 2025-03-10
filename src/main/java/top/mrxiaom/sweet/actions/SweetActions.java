package top.mrxiaom.sweet.actions;
        
import org.jetbrains.annotations.NotNull;
import top.mrxiaom.pluginbase.BukkitPlugin;
import top.mrxiaom.pluginbase.EconomyHolder;

public class SweetActions extends BukkitPlugin {
    public static SweetActions getInstance() {
        return (SweetActions) BukkitPlugin.getInstance();
    }

    public SweetActions() {
        super(options()
                .bungee(false)
                .adventure(true)
                .database(false)
                .reconnectDatabaseWhenReloadConfig(false)
                .vaultEconomy(false)
                .scanIgnore("top.mrxiaom.sweet.actions.libs")
        );
    }


    @Override
    protected void afterEnable() {
        getLogger().info("SweetActions 加载完毕");
    }
}
