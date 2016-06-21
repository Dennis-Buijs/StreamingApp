package wissink.matthias.streamingapp;

import android.os.Environment;

import java.io.FileInputStream;

public class VideoStream {



    FileInputStream fis; //video file
    int frame_nb; //current frame nb

    //-----------------------------------
    //constructor
    //-----------------------------------
    public VideoStream(String filename) throws Exception{

        String dir = Environment.getExternalStorageDirectory().getAbsolutePath();

        //init variables
        fis = new FileInputStream(dir + "/Movies/" + filename);
        frame_nb = 0;
    }

    //-----------------------------------
    // getnextframe
    //returns the next frame as an array of byte and the size of the frame
    //-----------------------------------
    public int getnextframe(byte[] frame) throws Exception
    {
        int length = 0;
        String length_string;
        byte[] frame_length = new byte[5];

        //read current frame length
        fis.read(frame_length,0,5);

        //transform frame_length to integer
        length_string = new String(frame_length);
        length = Integer.parseInt(length_string);
//    length = convertirOctetEnEntier(frame_length);

        return(fis.read(frame,0,length));
    }

//  public int convertirOctetEnEntier(byte[] b){
//    int MASK = 0xFF;
//    int result = 0;
//    result = b[0] & MASK;
//    result = result + ((b[1] & MASK));
//    result = result + ((b[2] & MASK));
//    result = result + ((b[3] & MASK));
//    result = result + ((b[4] & MASK));
//    return result;
//  }




}