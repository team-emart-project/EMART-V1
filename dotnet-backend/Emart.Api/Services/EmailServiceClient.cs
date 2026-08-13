using System;
using System.Net.Http;
using System.Net.Http.Json;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using Emart.Api.DTOs;
using Microsoft.Extensions.Options;

namespace Emart.Api.Services
{
    /// <summary>Bound from the "EmailService" section of appsettings.json.</summary>
    public class EmailServiceOptions
    {
        public const string SectionName = "EmailService";

        /// <summary>
        /// False turns the integration off: the order still saves, one line is
        /// logged and no HTTP call is attempted. Set it false whenever the
        /// microservice is not running, or every checkout logs a refused
        /// connection.
        /// </summary>
        public bool Enabled { get; set; } = true;

        /// <summary>Where backend-email-microservice listens. It defaults to 8082.</summary>
        public string BaseUrl { get; set; } = "http://localhost:8082";

        /// <summary>Must match emart.email.api-key in the microservice.</summary>
        public string ApiKey { get; set; } = string.Empty;

        /// <summary>Whole-request budget. The endpoint answers 202 without doing
        /// real work, so anything slower means it is down or hung.</summary>
        public int TimeoutSeconds { get; set; } = 5;
    }

    public interface IEmailServiceClient
    {
        /// <summary>
        /// Asks the email microservice to send an order-placed confirmation.
        /// Returns true when it accepted the request (202).
        /// </summary>
        Task<bool> SendOrderPlacedAsync(OrderEmailPayload payload,
                                        CancellationToken cancellationToken = default);
    }

    /// <summary>
    /// The one place this backend calls backend-email-microservice.
    ///
    /// IT NEVER THROWS. By the time it runs the order is committed — the
    /// customer's cart is empty and the row exists. Letting a notification
    /// failure bubble up would turn a successful checkout into a 500 and leave
    /// the shopper convinced their order did not go through, which is far worse
    /// than a missing email.
    ///
    /// Mirrors backend/src/main/java/com/example/demo/client/EmailServiceClient.java
    /// so the two backends behave identically from the microservice's side.
    /// </summary>
    public class EmailServiceClient : IEmailServiceClient
    {
        public const string ApiKeyHeader = "X-API-Key";
        private const string SendOrderEmailPath = "/api/send-order-email";

        /// <summary>
        /// camelCase on the wire. The MVC pipeline's JSON settings configured in
        /// Program.cs apply to RESPONSES only — an HttpClient serialises with
        /// whatever options it is handed, and the default is PascalCase, which
        /// Jackson on the other side would read as a payload of nulls.
        ///
        /// Public so OrderEmailPayloadTests can assert the exact JSON that goes
        /// on the wire. That cross-language contract is the one thing here no
        /// compiler checks, in either project.
        /// </summary>
        public static readonly JsonSerializerOptions JsonOptions = new()
        {
            PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
            DefaultIgnoreCondition = System.Text.Json.Serialization.JsonIgnoreCondition.WhenWritingNull
        };

        private readonly HttpClient _http;
        private readonly EmailServiceOptions _options;
        private readonly ILogger<EmailServiceClient> _logger;

        public EmailServiceClient(HttpClient http,
                                  IOptions<EmailServiceOptions> options,
                                  ILogger<EmailServiceClient> logger)
        {
            _http = http;
            _options = options.Value;
            _logger = logger;
        }

        public async Task<bool> SendOrderPlacedAsync(OrderEmailPayload payload,
                                                     CancellationToken cancellationToken = default)
        {
            if (!_options.Enabled)
            {
                _logger.LogDebug("EmailService:Enabled=false — not notifying for orderNo={OrderNo}",
                    payload.Order.OrderNo);
                return false;
            }

            try
            {
                using var response = await _http.PostAsJsonAsync(
                    SendOrderEmailPath, payload, JsonOptions, cancellationToken);

                if (!response.IsSuccessStatusCode)
                {
                    // Read the body: the microservice returns the same
                    // {error, message, fieldErrors} envelope this API does, and
                    // on a 400 it names the exact field that was wrong.
                    string body = await response.Content.ReadAsStringAsync(cancellationToken);

                    _logger.LogWarning(
                        "Email service rejected orderNo={OrderNo} with {StatusCode}: {Body}",
                        payload.Order.OrderNo, (int)response.StatusCode, body);
                    return false;
                }

                _logger.LogInformation("Order email requested for orderNo={OrderNo} via {BaseUrl}",
                    payload.Order.OrderNo, _options.BaseUrl);
                return true;
            }
            catch (Exception ex) when (ex is HttpRequestException or TaskCanceledException)
            {
                // Service down, DNS wrong, or slower than the timeout. All the
                // same to us: no email, order unaffected.
                _logger.LogWarning(ex,
                    "Could not reach the email service for orderNo={OrderNo}. " +
                    "The order is fine; only the confirmation email was not sent.",
                    payload.Order.OrderNo);
                return false;
            }
        }
    }
}
