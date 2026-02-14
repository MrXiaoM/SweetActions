package top.mrxiaom.sweet.actions.func;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.pluginbase.func.AbstractPluginHolder;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.ConfigUtils;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.actions.SweetActions;
import top.mrxiaom.sweet.actions.func.block.BlockLoc;
import top.mrxiaom.sweet.actions.func.block.EnumBlockTriggerType;
import top.mrxiaom.sweet.actions.func.script.Script;
import top.mrxiaom.sweet.actions.func.block.ScriptBlock;
import top.mrxiaom.sweet.actions.listeners.AbstractBlockListener;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static top.mrxiaom.pluginbase.actions.ActionProviders.loadActions;

@AutoRegister
public class ScriptBlockManager extends AbstractModule {
    private final Map<String, Script> scripts = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final List<ScriptBlock> all = new ArrayList<>();
    private final Map<String, Map<BlockLoc, ScriptBlock>> byWorld = new HashMap<>();
    private final File savesBlocksFolder;
    public ScriptBlockManager(SweetActions plugin) {
        super(plugin);
        this.savesBlocksFolder = plugin.resolve("./saves/blocks");
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        reloadScripts(config);
        reloadScriptBlocks();
    }

    private void reloadScripts(ConfigurationSection cfg) {
        scripts.clear();
        for (String path : cfg.getStringList("scripts-folder")) {
            File folder = plugin.resolve(path);
            if (!folder.exists()) {
                Util.mkdirs(folder);
                if (path.equals("./scripts")) {
                    plugin.saveResource("scripts/example.yml", new File(folder, "example.yml"));
                }
            }
            Util.reloadFolder(folder, false, (id, file) -> {
                if (!file.getName().endsWith(".yml")) return;
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                List<IAction> actions = loadActions(config, "actions");
                scripts.put(id, new Script(id, actions));
            });
        }
        info("[scripts] 加载了 " + scripts.size() + " 个脚本配置");
    }

    private void reloadScriptBlocks() {
        all.clear();
        if (!savesBlocksFolder.exists()) {
            Util.mkdirs(savesBlocksFolder);
        }
        File[] files = savesBlocksFolder.listFiles();
        if (files != null) for (File file : files) { // 世界名.yml
            if (file.isDirectory() || !file.getName().endsWith(".yml")) continue;
            String world = Util.getRelationPath(savesBlocksFolder, file, false);
            if (Bukkit.getWorld(world) == null) {
                warn("找不到世界 " + world);
                continue;
            }
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            List<ConfigurationSection> list = ConfigUtils.getSectionList(config, "blocks");
            for (ConfigurationSection section : list) {
                ScriptBlock scriptBlock = loadScriptBlock(section, world);
                if (scriptBlock != null) {
                    all.add(scriptBlock);
                }
            }
        }
        rebuildWorldMaps();
        info("[saves/blocks] 加载了 " + all.size() + " 个脚本方块，分布在 " + byWorld.size() + " 个世界");
    }

    private void rebuildWorldMaps() {
        byWorld.clear();
        for (ScriptBlock scriptBlock : all) {
            Map<BlockLoc, ScriptBlock> blockMap = getSubMap(byWorld, scriptBlock.world());
            blockMap.put(scriptBlock.loc(), scriptBlock);
        }
    }

    @Nullable
    private ScriptBlock loadScriptBlock(ConfigurationSection section, String world) {
        String typeStr = section.getString("type", "null");
        EnumBlockTriggerType type = Util.valueOr(EnumBlockTriggerType.class, typeStr, null);
        if (type == null) {
            warn("无效的类型 " + typeStr);
            return null;
        }
        BlockLoc loc = new BlockLoc(
                section.getInt("loc.x"),
                section.getInt("loc.y"),
                section.getInt("loc.z"));
        String scriptId = section.getString("script", null);
        Script script = scriptId == null ? null : scripts.get(scriptId);
        if (script == null) {
            warn("找不到脚本 " + scriptId);
            return null;
        }
        int cooldownGlobal = section.getInt("cooldown.global");
        int cooldownPerPlayer = section.getInt("cooldown.per-player");
        return new ScriptBlock(world, loc, type, script, cooldownGlobal, cooldownPerPlayer);
    }

