using System.Collections.Generic;
using System.Threading.Tasks;
using Emart.Api.DTOs;

namespace Emart.Api.Services
{
    public interface ICategoryService
    {
        Task<IEnumerable<CategoryDto>> GetRootCategoriesAsync();
        Task<IEnumerable<CategoryDto>> GetCategoryTreeAsync();
        Task<CategoryDto?> GetCategoryAsync(int catmasterId);
        Task<IEnumerable<CategoryDto>> GetSubCategoriesAsync(int catmasterId);
    }
}
