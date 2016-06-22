package wissink.matthias.streamingapp;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.*;
import java.lang.ref.WeakReference;
import java.net.*;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

public class StreamingClient extends Activity implements View.OnClickListener {

    private final static int INIT = 0;
    private final static int READY = 1;
    private final static int PLAYING = 2;
    private final static int REFRESH = 1;

    private int rtspServerPort;
    private String serverIp;
    static int state;
    private Socket rtspSocket;
    private BufferedReader rtspBufferedReader;
    private BufferedWriter rtspBufferedWriter;
    private int RTSPSeqNb = 0;
    private int RTSPid = 0;

    static int RTP_RCV_PORT; //port where the client will receive the RTP packets
    private static final int FRAME_PERIOD = 20;
    private final static String CRLF = "\r\n";
    private DatagramPacket receivedDataPacket;
    private DatagramSocket rtpSocket;

    private Timer timer; //timer used to receive data from the UDP socket
    private byte[] buf = new byte[15000]; //buffer used to store data received from the server

    private Bitmap bitmap;
    private Button connectBtnClient;
    private Button playBtn;
    private Button pauseBtn;
    private Button tearDownBtn;
    private Button setupBtn;
    private ImageView image;
    private EditText editTextServerIp;

    private ImageRefresher imageRefresher = new ImageRefresher(this);

    private String VideoFileName = "movie.Mjpeg";


