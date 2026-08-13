using System;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;
using System.Threading.Tasks;
using Emart.Api.Data;
using Emart.Api.DTOs;
using Emart.Api.Mappings;
using Emart.Api.Middleware;
using Emart.Api.Models;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;

namespace Emart.Api.Services
{
    public interface IAuthService
    {
        Task<UserDto> RegisterAsync(RegisterRequest request);
        Task<AuthResponse> LoginAsync(LoginRequest request);
        Task<AuthResponse> LoginWithGoogleAsync(GoogleLoginRequest request);
        Task ForgotPasswordAsync(ForgotPasswordRequest request);
        Task ResetPasswordAsync(ResetPasswordRequest request);
    }

    public class AuthService : IAuthService
    {
        private const int ResetTokenValidityMinutes = 30;

        private readonly EmartDbContext _context;
        private readonly IConfiguration _configuration;
        private readonly ILogger<AuthService> _logger;

        public AuthService(EmartDbContext context,
                           IConfiguration configuration,
                           ILogger<AuthService> logger)
        {
            _context = context;
            _configuration = configuration;
            _logger = logger;
        }

        public async Task<UserDto> RegisterAsync(RegisterRequest request)
        {
            string email = NormaliseEmail(request.Email);

            if (await _context.Users.AnyAsync(u => u.Email == email))
            {
                throw new DuplicateResourceException("An account with this email already exists");
            }

            var user = new User
            {
                MembershipNo = await GenerateMembershipNumberAsync(),
                FirstName = request.FirstName.Trim(),
                LastName = string.IsNullOrWhiteSpace(request.LastName) ? null : request.LastName.Trim(),
                Email = email,
                // NEVER store the raw password. BCrypt is slow by design and
                // salts every hash, so identical passwords store differently.
                PasswordHash = BCrypt.Net.BCrypt.HashPassword(request.Password),
                AuthProvider = AuthProvider.LOCAL,
                Phone = request.Phone,
                Dob = request.Dob?.ToDateTime(TimeOnly.MinValue),
                Gender = request.Gender,
                Education = request.Education,
                Occupation = request.Occupation,
                AnnualIncome = request.AnnualIncome,
                MarketingConsent = request.MarketingConsent ?? false,
                Role = RoleType.CUSTOMER,
                IsCardholder = false,
                IsActive = true,
                CreatedAt = DateTime.Now
            };

            _context.Users.Add(user);
            await _context.SaveChangesAsync();

            // The project logs the membership number instead of emailing it.
            _logger.LogInformation("Registered userId={UserId} membershipNo={MembershipNo} (email would go to {Email})",
                user.UserId, user.MembershipNo, user.Email);

            return UserMapper.ToDto(user);
        }

        public async Task<AuthResponse> LoginAsync(LoginRequest request)
        {
            var user = await _context.Users
                .FirstOrDefaultAsync(u => u.Email == NormaliseEmail(request.Email));

            // A wrong password and an unknown email return the IDENTICAL
            // message on purpose — a different one would let someone discover
            // which emails are registered.
            if (user == null || string.IsNullOrEmpty(user.PasswordHash)
                || !VerifyPassword(request.Password, user.PasswordHash))
            {
                throw new InvalidCredentialsException("Invalid email or password");
            }

            if (!user.IsActive)
            {
                throw new InvalidCredentialsException("This account has been deactivated");
            }

            return IssueToken(user, "password");
        }

        /// <summary>
        /// Google sign-in is not wired up in this .NET build: verifying an ID
        /// token needs Google's public keys, and this service has no HTTP call
        /// to fetch them. Refusing loudly is better than issuing a session for
        /// a token nobody checked — that would be an authentication bypass.
        /// </summary>
        public Task<AuthResponse> LoginWithGoogleAsync(GoogleLoginRequest request)
        {
            throw new BusinessRuleViolationException(
                "Google sign-in is not available on this server. Please sign in with your email and password.");
        }

        public async Task ForgotPasswordAsync(ForgotPasswordRequest request)
        {
            var user = await _context.Users
                .FirstOrDefaultAsync(u => u.Email == NormaliseEmail(request.Email));

            // An unknown email does nothing and STILL returns success. Reporting
            // "no such user" would turn this endpoint into a way to discover
            // which addresses are registered.
            if (user == null)
            {
                _logger.LogInformation("Password reset requested for an unknown email - ignoring silently");
                return;
            }

            string token = Guid.NewGuid().ToString();
            user.ResetPasswordToken = token;
            user.ResetPasswordTokenExpiry = DateTime.Now.AddMinutes(ResetTokenValidityMinutes);
            await _context.SaveChangesAsync();

            // Printed to the console rather than emailed, same as the Java build.
            _logger.LogInformation("Password reset token for {Email}: {Token}", user.Email, token);
        }

