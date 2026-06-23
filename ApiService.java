package com.example.kolokvijum2;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ApiService {

    @GET("comments/{id}")
    Call<Comment> getComment(@Path("id") int id);
}