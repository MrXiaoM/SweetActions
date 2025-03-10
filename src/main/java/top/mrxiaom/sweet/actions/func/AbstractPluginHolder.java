package top.mrxiaom.sweet.actions.func;
        
import top.mrxiaom.sweet.actions.SweetActions;

@SuppressWarnings({"unused"})
public abstract class AbstractPluginHolder extends top.mrxiaom.pluginbase.func.AbstractPluginHolder<SweetActions> {
    public AbstractPluginHolder(SweetActions plugin) {
        super(plugin);
    }

    public AbstractPluginHolder(SweetActions plugin, boolean register) {
        super(plugin, register);
    }
}
