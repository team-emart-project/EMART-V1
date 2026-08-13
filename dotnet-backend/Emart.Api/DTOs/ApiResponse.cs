using System;

namespace Emart.Api.DTOs
{
    public class ApiResponse<T>
    {
        public bool Success { get; set; }
        public string Message { get; set; } = string.Empty;
        public T? Data { get; set; }
        public DateTime Timestamp { get; set; } = DateTime.UtcNow;

        public ApiResponse() { }

        public ApiResponse(bool success, string message, T? data)
        {
            Success = success;
            Message = message;
            Data = data;
        }

        public static ApiResponse<T> CreateSuccess(string message, T data)
        {
            return new ApiResponse<T>(true, message, data);
        }

        public static ApiResponse<T> CreateSuccess(string message)
        {
            return new ApiResponse<T>(true, message, default);
        }
    }
}
