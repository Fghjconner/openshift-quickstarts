package com.pizzarizzia;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.google.gson.Gson;



public class Engine
{
	public static void main(String[] args)
	{
		Gson gson = new Gson();
		Engine engine = new Engine(1);
		
		Action[] actions = new Action[4];
		actions[0] = engine.new Action(ActionType.MOVE, Direction.LEFT);
		actions[1] = engine.new Action(ActionType.MOVE, Direction.RIGHT);
		actions[2] = engine.new Action(ActionType.MOVE, Direction.LEFT);
		actions[3] = engine.new Action(ActionType.MOVE, Direction.RIGHT);
		
		System.out.println(gson.toJson(actions));
	}
	
	
	public enum ActionType
	{
		USE, MOVE, THROW, STAND
	}

	public enum Direction
	{
		RIGHT, DOWN, LEFT, UP, NONE;

		public Direction reverse()
		{
			switch (this)
			{
				case RIGHT: return LEFT;
				case DOWN: return UP;
				case LEFT: return RIGHT;
				case UP: return DOWN;
				default: return NONE;
			}
		}
	}

	public class Action
	{
		public ActionType type;
		public Direction direction;

		public Action(ActionType actionType, Direction actionDirection)
		{
			type = actionType;
			direction = actionDirection;
		}
	}


	private static class Location
	{
		public int x;
		public int y;

		public Location(int xCoord, int yCoord)
		{
			x = xCoord;
			y = yCoord;
		}

		public Location(Location oldLoc)
		{
			x = oldLoc.x;
			y = oldLoc.y;
		}

		public Location getAdjacent(Direction direction)
		{
			Location adjacentLoc = new Location(this);

			switch(direction)
			{
				case RIGHT: adjacentLoc.x++; break;
				case LEFT: adjacentLoc.x--; break;
				case UP: adjacentLoc.y--; break;
				case DOWN: adjacentLoc.y++; break;
				case NONE: break;
			}

			return adjacentLoc;
		}

		@Override
		public boolean equals(Object other)
		{
			return other instanceof Location && x == ((Location) other).x && y == ((Location) other).y;
		}
	}

	private class Player
	{
		public Item held = null;
		public Location loc;
		public boolean isStunned = false;
		
		public Player(Location startingLoc)
		{
			loc = startingLoc;
		}
		
		public GraphicsCommunicationObject.Item makeHeldGraphicsItem()
		{
			if (held == null)
				return null;
			else
				return held.makeGraphicsItem();
		}
	}

	private enum Tile
	{
		EMPTY, WALL, OVEN, COUNTER, MEAT, VEGETABLES, CHEESE, SAUCE, CHEESAUCE, TRASH, DOUGH, PICKUP
	}

	public static abstract class Item
	{
		abstract GraphicsCommunicationObject.Item makeGraphicsItem();
	}

	public static class Pizza extends Item
	{
		public Direction flightDirection = Direction.NONE;

		public boolean sauce = false;
		public boolean cheese = false;
		public boolean meat = false;
		public boolean vegetables = false;

		public int cookedness = 0;
		
		public static Random rand = new Random();
		
		public static Pizza randomPizza()
		{
			Pizza out = new Pizza();
			
			out.sauce = rand.nextBoolean();
			out.cheese = rand.nextBoolean();
			out.meat = rand.nextBoolean();
			out.vegetables = rand.nextBoolean();
			
			float cookedness = rand.nextFloat();
			if (cookedness < .05)
				out.cookedness = 0;
			else if (cookedness < .95)
				out.cookedness = 4;
			else
				out.cookedness = 8;
			
			return out;
		}

		public boolean isRaw()
		{
			return cookedness < 4;
		}

		public boolean isCooked()
		{
			return cookedness >= 4 && cookedness < 8;
		}

		public boolean isBurnt()
		{
			return cookedness >= 8;
		}

		public GraphicsCommunicationObject.Item makeGraphicsItem()
		{
			return new GraphicsCommunicationObject.Pizza(this);
		}
		
		@Override
		public boolean equals(Object o)
		{
			if (o instanceof Pizza)
			{
				Pizza op = (Pizza) o;
				
				return flightDirection == op.flightDirection && sauce == op.sauce && cheese == op.cheese && meat == op.meat && vegetables == op.vegetables && isCooked() == op.isCooked() && isBurnt() == op.isBurnt();
			}
			else
				return false;
		}
	}

	public static class Meat extends Item
	{
		public GraphicsCommunicationObject.Item makeGraphicsItem()
		{
			return new GraphicsCommunicationObject.Meat();
		}
	}

