package com.haiman233.haimantech.customs;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.accelerators.AbstractGrowthAccelerator;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.accelerators.CropGrowthAccelerator;
import io.github.thebusybiscuit.slimefun4.implementation.items.misc.OrganicFertilizer;
import io.github.thebusybiscuit.slimefun4.implementation.items.misc.OrganicFood;
import io.github.thebusybiscuit.slimefun4.utils.compatibility.VersionedParticle;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

/**
 * 可配置参数的生长加速器（simple_machines.yml 的三种 accelerator 类型）。
 *
 * <p>作物加速器直接继承官方 {@code CropGrowthAccelerator}：官方把它做成抽象类，
 * 提供 {@code getEnergyConsumption() / getRadius() / getSpeed()} 供注入参数。</p>
 *
 * <p>动物/树木则不能这样复用。反编译 {@code Slimefun-f172239-Beta.jar} 可见，官方
 * {@code AnimalGrowthAccelerator} / {@code TreeGrowthAccelerator} 把半径与耗电写成
 * {@code private static final} 并内联进 {@code tick}（动物 3.0 格 / 14 J，树木 9 格 / 24 J），
 * 仅 {@code getCapacity()} 可覆写。若直接继承官方类，yml 中「终极」机器的
 * {@code radius: 16} / {@code consumption: 1000} 会静默失效。故这两个类型仍继承官方抽象的
 * {@link AbstractGrowthAccelerator}，自行实现 tick（对齐官方语义）以保留可配参数：
 * {@code capacity} 电容容量、{@code consumption} 每次加速耗电、{@code radius} 作用半径。</p>
 *
 * <p><b>耗材判定（关键，曾导致终极机器整机失效）</b>：{@code SlimefunItems.ORGANIC_FOOD} 与
 * {@code SlimefunItems.FERTILIZER} 是展示用的<b>占位物品</b>（ID 为 ORGANIC_FOOD / FERTILIZER，
 * lore 写「内含 ???」），玩家实际使用的是 11 种 {@code ORGANIC_FOOD_*} / {@code FERTILIZER_*}
 * 变体，分别注册为 {@link OrganicFood} / {@link OrganicFertilizer} 的实例。此前用
 * {@code isItemSimilar(slot, 占位物品)} 判定，永远匹配不上，tick 每刻空转。现与本体一致，改用
 * {@code SlimefunItem.getByItem(item) instanceof OrganicFood / OrganicFertilizer} 按类匹配全部变体
 * （{@code getByItem} 对 null / AIR 返回 null，安全）。</p>
 *
 * <p><b>26.x 兼容</b>：菜单统一走本体的 {@link StorageCacheUtils#getMenu(org.bukkit.Location)}
 * （数据未加载时会主动 requestLoad），取代旧的 {@code BlockStorage.getInventory(Block)}。
 * 树木加速器只走骨粉路径：1.21.x 与 26.x 均 &gt;= 1.17，旧版 {@code Sapling#setStage} 分支本就是
 * 死代码；且若保留 {@code isAtLeast(MINECRAFT_1_17)}，当 Slimefun 未识别 26.x 版本号而返回
 * {@code UNKNOWN} 时该判断恒为 false，会误走旧分支导致「只推进 stage、不真正催熟」。</p>
 */
public final class HTGrowthAccelerators {

    private HTGrowthAccelerators() {}

    // ---------------------------------------------------------------- 动物

    /** 动物生长加速器：消耗任意有机食物变体，催熟周围的幼年生物。 */
    public static class Animal extends AbstractGrowthAccelerator {

        private final int capacity;
        private final int consumption;
        private final double radius;

        public Animal(ItemGroup group, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe,
                      int capacity, int radius, int consumption) {
            super(group, item, recipeType, recipe);
            this.capacity = Math.max(1, capacity);
            this.consumption = Math.max(1, consumption);
            this.radius = Math.max(1, radius);
        }

        @Override
        public int getCapacity() {
            return capacity;
        }

