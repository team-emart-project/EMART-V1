using System.Linq;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;
using Emart.Api.Data;
using Emart.Api.Middleware;
using Emart.Api.Services;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
<<<<<<< HEAD
=======
using Microsoft.Extensions.Options;
>>>>>>> d5373e2ef28bd43e67b12b3e8d1dcff71723abeb
using Microsoft.IdentityModel.JsonWebTokens;
using Microsoft.IdentityModel.Tokens;

var builder = WebApplication.CreateBuilder(args);

// ---------------------------------------------------------------------------
// MVC + JSON
// ---------------------------------------------------------------------------
builder.Services.AddControllers()
    .AddJsonOptions(options =>
    {
        options.JsonSerializerOptions.PropertyNamingPolicy = JsonNamingPolicy.CamelCase;

        // Null fields are DROPPED from every response. That is not cosmetic:
        // the product payload uses null to mean both "you are not entitled to
        // see this member price" and "this product does not carry that offer",
        // and the UI needs the field to be absent in both cases.
        options.JsonSerializerOptions.DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull;

        // Enums travel as their NAME ("HYBRID"), matching the database columns,
        // the API reference and what the client sends back.
        options.JsonSerializerOptions.Converters.Add(new JsonStringEnumConverter());
    });

// Model-validation failures are returned in the same envelope as every other
// error, with the per-field map the client reads as `fieldErrors`.
builder.Services.Configure<ApiBehaviorOptions>(options =>
{
    options.InvalidModelStateResponseFactory = context =>
    {
        var fieldErrors = context.ModelState
            .Where(entry => entry.Value?.Errors.Count > 0)
            .ToDictionary(
                entry => JsonNamingPolicy.CamelCase.ConvertName(entry.Key),
                entry => entry.Value!.Errors.First().ErrorMessage);

        return new BadRequestObjectResult(new
        {
            success = false,
            status = 400,
            error = "Validation Failed",
            message = "One or more fields are invalid",
            path = context.HttpContext.Request.Path.Value,
            timestamp = DateTime.Now,
            fieldErrors
        });
    };
});

builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

// ---------------------------------------------------------------------------
// MySQL
//
// The connection string lives in appsettings.json and points at the SAME
// `emart` database the SQL scripts create. EF does not own this schema: there
// are no migrations in this project and nothing calls EnsureCreated().
// ---------------------------------------------------------------------------
var connectionString = builder.Configuration.GetConnectionString("DefaultConnection")
    ?? throw new InvalidOperationException(
        "ConnectionStrings:DefaultConnection is missing from appsettings.json");

builder.Services.AddDbContext<EmartDbContext>(options =>
{
    options.UseMySQL(connectionString);
    if (builder.Environment.IsDevelopment())
    {
        options.EnableDetailedErrors();
    }
});

// ---------------------------------------------------------------------------
// Services
// ---------------------------------------------------------------------------
builder.Services.AddHttpContextAccessor();

builder.Services.AddScoped<ISecurityUtils, SecurityUtils>();
builder.Services.AddScoped<IPricingService, PricingService>();
builder.Services.AddScoped<ICardholderService, CardholderService>();
builder.Services.AddScoped<IAuthService, AuthService>();
builder.Services.AddScoped<IUserService, UserService>();
builder.Services.AddScoped<IProductService, ProductService>();
builder.Services.AddScoped<ICategoryService, CategoryService>();
builder.Services.AddScoped<IHomeService, HomeService>();
builder.Services.AddScoped<ICartService, CartService>();
builder.Services.AddScoped<IWishlistService, WishlistService>();
builder.Services.AddScoped<IOrderService, OrderService>();
builder.Services.AddScoped<IAddressService, AddressService>();
builder.Services.AddScoped<IPaymentService, PaymentService>();
builder.Services.AddScoped<IEmartCardService, EmartCardService>();

// ---------------------------------------------------------------------------
<<<<<<< HEAD
=======
// backend-email-microservice
//
// Order confirmation + invoice emails are NOT sent from this API. OrderService
// POSTs the finished order to the notification service after the checkout
// transaction commits — see OrderService.PlaceOrderAsync.
//
// AddHttpClient, not `new HttpClient()`: the factory pools and recycles the
// underlying handler. A long-lived HttpClient holds its DNS resolution
// forever, and a new one per call leaks sockets in TIME_WAIT.
// ---------------------------------------------------------------------------
builder.Services.Configure<EmailServiceOptions>(
    builder.Configuration.GetSection(EmailServiceOptions.SectionName));

