package top.mrxiaom.sweet.actions.func;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.actions.SweetActions;
import top.mrxiaom.sweet.actions.func.entry.BlockLoc;
import top.mrxiaom.sweet.actions.func.entry.EnumBlockTriggerType;
import top.mrxiaom.sweet.actions.func.entry.Script;
import top.mrxiaom.sweet.actions.func.entry.ScriptBlock;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static top.mrxiaom.pluginbase.actions.ActionProviders.loadActions;

@AutoRegister
public class ScriptBlockManager extends AbstractModule {
    private final Map<String, Script> scripts = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final List<ScriptBlock> all = new ArrayList<>();
    private final Map<String, Map<BlockLoc, ScriptBlock>> byWorld = new HashMap<>();
    public ScriptBlockManager(SweetActions plugin) {
        super(plugin);
    }

    @Override
    public void reloadConfig(MemoryConfiguration cfg) {
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
                scripts.put(id, new Script(actions));
            });
        }
        all.clear();
        File savesFolder = plugin.resolve("./saves");
        if (!savesFolder.exists()) {
            Util.mkdirs(savesFolder);
        }
        File[] files = savesFolder.listFiles();
        if (files != null) for (File file : files) { // 世界名.yml
            if (file.isDirectory() || !file.getName().endsWith(".yml")) continue;
            String world = Util.getRelationPath(savesFolder, file, false);
            if (Bukkit.getWorld(world) == null) {
                warn("找不到世界 " + world);
                continue;
            }
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            List<ConfigurationSection> list = Util.getSectionList(config, "blocks");
            for (ConfigurationSection section : list) {
                ScriptBlock scriptBlock = loadScriptBlock(section, world);
                if (scriptBlock != null) {
                    all.add(scriptBlock);
                }
            }
        }
        byWorld.clear();
        for (ScriptBlock scriptBlock : all) {
            Map<BlockLoc, ScriptBlock> blockMap = getSubMap(byWorld, scriptBlock.world);
            blockMap.put(scriptBlock.loc, scriptBlock);
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
        return new ScriptBlock(world, loc, type, scriptId, script, cooldownGlobal, cooldownPerPlayer);
    }

    public void saveScriptBlocks() {
        File savesFolder = plugin.resolve("./saves");
        if (!savesFolder.exists()) {
            Util.mkdirs(savesFolder);
        }
        File[] files = savesFolder.listFiles();
        if (files != null) for (File file : files) {
            if (file.isDirectory() || !file.getName().endsWith(".yml")) continue;
            String id = Util.getRelationPath(savesFolder, file, false);
            if (!byWorld.containsKey(id) || byWorld.get(id).isEmpty()) {
                if (!file.delete()) {
                    warn("文件 " + file.getName() + " 删除失败");
                }
            }
        }
        for (Map.Entry<String, Map<BlockLoc, ScriptBlock>> entry : byWorld.entrySet()) {
            String fileName = entry.getKey() + ".yml";
            File file = new File(savesFolder, fileName);
            YamlConfiguration config = new YamlConfiguration();
            List<ConfigurationSection> list = new ArrayList<>();

            for (ScriptBlock scriptBlock : entry.getValue().values()) {
                ConfigurationSection section = new MemoryConfiguration();
                section.set("type", scriptBlock.type.name());
                section.set("loc.x", scriptBlock.loc.x);
                section.set("loc.y", scriptBlock.loc.y);
                section.set("loc.z", scriptBlock.loc.z);
                section.set("cooldown.global", scriptBlock.cooldownGlobal);
                section.set("cooldown.per-player", scriptBlock.cooldownPerPlayer);
                section.set("script", scriptBlock.scriptId);
                list.add(section);
            }

            config.set("blocks", list);
            try {
                config.save(file);
            } catch (IOException ex) {
                warn("保存配置 " + fileName + " 时出错", ex);
            }
        }
    }

    @Nullable
    public Script get(String id) {
        return scripts.get(id);
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

    @NotNull
    public List<ScriptBlock> all() {
        return Collections.unmodifiableList(all);
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
