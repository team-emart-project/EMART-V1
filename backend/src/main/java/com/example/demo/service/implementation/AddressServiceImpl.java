package com.example.demo.service.implementation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.demo.dto.request.AddressRequest;
import com.example.demo.dto.response.AddressResponse;
import com.example.demo.entity.Address;
import com.example.demo.entity.User;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.UnauthorizedActionException;
import com.example.demo.mapper.AddressMapper;
import com.example.demo.repository.AddressRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.SecurityUtils;
import com.example.demo.service.interfaces.AddressService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AddressServiceImpl implements AddressService {
    private static final Logger log = LoggerFactory.getLogger(AddressServiceImpl.class);

    public AddressServiceImpl(AddressRepository addressRepository, UserRepository userRepository, AddressMapper addressMapper, SecurityUtils securityUtils) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.addressMapper = addressMapper;
        this.securityUtils = securityUtils;
    }


    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final AddressMapper addressMapper;
    private final SecurityUtils securityUtils;

    @Override
    public List<AddressResponse> getMyAddresses() {
        Integer userId = securityUtils.getCurrentUserId();
        return addressRepository.findByUser_UserIdOrderByIsDefaultDescAddressIdAsc(userId).stream()
                .map(addressMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public AddressResponse addAddress(AddressRequest request) {

        User user = loadCurrentUser();

        // The very first address a user saves becomes their default
        // automatically — otherwise checkout would have nothing pre-selected.
        boolean isFirst = addressRepository.countByUser_UserId(user.getUserId()) == 0;
        boolean makeDefault = isFirst || Boolean.TRUE.equals(request.getIsDefault());

        if (makeDefault && !isFirst) {
            addressRepository.clearDefaultForUser(user.getUserId());
        }

        Address address = Address.builder()
                .user(user)
                .addressLine1(request.getAddressLine1().trim())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity().trim())
                .state(request.getState().trim())
                .zipCode(request.getZipCode().trim())
                .country(request.getCountry().trim())
                .addressType(request.getAddressType())
                .isDefault(makeDefault)
                .build();

        Address saved = addressRepository.save(address);
        log.debug("Added addressId={} for userId={}", saved.getAddressId(), user.getUserId());

        return addressMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(Integer addressId, AddressRequest request) {

        Address address = loadOwnedAddress(addressId);

        address.setAddressLine1(request.getAddressLine1().trim());
        address.setAddressLine2(request.getAddressLine2());
        address.setCity(request.getCity().trim());
        address.setState(request.getState().trim());
        address.setZipCode(request.getZipCode().trim());
        address.setCountry(request.getCountry().trim());
        address.setAddressType(request.getAddressType());

        // Promoting this one to default demotes whichever was default before.
        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(address.getIsDefault())) {
            addressRepository.clearDefaultForUser(address.getUser().getUserId());
            address.setIsDefault(true);
        }

        return addressMapper.toResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public void deleteAddress(Integer addressId) {

        Address address = loadOwnedAddress(addressId);
        Integer userId = address.getUser().getUserId();
        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());

        addressRepository.delete(address);

        // Never leave the user with addresses but no default — promote the
        // oldest remaining one.
        if (wasDefault) {
            addressRepository.findByUser_UserIdOrderByIsDefaultDescAddressIdAsc(userId).stream()
                    .findFirst()
                    .ifPresent(next -> {
                        next.setIsDefault(true);
                        addressRepository.save(next);
                        log.debug("Promoted addressId={} to default for userId={}",
                                next.getAddressId(), userId);
                    });
        }
    }

    @Override
    @Transactional
    public AddressResponse setDefaultAddress(Integer addressId) {

        Address address = loadOwnedAddress(addressId);

        addressRepository.clearDefaultForUser(address.getUser().getUserId());
        address.setIsDefault(true);

        return addressMapper.toResponse(addressRepository.save(address));
    }

    // ------------------------------------------------------------------

    private User loadCurrentUser() {
        Integer userId = securityUtils.getCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));
    }

    /**
     * Loads an address and proves it belongs to the caller.
     * Without this, changing the id in the URL would expose someone else's
     * home address — a real privacy leak, not just a bug.
     */
    private Address loadOwnedAddress(Integer addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "addressId", addressId));

        if (!address.getUser().getUserId().equals(securityUtils.getCurrentUserId())) {
            throw new UnauthorizedActionException("This address does not belong to the current user");
        }
        return address;
    }
}
