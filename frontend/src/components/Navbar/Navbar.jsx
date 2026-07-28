import "./Navbar.css";
import { Link } from "react-router-dom";
import { FaShoppingBag } from "react-icons/fa";

function Navbar() {
  return (
    <header className="navbar">
      <div className="navbar-container">

        {/* Logo */}
        <Link to="/" className="logo">
          <div className="logo-box">
            <FaShoppingBag />
          </div>

          <span className="logo-text">e-MART</span>
        </Link>

        {/* Navigation */}
        <nav className="nav-links">
          <Link to="/">Home</Link>
          <Link to="/products">Products</Link>
          <Link to="/categories">Categories</Link>
          <Link to="/about">About</Link>
          <Link to="/contact">Contact</Link>
        </nav>

        {/* Right Side */}
        <div className="nav-buttons">
          <button className="btn-login">Sign In</button>
          <button className="btn-register">Get Started</button>
        </div>

      </div>
    </header>
  );
}

export default Navbar;