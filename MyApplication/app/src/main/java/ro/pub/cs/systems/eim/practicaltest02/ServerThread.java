package ro.pub.cs.systems.eim.practicaltest02;

import android.util.Log;
import android.widget.EditText;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.ResponseHandler;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.BasicResponseHandler;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;

/**
 * Created by bobo on 19.05.2017.
 */

public class ServerThread extends Thread {

    private boolean isRunning;

    private ServerSocket serverSocket;
    private int port;

    private HashMap<String, String> data = new HashMap<String, String>();

    private EditText serverTextEditText;

    public ServerThread(int port) {

        this.port = port;
    }

    public void startServer() {
        isRunning = true;
        start();
        Log.v(Constants.TAG, "startServer() method was invoked");
    }

    public void stopServer() {
        isRunning = false;
        try {
            serverSocket.close();
        } catch (IOException ioException) {
            Log.e(Constants.TAG, "An exception has occurred: " + ioException.getMessage());
            ioException.printStackTrace();

        }
        Log.v(Constants.TAG, "stopServer() method was invoked");
    }

    public synchronized void setData (String word, String anagram) {
        this.data.put(word, anagram);
    }

    public synchronized HashMap<String, String> getData () {
        return data;
    }

    @Override
    public void run() {

        HttpClient httpClient;
        String pageSourceCode;
        String internetURL = "http://services.aonaware.com/CountCheatService/CountCheatService.asmx/LetterSolutions?anagram=";
        try {
            serverSocket = new ServerSocket(port);
            while (isRunning) {
                Socket socket = serverSocket.accept();
                if (socket != null) {
                    //#################################################################
                    BufferedReader br = Utilities.getReader(socket);
                    String queryStr = br.readLine().trim();

                    if (this.getData().containsKey(queryStr)) {
                        String toSend = this.getData().get(queryStr);

                        PrintWriter pw = Utilities.getWriter(socket);
                        pw.write(toSend + "\n");
                        Log.d(Constants.TAG, "[BACKUP] Sending from server: " + toSend);

                        pw.flush();
                        socket.close();

                    } else {

                        httpClient = new DefaultHttpClient();
                        HttpGet httpGet = new HttpGet(internetURL + queryStr);
                        ResponseHandler<String> responseHandler = new BasicResponseHandler();
                        pageSourceCode = httpClient.execute(httpGet, responseHandler);

                        String toSend = pageSourceCode;

                        int idx = pageSourceCode.indexOf("</string>");
                        if (idx == -1) {
                            Log.d(Constants.TAG, "Eroare index");
                        } else {
                            idx += 21;
                            pageSourceCode = pageSourceCode.substring(idx);
                            idx = 0;
                            toSend = "";
                            while (idx < pageSourceCode.length() && pageSourceCode.charAt(idx) != '<') {
                                toSend += pageSourceCode.charAt(idx);
                                idx++;
                            }
                        }

                        this.setData(queryStr, toSend);

                        PrintWriter pw = Utilities.getWriter(socket);
                        pw.write(toSend + "\n");
                        Log.d(Constants.TAG, "Sending from server: " + toSend);
                        Log.d(Constants.TAG, "Received page: " + pageSourceCode);
                        pw.flush();
                        socket.close();
                    }

                }
            }
        } catch (IOException ioException) {
            Log.e(Constants.TAG, "An exception has occurred: " + ioException.getMessage());
            ioException.printStackTrace();

        }
    }

}
