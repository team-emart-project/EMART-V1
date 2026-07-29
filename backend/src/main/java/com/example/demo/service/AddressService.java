package com.example.demo.service;

import java.util.List;

import com.example.demo.dto.request.AddressRequest;
import com.example.demo.dto.response.AddressResponse;

public interface AddressService {

    AddressResponse addAddress(Integer userId, AddressRequest request);

    List<AddressResponse> getAddresses(Integer userId);

    AddressResponse updateAddress(Integer addressId, AddressRequest request);

    void deleteAddress(Integer addressId);

    void setDefaultAddress(Integer userId, Integer addressId);
}