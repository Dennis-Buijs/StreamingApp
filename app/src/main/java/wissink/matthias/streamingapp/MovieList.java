package wissink.matthias.streamingapp;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import wissink.matthias.streamingapp.Model.Movie;

import java.util.ArrayList;
import static wissink.matthias.streamingapp.JsonHttpTask.GET;

/**
 * Created by Matthias on 21-6-2016.
 */
public class MovieList extends Activity implements AdapterView.OnItemClickListener {

    ListView movieListView;
    MovieAdapter movieAdapter;
    ArrayList movies = new ArrayList();
    EditText editText;
    EditText toServerIp;
    EditText toServerPort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movielist);

        Button clickBtn = (Button) findViewById(R.id.getMoviesbtn);
        clickBtn.setOnClickListener(btnClicked);

        Button useThisIpBtn = (Button) findViewById(R.id.useThisIpBtn);
        useThisIpBtn.setOnClickListener(useThisIpClicked);

        editText= (EditText) findViewById(R.id.getMoviesIp);
        toServerIp = (EditText) findViewById(R.id.EditTextServerIp);
        toServerPort = (EditText) findViewById(R.id.EditTextPortNb);

        movieListView = (ListView) findViewById(R.id.movieListView);
        movieAdapter = new MovieAdapter(this, getLayoutInflater(), movies);
        movieListView.setAdapter(movieAdapter);
        movieAdapter.notifyDataSetChanged();
        movieListView.setOnItemClickListener(this);
    }

    private View.OnClickListener btnClicked = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i("Json Parser", "Button clicked");
            if(editText.getText() != null){
                movies.clear();
                new HttpAsyncTask().execute("http://"+editText.getText()+"/JsonMovie.json");
            }else{
                Toast.makeText(getBaseContext(), "Voer een IP in", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private View.OnClickListener useThisIpClicked = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i("Json Parser", "Button clicked");
            if(toServerPort.getText() != null){
                if(toServerIp.getText() != null) {
                    Intent intent = new Intent(getApplicationContext(), StreamingClient.class);
                    intent.putExtra("serverIp", toServerIp.getText().toString());
                    intent.putExtra("portNb", toServerPort.getText().toString());
                    startActivity(intent);
                }else{
                    Toast.makeText(getBaseContext(), "Voer een server IP in", Toast.LENGTH_SHORT).show();
                }
            }else{
                Toast.makeText(getBaseContext(), "Voer een server port in", Toast.LENGTH_SHORT).show();
            }
        }
    };


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Movie movie = (Movie) movies.get(position);
        Intent k = new Intent(getApplicationContext(), StreamingClient.class);

        k.putExtra("serverIp", movie.clientIp);
        k.putExtra("fileName", movie.fileName);
        k.putExtra("portNb", movie.portNb);

        startActivity(k);
    }

    private class HttpAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            return GET(urls[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                JSONArray json = new JSONArray(result);
                int totalRepo = json.length();
                Log.i("Json", String.valueOf(totalRepo) + " " + json.toString());

                for(int i = 0; i < totalRepo; i++){
                    JSONObject jsonObject =  json.getJSONObject(i);

                    Movie movie = new Movie();
                    movie.clientIp = jsonObject.getString("clientIp");
                    movie.fileName = jsonObject.getString("fileName");
                    movie.portNb = jsonObject.getString("portNb");

                    movies.add(movie);
                    Log.i("Async", "Movies added");
                }
                movieAdapter.notifyDataSetChanged();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
