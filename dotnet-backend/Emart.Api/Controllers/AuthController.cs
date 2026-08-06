using System.Threading.Tasks;
using Emart.Api.DTOs;
using Emart.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Emart.Api.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class AuthController : ControllerBase
    {
        private readonly IAuthService _authService;

        public AuthController(IAuthService authService)
        {
            _authService = authService;
        }

        [HttpPost("register")]
        public async Task<ActionResult<ApiResponse<UserDto>>> Register([FromBody] RegisterRequest request)
        {
            var user = await _authService.RegisterAsync(request);
            return StatusCode(201, ApiResponse<UserDto>.CreateSuccess("Registration successful. Your membership number has been emailed to you.", user));
        }

        [HttpPost("login")]
        public async Task<ActionResult<ApiResponse<AuthResponse>>> Login([FromBody] LoginRequest request)
        {
            var response = await _authService.LoginAsync(request);
            return Ok(ApiResponse<AuthResponse>.CreateSuccess("Login successful", response));
        }

        [HttpPost("google")]
        public async Task<ActionResult<ApiResponse<AuthResponse>>> LoginWithGoogle([FromBody] GoogleLoginRequest request)
        {
            var response = await _authService.LoginWithGoogleAsync(request);
            return Ok(ApiResponse<AuthResponse>.CreateSuccess("Signed in with Google", response));
        }

        [HttpPost("logout")]
        [Authorize]
        public ActionResult<ApiResponse<object>> Logout()
        {
            // Stateless JWT logout is handled client-side.
            return Ok(ApiResponse<object>.CreateSuccess("Logged out. Please discard the access token on the client."));
        }

        [HttpPost("forgot-password")]
        public async Task<ActionResult<ApiResponse<object>>> ForgotPassword([FromBody] ForgotPasswordRequest request)
        {
            await _authService.ForgotPasswordAsync(request);
            return Ok(ApiResponse<object>.CreateSuccess("If that email is registered, a password reset link has been sent to it."));
        }

        [HttpPost("reset-password")]
        public async Task<ActionResult<ApiResponse<object>>> ResetPassword([FromBody] ResetPasswordRequest request)
        {
            await _authService.ResetPasswordAsync(request);
            return Ok(ApiResponse<object>.CreateSuccess("Password has been reset. You can now log in with your new password."));
        }
    }
}
