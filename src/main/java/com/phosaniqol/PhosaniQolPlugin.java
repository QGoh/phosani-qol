package com.phosaniqol;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.HitsplatID;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.Scene;
import net.runelite.api.Skill;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.FakeXpDrop;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.NPCManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(name = "Phosani QoL")
public class PhosaniQolPlugin extends Plugin
{
	// 1 = SW, 2 = SW, 3 = NW, 4 = NE
	private final Set<Integer> DORMANT_TOTEMS = ImmutableSet.of(
		NpcID.NIGHTMARE_TOTEM_1_DORMANT,
		NpcID.NIGHTMARE_TOTEM_2_DORMANT,
		NpcID.NIGHTMARE_TOTEM_3_DORMANT,
		NpcID.NIGHTMARE_TOTEM_4_DORMANT);

	private final Set<Integer> READY_TOTEMS = ImmutableSet.of(
		NpcID.NIGHTMARE_TOTEM_1_READY,
		NpcID.NIGHTMARE_TOTEM_2_READY,
		NpcID.NIGHTMARE_TOTEM_3_READY,
		NpcID.NIGHTMARE_TOTEM_4_READY);

	private final Set<Integer> CHARGED_TOTEMS = ImmutableSet.of(
		NpcID.NIGHTMARE_TOTEM_1_CHARGED,
		NpcID.NIGHTMARE_TOTEM_2_CHARGED,
		NpcID.NIGHTMARE_TOTEM_3_CHARGED,
		NpcID.NIGHTMARE_TOTEM_4_CHARGED);

	private final Map<Integer, Integer> PHOSANI_PHASES = ImmutableMap.<Integer, Integer>builder()
		.put(NpcID.NIGHTMARE_CHALLENGE_PHASE_01, 0)
		.put(NpcID.NIGHTMARE_CHALLENGE_PHASE_02, 1)
		.put(NpcID.NIGHTMARE_CHALLENGE_PHASE_03, 0)
		.put(NpcID.NIGHTMARE_CHALLENGE_PHASE_04, 1)
		.put(NpcID.NIGHTMARE_CHALLENGE_WEAK_PHASE_01, 0)
		.put(NpcID.NIGHTMARE_CHALLENGE_WEAK_PHASE_02, 1)
		.put(NpcID.NIGHTMARE_CHALLENGE_WEAK_PHASE_03, 0)
		.put(NpcID.NIGHTMARE_CHALLENGE_WEAK_PHASE_04, 1)
		.put(NpcID.NIGHTMARE_CHALLENGE_INITIAL, -1)
		.put(NpcID.NIGHTMARE_CHALLENGE_PHASE_05, -1)
		.put(NpcID.NIGHTMARE_CHALLENGE_DYING, -1)
		.put(NpcID.NIGHTMARE_CHALLENGE_DEAD, -1)
		.put(NpcID.NIGHTMARE_CHALLENGE_BLAST, -1)
		.build();

	private final Set<Integer> PHOSANI_ADDS = ImmutableSet.of(
		NpcID.NIGHTMARE_CHALLENGE_PARASITE,
		NpcID.NIGHTMARE_CHALLENGE_PARASITE_WEAK,
		NpcID.NIGHTMARE_CHALLENGE_HUSK_RANGED,
		NpcID.NIGHTMARE_CHALLENGE_HUSK_MAGIC,
		NpcID.NIGHTMARE_CHALLENGE_SLEEPWALKER
	);

	private final int PRE_PHOSANI_SPORE = 37738;
	private final int PHOSANI_SPORE = 37739;

	@Inject
	private Client client;

	@Inject
	private PhosaniQolConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private NPCManager npcManager;

	@Inject
	private PhosaniQolOverlay overlay;

