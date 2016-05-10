import java.util.*;
import java.net.*;
import java.util.concurrent.*;
import java.io.*;

public class SServer implements Runnable
{
    private final           ServerSocket      serverSocket;
    private final           ExecutorService   pool;
    private static final    int               port = 51515;

    public SServer (int port, int poolSize) throws IOException
    {
        serverSocket = new ServerSocket (port);
        pool = Executors.newFixedThreadPool (poolSize);
    }

    public void run ()
    {
        try {
            for (;;) {
                pool.execute (new Handler (serverSocket.accept ()));
            }
        } catch (IOException ex) {
            pool.shutdown ();
        }
    }

    static class Handler implements Runnable, Comparable <Handler>
    {
        private final Socket            socket;
        private final PrintStream       out;
        private final Scanner           input;

        private volatile static Set <Handler>    allClients =
            new ConcurrentSkipListSet <Handler> ();

        Handler (Socket socket) throws IOException
        {
            this.socket = socket;

            InputStream is = socket.getInputStream ();
            input = new Scanner (is);

            out = new PrintStream (socket.getOutputStream ());
            out.flush ();

            allClients.add (this);
        }

        public void run ()
        {
            out.println ("Hi! Welcome to chat.");

            String lastMessage;

            do {
                lastMessage = input.nextLine ();
                
                for (Handler h : Handler.allClients) {
                    if (!h.equals (this)) {
                        h.sendMessage (lastMessage);
                    }
                }
            } while (lastMessage.indexOf ("quit") != 0);

            out.println ("Quitting...");

            out.close ();

            allClients.remove (this);
        }

        public synchronized void sendMessage (String m)
        {
            out.println (m);
        }

        public int compareTo (Handler other)
        {
            return 1;
        }
    }

    /** The main server entry point.
        @throws Exception when it's not done.
     */
    public static void main (String [] args) throws Exception
    {
        SServer s = new SServer (SServer.port,100);

        s.run ();
    }
}