	public static class Vegetables extends Item
	{
		public GraphicsCommunicationObject.Item makeGraphicsItem()
		{
			return new GraphicsCommunicationObject.Vegetables();
		}
	}



	private static Tile[][] map =
	{  //1			2			3			4			5			6			7
		{Tile.CHEESAUCE,Tile.CHEESAUCE,	Tile.EMPTY,		Tile.OVEN,		Tile.OVEN,		Tile.EMPTY,		Tile.EMPTY},		//1
		{Tile.CHEESE,	Tile.EMPTY,		Tile.EMPTY,		Tile.EMPTY,		Tile.EMPTY,		Tile.EMPTY,		Tile.MEAT},			//2
		{Tile.SAUCE,	Tile.EMPTY,		Tile.EMPTY,		Tile.EMPTY,		Tile.EMPTY,		Tile.EMPTY,		Tile.MEAT},			//3
		{Tile.EMPTY,	Tile.EMPTY,		Tile.EMPTY,		Tile.COUNTER,	Tile.COUNTER,	Tile.EMPTY,		Tile.EMPTY},		//4
		{Tile.TRASH,	Tile.EMPTY,		Tile.EMPTY,		Tile.COUNTER,	Tile.COUNTER,	Tile.EMPTY,		Tile.EMPTY},		//5
		{Tile.EMPTY,	Tile.EMPTY,		Tile.EMPTY,		Tile.COUNTER,	Tile.COUNTER,	Tile.EMPTY,		Tile.VEGETABLES},	//6
		{Tile.DOUGH,	Tile.DOUGH,		Tile.EMPTY,		Tile.EMPTY,		Tile.EMPTY,		Tile.EMPTY,		Tile.VEGETABLES},	//7
		{Tile.EMPTY,	Tile.EMPTY,		Tile.EMPTY,		Tile.EMPTY,		Tile.EMPTY,		Tile.EMPTY,		Tile.EMPTY},		//8
		{Tile.WALL,		Tile.WALL,		Tile.WALL,		Tile.PICKUP,	Tile.PICKUP,	Tile.PICKUP,	Tile.PICKUP},		//9
	};

	private Item[][] items = new Item[9][7];
	private List<Pizza> orders;
	private Player[] players;
	private int stepCount = 0;

	private static final Location[] startingLocations = {new Location(4, 2), new Location(2, 5), new Location(5, 3), new Location(3, 6)};

	public Engine(int playerCount)
	{
		players = new Player[playerCount];
		for (int i = 0; i < playerCount; i++)
			players[i] = new Player(startingLocations[i]);
		
		orders = new ArrayList<Pizza>();
	}
	
	public GraphicsCommunicationObject getStanding()
	{
		GraphicsCommunicationObject graphics = new GraphicsCommunicationObject();
		
		for (int playerNumber = 0; playerNumber < players.length; playerNumber++)
			for (int step = 0; step < 4; step++)
				graphics.add(step, new GraphicsCommunicationObject.StandElement(playerNumber, players[playerNumber].loc.x, players[playerNumber].loc.y, players[playerNumber].makeHeldGraphicsItem()));
		
		return graphics;
	}

	public GraphicsCommunicationObject performTurn(Action[][] actions)
	{
		GraphicsCommunicationObject graphics = new GraphicsCommunicationObject();

		for (int currentStep = 0; currentStep < 4; currentStep++)
		{
			for (int playerNumber = 0; playerNumber < players.length; playerNumber++)
			{
				graphics.add(currentStep, performAction(currentStep, playerNumber, actions));
			}

			updateWorld(currentStep, actions, graphics);

			for (Pizza p:orders)
			{
				graphics.add(currentStep, new GraphicsCommunicationObject.OrderElement(p));
			}
			stepCount++;
		}

		return graphics;
	}

