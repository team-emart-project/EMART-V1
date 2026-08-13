using System;

namespace Emart.Api.DTOs
{
    public class UserDto
    {
        public int UserId { get; set; }
        public string MembershipNo { get; set; } = string.Empty;
        public string FirstName { get; set; } = string.Empty;
        public string? LastName { get; set; }
        public string Email { get; set; } = string.Empty;
        public string? Phone { get; set; }

        // DateOnly, not DateTime: dob is a DATE column, and the profile form
        // binds it to <input type="date">, which silently renders empty when
        // handed "1991-02-11T00:00:00" instead of "1991-02-11".
        public DateOnly? Dob { get; set; }

        public string? Gender { get; set; }
        public string? Education { get; set; }
        public string? Occupation { get; set; }
        public decimal? AnnualIncome { get; set; }
        public bool MarketingConsent { get; set; }
        public string Role { get; set; } = "CUSTOMER";

        // Named `cardholder` / `active` in the contract, not `isCardholder`.
        public bool Cardholder { get; set; }
        public bool Active { get; set; }

        public string AuthProvider { get; set; } = "LOCAL";
        public string? ProfileImageUrl { get; set; }
        public DateTime CreatedAt { get; set; }
    }

    /// <summary>
    /// email, password, role, membershipNo and isCardholder are absent on
    /// purpose: a field that does not exist cannot be mass-assigned by a
    /// crafted request body.
    /// </summary>
    public class UpdateProfileRequest
    {
        public string? FirstName { get; set; }
        public string? LastName { get; set; }
        public string? Phone { get; set; }
        public DateOnly? Dob { get; set; }
        public string? Gender { get; set; }
        public string? Education { get; set; }
        public string? Occupation { get; set; }
        public decimal? AnnualIncome { get; set; }
        public bool? MarketingConsent { get; set; }
    }
}
