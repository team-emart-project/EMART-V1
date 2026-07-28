import "./Hero.css";

import HeroCircle from "../HeroCircle/HeroCircle";
import FloatingCard from "../FloatingCard/FloatingCard";

import { FaShieldAlt, FaTruck, FaStar } from "react-icons/fa";

function Hero() {
  return (
    <section className="hero">
      <div className="hero-container">

        {/* LEFT CONTENT */}
        <div className="hero-left">

          {/* Logo */}
          <div className="logo">
            e
          </div>

          {/* Heading */}
          <h1 className="hero-title">
            e-MART
          </h1>

          <h2 className="hero-subtitle">
            India's Premium Shopping Destination
          </h2>

          <p className="hero-description">
            Discover over 5 million products from trusted brands.
            Shop smarter with exciting offers, fast delivery and
            exclusive rewards.
          </p>

          {/* Statistics */}
          <div className="hero-stats">

            <div>
              <h3>5M+</h3>
              <span>Products</span>
            </div>

            <div>
              <h3>50K+</h3>
              <span>Brands</span>
            </div>

            <div>
              <h3>2M+</h3>
              <span>Customers</span>
            </div>

          </div>

          {/* Buttons */}
          <div className="hero-buttons">

            <button className="btn-primary">
              Get Started →
            </button>

            <button className="btn-secondary">
              Sign In
            </button>

          </div>

          {/* Bottom Badges */}
          <div className="hero-badges">

            <div className="hero-badge">
              <FaShieldAlt />
              <span>100% Secure</span>
            </div>

            <div className="hero-badge">
              <FaTruck />
              <span>Fast Delivery</span>
            </div>

            <div className="hero-badge">
              <FaStar />
              <span>Top Rated</span>
            </div>

          </div>

        </div>

        {/* RIGHT CONTENT */}
        <div className="hero-right">

          <FloatingCard
            title="iPhone 16 Pro"
            subtitle="Premium Electronics"
            price="₹89,999"
            className="card-top-left"
          />

          <FloatingCard
            title="⭐ 4.9"
            subtitle="Customer Rating"
            className="card-top-right"
          />

          <FloatingCard
            title="🚚 Free Delivery"
            subtitle="On Orders Above ₹499"
            className="card-bottom-left"
          />

          <HeroCircle />

        </div>

      </div>
    </section>
  );
}

export default Hero;