package com.pizzarizzia;

import java.io.IOException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;


public class GsonDecoder
{
	public Gson gson;
	public Server server;

	private class PacketAdapter extends TypeAdapter<Server.IncomingPacket>
	{
		public Server.IncomingPacket read(JsonReader reader) throws IOException
		{
			JsonParser parser = new JsonParser();

			JsonObject object = parser.parse(reader).getAsJsonObject();

			Server.IncomingPacket.PacketType type = Server.IncomingPacket.PacketType.valueOf(object.get("type").getAsString());
			
			switch (type)
			{
			case END_OF_ROUND_ACTIONS: return server.new EndOfRoundActionsPacket(gson.fromJson(object.get("actions"), Engine.Action[].class));
			case START_GAME: return server.new StartGamePacket();
			case UPDATE_PLAYER_ACTION :return server.new UpdatePlayerActionPacket(gson.fromJson(object.get("actions"), Engine.Action[].class));
			default: return null;
			}
		}

		@Override
		public void write(JsonWriter writer, Server.IncomingPacket packet) throws IOException
		{
			// TODO Auto-generated method stub
			new Gson().toJson(packet, packet.getClass(), writer);
		}
	}

	public GsonDecoder(Server s)
	{
		gson = new GsonBuilder()
		.registerTypeAdapter(Server.IncomingPacket.class, new PacketAdapter().nullSafe())
		.setPrettyPrinting()
		.serializeNulls()
		.create();
		
		server = s;
	}

	public Server.IncomingPacket decode(String in)
	{
		return gson.fromJson(in, Server.IncomingPacket.class);
	}
}
