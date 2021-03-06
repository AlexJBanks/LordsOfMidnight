package com.lordsofmidnight.server;

import com.lordsofmidnight.utils.Input;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * ClientGameplayHandler class which creates the appropriate senders and recievers and starts them.
 * The sender turns {@link Input} into strings. they feed into telemetry.
 */
public class ClientGameplayHandler {

  public Queue<String> outgoingQueue;
  private Queue<String> incomingQueue;

  private BlockingQueue<Input> keypressQueue;
  private Queue<String> clientIn;

  private Thread outgoingPacketManager;
  private Thread incomingPacketManager;

  private PacketSender sender;
  private PacketReceiver receiver;

  private ArrayList<InetAddress> serverIP;

  private boolean running = true;

  // clientIn gets recievedStrings
  public ClientGameplayHandler(
      InetAddress serverIP, Queue<Input> keypressQueue, Queue<String> clientIn) throws IOException {
    outgoingQueue = new ConcurrentLinkedQueue<>();
    incomingQueue = new ConcurrentLinkedQueue<>();
    this.keypressQueue = (BlockingQueue<Input>) keypressQueue;
    this.clientIn = clientIn;

    this.serverIP = new ArrayList<>();
    this.serverIP.add(serverIP);

    initialisePacketManagers();

    this.sender =
        new PacketSender(NetworkUtility.SERVER_DGRAM_PORT, this.outgoingQueue, this.serverIP);
    this.receiver = new PacketReceiver(NetworkUtility.CLIENT_DGRAM_PORT, incomingQueue);
    this.incomingPacketManager.start();
    this.outgoingPacketManager.start();
    this.receiver.start();
    this.sender.start();
  }

  /**
   * Initialises the incoming and outgoing packet managers
   */
  private void initialisePacketManagers() {
    // puts inputs from queues into the outgoing queue as strings
    this.outgoingPacketManager =
        new Thread() {
          public void run() {
            Input key;
            while (!isInterrupted() && running) {
              try {
                key = keypressQueue.take();
                // sends inputs as strings, which are converted back by ServerGameplay handler
                outgoingQueue.add(key.toString());
                Thread.sleep(1);
              } catch (InterruptedException e) {
                e.printStackTrace();
              }
            }
          }
        };

    // reads inputs from the incoming queue into the relevant queue - position queue or collision
    // queue
    this.incomingPacketManager =
        new Thread() {
          public void run() {
            while (!isInterrupted() && running) {
              try {
                if (incomingQueue.isEmpty()) {
                  continue;
                }
                String data = incomingQueue.poll();

                if (data.startsWith(NetworkUtility.POSITION_CODE)
                    || data.startsWith(NetworkUtility.POWERUP_CODE)
                    || data.startsWith(NetworkUtility.COLLISIONS_CODE)
                    || data.startsWith(NetworkUtility.SCORE_CODE)) {
                  clientIn.add(data);
                } else if (data.startsWith(NetworkUtility.STOP_CODE)) {
                  clientIn.add(data);
                  close();
                } else {
                  System.out.println("Dodgy string " + data);
                  throw new Exception();
                }
                Thread.sleep(1);
              } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Argument in incoming queue had invalid code");
              }
            }
          }
        };
  }

  /**
   * Closes the class' threads
   */
  public void close() {
    receiver.shutdown();
    sender.shutdown();
    running = false;
  }
}
