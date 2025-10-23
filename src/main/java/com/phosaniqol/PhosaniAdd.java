package com.phosaniqol;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.NPC;
import net.runelite.api.gameval.NpcID;

@Getter
@Setter
public class PhosaniAdd extends PhosaniNpc
{
	private final Map<Integer, PhosaniQolConfig.HighlightStyle> HIGHLIGHT_STYLE_MAP;

	public PhosaniAdd(NPC npc, PhosaniQolConfig config)
	{
		super.setNpc(npc);
		this.HIGHLIGHT_STYLE_MAP = new HashMap<>();
		this.HIGHLIGHT_STYLE_MAP.put(NpcID.NIGHTMARE_CHALLENGE_PARASITE, config.highlightParasite());
		this.HIGHLIGHT_STYLE_MAP.put(NpcID.NIGHTMARE_CHALLENGE_PARASITE_WEAK, config.highlightParasite());
		this.HIGHLIGHT_STYLE_MAP.put(NpcID.NIGHTMARE_CHALLENGE_HUSK_RANGED, config.highlightRangedHusk());
		this.HIGHLIGHT_STYLE_MAP.put(NpcID.NIGHTMARE_CHALLENGE_HUSK_MAGIC, config.highlightMagicHusk());
		this.HIGHLIGHT_STYLE_MAP.put(NpcID.NIGHTMARE_CHALLENGE_SLEEPWALKER, config.highlightSleepwalkers());
		setHighlightConfig(config);
	}

	public void setHighlightConfig(PhosaniQolConfig config)
	{
		super.setHighlightConfig(
			HIGHLIGHT_STYLE_MAP.get(super.getNpc().getId()),
			config.addsBorderWidth(),
			config.addsBorderColor(),
			config.addsFillColor(),
			false,
			null,
			0,
			null
		);
	}
}