	private GraphicsCommunicationObject.GraphicsElement performAction(int currentStep, int playerNumber, Action[][] actions)
	{
		Action currentAction = actions[playerNumber][currentStep];
		Player currentPlayer = players[playerNumber];
		Location targetLoc = players[playerNumber].loc.getAdjacent(currentAction.direction);

		switch (currentAction.type)
		{
			case USE:
				if (!isValidLocation(targetLoc))
				{
					return new GraphicsCommunicationObject.StandElement(playerNumber,  currentPlayer.loc.x, currentPlayer.loc.y, currentPlayer.makeHeldGraphicsItem());
				}
				else if (currentPlayer.held == null)
				{
					if (getItem(targetLoc) != null)
					{
						currentPlayer.held = getItem(targetLoc);
						setItem(targetLoc, null);

						return new GraphicsCommunicationObject.PickUpElement(playerNumber, currentPlayer.loc.x, currentPlayer.loc.y, currentAction.direction, currentPlayer.makeHeldGraphicsItem());
					}
					else
					{
						switch(getTile(targetLoc))
						{
							case DOUGH: currentPlayer.held = new Pizza(); break;
							case MEAT: currentPlayer.held = new Meat(); break;
							case VEGETABLES: currentPlayer.held = new Vegetables(); break;
							default: return new GraphicsCommunicationObject.StandElement(playerNumber, currentPlayer.loc.x, currentPlayer.loc.y, currentPlayer.makeHeldGraphicsItem());
						}
						
						return new GraphicsCommunicationObject.UseElement(playerNumber, currentPlayer.loc.x, currentPlayer.loc.y, currentAction.direction, currentPlayer.makeHeldGraphicsItem());
					}
				}
				else if (currentPlayer.held instanceof Pizza)
				{
					Pizza heldPizza = (Pizza) currentPlayer.held;

					if (getItem(targetLoc) != null)
					{
						Item targetItem = getItem(targetLoc);

						if (targetItem instanceof Meat && !heldPizza.meat && heldPizza.isRaw())
							heldPizza.meat = true;
						else if (targetItem instanceof Vegetables && !heldPizza.vegetables && heldPizza.isRaw())
							heldPizza.vegetables = true;
						else
							return new GraphicsCommunicationObject.StandElement(playerNumber, currentPlayer.loc.x, currentPlayer.loc.y, currentPlayer.makeHeldGraphicsItem());

						setItem(targetLoc, null);
						return new GraphicsCommunicationObject.PickUpElement(playerNumber, currentPlayer.loc.x, currentPlayer.loc.y, currentAction.direction, currentPlayer.makeHeldGraphicsItem());
					}
					else
					{
						Tile targetTile = getTile(targetLoc);

						if (targetTile == Tile.TRASH)
							currentPlayer.held = null;
						else if (targetTile == Tile.SAUCE && !heldPizza.cheese && !heldPizza.meat && !heldPizza.vegetables && heldPizza.isRaw())
							heldPizza.sauce = true;
						else if (targetTile == Tile.CHEESAUCE && !heldPizza.cheese && !heldPizza.meat && !heldPizza.vegetables && heldPizza.isRaw())
							heldPizza.sauce = heldPizza.cheese = true;
						else if (targetTile == Tile.CHEESE && !heldPizza.meat && !heldPizza.vegetables && heldPizza.isRaw())
							heldPizza.cheese = true;
						else if (targetTile == Tile.COUNTER || targetTile == Tile.PICKUP || targetTile == Tile.OVEN || targetTile == Tile.EMPTY)
						{
							setItem(targetLoc, heldPizza);
							currentPlayer.held = null;

							return new GraphicsCommunicationObject.DropElement(playerNumber, currentPlayer.loc.x, currentPlayer.loc.y, currentAction.direction);
						}
						else
							return new GraphicsCommunicationObject.StandElement(playerNumber, currentPlayer.loc.x, currentPlayer.loc.y, currentPlayer.makeHeldGraphicsItem());

						return new GraphicsCommunicationObject.UseElement(playerNumber, currentPlayer.loc.x, currentPlayer.loc.y, currentAction.direction, currentPlayer.makeHeldGraphicsItem());
					}
				}
				else if (currentPlayer.held instanceof Meat)
				{
					Meat heldMeat = (Meat) currentPlayer.held;

					if (getItem(targetLoc) != null)
					{
						Item targetItem = getItem(targetLoc);

						if (targetItem instanceof Pizza && !((Pizza) targetItem).meat && ((Pizza) targetItem).isRaw())
						{
							((Pizza) targetItem).meat = true;
							currentPlayer.held = null;

							return new GraphicsCommunicationObject.DropElement(playerNumber, currentPlayer.loc.x, currentPlayer.loc.y, currentAction.direction);
						}
						else
							return new GraphicsCommunicationObject.StandElement(playerNumber, currentPlayer.loc.x, currentPlayer.loc.y, currentPlayer.makeHeldGraphicsItem());
					}
					else
					{
						Tile targetTile = getTile(targetLoc);

						if (targetTile == Tile.TRASH)
							currentPlayer.held = null;
						else if (targetTile == Tile.COUNTER || targetTile == Tile.PICKUP || targetTile == Tile.EMPTY)
						{
							setItem(targetLoc, heldMeat);
							currentPlayer.held = null;

							return new GraphicsCommunicationObject.DropElement(playerNumber, currentPlayer.loc.x, currentPlayer.loc.y, currentAction.direction);
						}
						else
							return new GraphicsCommunicationObject.StandElement(playerNumber, currentPlayer.loc.x, currentPlayer.loc.y, currentPlayer.makeHeldGraphicsItem());
					}
				}
				else if (currentPlayer.held instanceof Vegetables)
				{
					Vegetables heldVegetables = (Vegetables) currentPlayer.held;

					if (getItem(targetLoc) != null)
					{
						Item targetItem = getItem(targetLoc);

						if (targetItem instanceof Pizza && !((Pizza) targetItem).vegetables && ((Pizza) targetItem).isRaw())
						{
							((Pizza) targetItem).vegetables = true;
							currentPlayer.held = null;

							return new GraphicsCommunicationObject.DropElement(playerNumber, currentPlayer.loc.x, currentPlayer.loc.y, currentAction.direction);
						}
						else
							return new GraphicsCommunicationObject.StandElement(playerNumber, currentPlayer.loc.x, currentPlayer.loc.y, currentPlayer.makeHeldGraphicsItem());
					}
					else
					{
						Tile targetTile = getTile(targetLoc);

						if (targetTile == Tile.TRASH)
							currentPlayer.held = null;
						else if (targetTile == Tile.COUNTER || targetTile == Tile.PICKUP || targetTile == Tile.EMPTY)
						{
							setItem(targetLoc, heldVegetables);
							currentPlayer.held = null;

							return new GraphicsCommunicationObject.DropElement(playerNumber, currentPlayer.loc.x, currentPlayer.loc.y, currentAction.direction);
						}
						else
							return new GraphicsCommunicationObject.StandElement(playerNumber, currentPlayer.loc.x, currentPlayer.loc.y, currentPlayer.makeHeldGraphicsItem());
					}
				}
				else
					return null;
			case MOVE:
				if (isValidLocation(targetLoc) && getTile(targetLoc) == Tile.EMPTY)
				{
					GraphicsCommunicationObject.MoveElement graphicsElement = new GraphicsCommunicationObject.MoveElement(playerNumber, currentPlayer.loc.x, currentPlayer.loc.y, currentAction.direction, currentPlayer.makeHeldGraphicsItem());

					if (getItem(targetLoc) != null)
					{
						setItem(targetLoc, null);
						stunPlayer(playerNumber, currentStep, actions);
						graphicsElement.stunnedAfter = true;
					}

					currentPlayer.loc = targetLoc;
					return graphicsElement;
				}
				else
				{
					stunPlayer(playerNumber, currentStep, actions);

					return new GraphicsCommunicationObject.CollideElement(playerNumber, currentPlayer.loc.x, currentPlayer.loc.y, currentAction.direction, currentPlayer.makeHeldGraphicsItem());
				}
			case THROW:
				if (currentPlayer.held instanceof Pizza && isValidLocation(targetLoc) && (getTile(targetLoc) == Tile.EMPTY || getTile(targetLoc) == Tile.COUNTER))
				{
					Location adjacentLoc = currentPlayer.loc.getAdjacent(currentAction.direction);

					setItem(adjacentLoc, currentPlayer.held);
					((Pizza) getItem(adjacentLoc)).flightDirection = currentAction.direction;
					currentPlayer.held = null;

					return new GraphicsCommunicationObject.ThrowElement(playerNumber, currentPlayer.loc.x, currentPlayer.loc.y, currentAction.direction);
				}
				else
					return new GraphicsCommunicationObject.StandElement(playerNumber, currentPlayer.loc.x, currentPlayer.loc.y, currentPlayer.makeHeldGraphicsItem());
			case STAND:
			{
				boolean stunned = currentPlayer.isStunned;
				currentPlayer.isStunned = false;
				return new GraphicsCommunicationObject.StandElement(playerNumber,  currentPlayer.loc.x, currentPlayer.loc.y, currentPlayer.makeHeldGraphicsItem(), stunned);
			}
			default:
				return null;
		}

	}

