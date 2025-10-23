package com.phosaniqol;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.TileObject;

@Getter
@Slf4j
public class PhosaniObject extends PhosaniNpc
{
	private final TileObject tileObject;
	private final int regionId;
	private final int regionX;
	private final int regionY;
	private final int z;

	public PhosaniObject(TileObject tileObject, int regionId, int regionX, int regionY, int z, PhosaniQolConfig config)
	{
		super.setNpc(null);
		this.tileObject = tileObject;
		this.regionId = regionId;
		this.regionX = regionX;
		this.regionY = regionY;
		this.z = z;
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
