using System.Collections.Generic;
using System.Threading.Tasks;
using Emart.Api.DTOs;
using Emart.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Emart.Api.Controllers
{
    [ApiController]
    [Route("api/users/me/addresses")]
    [Authorize]
    public class AddressController : ControllerBase
    {
        private readonly IAddressService _addressService;

        public AddressController(IAddressService addressService)
        {
            _addressService = addressService;
        }

        [HttpGet]
        public async Task<ActionResult<ApiResponse<IEnumerable<AddressDto>>>> GetMyAddresses()
        {
            var addresses = await _addressService.GetMyAddressesAsync();
            return Ok(ApiResponse<IEnumerable<AddressDto>>.CreateSuccess("Addresses retrieved successfully", addresses));
        }

        [HttpPost]
        public async Task<ActionResult<ApiResponse<AddressDto>>> AddAddress([FromBody] AddressRequest request)
        {
            var address = await _addressService.AddAddressAsync(request);
            return StatusCode(201, ApiResponse<AddressDto>.CreateSuccess("Address added successfully", address));
        }

        [HttpPut("{addressId}")]
        public async Task<ActionResult<ApiResponse<AddressDto>>> UpdateAddress(int addressId, [FromBody] AddressRequest request)
        {
            var address = await _addressService.UpdateAddressAsync(addressId, request);
            return Ok(ApiResponse<AddressDto>.CreateSuccess("Address updated successfully", address));
        }

        [HttpDelete("{addressId}")]
        public async Task<ActionResult<ApiResponse<object>>> DeleteAddress(int addressId)
        {
            await _addressService.DeleteAddressAsync(addressId);
            return Ok(ApiResponse<object>.CreateSuccess("Address deleted successfully"));
        }

        [HttpPut("{addressId}/default")]
        public async Task<ActionResult<ApiResponse<AddressDto>>> SetDefaultAddress(int addressId)
        {
            var address = await _addressService.SetDefaultAddressAsync(addressId);
            return Ok(ApiResponse<AddressDto>.CreateSuccess("Default address updated", address));
        }
    }
}
