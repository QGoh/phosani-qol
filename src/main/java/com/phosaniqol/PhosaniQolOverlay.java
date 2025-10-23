package com.phosaniqol;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

@Slf4j
public class PhosaniQolOverlay extends Overlay
{
	private final Client client;
	private final PhosaniQolPlugin plugin;
	private final ModelOutlineRenderer renderer;

	@Inject
	private PhosaniQolOverlay(Client client, PhosaniQolPlugin plugin, ModelOutlineRenderer renderer)
	{
		this.client = client;
		this.plugin = plugin;
		this.renderer = renderer;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(Overlay.PRIORITY_HIGHEST);
		setLayer(OverlayLayer.UNDER_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics2D)
	{
		PhosaniPhase phosani = plugin.getPhosani();
		highlightNpc(graphics2D, phosani);

		Map<Integer, PhosaniTotem> totems = plugin.getTotems();
		totems.forEach((npcId, totem) -> highlightNpc(graphics2D, totem));

		Map<Integer, PhosaniAdd> adds = plugin.getAdds();
		adds.forEach((npcId, add) -> highlightNpc(graphics2D, add));

		ArrayList<PhosaniObject> spores = plugin.getSpores();
		spores.forEach((object) -> highlightNpc(graphics2D, object));

		return null;
	}

	private void highlightNpc(Graphics2D graphics2D, PhosaniNpc phosaniNpc)
	{
		if (phosaniNpc == null)
		{
			return;
		}

		NPC npc = phosaniNpc.getNpc();

		if (phosaniNpc.isTextOverlay() && npc != null)
		{
			String text = null;
			Color textOverlayColor = phosaniNpc.getTextOverlayColor();
			Font textOverlayFont = phosaniNpc.getTextOverlayFont();
			int textOverlayOffset = phosaniNpc.getTextOverlayOffset();
			if (phosaniNpc instanceof PhosaniTotem)
			{
				text = String.valueOf(((PhosaniTotem) phosaniNpc).getCharge());
			}
			else if (phosaniNpc instanceof PhosaniPhase)
			{
				int shield = ((PhosaniPhase) phosaniNpc).getShield();
				text = (shield > 0) ? String.valueOf(shield) : null;
			}
			if (text != null)
			{
				Point textLocation = npc.getCanvasTextLocation(graphics2D, text, textOverlayOffset);
				if (textLocation != null)
				{
					graphics2D.setFont(textOverlayFont);
					graphics2D.setColor(textOverlayColor);
					graphics2D.drawString(text, textLocation.getX(), textLocation.getY());
				}
			}
		}

		if (phosaniNpc.getHighlightStyle() == PhosaniQolConfig.HighlightStyle.NONE)
		{
			return;
		}
		if (phosaniNpc instanceof PhosaniTotem && ((PhosaniTotem) phosaniNpc).getCharge() < 200)
		{
			return;
		}

		LocalPoint localPoint = null;
		int size = 0;
		if (phosaniNpc instanceof PhosaniObject)
		{
			localPoint = ((PhosaniObject) phosaniNpc).getTileObject().getLocalLocation();
			size = 3;
		}
		else
		{
			localPoint = npc.getLocalLocation();
			size = npc.getComposition().getSize();
		}

		PhosaniQolConfig.HighlightStyle highlightStyle = phosaniNpc.getHighlightStyle();
		Color borderColor = phosaniNpc.getBorderColor();
		Color fillColor = phosaniNpc.getFillColor();
		double width = phosaniNpc.getBorderWidth();

		Shape shape = null;
		if (highlightStyle == PhosaniQolConfig.HighlightStyle.TILE)
		{
			shape = Perspective.getCanvasTileAreaPoly(client, localPoint, size);
		}
		else if (highlightStyle == PhosaniQolConfig.HighlightStyle.HULL)
		{
			shape = npc.getConvexHull();
		}
		else if (highlightStyle == PhosaniQolConfig.HighlightStyle.CLICKBOX)
		{
			shape = Perspective.getClickbox(client, client.getTopLevelWorldView(), npc.getModel(), npc.getCurrentOrientation(), localPoint.getX(), localPoint.getY(),
				Perspective.getTileHeight(client, localPoint, npc.getWorldLocation().getPlane()));
		}
		else if (highlightStyle == PhosaniQolConfig.HighlightStyle.OUTLINE)
		{
			renderer.drawOutline(npc, (int) width, borderColor, 5);
		}

		if (shape != null)
		{
			graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics2D.setColor(borderColor);
			graphics2D.setStroke(new BasicStroke((float) width));
			graphics2D.draw(shape);
			if (fillColor != null)
			{
				graphics2D.setColor(fillColor);
				graphics2D.fill(shape);
			}
		}
	}
}
