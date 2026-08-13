using System.Collections.Generic;

namespace Emart.Api.DTOs
{
    public class PageResponse<T>
    {
        public IEnumerable<T> Content { get; set; } = new List<T>();
        public int Page { get; set; }
        public int Size { get; set; }
        public long TotalElements { get; set; }
        public int TotalPages { get; set; }
        public bool First { get; set; }
        public bool Last { get; set; }

        public PageResponse() { }

        public PageResponse(IEnumerable<T> content, int page, int size, long totalElements, int totalPages, bool first, bool last)
        {
            Content = content;
            Page = page;
            Size = size;
            TotalElements = totalElements;
            TotalPages = totalPages;
            First = first;
            Last = last;
        }
    }
}
