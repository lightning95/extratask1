package com.example.android.picturemanager.model;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

import com.example.android.picturemanager.ImageAdapter;
import com.example.android.picturemanager.R;
import com.example.android.picturemanager.rest.RestClient;
import com.example.android.picturemanager.rest.model.Photo;
import com.example.android.picturemanager.rest.model.Response;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;

/**
 * Created by lightning95 on 1/26/15.
 */

public class MyFeed {
    public static final String LOAD_STARTED_BROADCAST = "Loading started";
    public static final String PROGRESS_BROADCAST = "Loading images";

    private int imageSizeSmall;
    private int imageSizeBig;
    private List<Photo> items;
    private RestClient restClient;
    private ImageAdapter imageAdapter;
    private HashSet<String> hashSet;
    private Context context;
    private String category;

    public List<String> getItems() {
        List<String> res = new ArrayList<>();
        for (Photo photo : items) {
            res.add(photo.toString());
        }
        return res;
    }

    public MyFeed(Context context, ImageAdapter imageAdapter, int imageSizeSmall, int imageSizeBig, String category) {
        this.context = context;
        this.category = category;
        hashSet = new HashSet<>();

        this.restClient = new RestClient();

        this.imageSizeSmall = imageSizeSmall;
        this.imageSizeBig = imageSizeBig;
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        this.imageAdapter = imageAdapter;
        this.imageAdapter.setItems(this.items);
    }

    private boolean isNetworkAvailable() {
        NetworkInfo activeNetworkInfo =
                ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void noInternetConnection() {
        Toast.makeText(context, context.getString(R.string.toastNoInternetConnection), Toast.LENGTH_SHORT).show();
    }


    public void loadItems(final int pageNumber, final int imagesPerPage) {
        Intent start = new Intent();
        start.setAction(LOAD_STARTED_BROADCAST);
        start.putExtra("category", category);
        start.putExtra("imagesPerPage", imagesPerPage);
        context.sendBroadcast(start);

        restClient.getApiService().getFeed(
                category,
                imageSizeSmall,
                pageNumber,
                imagesPerPage,
                restClient.getConsumerKey(),
                new Callback<Response>() {
                    @Override
                    public void success(Response response, retrofit.client.Response httpResponse) {
                        appendItems(response.getPhotos());
                        restClient.getApiService().getFeed(
                                category,
                                imageSizeBig,
                                pageNumber,
                                imagesPerPage,
                                restClient.getConsumerKey(),
                                new Callback<Response>() {
                                    @Override
                                    public void success(Response response, retrofit.client.Response httpResponse) {
                                        updateItems(response.getPhotos());
                                    }

                                    @Override
                                    public void failure(RetrofitError error) {
                                        if (!isNetworkAvailable()) {
                                            noInternetConnection();
                                        }
                                        Log.d("==MY_FEED__big==", "load failure, error = " + error.getMessage());
                                    }
                                });
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        if (!isNetworkAvailable()) {
                            noInternetConnection();
                        }

                        Log.d("==MY_FEED_small==", "load failure, error = " + error.getMessage());
                    }
                });
    }

    private void updateItems(List<Photo> other) {
        for (int i = 0; i < other.size(); i++) {
            Photo big_photo = other.get(i);
            if (hashSet.contains(big_photo.getTitle())) {
                for (Photo p : items) {
                    if (p.getTitle().equals(big_photo.getTitle())) {
                        p.setBig_image_url(big_photo.getImage_url());
                    }
                }
            }
        }
        this.imageAdapter.notifyDataSetChanged();
    }

    private void appendItems(List<Photo> other) {
        for (int i = 0; i < other.size(); i++) {
            Photo item = other.get(i);
            if (hashSet.add(item.getTitle())) {
                items.add(item);
            }
        }
        this.imageAdapter.notifyDataSetChanged();
    }

    public void invalidateData() {
        hashSet = new HashSet<>();
        items = new ArrayList<>();
        imageAdapter.setItems(items);
        imageAdapter.notifyDataSetChanged();
    }
}
