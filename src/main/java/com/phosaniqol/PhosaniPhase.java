package com.phosaniqol;

import java.awt.Color;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.NPC;

@Getter
@Setter
public class PhosaniPhase extends PhosaniNpc
{
	private int phase;
	private int shield;

	public PhosaniPhase(NPC npc, int phase, int shield, PhosaniQolConfig config)
	{
		super.setNpc(npc);
		this.phase = phase;
		this.shield = shield;
		setHighlightConfig(config);
	}

	public void setHighlightConfig(PhosaniQolConfig config)
	{
		Color color = (phase == 0) ? config.parasitePhaseColor()
			: (phase == 1) ? config.huskPhaseColor()
			: null;
		super.setHighlightConfig(
			(config.phosaniPhaseColors() && phase > -1) ? PhosaniQolConfig.HighlightStyle.TILE : PhosaniQolConfig.HighlightStyle.NONE,
			config.phosaniBorderWidth(),
			color,
			null,
			config.phosaniShieldOverlay(),
			config.phosaniShieldOverlayFont().getFont(),
			config.phosaniShieldOverlayOffset(),
			config.phosaniShieldOverlayColor()
		);
	}
}
