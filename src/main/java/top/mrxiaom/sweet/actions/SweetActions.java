package top.mrxiaom.sweet.actions;

import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import org.bukkit.configuration.file.YamlConfiguration;
import top.mrxiaom.pluginbase.BukkitPlugin;
import top.mrxiaom.pluginbase.resolver.DefaultLibraryResolver;
import top.mrxiaom.pluginbase.utils.ClassLoaderWrapper;
import top.mrxiaom.pluginbase.utils.ConfigUtils;
import top.mrxiaom.pluginbase.utils.scheduler.FoliaLibScheduler;

import java.io.File;
import java.net.URL;
import java.util.List;

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
        this.scheduler = new FoliaLibScheduler(this);

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

    @Override
    protected void beforeLoad() {
        MinecraftVersion.replaceLogger(getLogger());
        MinecraftVersion.disableUpdateCheck();
        MinecraftVersion.disableBStats();
        MinecraftVersion.getVersion();
    }

    @Override
    protected void afterEnable() {
        getLogger().info("SweetActions 加载完毕");
    }
}
