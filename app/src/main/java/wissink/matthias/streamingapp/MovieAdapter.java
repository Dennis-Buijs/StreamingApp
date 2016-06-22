package wissink.matthias.streamingapp;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import wissink.matthias.streamingapp.Model.Movie;

import java.util.ArrayList;

/**
 * Created by Matthias on 21-6-2016.
 */
public class MovieAdapter extends BaseAdapter{

    Context context;
    LayoutInflater inflator;
    ArrayList movieArrayList;

    public MovieAdapter(Context context, LayoutInflater layoutInflater, ArrayList<Movie> movieArrayList) {
        this.context = context;
        this.inflator = layoutInflater;
        this.movieArrayList = movieArrayList;
    }

    @Override
    public int getCount() {
        int size = movieArrayList.size();
        Log.i("getCount()","=" + size);
        return size;
    }

    @Override
    public Object getItem(int position) {
        return movieArrayList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        if(convertView == null) {
            convertView = inflator.inflate(R.layout.listview_row, null);

            viewHolder = new ViewHolder();
            viewHolder.clientIpTv = (TextView) convertView.findViewById(R.id.clientIpTv);
            viewHolder.fileNameTv = (TextView) convertView.findViewById(R.id.fileNameTv);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        Movie movie = (Movie) movieArrayList.get(position);

        viewHolder.clientIpTv.setText(movie.clientIp);
        viewHolder.fileNameTv.setText(movie.fileName);

        return convertView;
    }

    private static class ViewHolder {
        public TextView clientIpTv;
        public TextView fileNameTv;
    }
}
