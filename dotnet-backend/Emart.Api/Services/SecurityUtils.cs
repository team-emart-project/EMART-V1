using System;
using System.Security.Claims;
using Microsoft.AspNetCore.Http;

namespace Emart.Api.Services
{
    public interface ISecurityUtils
    {
        /// <summary>The signed-in user's id. Throws when there is no valid JWT.</summary>
        int GetCurrentUserId();

        /// <summary>Null for an anonymous visitor. Used by endpoints that are
        /// public but behave differently when signed in — the product listing
        /// only shows member pricing to a cardholder, for instance.</summary>
        int? GetCurrentUserIdOrNull();
    }

    public class SecurityUtils : ISecurityUtils
    {
        private readonly IHttpContextAccessor _httpContextAccessor;

        public SecurityUtils(IHttpContextAccessor httpContextAccessor)
        {
            _httpContextAccessor = httpContextAccessor;
        }

        public int GetCurrentUserId()
        {
            var userId = GetCurrentUserIdOrNull();
            if (userId == null)
            {
                throw new UnauthorizedAccessException("You need to be signed in to do that");
            }
            return userId.Value;
        }

        public int? GetCurrentUserIdOrNull()
        {
            var principal = _httpContextAccessor.HttpContext?.User;
            if (principal?.Identity?.IsAuthenticated != true) return null;

            // "sub" and "nameid" are both checked because whether the JWT
            // handler remaps them to ClaimTypes.NameIdentifier depends on the
            // inbound claim map, which this app clears.
            var claim = principal.FindFirst(ClaimTypes.NameIdentifier)
                        ?? principal.FindFirst("sub")
                        ?? principal.FindFirst("nameid");

            return claim != null && int.TryParse(claim.Value, out int userId) ? userId : null;
        }
    }
}
