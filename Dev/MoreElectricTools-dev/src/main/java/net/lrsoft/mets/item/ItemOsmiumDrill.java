package net.lrsoft.mets.item;

import java.util.ArrayList;
import java.util.List;

import ic2.api.item.ElectricItem;
import ic2.core.IC2;
import ic2.core.audio.PositionSpec;
import ic2.core.item.tool.HarvestLevel;
import ic2.core.item.tool.ItemDrill;
import ic2.core.ref.ItemName;
import ic2.core.util.StackUtil;
import net.lrsoft.mets.MoreElectricTools;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemOsmiumDrill extends ItemDrill {

    // === Параметры Осмиевого бура ===
    public static int maxCharg = 1500000;
    public static byte tir = 3;
    public static short perUse = 800;
    public static float eff = 16.0F;

    private static final String NBT_MODE = "osmiumDrillMode";

    // === Режимы работы ===
    public enum DrillMode {
        SINGLE   ("Обычный 1x1",       1, 1, 1),
        SILK     ("Шелковое касание",  1, 1, 1),
        TUNNEL_2 ("Туннель 1x2",       1, 2, 1),
        AREA_3   ("Копание 3x3",       3, 3, 1),
        CUBE_3   ("Копание 3x3x3",     3, 3, 3),
        AREA_5   ("Копание 5x5",       5, 5, 1),
        CUBE_5   ("Копание 5x5x5",     5, 5, 5);

        public final String displayName;
        public final int width;
        public final int height;
        public final int depth;

        DrillMode(String displayName, int width, int height, int depth) {
            this.displayName = displayName;
            this.width = width;
            this.height = height;
            this.depth = depth;
        }

        public DrillMode next() {
            return values()[(ordinal() + 1) % values().length];
        }

        public boolean isAreaMode() {
            return width > 1 || height > 1 || depth > 1;
        }
    }

    public ItemOsmiumDrill() {
        super((ItemName) null, perUse, HarvestLevel.Diamond, maxCharg, 160, tir, eff);
        setRegistryName(MoreElectricTools.MODID, "osmium_drill");
        setUnlocalizedName("osmium_drill");
        setCreativeTab(MoreElectricTools.CREATIVE_TAB);
    }

    // ===== Имя предмета =====
    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return I18n.translateToLocal("item.mets.osmium_drill.name");
    }

    // ===== Зачарование =====
    @Override
    public int getItemEnchantability() {
        return 10;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    /**
     * Разрешаем кирковые чары, но НЕ Silk Touch — потому что
     * "шёлковое касание" у нас работает через режим, а не через чару.
     */
    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        if (enchantment == Enchantments.SILK_TOUCH) return false;
        if (enchantment == Enchantments.EFFICIENCY) return true;
        if (enchantment == Enchantments.FORTUNE) return true;
        if (enchantment == Enchantments.UNBREAKING) return true;
        if (enchantment == Enchantments.MENDING) return true;
        return super.canApplyAtEnchantingTable(stack, enchantment);
    }

    // ===== Скорость копания (лопатные блоки +20%) =====
    @Override
    public float getDestroySpeed(ItemStack stack, IBlockState state) {
        float base = super.getDestroySpeed(stack, state);
        String tool = state.getBlock().getHarvestTool(state);
        if ("shovel".equals(tool)) {
            return base * 1.2F;
        }
        return base;
    }

    // ===== Модель =====
    @SideOnly(Side.CLIENT)
    public void registerModels(ItemName name) {
        ModelLoader.setCustomModelResourceLocation(this, 0,
                new ModelResourceLocation(MoreElectricTools.MODID + ":osmium_drill", (String) null));
    }

    // ===== NBT: режим работы =====
    public DrillMode getMode(ItemStack stack) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        NBTTagCompound nbt = stack.getTagCompound();
        if (!nbt.hasKey(NBT_MODE)) nbt.setInteger(NBT_MODE, DrillMode.SINGLE.ordinal());
        int ordinal = nbt.getInteger(NBT_MODE);
        DrillMode[] modes = DrillMode.values();
        if (ordinal < 0 || ordinal >= modes.length) {
            ordinal = DrillMode.SINGLE.ordinal();
            nbt.setInteger(NBT_MODE, ordinal);
        }
        return modes[ordinal];
    }

    private void setMode(ItemStack stack, DrillMode mode) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setInteger(NBT_MODE, mode.ordinal());
    }

    // ===== Звук для 1x1 =====
    @Override
    @SideOnly(Side.CLIENT)
    public String getBreakSoundForBlock(EntityPlayerSP player, World world, BlockPos pos, ItemStack stack) {
        if (player.capabilities.isCreativeMode) return null;
        if (ElectricItem.manager.getCharge(stack) >= perUse) {
            IBlockState state = world.getBlockState(pos);
            float hardness = state.getBlockHardness(world, pos);
            return hardness <= 1.0F && hardness >= 0.0F ? "Tools/Drill/DrillSoft.ogg" : "Tools/Drill/DrillHard.ogg";
        }
        signalErrorFor(player);
        return "";
    }

    @Override
    public boolean breakBlock(ItemStack stack, World world, BlockPos pos, IBlockState state) {
        return ElectricItem.manager.getCharge(stack) >= perUse && super.breakBlock(stack, world, pos, state);
    }

    // ===== Основная логика добычи =====
    @Override
    public boolean onBlockStartBreak(ItemStack stack, BlockPos pos, EntityPlayer player) {
        World world = player.world;
        DrillMode mode = getMode(stack);

        // Шёлковое касание — только через режим SILK
        boolean silk = (mode == DrillMode.SILK);

        // Обычный режим 1x1 БЕЗ Silk — делегируем IC2 (стандартное поведение)
        if (!mode.isAreaMode() && !silk) {
            return super.onBlockStartBreak(stack, pos, player);
        }

        if (world.isRemote) return true;

        EnumFacing sideHit = getSideHit(player, pos);

        // Для SILK — только центральный блок; для area — область
        List<BlockPos> blocks = getBlocksToBreak(world, pos, mode, sideHit);

        playDrillSound(player, pos);

        // Учитываем Fortune с бура (работает, если режим не SILK)
        int fortune = EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, stack);

        for (BlockPos target : blocks) {
            if (ElectricItem.manager.getCharge(stack) < perUse) {
                signalErrorFor(player);
                break;
            }

            IBlockState state = world.getBlockState(target);
            if (state.getBlock().isAir(state, world, target)) continue;
            if (state.getBlockHardness(world, target) < 0.0F) continue;

            List<ItemStack> drops = new ArrayList<>();
            if (silk) {
                ItemStack silkDrop = getSilkTouchDrop(state);
                if (!silkDrop.isEmpty()) drops.add(silkDrop);
            } else {
                drops = state.getBlock().getDrops(world, target, state, fortune);
            }

            world.setBlockToAir(target);

            if (!player.capabilities.isCreativeMode) {
                for (ItemStack drop : drops) {
                    Block.spawnAsEntity(world, target, drop);
                }
                ElectricItem.manager.use(stack, perUse, player);
            }
        }

        return true;
    }

    public static EnumFacing getSideHit(EntityPlayer player, BlockPos targetPos) {
        RayTraceResult rtr = player.rayTrace(6.0D, 1.0F);
        if (rtr != null && rtr.typeOfHit == RayTraceResult.Type.BLOCK
                && rtr.getBlockPos().equals(targetPos) && rtr.sideHit != null) {
            return rtr.sideHit;
        }
        if (player.rotationPitch < -45.0F) return EnumFacing.UP;
        if (player.rotationPitch > 45.0F) return EnumFacing.DOWN;
        return player.getHorizontalFacing();
    }

    private void playDrillSound(EntityPlayer player, BlockPos pos) {
        World world = player.world;
        IBlockState state = world.getBlockState(pos);
        float hardness = state.getBlockHardness(world, pos);
        String sound = (hardness <= 1.0F && hardness >= 0.0F)
                ? "Tools/Drill/DrillSoft.ogg"
                : "Tools/Drill/DrillHard.ogg";
        IC2.audioManager.playOnce(player, PositionSpec.Hand, sound, true, 1.0F);
    }

    public List<BlockPos> getBlocksToBreak(World world, BlockPos center, DrillMode mode, EnumFacing sideHit) {
        List<BlockPos> result = new ArrayList<>();

        // Специальный случай — TUNNEL_2 (таргет-блок + блок под ним)
        if (mode == DrillMode.TUNNEL_2) {
            result.add(center);
            result.add(center.down());
            return result;
        }

        if (!mode.isAreaMode()) {
            result.add(center);
            return result;
        }

        int radius = mode.width / 2;
        boolean isCube = mode.depth > 1;
        int depthCount = isCube ? mode.depth : 1;

        EnumFacing into = sideHit.getOpposite();

        int intoX = into.getFrontOffsetX();
        int intoY = into.getFrontOffsetY();
        int intoZ = into.getFrontOffsetZ();

        for (int w = -radius; w <= radius; w++) {
            for (int h = -radius; h <= radius; h++) {
                for (int d = 0; d < depthCount; d++) {
                    int dx = 0, dy = 0, dz = 0;

                    switch (sideHit) {
                        case UP:
                        case DOWN:
                            dx = w;
                            dz = h;
                            dy = intoY * d;
                            break;
                        case NORTH:
                        case SOUTH:
                            dx = w;
                            dy = h;
                            dz = intoZ * d;
                            break;
                        case EAST:
                        case WEST:
                            dz = w;
                            dy = h;
                            dx = intoX * d;
                            break;
                    }

                    result.add(center.add(dx, dy, dz));
                }
            }
        }

        return result;
    }

    // ===== Переключение режима (M + ПКМ) =====
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = StackUtil.get(player, hand);

        if (!world.isRemote && IC2.keyboard.isModeSwitchKeyDown(player)) {
            DrillMode newMode = getMode(stack).next();
            setMode(stack, newMode);
            IC2.platform.messagePlayer(player, "Режим: " + newMode.displayName);
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        return super.onItemRightClick(world, player, hand);
    }

    void signalErrorFor(EntityPlayer player) {
        IC2.audioManager.playOnce(player, PositionSpec.Hand, "mets:dehydratorError.ogg", true,
                IC2.audioManager.getDefaultVolume() - 1.0F);
        player.getCooldownTracker().setCooldown(this, 40);
        player.sendMessage(new TextComponentString("\u041d\u0435\u0434\u043e\u0441\u0442\u0430\u0442\u043e\u0447\u043d\u043e \u044d\u043d\u0435\u0440\u0433\u0438\u0438!"));
    }

    private ItemStack getSilkTouchDrop(IBlockState state) {
        try {
            java.lang.reflect.Method method = Block.class.getDeclaredMethod("getSilkTouchDrop", IBlockState.class);
            method.setAccessible(true);
            return (ItemStack) method.invoke(state.getBlock(), state);
        } catch (Exception e) {
            return new ItemStack(state.getBlock());
        }
    }
}