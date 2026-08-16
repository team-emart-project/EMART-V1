package com.example.demo.service;

import com.example.demo.dto.request.AddressRequest;
import com.example.demo.dto.response.AddressResponse;
import com.example.demo.entity.Address;
import com.example.demo.entity.User;
import com.example.demo.enums.AddressType;
import com.example.demo.exception.UnauthorizedActionException;
import com.example.demo.mapper.AddressMapper;
import com.example.demo.repository.AddressRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.SecurityUtils;
import com.example.demo.service.implementation.AddressServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AddressServiceImplTest {

    @Mock private AddressRepository addressRepository;
    @Mock private UserRepository userRepository;
    @Mock private SecurityUtils securityUtils;
    @Spy  private AddressMapper addressMapper = new AddressMapper();

    @InjectMocks private AddressServiceImpl addressService;

    private User me;
    private User someoneElse;

    @BeforeEach
    void setUp() {
        me = User.builder().userId(1).email("me@example.com").isActive(true).build();
        someoneElse = User.builder().userId(2).email("other@example.com").isActive(true).build();

        when(securityUtils.getCurrentUserId()).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(me));
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("the first address a user saves becomes their default automatically")
    void firstAddressBecomesDefault() {
        when(addressRepository.countByUser_UserId(1)).thenReturn(0L);

        AddressResponse response = addressService.addAddress(request(false));

        assertThat(response.getIsDefault()).isTrue();
        // nothing to demote yet
        verify(addressRepository, never()).clearDefaultForUser(anyInt());
    }

    @Test
    @DisplayName("marking a new address default demotes the previous one")
    void newDefaultDemotesPrevious() {
        when(addressRepository.countByUser_UserId(1)).thenReturn(2L);

        addressService.addAddress(request(true));

        verify(addressRepository).clearDefaultForUser(1);
        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressRepository).save(captor.capture());
        assertThat(captor.getValue().getIsDefault()).isTrue();
    }

    @Test
    @DisplayName("a non-default address does not disturb the existing default")
    void nonDefaultLeavesExistingAlone() {
        when(addressRepository.countByUser_UserId(1)).thenReturn(2L);

        addressService.addAddress(request(false));

        verify(addressRepository, never()).clearDefaultForUser(anyInt());
    }

    @Test
    @DisplayName("a user cannot update someone else's address")
    void cannotUpdateForeignAddress() {
        Address foreign = Address.builder()
                .addressId(99).user(someoneElse).isDefault(false).build();
        when(addressRepository.findById(99)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> addressService.updateAddress(99, request(false)))
                .isInstanceOf(UnauthorizedActionException.class);

        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("a user cannot delete someone else's address")
    void cannotDeleteForeignAddress() {
        Address foreign = Address.builder()
                .addressId(99).user(someoneElse).isDefault(false).build();
        when(addressRepository.findById(99)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> addressService.deleteAddress(99))
                .isInstanceOf(UnauthorizedActionException.class);

        verify(addressRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleting the default address promotes another one")
    void deletingDefaultPromotesAnother() {
        Address defaultAddr = Address.builder()
                .addressId(10).user(me).isDefault(true).build();
        Address other = Address.builder()
                .addressId(11).user(me).isDefault(false).build();

        when(addressRepository.findById(10)).thenReturn(Optional.of(defaultAddr));
        when(addressRepository.findByUser_UserIdOrderByIsDefaultDescAddressIdAsc(1))
                .thenReturn(List.of(other));

        addressService.deleteAddress(10);

        verify(addressRepository).delete(defaultAddr);
        assertThat(other.getIsDefault()).isTrue();
    }

    @Test
    @DisplayName("deleting a non-default address promotes nothing")
    void deletingNonDefaultPromotesNothing() {
        Address addr = Address.builder().addressId(12).user(me).isDefault(false).build();
        when(addressRepository.findById(12)).thenReturn(Optional.of(addr));

        addressService.deleteAddress(12);

        verify(addressRepository).delete(addr);
        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("set-default clears the old default first")
    void setDefaultClearsOld() {
        Address addr = Address.builder().addressId(13).user(me).isDefault(false).build();
        when(addressRepository.findById(13)).thenReturn(Optional.of(addr));

        AddressResponse response = addressService.setDefaultAddress(13);

        verify(addressRepository).clearDefaultForUser(1);
        assertThat(response.getIsDefault()).isTrue();
    }

    private AddressRequest request(boolean isDefault) {
        return AddressRequest.builder()
                .addressLine1("221B Baker Street").city("Pune").state("Maharashtra")
                .zipCode("411001").country("India")
                .addressType(AddressType.BOTH).isDefault(isDefault).build();
    }
}
