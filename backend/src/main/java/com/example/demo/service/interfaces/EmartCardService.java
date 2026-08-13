package com.example.demo.service.interfaces;

import com.example.demo.dto.request.EmartCardApplicationRequest;
import com.example.demo.dto.response.EmartCardResponse;
import com.example.demo.dto.response.PointsBalanceResponse;

/** Module 4 — e-MART card application and e-Points visibility. */
public interface EmartCardService {

    EmartCardResponse apply(EmartCardApplicationRequest request);

    EmartCardResponse getMyCard();

    /** Never throws: non-cardholders get cardholder=false, balance=0. */
    PointsBalanceResponse getMyPointsBalance();
}