        @Override
        protected void tick(Block b) {
            BlockMenu inv = StorageCacheUtils.getMenu(b.getLocation());
            if (inv == null) return;

            for (Entity n : b.getWorld().getNearbyEntities(b.getLocation(), radius, radius, radius, this::isReadyToGrow)) {
                for (int slot : getInputSlots()) {
                    if (!isOrganicFood(inv.getItemInSlot(slot))) continue;
                    if (getCharge(b.getLocation()) < consumption) return;

                    Ageable ageable = (Ageable) n;
                    removeCharge(b.getLocation(), consumption);
                    inv.consumeItem(slot);
                    ageable.setAge(ageable.getAge() + 2000);
                    if (ageable.getAge() > 0) ageable.setAge(0);

                    n.getWorld().spawnParticle(VersionedParticle.HAPPY_VILLAGER,
                            ((LivingEntity) n).getEyeLocation(), 8, 0.2F, 0.2F, 0.2F);
                    return;
                }
            }
        }

        /** 与本体 {@code AnimalGrowthAccelerator#isOrganicFood} 一致：按物品类匹配全部 11 种变体。 */
        protected boolean isOrganicFood(ItemStack item) {
            return SlimefunItem.getByItem(item) instanceof OrganicFood;
        }

        private boolean isReadyToGrow(Entity n) {
            return n instanceof Ageable ageable && n.isValid() && !ageable.isAdult();
        }
    }

    // ---------------------------------------------------------------- 树木

    /** 树木生长加速器：消耗任意有机肥料变体，催熟周围的树苗（模拟骨粉）。 */
    public static class Tree extends AbstractGrowthAccelerator {

        private final int capacity;
        private final int consumption;
        private final int radius;

        public Tree(ItemGroup group, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe,
                    int capacity, int radius, int consumption) {
            super(group, item, recipeType, recipe);
            this.capacity = Math.max(1, capacity);
            this.consumption = Math.max(1, consumption);
            this.radius = Math.max(1, radius);
        }

        @Override
        public int getCapacity() {
            return capacity;
        }

        @Override
        protected void tick(Block b) {
            BlockMenu inv = StorageCacheUtils.getMenu(b.getLocation());
            if (inv == null) return;
            if (getCharge(b.getLocation()) < consumption) return;

            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = b.getRelative(x, 0, z);
                    if (!Tag.SAPLINGS.isTagged(block.getType())) continue;
                    if (applyBoneMeal(b, inv, block)) return;
                }
            }
        }

        private boolean applyBoneMeal(Block machine, BlockMenu inv, Block sapling) {
            for (int slot : getInputSlots()) {
                if (!isFertilizer(inv.getItemInSlot(slot))) continue;
                removeCharge(machine.getLocation(), consumption);
                sapling.applyBoneMeal(BlockFace.UP);
                inv.consumeItem(slot);
                sapling.getWorld().spawnParticle(VersionedParticle.HAPPY_VILLAGER,
                        sapling.getLocation().add(0.5D, 0.5D, 0.5D), 4, 0.1F, 0.1F, 0.1F);
                return true;
            }
            return false;
        }

        /** 与本体 {@code TreeGrowthAccelerator#isFertilizer} 一致：按物品类匹配全部 11 种变体。 */
        protected boolean isFertilizer(ItemStack item) {
            return SlimefunItem.getByItem(item) instanceof OrganicFertilizer;
        }
    }

    // ---------------------------------------------------------------- 作物

    /** 作物生长加速器：复用官方逻辑（官方类为抽象），仅注入可配置参数。 */
    public static class Crop extends CropGrowthAccelerator {

        private final int capacity;
        private final int consumption;
        private final int radius;
        private final int speed;

        public Crop(ItemGroup group, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe,
                    int capacity, int radius, int consumption, int speed) {
            super(group, item, recipeType, recipe);
            this.capacity = Math.max(1, capacity);
            this.consumption = Math.max(1, consumption);
            this.radius = Math.max(1, radius);
            this.speed = Math.max(1, speed);
        }

        @Override
        public int getCapacity() {
            return capacity;
        }

        @Override
        public int getEnergyConsumption() {
            return consumption;
        }

        @Override
        public int getRadius() {
            return radius;
        }

        @Override
        public int getSpeed() {
            return speed;
        }
    }
}
