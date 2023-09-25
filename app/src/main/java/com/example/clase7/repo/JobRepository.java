package com.example.clase7.repo;

import com.example.clase7.entity.JobDto;

import retrofit2.Call;
import retrofit2.http.GET;

public interface JobRepository {

    @GET("/api/job")
    Call<JobDto> getJobs();
}
