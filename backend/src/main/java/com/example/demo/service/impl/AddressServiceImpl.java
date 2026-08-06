package com.example.demo.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.dto.request.AddressRequest;
import com.example.demo.dto.response.AddressResponse;
import com.example.demo.repository.AddressRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.AddressService;

@Service
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public AddressServiceImpl(AddressRepository addressRepository,
                              UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    @Override
    public AddressResponse addAddress(Integer userId, AddressRequest request) {

        // Logic will be added in next step

        return null;
    }

    @Override
    public List<AddressResponse> getAddresses(Integer userId) {

        // Logic will be added later

        return null;
    }

    @Override
    public AddressResponse updateAddress(Integer addressId,
                                         AddressRequest request) {

        // Logic will be added later

        return null;
    }

    @Override
    public void deleteAddress(Integer addressId) {

        // Logic will be added later

    }

    @Override
    public void setDefaultAddress(Integer userId,
                                  Integer addressId) {

        // Logic will be added later

    }

}