    private String fileNameFromList;
    private String clientIpFromList;
    private int portNbFromList = -1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_streaming_cient);

        fileNameFromList = getIntent().getStringExtra("fileName");
        clientIpFromList = getIntent().getStringExtra("serverIp");
        portNbFromList = Integer.parseInt(getIntent().getStringExtra("portNb"));

        connectBtnClient = (Button) findViewById(R.id.connectBtnClient);
        setupBtn = (Button) findViewById(R.id.setupBtn);
        playBtn = (Button) findViewById(R.id.playBtn);
        pauseBtn = (Button) findViewById(R.id.pauseBtn);
        tearDownBtn = (Button) findViewById(R.id.tearDownBtn);

        connectBtnClient.setOnClickListener(this);
        setupBtn.setOnClickListener(this);
        playBtn.setOnClickListener(this);
        pauseBtn.setOnClickListener(this);
        tearDownBtn.setOnClickListener(this);


        image = (ImageView) findViewById(R.id.imgView);
        editTextServerIp = (EditText) findViewById(R.id.EditTextServerIp);

        //init RTSP state:
        state = INIT;
    }

    private class SetupButtonTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            setupActionPerformed();
            return null;
        }
    }

    private class ConnectButtonTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            setupConnection();
            return null;
        }
    }

    private class PlayButtonTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... v) {
            startStreaming();
            return null;
        }
    }

    private class PauseButtonTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... v) {
            pauseStreaming();
            return null;
        }
    }

    private class TearDownButtonTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... v) {
            tearDownStreaming();
            return null;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.connectBtnClient:
                ConnectButtonTask connectButtonTask = new ConnectButtonTask();
                connectButtonTask.execute((Void[]) null);
                break;
            case R.id.setupBtn:
                SetupButtonTask setupButtonTask = new SetupButtonTask();
                setupButtonTask.execute((Void[]) null);
                break;
            case R.id.playBtn:
                PlayButtonTask playButtonTask = new PlayButtonTask();
                playButtonTask.execute((Void[]) null);
                break;
            case R.id.pauseBtn:
                PauseButtonTask pauseButtonTask = new PauseButtonTask();
                pauseButtonTask.execute((Void[]) null);
                break;
            case R.id.tearDownBtn:
                TearDownButtonTask tearDownButtonTask = new TearDownButtonTask();
                tearDownButtonTask.execute((Void[]) null);
                break;
        }
    }

    public void setupConnection() {
        Log.i("streaming app", "making connection");


        try {
            rtspServerPort = portNbFromList;
            serverIp = clientIpFromList;
            RTP_RCV_PORT = portNbFromList;

            System.out.println(serverIp);
            // get server RTSP port and IP address from the command line
            InetAddress ServerIPAddr = InetAddress.getByName(serverIp);

            // Establish a TCP connection with the server to exchange RTSP
            // messages
            rtspSocket = new Socket(ServerIPAddr, rtspServerPort);

            // Set input and output stream filters:
            rtspBufferedReader = new BufferedReader(new InputStreamReader(
                    rtspSocket.getInputStream()));
            rtspBufferedWriter = new BufferedWriter(new OutputStreamWriter(
                    rtspSocket.getOutputStream()));

            // init RTSP state:
            state = INIT;
            Log.i("streaming app", "connection OK!");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getBaseContext(), "Connection made", Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getBaseContext(), "Something went wrong, check connection", Toast.LENGTH_SHORT).show();
                }
            });
            Log.i("Streaming app", e.toString());
        }
    }


    //Handler for Setup button
    //-----------------------
    public void setupActionPerformed() {

        Log.i("Streaming app", "Setup Button pressed !");

        if (state == INIT) {
            //Init non-blocking RTPsocket that will be used to receive data
            try {
                //construct a new DatagramSocket to receive RTP packets from the server, on port RTP_RCV_PORT
                rtpSocket = new DatagramSocket(RTP_RCV_PORT);

                //set TimeOut value of the socket to 5msec.
                rtpSocket.setSoTimeout(5);

            } catch (SocketException se) {
                Log.i("Streaming app", "Socket exception: " + se);
                System.exit(0);
            }

            //init RTSP sequence number
            RTSPSeqNb = 1;

            //Send SETUP message to the server
            send_RTSP_request("SETUP");

            //Wait for the response
            if (parse_server_response() != 200)
                Log.i("Streaming app", "Invalid Server Response");
            else {
                //change RTSP state and print new state
                state = READY;
                Log.i("Streaming app", "New RTSP state: READY");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getBaseContext(), "Ready to play", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }


    //Handler for Play button
    //-----------------------
    public void startStreaming() {

        Log.i("Streaming app", "Play Button pressed !");

        if (state == READY) {
            //increase RTSP sequence number
            RTSPSeqNb = RTSPSeqNb + 1;

            //Send PLAY message to the server
            send_RTSP_request("PLAY");

            //Wait for the response
            if (parse_server_response() != 200)
                Log.i("Streaming app", "Invalid Server Response");
            else {
                //change RTSP state and print out new state
                state = PLAYING;
                Log.i("Streaming app", "New RTSP state: PLAYING");

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getBaseContext(), "Playing", Toast.LENGTH_SHORT).show();
                    }
                });



                //start the timer
                timer = new Timer();
                timer.schedule(new ReceiveFrame(), 0, FRAME_PERIOD);
            }
        }
    }


    //Handler for Pause button
    //-----------------------
    public void pauseStreaming() {

        Log.i("Streaming app", "Pause Button pressed !");

        if (state == PLAYING) {
            //increase RTSP sequence number
            RTSPSeqNb = RTSPSeqNb + 1;

            //Send PAUSE message to the server
            send_RTSP_request("PAUSE");

            //Wait for the response
            if (parse_server_response() != 200)
                Log.i("Streaming app", "Invalid Server Response");
            else {
                //change RTSP state and print out new state
                state = READY;
                Log.i("Streaming app", "New RTSP state: READY");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getBaseContext(), "Pause", Toast.LENGTH_SHORT).show();
                    }
                });


                //stop the timer
                timer.cancel();
            }
        }
        //else if state != PLAYING then do nothing
    }


    //Handler for Teardown button
    //-----------------------
    public void tearDownStreaming() {

        Log.i("Streaming app", "Teardown Button pressed !");

        //increase RTSP sequence number
        RTSPSeqNb = RTSPSeqNb + 1;


        //Send TEARDOWN message to the server
        send_RTSP_request("TEARDOWN");

        //Wait for the response
        if (parse_server_response() != 200)
            Log.i("Streaming app", "Invalid Server Response");
        else {
            //change RTSP state and print out new state
            state = INIT;
            Log.i("Streaming app", "New RTSP state: INIT");

            //stop the timer
            timer.cancel();

            //exit
            System.exit(0);
        }
    }


    //------------------------------------
    //Parse Server Response
    //------------------------------------
    private int parse_server_response() {
        int reply_code = 0;

        try {
            //parse status line and extract the reply_code:
            String StatusLine = rtspBufferedReader.readLine();
            //Log.i("Streaming app", "RTSP Client - Received from Server:");
            Log.i("Streaming app", StatusLine);

            StringTokenizer tokens = new StringTokenizer(StatusLine);
            tokens.nextToken(); //skip over the RTSP version
            reply_code = Integer.parseInt(tokens.nextToken());

            //if reply code is OK get and print the 2 other lines
            if (reply_code == 200) {
                String SeqNumLine = rtspBufferedReader.readLine();
                Log.i("Streaming app", SeqNumLine);

                String SessionLine = rtspBufferedReader.readLine();
                Log.i("Streaming app", SessionLine);

                //if state == INIT gets the Session Id from the SessionLine
                tokens = new StringTokenizer(SessionLine);
                tokens.nextToken(); //skip over the Session:
                RTSPid = Integer.parseInt(tokens.nextToken());
            }
        } catch (Exception ex) {
            Log.i("Streaming app", "Exception caught: " + ex);
            System.exit(0);
        }

        return (reply_code);
    }

    //------------------------------------
    //Send RTSP Request
    //------------------------------------

    //.............
    //TO COMPLETE
    //.............

    private void send_RTSP_request(String request_type) {
        try {
            //Use the RTSPBufferedWriter to write to the RTSP socket

            //write the request line:
            if(fileNameFromList != null){
                rtspBufferedWriter.write(request_type + " " + fileNameFromList + " RTSP/1.0" + CRLF);
            }else {
                rtspBufferedWriter.write(request_type + " " + VideoFileName + " RTSP/1.0" + CRLF);

            }

            //write the CSeq line:
            rtspBufferedWriter.write("CSeq: " + RTSPSeqNb + CRLF);

            //check if request_type is equal to "SETUP" and in this case write the Transport: line advertising to the server the port used to receive the RTP packets RTP_RCV_PORT
            //if ....
            //otherwise, write the Session line from the RTSPid field
            //else ....

            if (request_type.equals("SETUP")) {
                rtspBufferedWriter.write("Transport: RTP/UDP; client_port= " + RTP_RCV_PORT + CRLF);
            } else {
                rtspBufferedWriter.write("Session: " + RTSPid + CRLF);
            }


            rtspBufferedWriter.flush();
        } catch (Exception ex) {
            Log.i("Streaming app", "Exception caught: " + ex);
            System.exit(0);
        }
    }

    class ReceiveFrame extends TimerTask {
        public void run() {
            // Construct a DatagramPacket to receive data from the UDP socket
            receivedDataPacket = new DatagramPacket(buf, buf.length);

            try {
                // receive the DP from the socket:
                rtpSocket.receive(receivedDataPacket);

                // create an RTPpacket object from the DP
                RTPpacket rtp_packet = new RTPpacket(
                        receivedDataPacket.getData(),
                        receivedDataPacket.getLength());

                // print header bit stream:
                rtp_packet.printheader();

                // get the payload bitstream from the RTPpacket object
                int payload_length = rtp_packet.getpayload_length();
                byte[] payload = new byte[payload_length];
                rtp_packet.getpayload(payload);

                bitmap = BitmapFactory.decodeByteArray(payload, 0,
                        payload_length);

                Message message = Message.obtain(imageRefresher, REFRESH,
                        rtp_packet.getsequencenumber(), 0);
                imageRefresher.sendMessage(message);
            } catch (InterruptedIOException iioe) {
                Log.i("Streaming app", iioe.toString());
            } catch (IOException ioe) {
                Log.i("Streaming app", ioe.toString());
            }
        }
    }


    static class ImageRefresher extends Handler {

        private final WeakReference<StreamingClient> mStreamingClientActivity;

        ImageRefresher(StreamingClient streamingClientActivity) {
            mStreamingClientActivity = new WeakReference<StreamingClient>(
                    streamingClientActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            StreamingClient streamingClientActivity = mStreamingClientActivity.get();
            if (streamingClientActivity != null) {
                switch (msg.what) {
                    case REFRESH:
                    /* Refresh UI */
                        streamingClientActivity.image
                                .setImageBitmap(streamingClientActivity.bitmap);
                        break;
                }
            }
        }
    }

}//end of Class Client

