package it.zerozero.athome_app;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by David on 01/10/2017.
 */

public class TCPServer {

    ServerSocket ss = null;
    Socket s = null;
    public final int SERVER_INIT = 7000;
    public final int SERVER_CREATED = 7002;
    public final int SERVER_CONNECTED = 7004;
    public final int SERVER_WAITING = 7003;
    public final int SERVER_DONE = 7005;
    public final int SERVER_ERROR = 7007;
    public int Status = 0;
    private int port = 0;
    public static final int PACKAGE_RCV = 909;

    public TCPServer(int port) {
        this.port = port;
        Status = SERVER_CREATED;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public String listenConn() {
        String inStr = null;
        String outStr = null;
        BufferedReader in;
        BufferedWriter out;

        try {
            ss = new ServerSocket(port);
            s = ss.accept();
            Status = SERVER_INIT;
        }
        catch (Exception e) {
            e.printStackTrace();
            Status = SERVER_ERROR;
        }
        Log.i("TCPServer (1) Status", String.valueOf(Status));

        try {
            in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
            Status = SERVER_WAITING;
            Log.i("TCPServer (2) Status", String.valueOf(Status));
            inStr = in.readLine() + System.getProperty("line.separator");
            Status = SERVER_CONNECTED;
            Log.i("TCPServer (3) Status", String.valueOf(Status));
            // Log.i("listenConn", "received " + inStr);
            outStr = "<ConnectedToLedStripDevice>" + System.getProperty("line.separator");
            out.write(outStr);
            out.flush();
            Status = SERVER_DONE;
            Log.i("TCPServer (4) Status", String.valueOf(Status));
            s.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            Status = SERVER_ERROR;
            return "<error1>";
        }
        finally {
            try {
                if (ss != null) {
                    ss.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Status = SERVER_ERROR;
            }
        }
        Log.i("TCPServer (5) Status", String.valueOf(Status));

        return inStr;
    }

    public void close() {
        Log.i("TCPServer", "close()");
        try {
            s.close();
            ss.close();
        } catch (IOException e) {
            e.printStackTrace();
            Status = SERVER_ERROR;
        }

    }

    public static class ServerThread implements Runnable {

        // TODO: 16/01/2018 Set  serverIsOn = true/false

        private Handler hnd;
        private int port;
        private TCPServer tcpServer;

        private String msgString;
        private String msgTimeStamp;
        private Bitmap msgBitmap;

        public ServerThread(Handler hnd, int port) {
            this.hnd = hnd;
            this.port = port;
        }

        @Override
        public void run() {
            Gson gson = new Gson();
            tcpServer = new TCPServer(port);
            if (tcpServer != null) {
                Log.i("ServerThread", "server listening.");
                String incomingStr = tcpServer.listenConn();
                Log.i("incomingStr", incomingStr);
                /*
                MsgPackage mp = gson.fromJson(incomingStr, MsgPackage.class);
                mp.decodeImage();
                appStatus.setCurrentPackage(mp);
                */
                Message handlerMessage = hnd.obtainMessage(); // new Message();
                handlerMessage.what = PACKAGE_RCV;
                Bundle bundle = new Bundle();
                bundle.putString("receivedStr", incomingStr);
                handlerMessage.setData(bundle);
                hnd.sendMessage(handlerMessage);
            }
            else {
                Log.e("ServerThread", "something went wrong.");
            }
        }
    }

}
