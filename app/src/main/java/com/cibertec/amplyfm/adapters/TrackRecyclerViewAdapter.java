package com.cibertec.amplyfm.adapters;

import android.os.AsyncTask;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cibertec.amplyfm.R;
import com.cibertec.amplyfm.models.Image.ImageResponse;
import com.cibertec.amplyfm.models.Image.Value;
import com.cibertec.amplyfm.models.Track;
import com.cibertec.amplyfm.models.TrackInfoResponse;
import com.cibertec.amplyfm.network.GetImage;
import com.cibertec.amplyfm.network.GetTrackInfo;
import com.cibertec.amplyfm.ui.fragments.TopTracksFragment;
import com.cibertec.amplyfm.utils.Constants;
import com.cibertec.amplyfm.utils.DurationConverter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TrackRecyclerViewAdapter extends RecyclerView.Adapter<TrackRecyclerViewAdapter.ViewHolder> {

    // trackList para llenar el fragment
    private final Track[] trackList;
    private final TopTracksFragment.OnListFragmentInteractionListener interactionListener;
    public int position;
    Retrofit retrofitBing;

    Retrofit retrofitLastFM;
    String imageUrl;


    public TrackRecyclerViewAdapter(Track[] items, TopTracksFragment.OnListFragmentInteractionListener listener) {
        trackList = items;
        interactionListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // get the layout and inflate
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.track_prototype, parent, false);
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();


        retrofitLastFM = new Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        retrofitBing = new Retrofit.Builder()
                .baseUrl(Constants.BASE_URL_BING)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();


        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        Track dm = trackList[position];

        holder.trackItem = dm;
        holder.trackNameView.setText(dm.getName());
        holder.playCountView.setText(String.valueOf(dm.getPlaycount()));

        GetTrackInfo getTrackInfo = retrofitLastFM.create(GetTrackInfo.class);
        Call<TrackInfoResponse> trackInfoCall = getTrackInfo.getTrackInfo(dm.getArtist().getName(), dm.getName(), Constants.API_KEY);
        trackInfoCall.enqueue(new Callback<TrackInfoResponse>() {
            @Override
            public void onResponse(Call<TrackInfoResponse> call, Response<TrackInfoResponse> response) {
                if (response.isSuccessful()) {
                    TrackInfoResponse trackInfoResponse = response.body();
                    Track track = trackInfoResponse.getTrack();

                    holder.trackItem.setAlbum(track.getAlbum());

                    String albumTitle;
                    try {
                                                albumTitle = track.getAlbum().getTitle();



                                         String a;

                    } catch (Exception e) {
                        albumTitle = "";
                    }

                    holder.albumView.setText(albumTitle);

                    String duration = DurationConverter.getDurationInMinutesText(Long.parseLong(track.getDuration()));
                    holder.trackItem.setDuration(duration);
                    holder.durationView.setText(duration);



                }

            }

            @Override
            public void onFailure(Call<TrackInfoResponse> call, Throwable t) {
                Log.e("fail", t.getMessage() + " " + t.getCause());

            }

        });


        holder.mView.setOnClickListener(v -> {
            if (null != interactionListener) {
                // Notify the active callbacks interface (the activity, if the
                // fragment is attached to one) that an item has been selected.
                interactionListener.onListFragmentInteraction(holder.trackItem);
            }
        });
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                setPosition(holder.getAdapterPosition());
                return false;
            }
        });
    }

    @Override
    public int getItemCount() {
        return trackList.length;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public Track getItem(int position) {
        return trackList[position];
    }


    // Class for prototyping fields we're going to fill
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {
        public final View mView;
        public final TextView trackNameView;
        public final TextView playCountView;
        public final TextView albumView;
        public final ImageView imageView;
        public final TextView durationView;
        public Track trackItem;

        public ViewHolder(View view) {
            super(view);

            mView = view;
            albumView = (TextView) view.findViewById(R.id.tv_track_album);
            trackNameView = (TextView) view.findViewById(R.id.tv_track_name);
            playCountView = view.findViewById(R.id.tv_plays);
            imageView = view.findViewById(R.id.iv_track);
            durationView = (TextView) view.findViewById(R.id.tv_duration);

            view.setOnCreateContextMenuListener(this);

        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            menu.add(0, 1, 1, "Añadir a favoritos");
            menu.add(0, 2, 0, "Ver letra");
        }
    }

    private class LongOperation extends AsyncTask<String, Void, String> {
        ViewHolder holder;
        String artistName;
        String albumTitle;

        public LongOperation(ViewHolder holder, String artistName, String albumTitle) {
            this.holder = holder;
            this.artistName = artistName;
            this.albumTitle = albumTitle;
        }

        @Override
        protected String doInBackground(String... params) {
            try {

                GetImage getImage = retrofitBing.create(GetImage.class);

                Call<ImageResponse> imageResponseCall = getImage.getImage(artistName + " " + albumTitle);
                ImageResponse imageResponse = imageResponseCall.execute().body();
                List<Value> items = imageResponse.getValue();
                imageUrl = items.get(0).getThumbnailUrl();
                holder.trackItem.setUrl(imageUrl);
                Glide.with(holder.imageView.getContext())
                        .load(imageUrl)
                        .into(holder.imageView);

            } catch (Exception e) {
                e.printStackTrace();
            }
            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {

            // might want to change "executed" for the returned string passed
            // into onPostExecute() but that is upto you
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }

}




