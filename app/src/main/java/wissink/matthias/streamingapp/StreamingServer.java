package wissink.matthias.streamingapp;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.*;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Matthias on 20-6-2016.
 */
public class StreamingServer extends Activity implements View.OnClickListener {

    private static final String TAG = "Streaming server";

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };



    private int RTP_dest_port = 0; //destination port for RTP packets  (given by the RTSP Client)

    private int RTSPSeqNb = 0; //Sequence number of RTSP messages within the session

    private DatagramPacket senddp; //UDP packet containing the video frames
    private DatagramSocket RTPsocket; //socket to be used to send and receive UDP packets




    private static int MJPEG_TYPE = 26; //RTP payload type for MJPEG video
    private static int FRAME_PERIOD = 100; //Frame period of the video to stream, in ms
    private static int VIDEO_LENGTH = 500; //length of the video in frames
    private static int RTSP_SERVER_PORT = -1;



    private Socket RTSPsocket; //socket used to send/receive RTSP messages
    //input and output stream filters

    private static String VideoFileName; //video file requested from the client



    private final static String CRLF = "\r\n";

    private Button connectBtnServer;
    private EditText portToUse;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_streaming_server);

        connectBtnServer = (Button) findViewById(R.id.connectBtnServer);
        portToUse = (EditText) findViewById(R.id.portAsServer); 

        connectBtnServer.setOnClickListener(this);

        verifyStoragePermissions(this);
    }

    private class ConnectButtonTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            setupConnection();
            return null;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.connectBtnServer:
                ConnectButtonTask connectButtonTask = new ConnectButtonTask();
                connectButtonTask.execute((Void[]) null);
                Toast.makeText(this.getBaseContext(), "Ready to connect", Toast.LENGTH_LONG).show();
        }
    }


    public void setupConnection() {
        Log.i(TAG, "connection setup");

        RTSP_SERVER_PORT = Integer.parseInt(portToUse.getText().toString());
        boolean bool = false;
        
        try {
            ServerSocket listenSocket = null;
            if(RTSP_SERVER_PORT != -1) {
                //Initiate TCP connection with the client for the RTSP session
                listenSocket = new ServerSocket(RTSP_SERVER_PORT);
                bool = true;
            }else{
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getBaseContext(), "Voer poort in", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            while (bool) {
                try {
                    ClientWorker worker = new ClientWorker(listenSocket.accept());
                    worker.start();
                } catch (Exception e) {
                    Log.i(TAG, e.toString());
                }
            }

//            RTSPsocket = listenSocket.accept();
//            listenSocket.close();

            //Get Client IP address
//            ClientIPAddr = RTSPsocket.getInetAddress();

            //Initiate RTSPstate
//            state = INIT;

//            //Set input and output stream filters:
//            RTSPBufferedReader = new BufferedReader(new InputStreamReader(
//                    RTSPsocket.getInputStream()));
//            RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(
//                    RTSPsocket.getOutputStream()));

//            serverReaction();
        } catch (Exception e) {
            Log.i(TAG, e.toString());
        }
    }

    private class ClientWorker extends Thread {
        private Socket client;
        private BufferedReader in;
        private BufferedWriter out;
        private InetAddress ip; //Client IP address

        //RTSP variables
        //----------------
        //rtsp states
        private final static int INIT = 0;
        private final static int READY = 1;
        private final static int PLAYING = 2;
        //rtsp message types
        private final static int SETUP = 3;
        private final static int PLAY = 4;
        private final static int PAUSE = 5;
        private final static int TEARDOWN = 6;

        private int state; //RTSP Server state == INIT or READY or PLAY

        private VideoStream video; //VideoStream object used to access video frames

        private Timer timer; //timer used to send the images at the video frame rate
        private byte[] buf = new byte[15000]; //buffer used to store the images to send to the client

        Random rand = new Random();
        private int RTSP_ID = rand.nextInt(10000) + 1;

        //Constructor
        ClientWorker(Socket client) {
            this.client = client;
        }

        public void run() {
            ip = client.getInetAddress();
            try {
                //Set input and output stream filters:
                in = new BufferedReader(new InputStreamReader(
                        client.getInputStream()));
                out = new BufferedWriter(new OutputStreamWriter(
                        client.getOutputStream()));

                while (true) {
                    state = INIT;
//                    serverReaction(RTSPBufferedReader, RTSPBufferedWriter, ClientIPAddr);

                    //Wait for the SETUP message from the client
                    int request_type;
                    boolean done = false;
                    while (!done) {
                        request_type = parse_RTSP_request(); //blocking


                        if (request_type == SETUP) {
                            done = true;

                            //update RTSP state
                            state = READY;
                            Log.i(TAG, "New RTSP state: READY");

                            //Send response
                            send_RTSP_response();

                            //init the VideoStream object:
                            video = new VideoStream(VideoFileName);

                            //init RTP socket
                            RTPsocket = new DatagramSocket();
                        }
                    }

                    //loop to handle RTSP requests
                    while (true) {
                        //parse the request
                        request_type = parse_RTSP_request(); //blocking

                        if ((request_type == PLAY) && (state == READY)) {
                            //send back response
                            send_RTSP_response();


                            //start timer
                            //verzend niet goed na pauze, cuz of dit i think
                            timer = new Timer();
                            timer.schedule(new SendFrame(ip, video, timer, buf), 0, FRAME_PERIOD);


                            //update state
                            state = PLAYING;
                            Log.i(TAG, "New RTSP state: PLAYING");
                        } else if ((request_type == PAUSE) && (state == PLAYING)) {
                            //send back response
                            send_RTSP_response();
                            //stop timer
                            timer.cancel();
                            //update state
                            state = READY;
                            Log.i(TAG, "New RTSP state: READY");
                        } else if (request_type == TEARDOWN) {
                            //send back response
                            send_RTSP_response();
                            //stop timer
                            timer.cancel();
                            //close sockets
                            RTSPsocket.close();
                            RTPsocket.close();

                            finish();
                        }
                    }
                }
            } catch (Exception e) {
                Log.i(TAG, e.toString());
            }
        }


        private int parse_RTSP_request() {
            int request_type = -1;
            try {
                //parse request line and extract the request_type:
                String RequestLine = in.readLine();
                Log.i(TAG, "RTSP Server - Received from Client:");
                Log.i(TAG, RequestLine);

                StringTokenizer tokens = new StringTokenizer(RequestLine);
                String request_type_string = tokens.nextToken();

                //convert to request_type structure:
                if ((new String(request_type_string)).compareTo("SETUP") == 0)
                    request_type = SETUP;
                else if ((new String(request_type_string)).compareTo("PLAY") == 0)
                    request_type = PLAY;
                else if ((new String(request_type_string)).compareTo("PAUSE") == 0)
                    request_type = PAUSE;
                else if ((new String(request_type_string)).compareTo("TEARDOWN") == 0)
                    request_type = TEARDOWN;

                if (request_type == SETUP) {
                    //extract VideoFileName from RequestLine

                    VideoFileName = tokens.nextToken();
                }

                //parse the SeqNumLine and extract CSeq field
                String SeqNumLine = in.readLine();
                Log.i(TAG, SeqNumLine);
                tokens = new StringTokenizer(SeqNumLine);
                tokens.nextToken();
                RTSPSeqNb = Integer.parseInt(tokens.nextToken());

                //get LastLine
                String LastLine = in.readLine();
                Log.i(TAG, LastLine);

                if (request_type == SETUP) {
                    //extract RTP_dest_port from LastLine
                    tokens = new StringTokenizer(LastLine);
                    for (int i = 0; i < 3; i++)
                        tokens.nextToken(); //skip unused stuff
                    RTP_dest_port = Integer.parseInt(tokens.nextToken());
                }
                //else LastLine will be the SessionId line ... do not check for now.
            } catch (Exception ex) {
                Log.i(TAG, "Exception caught: " + ex);
                System.exit(0);
            }
            return (request_type);
        }

        //------------------------------------
        //Send RTSP Response
        //------------------------------------
        private void send_RTSP_response() {
            try {
                out.write("RTSP/1.0 200 OK" + CRLF);
                out.write("CSeq: " + RTSPSeqNb + CRLF);
                out.write("Session: " + RTSP_ID + CRLF);
                out.flush();
                System.out.println("RTSP Server - Sent response to Client.");
            } catch (Exception ex) {
                System.out.println("Exception caught: " + ex);
                System.exit(0);
            }
        }

    }

//    public void serverReaction(BufferedReader reader, BufferedWriter writer, InetAddress ip) throws Exception {
//
//    }

    private class SendFrame extends TimerTask {

        private InetAddress ClientIPAddr; //Client IP address
        private VideoStream video;
        private Timer timer;
        private byte[] buf;
        private int imagenb = 0;

        public SendFrame(InetAddress ip, VideoStream video, Timer timer, byte[] buf) {
            this.ClientIPAddr = ip;
            this.video = video;
            this.timer = timer;
            this.buf = buf;
        }

        public void run() {
            //if the current image nb is less than the length of the video
            if (imagenb < VIDEO_LENGTH) {
                //update current imagenb
                imagenb++;

                try {
                    //get next frame to send from the video, as well as its size
                    int image_length = video.getnextframe(buf);

                    //Builds an RTPpacket object containing the frame
                    RTPpacket rtp_packet = new RTPpacket(MJPEG_TYPE, imagenb, imagenb * FRAME_PERIOD, buf, image_length);

                    //get to total length of the full rtp packet to send
                    int packet_length = rtp_packet.getlength();

                    //retrieve the packet bitstream and store it in an array of bytes
                    byte[] packet_bits = new byte[packet_length];
                    rtp_packet.getpacket(packet_bits);

                    //send the packet as a DatagramPacket over the UDP socket
                    senddp = new DatagramPacket(packet_bits, packet_length, ClientIPAddr, RTP_dest_port);
                    RTPsocket.send(senddp);

                    //	  Log.i(TAG, "Send frame #"+imagenb);
                    //print the header bitstream
                    rtp_packet.printheader();

                } catch (Exception ex) {
                    Log.i(TAG, "Exception caught: " + ex);
                    System.exit(0);
                }
            } else {
                //if we have reached the end of the video file, stop the timer
                timer.cancel();
            }
        }
    }



    /**
     * Checks if the app has permission to write to device storage
     * <p>
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }


}


