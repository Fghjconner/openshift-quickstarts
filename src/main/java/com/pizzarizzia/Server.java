package com.pizzarizzia;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.websocket.EncodeException;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/game", encoders = GsonEncoder.class, configurator = ServerConfigurator.class)
// @ServerEndpoint("/echo")
public class Server
{
	volatile Session[] playerConnections;
	Engine.Action[][] lastActions;
	Engine engine;
	volatile ServerState state;
	Timer endOfRoundTimer;
	static GsonDecoder decoder;

	public Server()
	{
		decoder = new GsonDecoder(this);
		resetServer();
		
		System.out.println("Made a new Server object :(");
	}
	
	public void resetServer()
	{
		playerConnections = new Session[4];
		lastActions = new Engine.Action[4][];
		state = ServerState.WAITING_TO_START;
		
		if (endOfRoundTimer != null)
			endOfRoundTimer.cancel();
		endOfRoundTimer = new Timer();
	}

	@OnOpen
	public void openHandler(Session session)
	{
		if (state == ServerState.WAITING_TO_START)
		{
			int i = 0;
			while (i < getMaxPlayers())
			{
				if (playerSessionEquals(i, null))
				{
					sendPacketToConnection(session, new JoinResponsePacket(true));
					setPlayer(i, session);
					sendPacketToPlayer(i, new PlayerNumberUpdatePacket(i));
					break;
				}
				i++;
			}
			if (i == getMaxPlayers())
				sendPacketToConnection(session, new JoinResponsePacket(false));
		} else
			sendPacketToConnection(session, new JoinResponsePacket(false));
	}

	
	@OnMessage
	public void onMessageReceived(String in, Session session)
	{
		IncomingPacket input = decoder.decode(in);

		for (int i = 0; i < getMaxPlayers(); i++)
			if (playerSessionEquals(i, session))
			{
				input.process(session, i);
				return;
			}
	}
	
	@OnClose
	public void onConnectionClose(Session session)
	{
		for (int i = 0; i < getMaxPlayers(); i++)
			if (playerSessionEquals(i, session))
			{
				setPlayer(i, null);
			}
		
		for (int i = 0; i < getMaxPlayers(); i++)
			if (!playerSessionEquals(i, null))
				return;
		
		resetServer();
	}

	private synchronized int getMaxPlayers()
	{
		return playerConnections.length;
	}

	private synchronized boolean playerSessionEquals(int player, Session session)
	{
		return playerConnections[player] == session;
	}

	private synchronized void setPlayer(int player, Session session)
	{
		playerConnections[player] = session;
	}

	private synchronized void sendPacketToPlayer(int player, OutgoingPacket out)
	{
		if (!sendPacketToConnection(playerConnections[player], out))
			playerConnections[player] = null;
	}

