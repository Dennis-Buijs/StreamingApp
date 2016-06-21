package wissink.matthias.streamingapp;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 * Created by Matthias on 20-6-2016.
 */
public class MainActivity extends Activity implements View.OnClickListener{



    private Button clientBtn;
    private Button serverBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        clientBtn = (Button) findViewById(R.id.clientBtn);
        serverBtn = (Button) findViewById(R.id.serverBtn);

        clientBtn.setOnClickListener(this);
        serverBtn.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.clientBtn:
                toStreamingClient();
                break;
            case R.id.serverBtn:
                toStreamingServer();
        }
    }

    public void toStreamingClient(){
        startActivity(new Intent(this, StreamingClient.class));
    }

    public void toStreamingServer(){
        startActivity(new Intent(this, StreamingServer.class));
    }




}
