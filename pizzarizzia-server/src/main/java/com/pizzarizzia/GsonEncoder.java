package com.pizzarizzia;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;



public class GsonEncoder implements Encoder.Text<Server.OutgoingPacket>
{
	Gson gson;

	@Override
	public void init(EndpointConfig config)
	{
		gson = new GsonBuilder()
//		.setPrettyPrinting()
		.serializeNulls()
		.create();
	}

	@Override
	public void destroy()
	{
	}

	@Override
	public String encode(Server.OutgoingPacket packet) throws EncodeException
	{
		return gson.toJson(packet);
	}
}
