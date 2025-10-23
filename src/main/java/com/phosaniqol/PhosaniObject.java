package com.phosaniqol;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameObject;

@Getter
@Slf4j
public class PhosaniObject extends PhosaniNpc
{
	private final GameObject gameObject;

	public PhosaniObject(GameObject gameObject, PhosaniQolConfig config)
	{
		super.setNpc(null);
		this.gameObject = gameObject;
		setHighlightConfig(config);
	}

	public void setHighlightConfig(PhosaniQolConfig config)
	{
		super.setHighlightConfig(
			config.highlightSpores(),
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
