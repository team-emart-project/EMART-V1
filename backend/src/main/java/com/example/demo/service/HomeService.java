package com.example.demo.service;

import com.example.demo.dto.response.HomePageResponse;

public interface HomeService {

    // Single aggregated payload for rendering the storefront homepage
    HomePageResponse getHomePageData();
}
