package com.haiman233.haimantech.customs;

import com.haiman233.haimantech.HT;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.handlers.SimpleBlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import java.util.Locale;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.interfaces.InventoryBlock;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * 粘液书皮压缩机（代码定义的特殊机器，不走 content/*.yml）。
 *
 * <p>用途：当粘液书无法再通过魔法工作台合成时的兜底合成机。不耗电、无 tick 逻辑，
 * 纯点击驱动：</p>
 * <ul>
 *   <li>界面 3 行（27 格）：左侧 0-2/9-11/18-20 共 9 个输入槽，
 *       右侧 6-8/15-17/24-26 共 9 个输出槽，中间 13 号为绿色"点击合成"按钮，
 *       其余槽位白色玻璃板装饰（均不可操作）。</li>
 *   <li>合成条件：左侧 9 个槽位全部放入"显示名含 slimefun 或 指南（忽略大小写、去色）"的物品。
 *       不满足时在聊天栏提示"材料不足"。</li>
 *   <li>点击按钮：份数 = 9 个输入槽中最少的物品数，每份扣除各槽 1 个物品、产出 1 个
 *       Slimefun ID 为 {@link #OUTPUT_ID} 的物品（多放多产，比例 9:1）；
 *       输出空间不足时只合成放得下的部分并提示（材料保留）。
 *       该 ID 由外部附属注册，未安装时聊天栏提示。</li>
 *   <li>合成配方：增强型工作台——4 号书架 + 5 号工作台 + 6 号发射器（原版材料）。</li>
 * </ul>
 */
public class HTSlimeBookCompressor extends SlimefunItem implements InventoryBlock {

    /** 产物 Slimefun ID（外部附属注册，运行时按 ID 查找）。 */
    public static final String OUTPUT_ID = "YASUO_SFBOOK";

    /** 本机 Slimefun ID。 */
    public static final String MACHINE_ID = "HAIMAN_SFBOOK_COMPRESSOR";

    private static final int[] INPUT_SLOTS = {0, 1, 2, 9, 10, 11, 18, 19, 20};
    private static final int[] OUTPUT_SLOTS = {6, 7, 8, 15, 16, 17, 24, 25, 26};
    private static final int BUTTON_SLOT = 13;
    private static final int[] BORDER_SLOTS = {3, 4, 5, 12, 14, 21, 22, 23};

    private static final ItemStack WHITE_PANE =
            new CustomItemStack(Material.WHITE_STAINED_GLASS_PANE, " ");
    private static final ItemStack BUTTON = new CustomItemStack(Material.LIME_STAINED_GLASS_PANE,
            "&a点击合成",
            "&7在左侧槽位放入slimefun指南书");

    public HTSlimeBookCompressor(ItemGroup group, SlimefunItemStack item) {
        super(group, item, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                null, null, null,
                new ItemStack(Material.DISPENSER), new ItemStack(Material.BOOKSHELF), new ItemStack(Material.CRAFTING_TABLE),
                null, null, null
        });

