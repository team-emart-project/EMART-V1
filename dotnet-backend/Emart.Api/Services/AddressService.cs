using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Emart.Api.Data;
using Emart.Api.DTOs;
using Emart.Api.Mappings;
using Emart.Api.Middleware;
using Emart.Api.Models;
using Microsoft.EntityFrameworkCore;

namespace Emart.Api.Services
{
    public interface IAddressService
    {
        Task<IEnumerable<AddressDto>> GetMyAddressesAsync();
        Task<AddressDto> AddAddressAsync(AddressRequest request);
        Task<AddressDto> UpdateAddressAsync(int addressId, AddressRequest request);
        Task DeleteAddressAsync(int addressId);
        Task<AddressDto> SetDefaultAddressAsync(int addressId);
    }

    public class AddressService : IAddressService
    {
        private readonly EmartDbContext _context;
        private readonly ISecurityUtils _securityUtils;

        public AddressService(EmartDbContext context, ISecurityUtils securityUtils)
        {
            _context = context;
            _securityUtils = securityUtils;
        }

        public async Task<IEnumerable<AddressDto>> GetMyAddressesAsync()
        {
            int userId = _securityUtils.GetCurrentUserId();

            var addresses = await _context.Addresses
                .AsNoTracking()
                .Where(a => a.UserId == userId)
                .OrderByDescending(a => a.IsDefault)
                .ThenByDescending(a => a.AddressId)
                .ToListAsync();

            return addresses.Select(a => AddressMapper.ToDto(a)!).ToList();
        }

        public async Task<AddressDto> AddAddressAsync(AddressRequest request)
        {
            int userId = _securityUtils.GetCurrentUserId();

            var address = new Address
            {
                UserId = userId,
                AddressLine1 = request.AddressLine1,
                AddressLine2 = request.AddressLine2,
                City = request.City,
                State = request.State,
                ZipCode = request.ZipCode,
                Country = string.IsNullOrWhiteSpace(request.Country) ? "India" : request.Country,
                AddressType = request.AddressType,
                IsDefault = request.IsDefault,
                CreatedAt = DateTime.Now
            };

            bool hasExisting = await _context.Addresses.AnyAsync(a => a.UserId == userId);

            if (!hasExisting)
            {
                // The FIRST address a user saves becomes the default, so
                // checkout always has something to pre-select.
                address.IsDefault = true;
            }
            else if (address.IsDefault)
            {
                await DemoteCurrentDefaultAsync(userId);
            }

            _context.Addresses.Add(address);
            await _context.SaveChangesAsync();

            return AddressMapper.ToDto(address)!;
        }

        public async Task<AddressDto> UpdateAddressAsync(int addressId, AddressRequest request)
        {
            var address = await LoadOwnedAddressAsync(addressId);

            address.AddressLine1 = request.AddressLine1;
            address.AddressLine2 = request.AddressLine2;
            address.City = request.City;
            address.State = request.State;
            address.ZipCode = request.ZipCode;
            address.Country = string.IsNullOrWhiteSpace(request.Country) ? address.Country : request.Country;
            address.AddressType = request.AddressType;

            if (request.IsDefault && !address.IsDefault)
            {
                await DemoteCurrentDefaultAsync(address.UserId, address.AddressId);
                address.IsDefault = true;
            }

            await _context.SaveChangesAsync();
            return AddressMapper.ToDto(address)!;
        }

        public async Task DeleteAddressAsync(int addressId)
        {
            var address = await LoadOwnedAddressAsync(addressId);
            bool wasDefault = address.IsDefault;
            int userId = address.UserId;

            _context.Addresses.Remove(address);
            await _context.SaveChangesAsync();

            // Deleting the default promotes the next one, so a user who still
            // has addresses always has a default among them.
            if (wasDefault)
            {
                var next = await _context.Addresses
                    .Where(a => a.UserId == userId)
                    .OrderBy(a => a.AddressId)
                    .FirstOrDefaultAsync();

                if (next != null)
                {
                    next.IsDefault = true;
                    await _context.SaveChangesAsync();
                }
            }
        }

        public async Task<AddressDto> SetDefaultAddressAsync(int addressId)
        {
            var address = await LoadOwnedAddressAsync(addressId);

            // Promoting one demotes the previous default in the same save, so
            // there is never a moment with two defaults or none.
            await DemoteCurrentDefaultAsync(address.UserId, address.AddressId);
            address.IsDefault = true;
            await _context.SaveChangesAsync();

            return AddressMapper.ToDto(address)!;
        }

        // ------------------------------------------------------------------

        private async Task<Address> LoadOwnedAddressAsync(int addressId)
        {
            int userId = _securityUtils.GetCurrentUserId();

            var address = await _context.Addresses.FirstOrDefaultAsync(a => a.AddressId == addressId)
                          ?? throw new ResourceNotFoundException("Address", "addressId", addressId);

            if (address.UserId != userId)
            {
                throw new UnauthorizedActionException("That address does not belong to you");
            }
            return address;
        }

        private async Task DemoteCurrentDefaultAsync(int userId, int? excludeId = null)
        {
            var current = await _context.Addresses
                .Where(a => a.UserId == userId && a.IsDefault
                            && (excludeId == null || a.AddressId != excludeId))
                .ToListAsync();

            foreach (var a in current) a.IsDefault = false;
        }
    }
}
