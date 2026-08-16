using System.Collections.Generic;
using System.Text.Json.Serialization;

namespace Emart.Api.DTOs
{
    public class CategoryDto
    {
        public int CatmasterId { get; set; }
        public string CatId { get; set; } = string.Empty;
        public string? SubcatId { get; set; }
        public string CatName { get; set; } = string.Empty;
        public string? CatImagePath { get; set; }
        public bool? Flag { get; set; }
        
        [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
        public List<CategoryDto>? Children { get; set; }
    }
}