	@Getter
	private final Map<Integer, PhosaniTotem> totems = new HashMap<>();
	@Getter
	private PhosaniPhase phosani = null;
	@Getter
	private final Map<Integer, PhosaniAdd> adds = new HashMap<>();
	@Getter
	private final ArrayList<PhosaniObject> spores = new ArrayList<>();
	private final Set<Skill> xpDrops = new HashSet<>();

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(overlay);
		clearAll();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals(PhosaniQolConfig.CONFIG_GROUP))
		{
			switch (event.getKey())
			{
				case "highlightChargedTotems":
				case "chargedBorderWidth":
				case "chargedBorderColor":
				case "chargedFillColor":
				case "totemChargeOverlay":
				case "totemChargeOverlayFont":
				case "totemChargeOverlayOffset":
				case "totemChargeOverlayColor":
					totems.forEach((k, v) -> v.setHighlightConfig(config));
					break;
				case "phosaniPhaseColors":
				case "phosaniBorderWidth":
				case "parasitePhaseColor":
				case "huskPhaseColor":
				case "phosaniShieldOverlay":
				case "phosaniShieldOverlayFont":
				case "phosaniShieldOverlayOffset":
				case "phosaniShieldOverlayColor":
					if (phosani != null)
					{
						phosani.setHighlightConfig(config);
					}
					break;
				case "highlightRangedHusk":
				case "highlightMagicHusk":
				case "highlightParasite":
				case "highlightSleepwalkers":
				case "addsBorderWidth":
				case "addsBorderColor":
				case "addsFillColor":
					adds.forEach((k, v) -> v.setHighlightConfig(config));
					break;
				case "hideHealthOverlay":
					Widget healthBar = client.getWidget(InterfaceID.HpbarHud.HP);
					if (healthBar != null)
					{
						healthBar.setHidden(config.hideHealthOverlay());
					}
			}
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() != GameState.LOGGED_IN)
		{
			clearAll();
		}
	}

	@Subscribe
	public void onFakeXpDrop(FakeXpDrop event)
	{
		Skill skill = event.getSkill();
		xpDrops.add(skill);

		if (skill == Skill.HITPOINTS)
		{
			Player player = client.getLocalPlayer();
			Actor actor = player.getInteracting();
			if (actor instanceof NPC)
			{
				int npcId = ((NPC) actor).getId();
				if (READY_TOTEMS.contains((npcId)))
				{
					int hit = (event.getXp() == 0) ? 1 : (int) Math.round(event.getXp() * (3.0d / 4.0d));
					int multiplier = (xpDrops.contains(Skill.MAGIC)) ? 2 : 1;
					int charge = totems.get(npcId).getCharge() + (hit * multiplier);
					if (charge > 200)
					{
						charge = 200;
					}
					//log.info(npcId + " charged totem = " + totems.get(npcId).getCharge() + " --> " + charge);
					totems.get(npcId).setCharge(charge);
				}
			}
			xpDrops.clear();
		}
	}

	/*
	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (event.getSkill() != Skill.HITPOINTS) return;

		int currentHpXp = event.getXp();
		Player player = client.getLocalPlayer();
		Actor actor = player.getInteracting();
		if (actor instanceof NPC)
		{
			int npcId = ((NPC) actor).getId();
			int gainedHpXp = currentHpXp - previousHpXp;
			if (PHOSANI_PHASES.containsKey(npcId))
			{
				// TODO: fix exp from dihn's spec overcounting
				int hit = (int) Math.round(gainedHpXp * (3.0d / 4.0d));
				int shield = phosani.getShield() + hit;
				phosani.setShield(shield);
			}
		}

		previousHpXp = currentHpXp;
	}
 	*/

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		Actor actor = event.getActor();
		if (actor instanceof NPC)
		{
			int npcId = ((NPC) actor).getId();
			int hitsplatType = event.getHitsplat().getHitsplatType();
			int hitsplatAmount = event.getHitsplat().getAmount();

			if (hitsplatType == HitsplatID.DAMAGE_OTHER_WHITE && READY_TOTEMS.contains(npcId))
			{
				int charge = Math.max(0, totems.get(npcId).getCharge() - hitsplatAmount);
				totems.get(npcId).setCharge(charge);
			}
			else
			{
				if (READY_TOTEMS.contains(npcId))
				{
					int charge = Math.min(200, recalculateCharge(actor) + hitsplatAmount);
					// to avoid double counting the hit processed from xp drop
					if (charge > -1 && charge > totems.get(npcId).getCharge())
					{
						totems.get(npcId).setCharge(charge);
					}
				}
			}
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (client.getVarbitValue(VarbitID.PLAYER_IS_IN_NIGHTMARE_CHALLENGE) == 1 && phosani != null && phosani.getShield() > 0)
		{
			int shield = client.getVarbitValue(6099);
			phosani.setShield(shield);
		}
	}

	@Subscribe
	public void onNpcChanged(NpcChanged event)
	{
		NPCComposition oldNpc = event.getOld();
		NPC newNpc = event.getNpc();
		int oldNpcId = oldNpc.getId();
		int newNpcId = newNpc.getId();

		if (DORMANT_TOTEMS.contains(oldNpcId) || READY_TOTEMS.contains(oldNpcId) || CHARGED_TOTEMS.contains(oldNpcId))
		{
			totems.remove(oldNpcId);
			int charge = (CHARGED_TOTEMS.contains(newNpcId)) ? 200
				: (READY_TOTEMS.contains(newNpcId)) ? 0
				: -1;
			totems.put(newNpcId, new PhosaniTotem(newNpc, charge, config));
		}
		else if (PHOSANI_PHASES.containsKey(newNpcId))
		{
			phosani.setNpc(newNpc);
			phosani.setPhase(PHOSANI_PHASES.get(newNpcId));
			int shield = -1;
			switch (newNpcId)
			{
				case NpcID.NIGHTMARE_CHALLENGE_PHASE_01:
					shield = 400;
					break;
				case NpcID.NIGHTMARE_CHALLENGE_PHASE_02:
					shield = 400;
					break;
				case NpcID.NIGHTMARE_CHALLENGE_PHASE_03:
					shield = 400;
					break;
				case NpcID.NIGHTMARE_CHALLENGE_PHASE_04:
					shield = 400;
					break;
				case NpcID.NIGHTMARE_CHALLENGE_PHASE_05:
					shield = 150;
					break;
			}
			phosani.setShield(shield);
			phosani.setHighlightConfig(config);
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		NPC npc = event.getNpc();
		int npcId = npc.getId();
		if (DORMANT_TOTEMS.contains(npcId))
		{
			totems.put(npcId, new PhosaniTotem(npc, -1, config));
		}
		else if (PHOSANI_PHASES.containsKey(npcId))
		{
			phosani = new PhosaniPhase(npc, -1, -1, config);
		}
		else if (PHOSANI_ADDS.contains(npcId))
		{
			int index = npc.getIndex();
			int key = -(npcId + index);
			adds.put(key, new PhosaniAdd(npc, config));
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		NPC npc = event.getNpc();
		int npcId = npc.getId();
		if (PHOSANI_ADDS.contains(npcId))
		{
			int index = npc.getIndex();
			int key = -(npcId + index);
			adds.remove(key);
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		GameObject object = event.getGameObject();
		if (object.getId() == PHOSANI_SPORE || object.getId() == PRE_PHOSANI_SPORE)
		{
			markObject(object);
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		GameObject object = event.getGameObject();
		if (object.getId() == PHOSANI_SPORE || object.getId() == PRE_PHOSANI_SPORE)
		{
			spores.removeIf(spore -> spore.getTileObject() == object);
		}
	}

	// copied from objectindicators
	private TileObject findTileObject(WorldView wv, int x, int y, int id)
	{
		int level = wv.getPlane();
		Scene scene = wv.getScene();
		Tile[][][] tiles = scene.getTiles();
		final Tile tile = tiles[level][x][y];
		if (tile == null)
		{
			return null;
		}

		final GameObject[] tileGameObjects = tile.getGameObjects();

		for (GameObject object : tileGameObjects)
		{
			if (object != null && object.getId() == id)
			{
				return object;
			}
		}

		return null;
	}

	// copied from objectindicators
	private void markObject(TileObject object)
	{
		WorldView wv = client.getLocalPlayer().getWorldView();
		if (wv == null)
		{
			return;
		}

		LocalPoint localPoint = object.getLocalLocation();
		TileObject tileObject = findTileObject(wv, localPoint.getSceneX(), localPoint.getSceneY(), object.getId());
		if (tileObject == null)
		{
			return;
		}

		// object.getId() is always the base object id, getObjectComposition transforms it to
		// the correct object we see
		ObjectComposition objectComposition = client.getObjectDefinition(object.getId());
		ObjectComposition objectDefinition = objectComposition.getImpostorIds() == null ? objectComposition : objectComposition.getImpostor();
		String name = objectDefinition.getName();
		// Name is probably never "null" - however prevent adding it if it is, as it will
		// become ambiguous as objects with no name are assigned name "null"
		if (Strings.isNullOrEmpty(name) || name.equals("null"))
		{
			return;
		}

		final WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, object.getLocalLocation());
		final int regionId = worldPoint.getRegionID();
		spores.add(
			new PhosaniObject(
				object,
				regionId,
				worldPoint.getRegionX(),
				worldPoint.getRegionY(),
				worldPoint.getPlane(),
				config
			)
		);
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		Widget healthBar = client.getWidget(InterfaceID.HpbarHud.HP);
		if (healthBar != null)
		{
			healthBar.setHidden(config.hideHealthOverlay());
		}
	}

	// copied from opponentInfo plugin
	private int recalculateCharge(Actor actor)
	{
		if (!(actor instanceof NPC))
		{
			return -1;
		}

		int npcId = ((NPC) actor).getId();
		int chargeRatio = actor.getHealthRatio();
		int chargeScale = actor.getHealthScale();
		int maxCharge = 200;
		if (chargeRatio >= 0 && chargeScale > 0)
		{
			// This is the reverse of the calculation of chargeRatio done by the server
			// which is: chargeRatio = 1 + (chargeScale - 1) * charge / maxCharge (if charge > 0, 0 otherwise)
			// It's able to recover the exact charge if maxCharge <= chargeScale.
			int floor = 0;
			int ceiling;
			if (chargeRatio > 1)
			{
				// This doesn't apply if chargeRatio = 1, because of the special case in the server calculation that
				// charge = 0 forces chargeRatio = 0 instead of the expected chargeRatio = 1
				floor = (maxCharge * (chargeRatio - 1) + chargeScale - 2) / (chargeScale - 1);
			}
			ceiling = (maxCharge * chargeRatio - 1) / (chargeScale - 1);
			if (ceiling > maxCharge)
			{
				ceiling = maxCharge;
			}
			// Take the average of min and max possible charges
			return (floor + ceiling + 1) / 2;
		}

		return -1;
	}

	private void clearAll()
	{
		totems.clear();
		phosani = null;
		xpDrops.clear();
	}

	@Provides
	PhosaniQolConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PhosaniQolConfig.class);
	}
}
