using System.Threading.Tasks;
using Emart.Api.DTOs;
using Emart.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Emart.Api.Controllers
{
    /// <summary>
    /// There is no "get user by id" and no "list all users" here on purpose.
    /// The only profile a signed-in customer may read or write is their own,
    /// resolved from the JWT — so there is no id in any of these URLs to
    /// substitute with somebody else's.
    /// </summary>
    [ApiController]
    [Route("api/users")]
    [Authorize]
    public class UsersController : ControllerBase
    {
        private readonly IUserService _userService;

        public UsersController(IUserService userService)
        {
            _userService = userService;
        }

        [HttpGet("me")]
        public async Task<ActionResult<ApiResponse<UserDto>>> GetMyProfile()
        {
            var user = await _userService.GetMyProfileAsync();
            return Ok(ApiResponse<UserDto>.CreateSuccess("Profile retrieved successfully", user));
        }

        [HttpPut("me")]
        public async Task<ActionResult<ApiResponse<UserDto>>> UpdateMyProfile([FromBody] UpdateProfileRequest request)
        {
            var user = await _userService.UpdateMyProfileAsync(request);
            return Ok(ApiResponse<UserDto>.CreateSuccess("Profile updated successfully", user));
        }
    }
}
