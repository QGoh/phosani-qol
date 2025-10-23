package com.phosaniqol;

import java.awt.Color;
import java.awt.Font;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.NPC;

@Getter
@Setter
public class PhosaniNpc
{
	private NPC npc;

	private PhosaniQolConfig.HighlightStyle highlightStyle;
	private double borderWidth;
	private Color borderColor;
	private Color fillColor;

	private boolean textOverlay;
	private Font textOverlayFont;
	private int textOverlayOffset;
	private Color textOverlayColor;

	public void setHighlightConfig(PhosaniQolConfig.HighlightStyle highlightStyle, double borderWidth, Color borderColor, Color fillColor,
								   boolean textOverlay, Font textOverlayFont, int textOverlayOffset, Color textColor)
	{
		this.highlightStyle = highlightStyle;
		this.borderWidth = borderWidth;
		this.borderColor = borderColor;
		this.fillColor = fillColor;

		this.textOverlay = textOverlay;
		this.textOverlayFont = textOverlayFont;
		this.textOverlayOffset = textOverlayOffset;
		this.textOverlayColor = textColor;
	}
}