        addItemHandler(new SimpleBlockBreakHandler() {
            @Override
            public void onBlockBreak(Block b) {
                BlockMenu inv = StorageCacheUtils.getMenu(b.getLocation());
                if (inv != null) {
                    inv.dropItems(b.getLocation(), getInputSlots());
                    inv.dropItems(b.getLocation(), getOutputSlots());
                }
            }
        });
        createPreset(this, getItemName(), this::constructMenu);
    }

    /** 注册入口（Setup 在机器类之后调用）：放入 slime_customizer_slimefun_machine 物品组。 */
    public static void registerMachine() {
        ItemGroup group = HT.group("slime_customizer_slimefun_machine");
        if (group == null) {
            HT.warn("粘液书皮压缩机注册失败：物品组 slime_customizer_slimefun_machine 不存在");
            return;
        }
        SlimefunItemStack item = new SlimefunItemStack(MACHINE_ID,
                Material.BOOKSHELF,
                "&a粘液书皮压缩机",
                "&7当粘液书无法再魔法工作台合成时，使用该机器");
        new HTSlimeBookCompressor(group, item).register(HT.plugin);
    }

    @Override
    public int[] getInputSlots() {
        return INPUT_SLOTS;
    }

    @Override
    public int[] getOutputSlots() {
        return OUTPUT_SLOTS;
    }

    private void constructMenu(BlockMenuPreset preset) {
        // 3 行 27 格：BlockMenuPreset 尺寸由"触碰过的最高槽位"决定，必须显式设定
        preset.setSize(27);
        for (int i : BORDER_SLOTS) {
            preset.addItem(i, WHITE_PANE.clone(), ChestMenuUtils.getEmptyClickHandler());
        }
        preset.addItem(BUTTON_SLOT, BUTTON.clone());
        // 按钮点击：preset 级处理器拿不到 BlockMenu 实例，
        // 通过玩家当前打开界面的 InventoryHolder（BlockMenu）解析
        preset.addMenuClickHandler(BUTTON_SLOT, (player, slot, item, action) -> {
            BlockMenu menu = openMenu(player);
            if (menu != null) {
                craft(menu, player);
            }
            return false;
        });
    }

    /** 取玩家当前打开的机器界面；非本类界面（理论不可达）返回 null。 */
    private static BlockMenu openMenu(Player player) {
        Inventory top = player.getOpenInventory().getTopInventory();
        return top.getHolder() instanceof BlockMenu menu ? menu : null;
    }

    /** 执行合成：校验材料 → 按份数计算产物 → 逐份扣料并推入产物。 */
    private void craft(BlockMenu inv, Player player) {
        ItemStack out = outputItem();
        if (out == null) {
            player.sendMessage(ChatColor.RED + "合成失败：物品 " + OUTPUT_ID + " 未注册（请确认对应附属已安装）");
            return;
        }
        // 条件：左侧 9 个槽位全部放入名称含 slimefun / 指南 的物品
        for (int slot : INPUT_SLOTS) {
            ItemStack it = inv.getItemInSlot(slot);
            if (it == null || it.getType().isAir() || !nameMatches(it)) {
                player.sendMessage(ChatColor.RED + "材料不足");
                return;
            }
        }
        // 份数 = 9 个输入槽中最少的数量（每份 = 每槽 1 个 → 产物 1 个）
        int sets = Integer.MAX_VALUE;
        for (int slot : INPUT_SLOTS) {
            sets = Math.min(sets, inv.getItemInSlot(slot).getAmount());
        }
        // 输出空间：最多还能放多少个产物
        int capacity = outputCapacity(inv, out);
        if (capacity <= 0) {
            player.sendMessage(ChatColor.RED + "输出槽空间不足，请先清空产物");
            return;
        }
        int craftCount = Math.min(sets, capacity);
        // 每槽扣除 1 份 × craftCount
        for (int slot : INPUT_SLOTS) {
            inv.consumeItem(slot, craftCount);
        }
        for (int i = 0; i < craftCount; i++) {
            inv.pushItem(out.clone(), OUTPUT_SLOTS);
        }
        if (craftCount < sets) {
            player.sendMessage(ChatColor.YELLOW + "输出槽空间不足，本次仅合成 " + craftCount + " 份（材料已保留）");
        }
    }

    /** 产物堆（按 Slimefun ID 查找，运行时解析以兼容任意加载顺序）；未注册返回 null。 */
    private static ItemStack outputItem() {
        SlimefunItem sf = SlimefunItem.getById(OUTPUT_ID);
        return sf != null ? sf.getItem() : null;
    }

    /** 显示名（去色、忽略大小写）是否含 slimefun 或 指南；无自定义显示名的原版物品不满足。 */
    private static boolean nameMatches(ItemStack it) {
        if (!it.hasItemMeta()) return false;
        String display = it.getItemMeta().getDisplayName();
        if (display == null || display.isEmpty()) return false;
        String plain = ChatColor.stripColor(display).toLowerCase(Locale.ROOT);
        return plain.contains("slimefun") || plain.contains("指南");
    }

    /** 输出槽还能放多少个产物（空槽按满叠计，同类未满叠按剩余空间计）。 */
    private static int outputCapacity(BlockMenu inv, ItemStack out) {
        int capacity = 0;
        for (int slot : OUTPUT_SLOTS) {
            ItemStack cur = inv.getItemInSlot(slot);
            if (cur == null || cur.getType().isAir()) {
                capacity += out.getMaxStackSize();
            } else if (cur.isSimilar(out)) {
                capacity += Math.max(0, cur.getMaxStackSize() - cur.getAmount());
            }
        }
        return capacity;
    }
}
