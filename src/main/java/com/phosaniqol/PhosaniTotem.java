package com.phosaniqol;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.NPC;

@Getter
@Setter
public class PhosaniTotem extends PhosaniNpc
{
	private int charge;

	public PhosaniTotem(NPC npc, int charge, PhosaniQolConfig config)
	{
		super.setNpc(npc);
		this.charge = charge;
		setHighlightConfig(config);
	}

	public void setHighlightConfig(PhosaniQolConfig config)
	{
		super.setHighlightConfig(
			(charge >= 0) ? config.highlightChargedTotems() : PhosaniQolConfig.HighlightStyle.NONE,
			config.chargedBorderWidth(),
			config.chargedBorderColor(),
			config.chargedFillColor(),
			charge >= 0 && config.totemChargeOverlay(),
			config.totemChargeOverlayFont().getFont(),
			config.totemChargeOverlayOffset(),
			config.totemChargeOverlayColor()
		);
	}
}
