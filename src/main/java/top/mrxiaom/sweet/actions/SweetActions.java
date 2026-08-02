package top.mrxiaom.sweet.actions;

import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import top.mrxiaom.pluginbase.BukkitPlugin;
import top.mrxiaom.pluginbase.paper.PaperFactory;
import top.mrxiaom.pluginbase.resolver.DefaultLibraryResolver;
import top.mrxiaom.pluginbase.utils.ClassLoaderWrapper;
import top.mrxiaom.pluginbase.utils.ConfigUtils;
import top.mrxiaom.pluginbase.utils.inventory.InventoryFactory;
import top.mrxiaom.pluginbase.utils.item.ItemEditor;
import top.mrxiaom.sweet.actions.api.ItemMatcher;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SweetActions extends BukkitPlugin {
    public static SweetActions getInstance() {
        return (SweetActions) BukkitPlugin.getInstance();
    }

    public SweetActions() throws Exception {
        super(options()
                .bungee(false)
                .adventure(true)
                .database(false)
                .reconnectDatabaseWhenReloadConfig(false)
                .scanIgnore("top.mrxiaom.sweet.actions.libs")
        );

        try {
            //noinspection ResultOfMethodCallIgnored
            getDescription().getLibraries();
        } catch (LinkageError ignored) {
            info("正在检查依赖库状态");
            File librariesDir = ClassLoaderWrapper.isSupportLibraryLoader
                    ? new File("libraries")
                    : new File(this.getDataFolder(), "libraries");
            DefaultLibraryResolver resolver = new DefaultLibraryResolver(getLogger(), librariesDir);

            YamlConfiguration overrideLibraries = ConfigUtils.load(resolve("./.override-libraries.yml"));
            for (String key : overrideLibraries.getKeys(false)) {
                resolver.getStartsReplacer().put(key, overrideLibraries.getString(key));
            }
            resolver.addResolvedLibrary(BuildConstants.RESOLVED_LIBRARIES);

            List<URL> libraries = resolver.doResolve();
            info("正在添加 " + libraries.size() + " 个依赖库到类加载器");
            for (URL library : libraries) {
                this.classLoader.addURL(library);
            }
        }
    }

    @Override
    public @NotNull ItemEditor initItemEditor() {
        return PaperFactory.createItemEditor();
    }

    @Override
    public @NotNull InventoryFactory initInventoryFactory() {
        return PaperFactory.createInventoryFactory();
    }

    @Override
    protected void beforeLoad() {
        MinecraftVersion.replaceLogger(getLogger());
        MinecraftVersion.disableUpdateCheck();
        MinecraftVersion.disableBStats();
        MinecraftVersion.getVersion();
    }

    private final Map<String, ItemMatcher.Provider> itemMatcherRegistry = new HashMap<>();

    public void registerItemMatcher(@NotNull String type, @NotNull ItemMatcher.Provider provider) {
        itemMatcherRegistry.put(type, provider);
    }

    public void unregisterItemMatcher(@NotNull String type) {
        itemMatcherRegistry.remove(type);
    }

    @NotNull
    public ItemMatcher parseItemMatcher(@NotNull ConfigurationSection section) throws RuntimeException {
        String type = section.getString("type");
        if (type == null) {
            throw new IllegalArgumentException("未输入参数 type");
        }
        ItemMatcher.Provider provider = itemMatcherRegistry.get(type);
        if (provider != null) {
            return provider.load(section);
        }
        throw new IllegalArgumentException("找不到物品匹配器类型 " + type);
    }

    @Override
    protected void afterEnable() {
        getLogger().info("SweetActions 加载完毕");
    }
}
