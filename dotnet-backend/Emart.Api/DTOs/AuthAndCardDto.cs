using System;
using System.ComponentModel.DataAnnotations;

namespace Emart.Api.DTOs
{
    public class RegisterRequest
    {
        [Required(ErrorMessage = "First name is required")]
        public string FirstName { get; set; } = string.Empty;

        public string? LastName { get; set; }

        [Required(ErrorMessage = "Email is required")]
        [EmailAddress(ErrorMessage = "Email must be a valid address")]
        public string Email { get; set; } = string.Empty;

        [Required(ErrorMessage = "Password is required")]
        [MinLength(8, ErrorMessage = "Password must be at least 8 characters")]
        [RegularExpression(@"^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$",
            ErrorMessage = "Password must contain an uppercase letter, a lowercase letter and a digit")]
        public string Password { get; set; } = string.Empty;

        public string? Phone { get; set; }
        public DateOnly? Dob { get; set; }
        public string? Gender { get; set; }
        public string? Education { get; set; }
        public string? Occupation { get; set; }
        public decimal? AnnualIncome { get; set; }
        public bool? MarketingConsent { get; set; }
    }

    public class LoginRequest
    {
        [Required]
        public string Email { get; set; } = string.Empty;

        [Required]
        public string Password { get; set; } = string.Empty;
    }

    public class GoogleLoginRequest
    {
        /// <summary>The ID token the browser received from Google.</summary>
        public string Credential { get; set; } = string.Empty;
    }

    public class ForgotPasswordRequest
    {
        [Required]
        [EmailAddress]
        public string Email { get; set; } = string.Empty;
    }

    public class ResetPasswordRequest
    {
        [Required]
        public string Token { get; set; } = string.Empty;

        [Required]
        [MinLength(8, ErrorMessage = "Password must be at least 8 characters")]
        [RegularExpression(@"^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$",
            ErrorMessage = "Password must contain an uppercase letter, a lowercase letter and a digit")]
        public string NewPassword { get; set; } = string.Empty;
    }

    public class AuthResponse
    {
        // `accessToken`, not `token` — that is the field the client reads.
        public string AccessToken { get; set; } = string.Empty;
        public string TokenType { get; set; } = "Bearer";
        public long ExpiresInMs { get; set; }
        public UserDto User { get; set; } = new UserDto();
    }

    public class EmartCardApplicationRequest
    {
        public string? EmploymentDetails { get; set; }
        public string? BankAccountNo { get; set; }
        public string? PanNumber { get; set; }
    }

    public class EmartCardDto
    {
        public int CardId { get; set; }
        public string CardNumber { get; set; } = string.Empty;
        public string Status { get; set; } = string.Empty;

        // DATE columns in the schema, so they serialize as "2026-07-29"
        // rather than a midnight timestamp.
        public DateOnly ApplicationDate { get; set; }
        public DateOnly? ApprovalDate { get; set; }
        public int PointsBalance { get; set; }
        public string? EmploymentDetails { get; set; }

        // The PAN is never returned at all and the account number is masked.
        public string? BankAccountMasked { get; set; }
    }

    public class PointsBalanceResponse
    {
        public bool Cardholder { get; set; }
        public int PointsBalance { get; set; }
        public string CardStatus { get; set; } = "NONE";
    }
}