    public void saveScriptBlocks() {
        if (!savesBlocksFolder.exists()) {
            Util.mkdirs(savesBlocksFolder);
        }
        File[] files = savesBlocksFolder.listFiles();
        if (files != null) for (File file : files) {
            if (file.isDirectory() || !file.getName().endsWith(".yml")) continue;
            String id = Util.getRelationPath(savesBlocksFolder, file, false);
            // 如果按世界分类的脚本方块列表是空的，且数据文件存在，则删除文件
            if (!byWorld.containsKey(id) || byWorld.get(id).isEmpty()) {
                if (!file.delete()) {
                    warn("文件 " + file.getName() + " 删除失败");
                }
            }
        }
        // 按世界分类保存脚本方块数据文件
        for (String world : byWorld.keySet()) {
            saveScriptBlocks(world);
        }
    }

    public void saveScriptBlocks(String world) {
        Map<BlockLoc, ScriptBlock> blockMap = byWorld.get(world);
        if (blockMap != null) {
            saveScriptBlocks(world, blockMap);
        }
    }

    public void saveScriptBlocks(String world, Map<BlockLoc, ScriptBlock> blockMap) {
        if (!savesBlocksFolder.exists()) {
            Util.mkdirs(savesBlocksFolder);
        }
        String fileName = world + ".yml";
        File file = new File(savesBlocksFolder, fileName);
        if (blockMap.isEmpty()) {
            if (file.exists() && !file.delete()) {
                warn("文件 " + file.getName() + " 删除失败");
            }
            return;
        }
        YamlConfiguration config = new YamlConfiguration();
        List<ConfigurationSection> list = new ArrayList<>();

        for (ScriptBlock scriptBlock : blockMap.values()) {
            ConfigurationSection section = new MemoryConfiguration();
            section.set("type", scriptBlock.type().name());
            section.set("loc.x", scriptBlock.loc().x());
            section.set("loc.y", scriptBlock.loc().y());
            section.set("loc.z", scriptBlock.loc().z());
            section.set("cooldown.global", scriptBlock.cooldownGlobal());
            section.set("cooldown.per-player", scriptBlock.cooldownPerPlayer());
            section.set("script", scriptBlock.script().id());
            list.add(section);
        }

        config.set("blocks", list);
        try {
            config.save(file);
        } catch (IOException ex) {
            warn("保存配置 " + fileName + " 时出错", ex);
        }
    }

    @Nullable
    public Script getScript(String id) {
        return scripts.get(id);
    }

    @Nullable
    @Contract("null->null")
    public ScriptBlock get(@Nullable Block block) {
        return block == null ? null : get(block.getWorld().getName(), BlockLoc.of(block));
    }

    @Nullable
    public ScriptBlock get(Location loc) {
        World world = loc.getWorld();
        if (world == null) return null;
        return get(world.getName(), BlockLoc.of(loc));
    }

    @Nullable
    public ScriptBlock get(String world, BlockLoc loc) {
        Map<BlockLoc, ScriptBlock> blockMap = byWorld.get(world);
        if (blockMap == null) return null;
        return blockMap.get(loc);
    }

    /**
     * 添加一个脚本方块
     */
    public void put(ScriptBlock scriptBlock) {
        all.add(scriptBlock);
        rebuildWorldMaps();
        reloadListenerMaps();
    }

    /**
     * 移除一个脚本方块
     */
    public void remove(ScriptBlock scriptBlock) {
        all.remove(scriptBlock);
        rebuildWorldMaps();
        reloadListenerMaps();
    }

    @NotNull
    public List<ScriptBlock> all() {
        return Collections.unmodifiableList(all);
    }

    private static void reloadListenerMaps() {
        for (AbstractPluginHolder<?> holder : getAllRegisteredHolders()) {
            if (holder instanceof AbstractBlockListener) {
                ((AbstractBlockListener) holder).reloadMap();
            }
        }
    }

    @NotNull
    public static Map<BlockLoc, ScriptBlock> getSubMap(Map<String, Map<BlockLoc, ScriptBlock>> map, String key) {
        Map<BlockLoc, ScriptBlock> blockMap = map.get(key);
        if (blockMap != null) return blockMap;
        Map<BlockLoc, ScriptBlock> newMap = new HashMap<>();
        map.put(key, newMap);
        return newMap;
    }

    public static ScriptBlockManager inst() {
        return instanceOf(ScriptBlockManager.class);
    }
}
