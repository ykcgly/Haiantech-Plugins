package com.haiman233.haimantech;

import com.haiman233.haimantech.load.Setup;
import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 海曼科技院 HaimanTech2 —— 独立 Slimefun 附属插件主类。
 *
 * <p>由原 RSC（RykenSlimeCustomizer）配置版改写而来：内容仍来自同一组 YAML（已打包进 jar
 * 的 content/，运行期从自身资源读取），但加载逻辑全部以原生 Java 实现，不再依赖 RSC
 * 运行时及其脚本引擎。</p>
 *
 * <p>依赖策略与原 info.yml 的 pluginDepends 一致，均为软依赖：缺失仅提示，不阻断加载。
 * 另按 RSC 版 README 的「软前置」清单补全 Gastronomicon / SlimyTreeTaps，
 * 与 plugin.yml 的 softdepend 保持同步（后者决定加载顺序，此处决定启动日志提示）。
 * （这些插件仅作为配方可引用来源存在；若想恢复「缺失即卸载」的强约束，
 * 把它移入 {@link #HARD_DEPS} 即可。）</p>
 */
public final class HaimanTechPlugin extends JavaPlugin implements SlimefunAddon {

    private static HaimanTechPlugin instance;

    /** 软依赖：{插件名, 中文名}，缺失仅提示后继续加载。 */
    private static final String[][] SOFT_DEPS = {
            {"FoxyMachines", "FoxyMachines"},
            {"InfinityExpansion", "无尽贪婪"},
            {"LiteXpansion", "LiteXpansion"},
            {"TranscEndence", "TranscEndence"},
            {"GuizhanLibPlugin", "鬼斩库"},
            {"Gastronomicon", "美食家"},
            {"SlimyTreeTaps", "粘液木龙头"},
    };

    /** 硬依赖；当前为必装 Slimefun。 */
    private static final String[][] HARD_DEPS = {{"Slimefun", "粘液科技"}};

    @Override
    public void onEnable() {
        instance = this;
        HT.plugin = this;
        saveDefaultConfig();
        getLogger().info("=========================================================");
        getLogger().info("                 海曼科技院 开始加载（独立版）");
        getLogger().info("                   作者： haiman");
        getLogger().info("                  当前版本：1.10.0");
        getLogger().info("=========================================================");

        if (!checkDependencies()) return;
        try {
            Setup.loadAll();
            getLogger().info("海曼科技院 加载成功");
        } catch (Throwable e) {
            getLogger().severe("海曼科技院 加载过程中出现异常: " + e);
            e.printStackTrace();
        }
    }

    private boolean checkDependencies() {
        for (String[] dep : HARD_DEPS) {
            if (Bukkit.getPluginManager().getPlugin(dep[0]) == null) {
                getLogger().severe("缺少 " + label(dep) + "，海曼科技院已自动卸载！");
                Bukkit.getPluginManager().disablePlugin(this);
                return false;
            }
        }
        for (String[] dep : SOFT_DEPS) {
            if (Bukkit.getPluginManager().getPlugin(dep[0]) == null) {
                getLogger().warning("缺少 " + label(dep) + "，部分配方可能无法加载");
            }
        }
        return true;
    }

    private static String label(String[] dep) {
        return dep[1].equalsIgnoreCase(dep[0]) ? dep[0] : dep[0] + "（" + dep[1] + "）";
    }

    @Override
    public void onDisable() {
        // 移除矿物生成器（与 RSC 卸载行为一致）
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            world.getPopulators().removeIf(x -> x instanceof com.haiman233.haimantech.customs.HTOrePopulator);
        }
        getLogger().info("海曼科技院 已卸载");
    }

    public static HaimanTechPlugin getInstance() {
        return instance;
    }

    @Override
    public JavaPlugin getJavaPlugin() {
        return this;
    }

    @Override
    public String getBugTrackerURL() {
        return "https://github.com/haiman233/HaimanTech2/issues";
    }
}