	//Preforms action independent updates to the world (eg. removing delivered pizzas, pizzas cooking, pizzas flying, player collisions)
	private void updateWorld(int currentStep, Action[][] actions, GraphicsCommunicationObject graphics)
	{
		for (int y = 0; y < map.length; y++)
			for (int x = 0; x < map[0].length; x++)
			{
				Location currentLoc = new Location(x, y);

				if (getItem(currentLoc) != null)
				{
					Item currentItem = getItem(currentLoc);

					int player = getPlayerAt(currentLoc);
					if (player != -1)
					{
						stunPlayer(player, currentStep, actions);
						graphics.addCollision(currentStep, player, false);
						setItem(currentLoc, null);
					}
					else if (currentItem instanceof Pizza)
					{
						Pizza currentPizza = ((Pizza) currentItem);

						if (currentPizza.flightDirection != Direction.NONE)
						{
							setItem(currentLoc, null);

							int distance = -1;
							Location flightLoc = new Location(currentLoc);
							boolean impacted = false;
							while(!impacted)
							{
								Location nextLoc = flightLoc.getAdjacent(currentPizza.flightDirection);
								distance++;

								player = getPlayerAt(flightLoc);

								if (player != -1)
								{
									stunPlayer(player, currentStep, actions);
									graphics.addCollision(currentStep, player, false);
									impacted = true;
								}
								else if (!isValidLocation(nextLoc) || (getTile(nextLoc) != Tile.EMPTY && getTile(nextLoc) != Tile.COUNTER && getTile(nextLoc) != Tile.OVEN) || getTile(flightLoc) == Tile.OVEN)
								{
									setItem(flightLoc, currentPizza);
									impacted = true;
								}
								flightLoc = nextLoc;
							}
							graphics.add(currentStep, new GraphicsCommunicationObject.FlyElement(currentLoc.x, currentLoc.y, currentPizza.flightDirection, distance, currentPizza.makeGraphicsItem()));

							currentPizza.flightDirection = Direction.NONE;
						}
						else
						{
							if (getTile(currentLoc) == Tile.PICKUP  && orders.contains(currentPizza))
							{
								orders.remove(currentPizza);
								setItem(currentLoc, null);
							}
							else
							{
								if (getTile(currentLoc) == Tile.OVEN)
								{
									currentPizza.cookedness++;
								}
	
								graphics.add(currentStep, new GraphicsCommunicationObject.SitElement(currentLoc.x, currentLoc.y, currentItem.makeGraphicsItem()));
							}
						}
					}
					else
						graphics.add(currentStep, new GraphicsCommunicationObject.SitElement(currentLoc.x, currentLoc.y, currentItem.makeGraphicsItem()));
				}
			}
		
		if (stepCount % 15 == 0)
		{
			orders.add(Pizza.randomPizza());
		}

		while (handleCollision(currentStep, actions, graphics));
	}

