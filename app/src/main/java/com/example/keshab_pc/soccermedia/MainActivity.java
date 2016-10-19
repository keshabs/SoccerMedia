package com.example.keshab_pc.soccermedia;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.vstechlab.easyfonts.EasyFonts;

import net.dean.jraw.RedditClient;

import net.dean.jraw.http.UserAgent;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthData;
import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.Sorting;
import net.dean.jraw.paginators.SubredditPaginator;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private List<Submission> mSubmissions;
    private List<String> mThumbnails;
    private RedditClient redditClient;
    private MediaAdapter mMediaAdapter;
    private SubredditPaginator subreddit;

    private boolean mLoading = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        authenticate();
        mSubmissions = new ArrayList<Submission>();
        mThumbnails = new ArrayList<String>();

        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);



        new fetchSubmission().execute();
        updateUI();

        mRecyclerView.setOnScrollListener(new EndlessRecyclerOnScrollListener(mLayoutManager) {
            @Override
            public void onLoadMore(int current_page) {
                new fetchSubmission().execute();
            }
        });


    }
    public void authenticate(){
        UserAgent myUserAgent = UserAgent.of("android", "com.example.keshab_pc.soccermedia", "v1", "pokhara80");
        redditClient = new RedditClient(myUserAgent);
        final Credentials credentials = Credentials.userless("k4QNEFinMkLXNQ", "6zlRFGI-xWCAa6Edwn3CYLnMcrw", UUID.randomUUID());


        new AsyncTask<Void,Void,Void>(){
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    OAuthData authData = redditClient.getOAuthHelper().easyAuth(credentials);

                    redditClient.authenticate(authData);

                } catch(Exception ie){
                    ie.printStackTrace();
                }
                subreddit = new SubredditPaginator(redditClient,"soccer");
                return null;
            }
        }.execute();



    }
    private class MediaHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private TextView mTitleTextView;
        private ImageView mImageView;
        private Submission sub;
        public MediaHolder(View itemView){
            super(itemView);
            itemView.setOnClickListener(this);
            mTitleTextView = (TextView)itemView.findViewById(R.id.item_title);
            mTitleTextView.setTypeface(EasyFonts.robotoRegular(getApplicationContext()));
            mImageView = (ImageView)itemView.findViewById(R.id.item_thumbnail);
        }
        public void bindData(Submission item, String thumbnail){
            mTitleTextView.setText(item.getTitle());
            sub = item;
            Picasso.with(getApplicationContext()).load(thumbnail).into(mImageView);
        }

        @Override
        public void onClick(View view) {
            Intent intent = new Intent(getApplication(),MediaViewer.class);
            intent.putExtra(MediaViewer.EXTRA,sub.getUrl());
            startActivity(intent);
        }
    }

    private class MediaAdapter extends RecyclerView.Adapter<MediaHolder>{
        List<Submission> subs;
        public MediaAdapter(List<Submission> s){
            subs = s;
        }
        @Override
        public MediaHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            View view = layoutInflater.inflate(R.layout.recyclerview_item_row,parent,false);
            return new MediaHolder(view);
        }

        @Override
        public void onBindViewHolder(MediaHolder holder, int position) {
            holder.bindData(subs.get(position),mThumbnails.get(position));
        }

        @Override
        public int getItemCount() {
            return subs.size();
        }
    }
    public class fetchSubmission extends AsyncTask<Void,Void,Void>{



        @Override
        protected Void doInBackground(Void... voids) {




           // subreddit.setSorting(Sorting.NEW);

            int count = 0;
            while(count < 15){
                for(Submission link: subreddit.next()){
                    if(link.getOEmbedMedia() != null ){
                        count++;

                        try {

                            mThumbnails.add(link.getOEmbedMedia().getThumbnail().getUrl().toString());
                            mSubmissions.add(link);
                        } catch (Exception ie){

                            ie.printStackTrace();
                        }
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            updateUI();
        }
    }

    private void updateUI(){
        if(mMediaAdapter == null){
            mMediaAdapter = new MediaAdapter(mSubmissions);
            mRecyclerView.setAdapter(mMediaAdapter);
        } else {
            mMediaAdapter.notifyDataSetChanged();
        }

    }

}
