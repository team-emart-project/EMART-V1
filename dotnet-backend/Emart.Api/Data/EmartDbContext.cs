using Emart.Api.Models;
using Microsoft.EntityFrameworkCore;

namespace Emart.Api.Data
{
    /// <summary>
    /// Maps onto the EXISTING MySQL `emart` schema created by
    /// backend/emart_schema.sql. The SQL scripts own the tables; EF only reads
    /// and writes them. There is deliberately no migration and no
    /// EnsureCreated() call anywhere — either would try to rewrite a schema
    /// that another application also depends on.
    /// </summary>
    public class EmartDbContext : DbContext
    {
        public EmartDbContext(DbContextOptions<EmartDbContext> options) : base(options)
        {
        }

        public DbSet<Address> Addresses { get; set; } = null!;
        public DbSet<Cart> Carts { get; set; } = null!;
        public DbSet<CartItem> CartItems { get; set; } = null!;
        public DbSet<CategoryMaster> CategoryMasters { get; set; } = null!;
        public DbSet<ConfigMaster> ConfigMasters { get; set; } = null!;
        public DbSet<EmartCard> EmartCards { get; set; } = null!;
        public DbSet<OrderDetail> OrderDetails { get; set; } = null!;
        public DbSet<Orders> Orders { get; set; } = null!;
        public DbSet<Payment> Payments { get; set; } = null!;
        public DbSet<ProdDtlMaster> ProdDtlMasters { get; set; } = null!;
        public DbSet<ProductImage> ProductImages { get; set; } = null!;
        public DbSet<ProductMaster> ProductMasters { get; set; } = null!;
        public DbSet<User> Users { get; set; } = null!;
        public DbSet<Wishlist> Wishlists { get; set; } = null!;

        protected override void OnModelCreating(ModelBuilder modelBuilder)
        {
            base.OnModelCreating(modelBuilder);

            // ----------------------------------------------------------------
            // Enums are VARCHAR in MySQL, not ints.
            //
            // Every enum column in emart_schema.sql is a VARCHAR holding the
            // name ('PLACED', 'HYBRID', 'APPROVED'). EF Core's default is to
            // store the ordinal, which on this database means reading '2' out
            // of a column containing 'CANCELLED' — a conversion failure on
            // every single row. Registering the string conversion once here
            // covers reads and writes for all of them.
            // ----------------------------------------------------------------
            modelBuilder.Entity<User>().Property(u => u.Role).HasConversion<string>();
            modelBuilder.Entity<User>().Property(u => u.AuthProvider).HasConversion<string>();
            modelBuilder.Entity<Address>().Property(a => a.AddressType).HasConversion<string>();
            modelBuilder.Entity<EmartCard>().Property(c => c.Status).HasConversion<string>();
            modelBuilder.Entity<Cart>().Property(c => c.Status).HasConversion<string>();
            modelBuilder.Entity<CartItem>().Property(i => i.PriceOption).HasConversion<string>();
            modelBuilder.Entity<Orders>().Property(o => o.PaymentStatus).HasConversion<string>();
            modelBuilder.Entity<Orders>().Property(o => o.OrderStatus).HasConversion<string>();
            modelBuilder.Entity<OrderDetail>().Property(d => d.PriceOption).HasConversion<string>();
            modelBuilder.Entity<Payment>().Property(p => p.Status).HasConversion<string>();

            // MySQL DATE, not DATETIME.
            modelBuilder.Entity<EmartCard>().Property(c => c.ApplicationDate).HasColumnType("date");
            modelBuilder.Entity<EmartCard>().Property(c => c.ApprovalDate).HasColumnType("date");
            modelBuilder.Entity<User>().Property(u => u.Dob).HasColumnType("date");

            // Decimal precision, so MySQL never silently truncates a price.
            modelBuilder.Entity<ProductMaster>().Property(p => p.MrpPrice).HasPrecision(10, 2);
            modelBuilder.Entity<ProductMaster>().Property(p => p.CardholderPrice).HasPrecision(10, 2);
            modelBuilder.Entity<ProductMaster>().Property(p => p.HybridCashPrice).HasPrecision(10, 2);
            modelBuilder.Entity<ProductMaster>().Property(p => p.Rating).HasPrecision(2, 1);
            modelBuilder.Entity<ProductMaster>().Property(p => p.DiscountPercentage).HasPrecision(5, 2);
            modelBuilder.Entity<OrderDetail>().Property(d => d.MrpPrice).HasPrecision(10, 2);
            modelBuilder.Entity<OrderDetail>().Property(d => d.CardholderPrice).HasPrecision(10, 2);
            modelBuilder.Entity<OrderDetail>().Property(d => d.PriceCharged).HasPrecision(10, 2);
            modelBuilder.Entity<Orders>().Property(o => o.SubtotalAmount).HasPrecision(12, 2);
            modelBuilder.Entity<Orders>().Property(o => o.TotalAmount).HasPrecision(12, 2);
            modelBuilder.Entity<Payment>().Property(p => p.Amount).HasPrecision(12, 2);
            modelBuilder.Entity<User>().Property(u => u.AnnualIncome).HasPrecision(12, 2);

            // ----------------------------------------------------------------
            // Relationships
            // ----------------------------------------------------------------
            modelBuilder.Entity<Orders>()
                .HasOne(o => o.ShippingAddress)
                .WithMany()
                .HasForeignKey(o => o.ShippingAddressId)
                .OnDelete(DeleteBehavior.Restrict);

            modelBuilder.Entity<Orders>()
                .HasOne(o => o.BillingAddress)
                .WithMany()
                .HasForeignKey(o => o.BillingAddressId)
                .OnDelete(DeleteBehavior.Restrict);

            modelBuilder.Entity<Orders>()
                .HasMany(o => o.Items)
                .WithOne(d => d.Order!)
                .HasForeignKey(d => d.OrderId)
                .OnDelete(DeleteBehavior.Cascade);

            modelBuilder.Entity<Cart>()
                .HasMany(c => c.Items)
                .WithOne(i => i.Cart!)
                .HasForeignKey(i => i.CartId)
                .OnDelete(DeleteBehavior.Cascade);

            modelBuilder.Entity<ProductMaster>()
                .HasMany(p => p.Images)
                .WithOne(i => i.Product!)
                .HasForeignKey(i => i.ProdId)
                .OnDelete(DeleteBehavior.Cascade);

            modelBuilder.Entity<ProductMaster>()
                .HasMany(p => p.Details)
                .WithOne(d => d.Product!)
                .HasForeignKey(d => d.ProdId)
                .OnDelete(DeleteBehavior.Cascade);

            // cart.user_id is UNIQUE in the schema: a user cannot hold two carts.
            modelBuilder.Entity<Cart>()
                .HasIndex(c => c.UserId)
                .IsUnique();

            modelBuilder.Entity<EmartCard>()
                .HasIndex(c => c.UserId)
                .IsUnique();

            modelBuilder.Entity<Wishlist>()
                .HasIndex(w => new { w.UserId, w.ProdId })
                .IsUnique();
        }
    }
}