	private void stunPlayer(int playerNumber, int currentStep, Action[][] actions)
	{
		if (currentStep < 3)
		{
			players[playerNumber].isStunned = true;
			if (actions[playerNumber][currentStep].type != ActionType.STAND)
			{
				for (int step = 3; step > currentStep + 1; step--)
					actions[playerNumber][step] = actions[playerNumber][step-1];

				actions[playerNumber][currentStep+1] = new Action(ActionType.STAND, Direction.NONE);
			}
		}
	}

	//Loops through players until it finds a collision, then resolves it. Returns true if a collision is found, false otherwise
	//Should be called repeatedly until all collisions are resolved
	private boolean handleCollision(int currentStep, Action[][] actions, GraphicsCommunicationObject graphics)
	{
		for (int playerNumber = 0; playerNumber < players.length; playerNumber++)
			for (int otherPlayer = playerNumber + 1; otherPlayer < players.length; otherPlayer++)
				if (players[playerNumber].loc.equals(players[otherPlayer].loc))
				{
					if (actions[playerNumber][currentStep].type == ActionType.MOVE)
					{
						players[playerNumber].loc = players[playerNumber].loc.getAdjacent(actions[playerNumber][currentStep].direction.reverse());

						graphics.addCollision(currentStep, playerNumber, true);
					}
					else
						graphics.addCollision(currentStep, playerNumber, false);
					
					stunPlayer(playerNumber, currentStep, actions);

					if (actions[otherPlayer][currentStep].type == ActionType.MOVE)
					{
						players[otherPlayer].loc = players[otherPlayer].loc.getAdjacent(actions[otherPlayer][currentStep].direction.reverse());

						graphics.addCollision(currentStep, otherPlayer, true);
					}
					else
						graphics.addCollision(currentStep, otherPlayer, false);

					stunPlayer(otherPlayer, currentStep, actions);
					
					return true;
				}

		return false;
	}

	private Tile getTile(Location loc)
	{
		return map[loc.y][loc.x];
	}

	private Item getItem(Location loc)
	{
		return items[loc.y][loc.x];
	}

	private void setItem(Location loc, Item item)
	{
		items[loc.y][loc.x] = item;
	}

	private int getPlayerAt(Location loc)
	{
		for (int i = 0; i < players.length; i++)
			if (players[i].loc.equals(loc))
				return i;
		return -1;
	}

	private boolean isValidLocation(Location loc)
	{
		return !(loc.x < 0 || loc.y < 0 || loc.y >= map.length || loc.x >= map[0].length);
	}
}