package com.pizzarizzia;

import javax.websocket.server.ServerEndpointConfig.Configurator;


public class ServerConfigurator extends Configurator
{
	public static final Server serv = new Server();
	
	@Override
	public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException
	{
		if(endpointClass.equals(Server.class))
		{
			return (T) serv;
		}
		
		throw new InstantiationException();
	}
}
