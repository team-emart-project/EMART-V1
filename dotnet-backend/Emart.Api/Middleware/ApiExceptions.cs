using System;
using System.Net;

namespace Emart.Api.Middleware
{
    /// <summary>
    /// Base for every exception this API raises deliberately.
    ///
    /// Carrying the status code on the exception is what lets one middleware
    /// translate all of them without a chain of `is` checks that has to be
    /// edited every time a new failure mode appears.
    /// </summary>
    public abstract class ApiException : Exception
    {
        public HttpStatusCode StatusCode { get; }
        public string ErrorTitle { get; }

        protected ApiException(HttpStatusCode statusCode, string errorTitle, string message)
            : base(message)
        {
            StatusCode = statusCode;
            ErrorTitle = errorTitle;
        }
    }

    /// <summary>404 — no such product / order / address.</summary>
    public class ResourceNotFoundException : ApiException
    {
        public ResourceNotFoundException(string message)
            : base(HttpStatusCode.NotFound, "Not Found", message) { }

        public ResourceNotFoundException(string resource, string field, object value)
            : base(HttpStatusCode.NotFound, "Not Found",
                   $"{resource} not found with {field}: {value}") { }
    }

    /// <summary>400 — a business rule said no.</summary>
    public class BusinessRuleViolationException : ApiException
    {
        public BusinessRuleViolationException(string message)
            : base(HttpStatusCode.BadRequest, "Bad Request", message) { }
    }

    /// <summary>409 — duplicate email, duplicate card, duplicate wishlist row.</summary>
    public class DuplicateResourceException : ApiException
    {
        public DuplicateResourceException(string message)
            : base(HttpStatusCode.Conflict, "Conflict", message) { }
    }

    /// <summary>401 — bad credentials or a deactivated account.</summary>
    public class InvalidCredentialsException : ApiException
    {
        public InvalidCredentialsException(string message)
            : base(HttpStatusCode.Unauthorized, "Unauthorized", message) { }
    }

    /// <summary>400 — an expired or already-used reset token.</summary>
    public class InvalidTokenException : ApiException
    {
        public InvalidTokenException(string message)
            : base(HttpStatusCode.BadRequest, "Bad Request", message) { }
    }

    /// <summary>403 — the row exists but belongs to someone else.</summary>
    public class UnauthorizedActionException : ApiException
    {
        public UnauthorizedActionException(string message)
            : base(HttpStatusCode.Forbidden, "Forbidden", message) { }
    }
}