	private synchronized boolean sendPacketToConnection(Session session, OutgoingPacket out)
	{
		try
		{
			if (session != null)
				session.getBasicRemote().sendObject(out);
		} catch (IOException e)
		{
			try
			{
				session.close();
			} catch (IOException e1)
			{
				e1.printStackTrace();
			}
			e.printStackTrace();
			return false;
		}
		catch (EncodeException e)
		{
			try
			{
				session.close();
			} catch (IOException e1)
			{
				e1.printStackTrace();
			}
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public static enum ServerState
	{
		WAITING_TO_START, IN_ROUND, POST_ROUND
	}

	private class StartRoundTask extends TimerTask
	{
		@Override
		public void run()
		{
			state = ServerState.IN_ROUND;

			endOfRoundTimer.schedule(new EndRoundTask(), 60000);

			for (int i = 0; i < getMaxPlayers(); i++)
				sendPacketToPlayer(i, new RoundStartPacket(60));
		}
	}

	private class EndRoundTask extends TimerTask
	{
		@Override
		public void run()
		{
			state = ServerState.POST_ROUND;

			for (int i = 0; i < getMaxPlayers(); i++)
				sendPacketToPlayer(i, new RoundEndPacket());
		}
	}

	public static abstract class IncomingPacket
	{
		public enum PacketType
		{
			UPDATE_PLAYER_ACTION, END_OF_ROUND_ACTIONS, START_GAME;
		}

		public PacketType type;

		public IncomingPacket(PacketType t)
		{
			type = t;
		}

		public abstract void process(Session session, int sourcePlayer);
	}

	// Update packet sent to the server whenever the user changes an action.
	// Allows all other clients to display current action choices.
	public class UpdatePlayerActionPacket extends IncomingPacket
	{
		public Engine.Action[] actions;

		public UpdatePlayerActionPacket(Engine.Action[] acts)
		{
			super(PacketType.UPDATE_PLAYER_ACTION);
			
			actions = acts;
		}

		@Override
		public void process(Session session, int sourcePlayer)
		{
			if (state == ServerState.IN_ROUND)
				for (int i = 0; i < getMaxPlayers(); i++)
				{
					sendPacketToPlayer(i, new SharePlayerActionPacket(this, sourcePlayer));
				}
		}
	}

	// Packet sent by client at end of round with finalized actions. Ensures
	// synchronization between clients and server
	public class EndOfRoundActionsPacket extends IncomingPacket
	{
		public Engine.Action[] actions;

		public EndOfRoundActionsPacket(Engine.Action[] acts)
		{
			super(PacketType.END_OF_ROUND_ACTIONS);
			
			actions = acts;
		}

		@Override
		public void process(Session session, int sourcePlayer)
		{
			if (state == ServerState.POST_ROUND)
			{
				lastActions[sourcePlayer] = new Engine.Action[4];

				for (int i = 0; i < 4; i++)
					lastActions[sourcePlayer][i] = actions[i];

				for (int i = 0; i < 4; i++)
					if (!playerSessionEquals(i, null) && lastActions[i] == null)
						return;

				GraphicsCommunicationObject gco = engine.performTurn(lastActions);
				for (int i = 0; i < 4; i++)
				{
					sendPacketToPlayer(i, gco);
				}
				
				for (int i = 0; i < 4; i++)
					lastActions[i] = null;

				endOfRoundTimer.schedule(new StartRoundTask(), 4000);
			}
		}
	}

	public class StartGamePacket extends IncomingPacket
	{
		public StartGamePacket()
		{
			super(PacketType.START_GAME);
		}

		@Override
		public void process(Session session, int sourcePlayer)
		{
			if (state == ServerState.WAITING_TO_START && sourcePlayer == 0)
			{
				int playerCount = 0;
				for (int i = 0; i < getMaxPlayers(); i++)					
					if (!playerSessionEquals(i, null))
						playerCount++;

				engine = new Engine(playerCount);
				
				GraphicsCommunicationObject packet = engine.getStanding();
				
				for (int i = 0; i < getMaxPlayers(); i++)
					sendPacketToPlayer(i, packet);

				new StartRoundTask().run();
			}
		}

	}

	public static abstract class OutgoingPacket
	{
		public enum PacketType
		{
			JOIN_RESPONSE, SHARE_PLAYER_ACTION, ROUND_START, ROUND_END, PLAYER_NUMBER_UPDATE, GRAPHICS
		}

		public PacketType type;

		public OutgoingPacket(PacketType t)
		{
			type = t;
		}
	}

	public class JoinResponsePacket extends OutgoingPacket
	{
		public boolean response;

		public JoinResponsePacket(boolean r)
		{
			super(PacketType.JOIN_RESPONSE);

			response = r;
		}
	}

	public class SharePlayerActionPacket extends OutgoingPacket
	{
		public Engine.Action[] actions;
		int player;

		public SharePlayerActionPacket(UpdatePlayerActionPacket otherPacket, int playerNumber)
		{
			super(PacketType.SHARE_PLAYER_ACTION);

			player = playerNumber;
			
			actions = new Engine.Action[4];
			for (int i = 0; i < 4; i++)
				actions[i] = otherPacket.actions[i];
		}
	}

	public class RoundStartPacket extends OutgoingPacket
	{
		public int duration;

		public RoundStartPacket(int dur)
		{
			super(PacketType.ROUND_START);

			duration = dur;
		}
	}

	public class PlayerNumberUpdatePacket extends OutgoingPacket
	{
		int playerNumber;

		public PlayerNumberUpdatePacket(int pNum)
		{
			super(PacketType.PLAYER_NUMBER_UPDATE);

			playerNumber = pNum;
		}
	}

	public class RoundEndPacket extends OutgoingPacket
	{
		public RoundEndPacket()
		{
			super(PacketType.ROUND_END);
		}
	}
}