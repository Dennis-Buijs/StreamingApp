package wissink.matthias.streamingapp;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.StringTokenizer;

/**
 * Created by Matthias on 21-6-2016.
 */
public class StreamingRegister extends Activity implements View.OnClickListener {

    private String TAG = "Streaming Register";
    private final static String CRLF = "\r\n";

    private final static int INIT = 0;
    private final static int READY = 1;
    private final static int REGISTER = 2;

    private int RTSPSeqNb = 0;

    private Button registerToServerBtn;
    private TextView serverIpEt;
    private TextView portNbEt;
    private TextView fileNameEt;

    private String serverIp;
    private int portNb;
    private String fileName;

    private Socket rtspSocket;
    private BufferedReader in;
    private BufferedWriter out;

    private static int state;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_streaming_register);

        registerToServerBtn = (Button) findViewById(R.id.registerToServer);
        serverIpEt = (TextView) findViewById(R.id.serverIp);
        portNbEt = (TextView) findViewById(R.id.portNb);
        fileNameEt = (TextView) findViewById(R.id.fileName);

        registerToServerBtn.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.registerToServer:
                RegisterFileTask registerFileTask = new RegisterFileTask();
                registerFileTask.execute((Void[]) null);
                break;
        }
    }

    private class RegisterFileTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            registerFile();
            return null;
        }
    }

    public void registerFile() {
        Log.i(TAG, "registering file");

        try {
            serverIp = serverIpEt.getText().toString();
            portNb = Integer.parseInt(portNbEt.getText().toString());
            fileName = fileNameEt.getText().toString();

            InetAddress ServerIpAddress = InetAddress.getByName(serverIp);

            rtspSocket = new Socket(ServerIpAddress, portNb);

            // Set input and output stream filters:
            in = new BufferedReader(new InputStreamReader(
                    rtspSocket.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(
                    rtspSocket.getOutputStream()));

            state = INIT;

            if (state == INIT) {
                Log.i(TAG, "sending");
                RTSPSeqNb++;
                sendRequest("SENDING");

                if(parse_server_response() == 200){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            registerToServerBtn.setClickable(false);
                            Toast.makeText(getBaseContext(), "Registered", Toast.LENGTH_LONG).show();
                        }
                    });

                }
            }
        } catch (Exception e) {
            Log.i(TAG, e.toString());
        }
    }


    public void sendRequest(String request_type) {
        try {
            out.write(request_type + " " + fileName + " " + portNb + " RTSP/1.0" + CRLF);
            //write the CSeq line:
            out.write("CSeq: " + RTSPSeqNb + CRLF);
            out.flush();
        } catch (Exception ex) {
            Log.i(TAG, ex.toString());
        }
    }

    private int parse_server_response() {
        int reply_code = 0;

        try {
            //parse status line and extract the reply_code:
            String StatusLine = in.readLine();
            Log.i("Streaming app", StatusLine);

            StringTokenizer tokens = new StringTokenizer(StatusLine);
            tokens.nextToken(); //skip over the RTSP version
            reply_code = Integer.parseInt(tokens.nextToken());

            //if reply code is OK get and print the 2 other lines
            if (reply_code == 200) {
                String SeqNumLine = in.readLine();
                Log.i("Streaming app", SeqNumLine);

                String SessionLine = in.readLine();
                Log.i("Streaming app", SessionLine);
            }
        } catch (Exception ex) {
            System.exit(0);
        }

        return (reply_code);
    }

}
