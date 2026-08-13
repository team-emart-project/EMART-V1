using Microsoft.AspNetCore.Http;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using System;
using System.Collections.Generic;
using System.Net;
using System.Text.Json;
using System.Text.Json.Serialization;
using System.Threading.Tasks;

namespace Emart.Api.Middleware
{
    /// <summary>
    /// Turns every exception into the ONE error shape the client already knows
    /// how to read:
    ///
    ///   { success, status, error, message, path, timestamp[, fieldErrors] }
    ///
    /// The axios interceptor reads `message` and `fieldErrors` off this body,
    /// so anything that escaped here as a stack trace or an ASP.NET
    /// ProblemDetails would surface in the UI as "Something went wrong".
    /// </summary>
    public class GlobalExceptionMiddleware
    {
        private static readonly JsonSerializerOptions JsonOptions = new()
        {
            PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
            DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull
        };

        private readonly RequestDelegate _next;
        private readonly ILogger<GlobalExceptionMiddleware> _logger;
        private readonly IHostEnvironment _environment;

        public GlobalExceptionMiddleware(RequestDelegate next,
                                         ILogger<GlobalExceptionMiddleware> logger,
                                         IHostEnvironment environment)
        {
            _next = next;
            _logger = logger;
            _environment = environment;
        }

        public async Task InvokeAsync(HttpContext context)
        {
            try
            {
                await _next(context);
            }
            catch (Exception ex)
            {
                await HandleExceptionAsync(context, ex);
            }
        }

        private async Task HandleExceptionAsync(HttpContext context, Exception exception)
        {
            if (context.Response.HasStarted)
            {
                // Too late to rewrite the response; all we can do is record it.
                _logger.LogError(exception, "Exception after the response had already started");
                throw exception;
            }

            var (status, title, message) = Translate(exception);

            if ((int)status >= 500)
            {
                _logger.LogError(exception, "Unhandled exception on {Path}", context.Request.Path);
            }
            else
            {
                _logger.LogWarning("{Status} on {Path}: {Message}",
                    (int)status, context.Request.Path, message);
            }

            context.Response.Clear();
            context.Response.ContentType = "application/json";
            context.Response.StatusCode = (int)status;

            var body = new Dictionary<string, object?>
            {
                ["success"] = false,
                ["status"] = (int)status,
                ["error"] = title,
                ["message"] = message,
                ["path"] = context.Request.Path.Value,
                ["timestamp"] = DateTime.Now
            };

            // The real exception text is useful while developing and is a
            // disclosure risk in production, so it is only attached in Development.
            if ((int)status >= 500 && _environment.IsDevelopment())
            {
                body["detail"] = exception.ToString();
            }

            await context.Response.WriteAsync(JsonSerializer.Serialize(body, JsonOptions));
        }

        private static (HttpStatusCode Status, string Title, string Message) Translate(Exception exception)
        {
            return exception switch
            {
                ApiException api => (api.StatusCode, api.ErrorTitle, api.Message),

                // Thrown by SecurityUtils when there is no usable JWT.
                UnauthorizedAccessException => (HttpStatusCode.Unauthorized, "Unauthorized", exception.Message),

                _ => (HttpStatusCode.InternalServerError, "Internal Server Error",
                      "Something went wrong on the server. Please try again.")
            };
        }
    }
}
