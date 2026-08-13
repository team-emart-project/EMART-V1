using System;
using System.Threading.Tasks;
using Emart.Api.Data;
using Emart.Api.DTOs;
using Emart.Api.Mappings;
using Emart.Api.Middleware;
using Emart.Api.Models;
using Microsoft.EntityFrameworkCore;

namespace Emart.Api.Services
{
    public interface IUserService
    {
        Task<UserDto> GetMyProfileAsync();
        Task<UserDto> UpdateMyProfileAsync(UpdateProfileRequest request);
    }

    public class UserService : IUserService
    {
        private readonly EmartDbContext _context;
        private readonly ISecurityUtils _securityUtils;
        private readonly ICardholderService _cardholderService;

        public UserService(EmartDbContext context,
                           ISecurityUtils securityUtils,
                           ICardholderService cardholderService)
        {
            _context = context;
            _securityUtils = securityUtils;
            _cardholderService = cardholderService;
        }

        public async Task<UserDto> GetMyProfileAsync()
        {
            var user = await LoadCurrentUserAsync();
            var dto = UserMapper.ToDto(user);

            // Read from emart_card.status rather than the denormalised
            // users.is_cardholder flag — the two have drifted apart before.
            dto.Cardholder = await _cardholderService.IsActiveCardholderAsync(user.UserId);
            return dto;
        }

        public async Task<UserDto> UpdateMyProfileAsync(UpdateProfileRequest request)
        {
            var user = await LoadCurrentUserAsync();

            // Only these nine fields can ever be written from a request body.
            // Email, role, membershipNo and isCardholder are not on the request
            // DTO at all, so no crafted body can reach them.
            if (request.FirstName != null) user.FirstName = request.FirstName.Trim();
            if (request.LastName != null) user.LastName = request.LastName.Trim();
            if (request.Phone != null) user.Phone = request.Phone;
            if (request.Dob != null) user.Dob = request.Dob.Value.ToDateTime(TimeOnly.MinValue);
            if (request.Gender != null) user.Gender = request.Gender;
            if (request.Education != null) user.Education = request.Education;
            if (request.Occupation != null) user.Occupation = request.Occupation;
            if (request.AnnualIncome != null) user.AnnualIncome = request.AnnualIncome;
            if (request.MarketingConsent != null) user.MarketingConsent = request.MarketingConsent.Value;

            user.UpdatedAt = DateTime.Now;
            await _context.SaveChangesAsync();

            var dto = UserMapper.ToDto(user);
            dto.Cardholder = await _cardholderService.IsActiveCardholderAsync(user.UserId);
            return dto;
        }

        private async Task<User> LoadCurrentUserAsync()
        {
            int userId = _securityUtils.GetCurrentUserId();
            return await _context.Users.FirstOrDefaultAsync(u => u.UserId == userId)
                   ?? throw new ResourceNotFoundException("User", "userId", userId);
        }
    }
}
