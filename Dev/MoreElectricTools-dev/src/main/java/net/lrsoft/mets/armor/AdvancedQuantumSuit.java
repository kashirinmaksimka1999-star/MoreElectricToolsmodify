package net.lrsoft.mets.armor;

import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.Multimap;

import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import ic2.api.item.IHazmatLike;
import ic2.api.item.IItemHudInfo;
import ic2.core.IC2;
import ic2.core.IC2Potion;
import ic2.core.init.Localization;
import ic2.core.item.IPseudoDamageItem;
import ic2.core.item.armor.jetpack.IBoostingJetpack;
import ic2.core.item.armor.jetpack.IJetpack;
import net.lrsoft.mets.MoreElectricTools;
import net.lrsoft.mets.manager.ConfigManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.util.DamageSource;
import net.minecraft.item.ItemArmor.ArmorMaterial;
import net.minecraft.world.World;
import net.minecraftforge.common.ISpecialArmor;
import net.minecraftforge.common.ISpecialArmor.ArmorProperties;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AdvancedQuantumSuit extends ItemArmor
		implements ISpecialArmor, IPseudoDamageItem, IElectricItem, IItemHudInfo, IBoostingJetpack, IHazmatLike {
	private static ArmorMaterial defaultMaterial = EnumHelper.addArmorMaterial(
			"advanced_quantum_suit", MoreElectricTools.MODID + ":advanced_quantum_suit", 50, new int[]{7, 15, 9, 6}, 40, SoundEvents.ITEM_ARMOR_EQUIP_IRON, 2);
	public final static UUID KNOWBACK_MODIFER = UUID.fromString("ba65c41a-e46e-47da-b9ee-4d3ad479b50b");
	private static double maxStorageEnergy = 100000000d, transferSpeed = 245760d;
	private static int suitTier = 5;

	private static final double ARMOR_CHARGE_RATE = 61440.0;

	private EntityEquipmentSlot currentType;
	public AdvancedQuantumSuit(String itemName, EntityEquipmentSlot type) {
		super(defaultMaterial, 0, type);
		setUnlocalizedName("mets." + itemName);
		setRegistryName(MoreElectricTools.MODID, itemName);
		setCreativeTab(MoreElectricTools.CREATIVE_TAB);
		setMaxDamage(2333);
		setMaxStackSize(1);
		setNoRepair();
		currentType = type;
	}

	public static int getSuitTier() { return suitTier; }

	@Override
	public void onArmorTick(World world, EntityPlayer player, ItemStack itemStack) {
		switch(currentType)
		{
			case CHEST:
				IC2.platform.profilerStartSection("QuantumBodyarmor");
				player.extinguish();

				rechargeHeldToolAfterUse(player, itemStack);
				rechargeArmorPieces(player, itemStack);

				float currentHealth = player.getHealth();
				if(currentHealth < player.getMaxHealth())
				{
					if (ElectricItem.manager.use(itemStack, ConfigManager.AdvancedQuantumSuitCureCost, player)) {
						player.setHealth(currentHealth+1);
					}
				}
				IC2.platform.profilerEndSection();
				break;
			default:
				break;
		}
	}

	private void rechargeHeldToolAfterUse(EntityPlayer player, ItemStack chest) {
		if (player.world.isRemote) return;

		ItemStack held = player.getHeldItemMainhand();
		if (held.isEmpty() || held == chest) return;
		if (!(held.getItem() instanceof IElectricItem)) return;

		IElectricItem electricItem = (IElectricItem) held.getItem();
		double maxCharge = electricItem.getMaxCharge(held);
		double currentCharge = ElectricItem.manager.getCharge(held);
		String currentName = held.getItem().getRegistryName().toString();

		NBTTagCompound data = player.getEntityData();
		String lastName = data.getString("METS_LastHeldName");
		double lastCharge = data.getDouble("METS_LastHeldCharge");

		if (!currentName.equals(lastName)) {
			data.setString("METS_LastHeldName", currentName);
			data.setDouble("METS_LastHeldCharge", currentCharge);
			return;
		}

		boolean used = currentCharge < lastCharge;

		if (used) {
			double spaceLeft = maxCharge - currentCharge - 1.0;
			if (spaceLeft > 0) {
				double available = ElectricItem.manager.getCharge(chest);
				if (available > 0) {
					double transfer = Math.min(spaceLeft, available);
					ElectricItem.manager.discharge(chest, transfer, Integer.MAX_VALUE, true, false, false);
					ElectricItem.manager.charge(held, transfer, Integer.MAX_VALUE, true, false);
					if (player instanceof EntityPlayerMP) {
						((EntityPlayerMP) player).inventoryContainer.detectAndSendChanges();
					}
				}
			}
		}

		data.setDouble("METS_LastHeldCharge", ElectricItem.manager.getCharge(held));
	}

	private void rechargeArmorPieces(EntityPlayer player, ItemStack chest) {
		if (player.world.isRemote) return;

		for (ItemStack armor : player.inventory.armorInventory) {
			if (armor.isEmpty() || armor == chest) continue;
			if (!(armor.getItem() instanceof IElectricItem)) continue;

			IElectricItem electricItem = (IElectricItem) armor.getItem();
			double maxCharge = electricItem.getMaxCharge(armor);
			double currentCharge = ElectricItem.manager.getCharge(armor);

			double spaceLeft = maxCharge - currentCharge - 1.0;
			if (spaceLeft <= 0) continue;

			double available = ElectricItem.manager.getCharge(chest);
			if (available <= 0) return;

			double transfer = Math.min(Math.min(spaceLeft, available), ARMOR_CHARGE_RATE);

			ElectricItem.manager.discharge(chest, transfer, Integer.MAX_VALUE, true, false, false);
			ElectricItem.manager.charge(armor, transfer, Integer.MAX_VALUE, true, false);
		}

		if (player instanceof EntityPlayerMP) {
			((EntityPlayerMP) player).inventoryContainer.detectAndSendChanges();
		}
	}

	@Override
	public ArmorProperties getProperties(EntityLivingBase player, ItemStack armor, DamageSource source, double damage,
										 int slot) {
		// Если это ручная обработка падения из FlightManager — не поглощаем
		// (FlightManager уже списал EU с ботинок сам)
		if (source == DamageSource.FALL && player instanceof EntityPlayer) {
			NBTTagCompound data = ((EntityPlayer)player).getEntityData();
			if (data.getBoolean("METS_ProcessingFall")) {
				return new ISpecialArmor.ArmorProperties(0, 0.0, 0);
			}
		}

		// ===== УРОН ОТ ПАДЕНИЯ =====
		// Поглощают ТОЛЬКО ботинки — 100% урона за EU. Остальные части — 0.
		// Нужно для режима F = Off (ванильная механика).
		if (source == DamageSource.FALL) {
			if (currentType == EntityEquipmentSlot.FEET) {
				int energyPerDamage = (int) ConfigManager.AdvancedQuantumSuitDamageEnergyCost;
				int damageLimit = Integer.MAX_VALUE;
				if (energyPerDamage > 0) {
					damageLimit = (int) Math.min(damageLimit,
							25.0D * ElectricItem.manager.getCharge(armor) / energyPerDamage);
				}
				return new ISpecialArmor.ArmorProperties(10, 1.0, damageLimit);
			}
			return new ISpecialArmor.ArmorProperties(0, 0.0, 0);
		}

		// ===== ОБЫЧНЫЙ УРОН =====
		int energyPerDamage = (int) ConfigManager.AdvancedQuantumSuitDamageEnergyCost;
		int damageLimit = Integer.MAX_VALUE;
		if (energyPerDamage > 0)
			damageLimit = (int) Math.min(damageLimit, 25.0D * ElectricItem.manager.getCharge(armor) / energyPerDamage);
		return new ISpecialArmor.ArmorProperties(8, 0.5, damageLimit);
	}

	@Override
	public boolean addsProtection(EntityLivingBase entity, EntityEquipmentSlot slot, ItemStack stack) {
		return (ElectricItem.manager.getCharge(stack) > 0.0D);
	}

	@Override
	public boolean drainEnergy(ItemStack pack, int amount) {
		return (ElectricItem.manager.discharge(pack, (amount + 10), Integer.MAX_VALUE, true, false, false) > 0.0D);
	}

	@Override
	public void damageArmor(EntityLivingBase entity, ItemStack stack, DamageSource source, int damage, int slot)
	{
		ElectricItem.manager.discharge(stack, (damage *  ConfigManager.AdvancedQuantumSuitDamageEnergyCost), 2147483647, true, false, false);
	}

	public Multimap<String, AttributeModifier> getItemAttributeModifiers(EntityEquipmentSlot equipmentSlot) {
		Multimap<String, AttributeModifier> multimap = super.getItemAttributeModifiers(equipmentSlot);
		if (equipmentSlot == EntityEquipmentSlot.CHEST)
		{
			multimap.put(SharedMonsterAttributes.KNOCKBACK_RESISTANCE.getName(),  new AttributeModifier(KNOWBACK_MODIFER, "Weapon modifier", 0.5f, 0));
		}
		return multimap;
	}

	@Override
	public double getChargeLevel(ItemStack stack) { return ElectricItem.manager.getCharge(stack) / getMaxCharge(stack);}

	@Override
	public float getDropPercentage(ItemStack arg0) {return 0.0f;}

	// ===== ДЖЕТПАК ОТКЛЮЧЁН =====

	@Override
	public float getHoverMultiplier(ItemStack arg0, boolean arg1) {return 0.1f;}

	@Override
	public float getPower(ItemStack arg0) {return 0.0f;}

	@Override
	public float getWorldHeightDivisor(ItemStack arg0) {return 1.0f;}

	@Override
	public boolean isJetpackActive(ItemStack arg0) {return false;}

	@Override
	public void setStackDamage(ItemStack stack, int damage) {setDamage(stack, damage);}

	@Override
	public List<String> getHudInfo(ItemStack stack, boolean advanced) {
		List<String> info = new LinkedList<>();
		info.add(ElectricItem.manager.getToolTip(stack));
		info.add(Localization.translate("ic2.item.tooltip.PowerTier", new Object[] { Integer.valueOf(this.suitTier) }));
		return info;
	}

	@Override
	public boolean canProvideEnergy(ItemStack stack) {return false;}

	@Override
	public EnumRarity getRarity(ItemStack stack) {return EnumRarity.RARE;}

	@Override
	public double getMaxCharge(ItemStack stack) {return maxStorageEnergy;}

	@Override
	public int getTier(ItemStack stack) {return suitTier;}

	@Override
	public double getTransferLimit(ItemStack stack) {return transferSpeed;}

	public Item getChargedItem(ItemStack itemStack) {return this;}

	public Item getEmptyItem(ItemStack itemStack) {return this;}

	@Override
	public int getArmorDisplay(EntityPlayer player, ItemStack armor, int slot) {return 0;}

	@Override
	public boolean isRepairable() {return false;}

	@Override
	public boolean isEnchantable(ItemStack stack) {return false;}

	// ===== ТЯГА ДЖЕТПАКА ОБНУЛЕНА =====

	@Override
	public float getBaseThrust(ItemStack arg0, boolean arg1) {return 0.0f;}

	@Override
	public float getBoostThrust(EntityPlayer arg0, ItemStack arg1, boolean arg2) {return 0.0f;}

	@Override
	public float getHoverBoost(EntityPlayer arg0, ItemStack arg1, boolean arg2) {return 0.0f;}

	@Override
	public boolean useBoostPower(ItemStack pack, float amount) {
		return false;
	}
}