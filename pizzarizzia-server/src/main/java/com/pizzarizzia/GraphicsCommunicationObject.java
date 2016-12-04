package com.pizzarizzia;

import com.google.gson.*;
import com.google.gson.reflect.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class GraphicsCommunicationObject extends Server.OutgoingPacket
{
//	private class GraphicsElementGsonAdapter extends TypeAdapter<GraphicsElement>
//	{
//		private final Map<String, Type> types;
//
//		public GraphicsElementGsonAdapter()
//		{
//			types = new HashMap<String, Type>();
//
//			types.put("stand", StandElement.class);
//			types.put("move", MoveElement.class);
//			types.put("collide", CollideElement.class);
//			types.put("throw", ThrowElement.class);
//			types.put("drop", DropElement.class);
//			types.put("pick up", PickUpElement.class);
//			types.put("use", UseElement.class);
//			types.put("catch", CatchElement.class);
//			types.put("fly", FlyElement.class);
//			types.put("sit", SitElement.class);
//		}
//
//		@Override
//		public GraphicsElement read(JsonReader reader) throws IOException
//		{
//			JsonParser parser = new JsonParser();
//
//			JsonElement object = parser.parse(reader);
//
//			String typeName = object.getAsJsonObject().get("type").getAsString();
//
//
//			return getGsonObject().fromJson(object, types.get(typeName));
//		}
//
//		@Override
//		public void write(JsonWriter writer, GraphicsElement value) throws IOException
//		{
//			new Gson().toJson(value, value.getClass(), writer);
//		}
//	}
//
//	private class ItemGsonAdapter extends TypeAdapter<Item>
//	{
//		private final Map<String, Type> types;
//
//		public ItemGsonAdapter()
//		{
//			types = new HashMap<String, Type>();
//
//			types.put("pizza", Pizza.class);
//			types.put("meat", Meat.class);
//			types.put("vegetables", Vegetables.class);
//		}
//
//		@Override
//		public Item read(JsonReader reader) throws IOException
//		{
//			JsonParser parser = new JsonParser();
//
//			JsonElement object = parser.parse(reader);
//
//			String typeName = object.getAsJsonObject().get("type").getAsString();
//
//
//			return getGsonObject().fromJson(object, types.get(typeName));
//		}
//
//		@Override
//		public void write(JsonWriter writer, Item value) throws IOException
//		{
//			new Gson().toJson(value, value.getClass(), writer);
//		}
//	}





	public abstract static class Item
	{
		public String type;
	}

	public static class Pizza extends Item
	{
		public enum CookedState {RAW, COOKED, BURNT}

		public boolean sauce = false;
		public boolean cheese = false;
		public boolean meat = false;
		public boolean vegetables = false;
		public CookedState cooked = CookedState.RAW;

		public Pizza()
		{
			type = "pizza";
		}

		public Pizza(Engine.Pizza pizza)
		{
			this();
			
			sauce = pizza.sauce;
			cheese = pizza.cheese;
			meat = pizza.meat;
			vegetables = pizza.vegetables;

			cooked = pizza.isBurnt() ? CookedState.BURNT : (pizza.isCooked() ? CookedState.COOKED : CookedState.RAW);
		}
	}

	public static class Meat extends Item
	{
		public Meat()
		{
			type = "meat";
		}
	}

	public static class Vegetables extends Item
	{
		public Vegetables()
		{
			type = "vegetables";
		}
	}




	public abstract static class GraphicsElement
	{
		protected String type;

		public GraphicsElement(String elementType)
		{
			type = elementType;
		}
	}
	
	public abstract static class LocatedGraphicsElement extends GraphicsElement
	{
		public int locationX;
		public int locationY;

		public LocatedGraphicsElement(String elementType, int x, int y)
		{
			super(elementType);

			locationX = x;
			locationY = y;
		}
	}

	public abstract static class PlayerGraphicsElement extends LocatedGraphicsElement
	{
		public Item held;
		public int player;
		public boolean stunnedAfter;

		public PlayerGraphicsElement(String elementType, int playerNumber, int x, int y, Item heldItem)
		{
			super(elementType, x, y);
			player = playerNumber;
			held = heldItem;
			stunnedAfter = false;
		}
	}

	public static class StandElement extends PlayerGraphicsElement
	{
		public StandElement(int playerNumber, int x, int y, Item heldItem)
		{
			super("stand", playerNumber, x, y, heldItem);
		}
	}

	public static class MoveElement extends PlayerGraphicsElement
	{
		public Engine.Direction direction;

		public MoveElement(int playerNumber, int x, int y, Engine.Direction dir, Item heldItem)
		{
			super("move", playerNumber, x, y, heldItem);
			direction = dir;
		}
	}

	public static class CollideElement extends PlayerGraphicsElement
	{
		public Engine.Direction direction;

		public CollideElement(int playerNumber, int x, int y, Engine.Direction dir, Item heldItem)
		{
			super("collide", playerNumber, x, y, heldItem);
			stunnedAfter = true;
			direction = dir;
		}
	}

	public static class ThrowElement extends PlayerGraphicsElement
	{
		public Engine.Direction direction;

		public ThrowElement(int playerNumber, int x, int y, Engine.Direction dir)
		{
			super("throw", playerNumber, x, y, null);
			direction = dir;
		}
	}

	public static class DropElement extends PlayerGraphicsElement
	{
		public Engine.Direction direction;

		public DropElement(int playerNumber, int x, int y, Engine.Direction dir)
		{
			super("drop", playerNumber, x, y, null);
			direction = dir;
		}
	}

	public static class PickUpElement extends PlayerGraphicsElement
	{
		public Engine.Direction direction;

		public PickUpElement(int playerNumber, int x, int y, Engine.Direction dir, Item heldItem)
		{
			super("pick up", playerNumber, x, y, heldItem);
			direction = dir;
		}
	}

	public static class UseElement extends PlayerGraphicsElement
	{
		public Engine.Direction direction;

		public UseElement(int playerNumber, int x, int y, Engine.Direction dir, Item heldItem)
		{
			super("use", playerNumber, x, y, heldItem);
			direction = dir;
		}
	}

	public static class CatchElement extends PlayerGraphicsElement
	{
		public Engine.Direction direction;

		public CatchElement(int playerNumber, int x, int y, Engine.Direction dir, Item heldItem)
		{
			super("catch", playerNumber, x, y, heldItem);
			direction = dir;
		}
	}

	public static class FlyElement extends LocatedGraphicsElement
	{
		public Engine.Direction direction;
		public int distance;
		public Item item;

		public FlyElement(int x, int y, Engine.Direction dir, int dist, Item flyingItem)
		{
			super("fly", x, y);
			direction = dir;
			distance = dist;
			item = flyingItem;
		}
	}

	public static class SitElement extends LocatedGraphicsElement
	{
		public Item item;

		public SitElement(int x, int y, Item sittingItem)
		{
			super("sit", x, y);
			item = sittingItem;
		}
	}
	
	public static class OrderElement extends GraphicsElement
	{
		public Pizza order;
		
		public OrderElement(Pizza p)
		{
			super("order");
			
			order = p;
		}
		
		public OrderElement(Engine.Pizza p)
		{
			this(new Pizza(p));
		}
	}






	public List<List<GraphicsElement>> data;
	private static GsonBuilder builder = new GsonBuilder()
//		.setPrettyPrinting()
		.serializeNulls();

	private Gson getGsonObject()
	{
		return builder.create();
	}

	public GraphicsCommunicationObject()
	{
		super(PacketType.GRAPHICS);

		data = new ArrayList<List<GraphicsElement>>();
		for (int i = 0; i < 4; i++)
			data.add(new ArrayList<GraphicsElement>());
	}

	public GraphicsCommunicationObject(String json)
	{
		this();
		Type dataType = new TypeToken<List<List<GraphicsElement>>>(){}.getType();
		data = getGsonObject().fromJson(json, dataType);
	}

	public void addCollision(int step, int playerNumber, boolean knockback)
	{
		for (int elementNumber = 0; elementNumber < data.get(step).size(); elementNumber++)
		{
			GraphicsElement element = data.get(step).get(elementNumber);

			if (element instanceof PlayerGraphicsElement && ((PlayerGraphicsElement) element).player == playerNumber)
				if (element instanceof MoveElement && knockback)
				{
					MoveElement moveElement = (MoveElement) element;
					data.get(step).set(elementNumber, new CollideElement(moveElement.player, moveElement.locationX, moveElement.locationY, moveElement.direction, moveElement.held));
				}
				else
					((PlayerGraphicsElement) element).stunnedAfter = true;
		}
	}

	public void add(int step, GraphicsElement element)
	{
		data.get(step).add(element);
	}

	public String toJson()
	{
		return getGsonObject().toJson(data);
	}
}