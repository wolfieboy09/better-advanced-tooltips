package dev.latvian.mods.betteradvancedtooltips;

import net.neoforged.neoforge.common.ModConfigSpec;

public class BATConfig {
	public static final BATConfig CONFIG;
	public static final ModConfigSpec CONFIG_SPEC;

	static {
		var pair = new ModConfigSpec.Builder().configure(BATConfig::new);
		CONFIG = pair.getLeft();
		CONFIG_SPEC = pair.getRight();
	}

	public final ModConfigSpec.BooleanValue removeCreativeTabTooltip;
	public final ModConfigSpec.BooleanValue removeComponentCountTooltip;
	@Deprecated(since = "2603-1.0.0")
	public final ModConfigSpec.BooleanValue fuelTooltip;
	public final ModConfigSpec.BooleanValue tagTooltip;
	public final ModConfigSpec.BooleanValue componentTooltip;

	private BATConfig(ModConfigSpec.Builder builder) {
		this.removeCreativeTabTooltip = builder.define("remove_creative_tab_tooltip", false);
		this.removeComponentCountTooltip = builder.define("remove_component_count_tooltip", true);
		this.fuelTooltip = builder.comment("Fuel burn times can no longer be resolved on the client. This config option does nothing").define("fuel_tooltip", true);
		this.tagTooltip = builder.define("tag_tooltip", true);
		this.componentTooltip = builder.define("component_tooltip", true);
	}
}
