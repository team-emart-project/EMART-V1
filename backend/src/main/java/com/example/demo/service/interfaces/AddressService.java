package com.example.demo.service.interfaces;

import com.example.demo.dto.request.AddressRequest;
import com.example.demo.dto.response.AddressResponse;

import java.util.List;

/** Module 3 — the logged-in user's address book. */
public interface AddressService {

    List<AddressResponse> getMyAddresses();

    AddressResponse addAddress(AddressRequest request);

    AddressResponse updateAddress(Integer addressId, AddressRequest request);

    void deleteAddress(Integer addressId);

    AddressResponse setDefaultAddress(Integer addressId);
}