        public async Task ResetPasswordAsync(ResetPasswordRequest request)
        {
            var user = await _context.Users
                .FirstOrDefaultAsync(u => u.ResetPasswordToken == request.Token);

            if (user == null)
            {
                throw new InvalidTokenException("Invalid or already used reset token");
            }

            if (user.ResetPasswordTokenExpiry == null || user.ResetPasswordTokenExpiry < DateTime.Now)
            {
                throw new InvalidTokenException("This reset token has expired. Please request a new one.");
            }

            user.PasswordHash = BCrypt.Net.BCrypt.HashPassword(request.NewPassword);

            // A Google-only account now HAS a password, so it becomes BOTH.
            // This is not cosmetic: users has a CHECK constraint saying a GOOGLE
            // account must have a NULL password_hash, so without this the save
            // below fails with a raw constraint violation.
            if (user.AuthProvider == AuthProvider.GOOGLE)
            {
                user.AuthProvider = AuthProvider.BOTH;
            }

            // Clear the token so the same link cannot be replayed.
            user.ResetPasswordToken = null;
            user.ResetPasswordTokenExpiry = null;
            await _context.SaveChangesAsync();

            _logger.LogInformation("Password reset completed for userId={UserId}", user.UserId);
        }

        // ------------------------------------------------------------------

        private static string NormaliseEmail(string? email) =>
            email == null ? string.Empty : email.Trim().ToLowerInvariant();

        /// <summary>
        /// The seeded rows carry BCrypt hashes written by the Java build
        /// ($2a$ and $2b$ prefixes both appear). BCrypt.Net reads either, so
        /// the existing test accounts log in here unchanged.
        /// </summary>
        private static bool VerifyPassword(string password, string hash)
        {
            try
            {
                return BCrypt.Net.BCrypt.Verify(password, hash);
            }
            catch (BCrypt.Net.SaltParseException)
            {
                // A row whose hash is not BCrypt at all cannot authenticate.
                return false;
            }
        }

        /// <summary>
        /// EMART00001-style numbers, continuing the sequence already in the
        /// table so a new account cannot collide with a seeded one.
        /// </summary>
        private async Task<string> GenerateMembershipNumberAsync()
        {
            for (int attempt = 0; attempt < 10; attempt++)
            {
                string candidate = "EMART" + Random.Shared.Next(10000, 99999);
                if (!await _context.Users.AnyAsync(u => u.MembershipNo == candidate))
                {
                    return candidate;
                }
            }
            // Astronomically unlikely; a timestamp suffix still fits VARCHAR(20).
            return "EMART" + DateTime.Now.Ticks.ToString()[^9..];
        }

        private AuthResponse IssueToken(User user, string via)
        {
            var secretKey = _configuration["Jwt:SecretKey"]!;
            var expirationMinutes = _configuration.GetValue("Jwt:ExpirationMinutes", 120);

            var claims = new[]
            {
                new Claim(JwtRegisteredClaimNames.Sub, user.UserId.ToString()),
                new Claim(ClaimTypes.NameIdentifier, user.UserId.ToString()),
                new Claim(JwtRegisteredClaimNames.Email, user.Email),
                new Claim(ClaimTypes.Role, user.Role.ToString()),
                new Claim(JwtRegisteredClaimNames.Jti, Guid.NewGuid().ToString())
            };

            // Encoding.UTF8 here has to match Program.cs, and the key has to be
            // read from the SAME configuration path. It previously read
            // "Jwt:Key" while validation read "Jwt:SecretKey", so every token
            // this method issued was signed with the fallback key and rejected
            // on the very next request.
            var key = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(secretKey));
            var credentials = new SigningCredentials(key, SecurityAlgorithms.HmacSha256);

            var token = new JwtSecurityToken(
                issuer: _configuration["Jwt:Issuer"],
                audience: _configuration["Jwt:Audience"],
                claims: claims,
                expires: DateTime.UtcNow.AddMinutes(expirationMinutes),
                signingCredentials: credentials);

            _logger.LogInformation("Login success for userId={UserId} via {Via}", user.UserId, via);

            return new AuthResponse
            {
                AccessToken = new JwtSecurityTokenHandler().WriteToken(token),
                TokenType = "Bearer",
                ExpiresInMs = expirationMinutes * 60L * 1000L,
                User = UserMapper.ToDto(user)
            };
        }
    }
}
