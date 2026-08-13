namespace Emart.Api.Models
{
    public enum AddressType
    {
        BILLING,
        SHIPPING,
        BOTH
    }

    public enum AuthProvider
    {
        LOCAL,
        GOOGLE,
        BOTH
    }

    public enum CardStatus
    {
        PENDING,
        APPROVED,
        REJECTED
    }

    public enum CartStatus
    {
        ACTIVE,
        CONVERTED
    }

    public enum OrderStatus
    {
        PLACED,
        PAID,
        CANCELLED,
        SHIPPED,
        DELIVERED
    }

    public enum PaymentStatus
    {
        PENDING,
        PAID,
        FAILED,
        SUCCESS
    }

    public enum PriceOption
    {
        REGULAR,
        MEMBER,
        POINTS,
        HYBRID
    }

    // The users.role column is CHECKed against 'CUSTOMER' | 'ADMIN'. ADMIN was
    // missing here, so any admin row would have failed to materialise.
    public enum RoleType
    {
        CUSTOMER,
        ADMIN
    }

    public static class PriceOptionExtensions
    {
        /// <summary>Only an active cardholder may pick these three.</summary>
        public static bool RequiresCardholder(this PriceOption option) =>
            option is PriceOption.MEMBER or PriceOption.POINTS or PriceOption.HYBRID;

        /// <summary>Whether choosing this option debits e-Points.</summary>
        public static bool SpendsPoints(this PriceOption option) =>
            option is PriceOption.POINTS or PriceOption.HYBRID;
    }
}
