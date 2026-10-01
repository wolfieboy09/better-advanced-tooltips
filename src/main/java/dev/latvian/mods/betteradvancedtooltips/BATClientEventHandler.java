package dev.latvian.mods.betteradvancedtooltips;

import com.mojang.serialization.DynamicOps;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.LinkedHashMap;
import java.util.List;

@EventBusSubscriber(modid = BATMod.ID, value = Dist.CLIENT)
public class BATClientEventHandler {
	private static String reduce(Identifier id) {
		return id.getNamespace().equals("minecraft") ? id.getPath() : id.toString();
	}

	@SubscribeEvent(priority = EventPriority.LOW)
	public static void onItemTooltip(ItemTooltipEvent event) {
		var mc = Minecraft.getInstance();

		if (mc.level == null) {
			return;
		}

		var flags = event.getFlags();

		if (!flags.isAdvanced()) {
			return;
		}

		var stack = event.getItemStack();

		if (stack.isEmpty()) {
			return;
		}

		var registryAccess = mc.level.registryAccess();

		var lines = event.getToolTip();

		if (event.getFlags().hasAltDown()) {
			if (BATConfig.CONFIG.componentTooltip.getAsBoolean()) {
				var components = BuiltInRegistries.DATA_COMPONENT_TYPE;
				var ops = registryAccess.createSerializationContext(NbtOps.INSTANCE);
				for (var entry : stack.getComponentsPatch().map.entrySet()) {
					var id = components.getKey(entry.getKey());

					if (id != null) {
						var line = Component.empty();
						line.append(BATIcons.PATCHED_COMPONENT);
						line.append(BATIcons.SMALL_SPACE);

						line.append(Component.literal("!"));

						line.append(Component.literal(reduce(id)).withStyle(ChatFormatting.YELLOW));

						line.append(Component.literal("="));
						var errors0 = appendComponentValue(ops, line, (DataComponentType) entry.getKey(), entry.getValue());

						if (!errors0.isEmpty()) {
							lines.add(Component.literal(reduce(id) + " errored, see log").withStyle(ChatFormatting.DARK_RED));
						}

						lines.add(line);
					}
				}

				if (event.getFlags().hasShiftDown()) {
					for (var type : stack.getPrototype()) {
						var id = components.getKey(type.type());

						if (id != null && stack.getComponentsPatch().getPatch(type.type()) == null) {
							var line = Component.empty();
							line.append(BATIcons.PROTOTYPE_COMPONENT);
							line.append(BATIcons.SMALL_SPACE);
							line.append(Component.literal(reduce(id)).withStyle(ChatFormatting.GRAY));
							line.append(Component.literal("="));
							var errors0 = appendComponentValue(ops, line, (DataComponentType) type.type(), type.value());

							if (!errors0.isEmpty()) {
								lines.add(Component.literal(reduce(id) + " errored, see log").withStyle(ChatFormatting.DARK_RED));
							}

							lines.add(line);
						}
					}
				}
			}
		} else if (event.getFlags().hasShiftDown()) {
			if (BATConfig.CONFIG.tagTooltip.getAsBoolean()) {
				var tempTagNames = new LinkedHashMap<Identifier, TagInstance>();
				var tEvent = new ItemTagIconsEvent(event, tempTagNames);

				tEvent.append(TooltipTagType.ITEM, stack.getItem().builtInRegistryHolder());

				if (stack.getItem() instanceof BlockItem item) {
					tEvent.append(TooltipTagType.BLOCK, item.getBlock().builtInRegistryHolder());
				}

				if (stack.getItem() instanceof BucketItem bucket) {
					var fluid = bucket.content;

					if (fluid != Fluids.EMPTY) {
						tEvent.append(TooltipTagType.FLUID, fluid.builtInRegistryHolder());
					}
				}

				if (stack.getItem() instanceof SpawnEggItem) {
					var entityType = SpawnEggItem.getType(stack);

					if (entityType != null) {
						tEvent.append(TooltipTagType.ENTITY_TYPE, entityType.builtInRegistryHolder());
					}
				}

				var enchantments = stack.get(DataComponents.STORED_ENCHANTMENTS);

				if (enchantments != null && enchantments.size() == 1) {
					tEvent.append(TooltipTagType.ENCHANTMENT, enchantments.entrySet().iterator().next().getKey());
				}

				var instrumentComponent = stack.get(DataComponents.INSTRUMENT);

				if (instrumentComponent != null) {
					var instrument = instrumentComponent.instrument();
					tEvent.append(TooltipTagType.INSTRUMENT, instrument);
				}

				var paintingVariant = stack.get(DataComponents.PAINTING_VARIANT);

				if (paintingVariant != null) {
					tEvent.append(TooltipTagType.PAINTING_VARIANT, paintingVariant);
				}

				var bannerPattern = stack.get(DataComponents.PROVIDES_BANNER_PATTERNS);

				if (bannerPattern != null) {
					tEvent.append(TooltipTagType.BANNER_PATTERN, bannerPattern);
				}

				NeoForge.EVENT_BUS.post(tEvent);

				if (!tempTagNames.isEmpty()) {
					tempTagNames.values().stream().sorted().map(TagInstance::toText).forEach(lines::add);
				}
			}
		}
	}

	private static <T> List<String> appendComponentValue(DynamicOps<Tag> ops, MutableComponent line, DataComponentType<T> type, T value) {
		if (value == null) {
			line.append(Component.literal("null").withStyle(ChatFormatting.RED));
			return List.of();
		} else if (value instanceof Component c) {
			line.append(Component.empty().withStyle(ChatFormatting.GOLD).append(c));
		}

		try {
			var tag = type.codecOrThrow().encodeStart(ops, value).getOrThrow();
			line.append(NbtUtils.toPrettyComponent(tag));
			return List.of();
		} catch (Throwable ex) {
			line.append(Component.literal(String.valueOf(value)).withStyle(ChatFormatting.RED));
			return List.of();
		}
	}
}