builder.Services.AddHttpClient<IEmailServiceClient, EmailServiceClient>((provider, client) =>
{
    var emailOptions = provider.GetRequiredService<IOptions<EmailServiceOptions>>().Value;

    client.BaseAddress = new Uri(emailOptions.BaseUrl);
    client.Timeout = TimeSpan.FromSeconds(emailOptions.TimeoutSeconds);

    if (!string.IsNullOrWhiteSpace(emailOptions.ApiKey))
    {
        client.DefaultRequestHeaders.Add(EmailServiceClient.ApiKeyHeader, emailOptions.ApiKey);
    }
});

// ---------------------------------------------------------------------------
>>>>>>> d5373e2ef28bd43e67b12b3e8d1dcff71723abeb
// JWT
//
// Both the signing key here and the one AuthService signs with are read from
// Jwt:SecretKey. They were previously read from two different configuration
// paths, so every token the server issued failed validation on the next request.
// ---------------------------------------------------------------------------
var jwtSecret = builder.Configuration["Jwt:SecretKey"]
    ?? throw new InvalidOperationException("Jwt:SecretKey is missing from appsettings.json");

// Keep the token's own claim names ("sub") instead of silently rewriting them
// to the long WS-Federation URIs, so the claim SecurityUtils looks for is the
// claim that is actually present.
JsonWebTokenHandler.DefaultInboundClaimTypeMap.Clear();

builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        options.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuer = true,
            ValidateAudience = true,
            ValidateLifetime = true,
            ValidateIssuerSigningKey = true,
            ValidIssuer = builder.Configuration["Jwt:Issuer"],
            ValidAudience = builder.Configuration["Jwt:Audience"],
            IssuerSigningKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(jwtSecret)),
            NameClaimType = "sub",
            RoleClaimType = "role",
            // No grace period on expiry — the default five minutes would keep
            // an expired token working well past the lifetime we advertise.
            ClockSkew = TimeSpan.Zero
        };

        // Without these, a missing or expired token produces a bare 401 with an
        // EMPTY body. The client reads `message` off the error payload, so an
        // empty body shows the user "Something went wrong" instead of telling
        // them their session expired.
        options.Events = new JwtBearerEvents
        {
            OnChallenge = async context =>
            {
                context.HandleResponse();
                await WriteErrorAsync(context.HttpContext, StatusCodes.Status401Unauthorized,
                    "Unauthorized", "Your session has expired. Please sign in again.");
            },
            OnForbidden = context =>
                WriteErrorAsync(context.HttpContext, StatusCodes.Status403Forbidden,
                    "Forbidden", "You are not allowed to do that.")
        };
    });

static Task WriteErrorAsync(HttpContext context, int status, string error, string message)
{
    if (context.Response.HasStarted) return Task.CompletedTask;

    context.Response.StatusCode = status;
    context.Response.ContentType = "application/json";

    return context.Response.WriteAsJsonAsync(new
    {
        success = false,
        status,
        error,
        message,
        path = context.Request.Path.Value,
        timestamp = DateTime.Now
    });
}

builder.Services.AddAuthorization();

// The Vite dev server proxies /api to this origin, so the browser only ever
// talks to one host and CORS does not come into play. This policy is here for
// the case where the frontend is served from its own origin instead.
const string DevCorsPolicy = "emart-dev";
builder.Services.AddCors(options =>
{
    options.AddPolicy(DevCorsPolicy, policy => policy
        .WithOrigins("http://localhost:5173", "http://127.0.0.1:5173")
        .AllowAnyHeader()
        .AllowAnyMethod());
});

var app = builder.Build();

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

// UseHttpsRedirection is deliberately absent: Kestrel is configured with an
// HTTP endpoint only, so redirecting would send every API call to a port
// nothing is listening on.

// First in the pipeline, so it also catches failures raised inside auth.
app.UseMiddleware<GlobalExceptionMiddleware>();

app.UseCors(DevCorsPolicy);
app.UseAuthentication();
app.UseAuthorization();

app.MapControllers();

// Matches the Spring Boot actuator endpoint the docs mention, so an existing
// health check keeps working against this server.
app.MapGet("/actuator/health", () => Results.Ok(new { status = "UP" }));

app.Run();
