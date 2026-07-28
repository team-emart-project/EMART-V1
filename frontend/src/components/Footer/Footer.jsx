import { FaFacebookF, FaInstagram, FaTwitter, FaYoutube } from 'react-icons/fa';
import './Footer.css';

export default function Footer() {
  return (
    <footer className="emart-footer">
      <div className="container">
        <div className="row g-4">
          <div className="col-6 col-md-3">
            <h3 className="emart-footer__heading">Company</h3>
            <ul>
              <li><a href="/about">About us</a></li>
              <li><a href="/careers">Careers</a></li>
              <li><a href="/press">Press</a></li>
            </ul>
          </div>
          <div className="col-6 col-md-3">
            <h3 className="emart-footer__heading">Help</h3>
            <ul>
              <li><a href="/orders">Track order</a></li>
              <li><a href="/returns">Returns &amp; refunds</a></li>
              <li><a href="/support">Contact support</a></li>
            </ul>
          </div>
          <div className="col-6 col-md-3">
            <h3 className="emart-footer__heading">Policy</h3>
            <ul>
              <li><a href="/privacy">Privacy policy</a></li>
              <li><a href="/terms">Terms of use</a></li>
              <li><a href="/shipping">Shipping policy</a></li>
            </ul>
          </div>
          <div className="col-6 col-md-3">
            <h3 className="emart-footer__heading">Stay in the loop</h3>
            <p className="emart-footer__note">Get deal alerts before anyone else.</p>
            <form className="emart-footer__newsletter" onSubmit={(e) => e.preventDefault()}>
              <input type="email" placeholder="Email address" aria-label="Email address" required />
              <button type="submit">Subscribe</button>
            </form>
            <div className="emart-footer__social">
              <a href="/" aria-label="Facebook"><FaFacebookF /></a>
              <a href="/" aria-label="Instagram"><FaInstagram /></a>
              <a href="/" aria-label="Twitter"><FaTwitter /></a>
              <a href="/" aria-label="YouTube"><FaYoutube /></a>
            </div>
          </div>
        </div>

        <div className="emart-footer__bottom">
          <span>© {new Date().getFullYear()} E-Mart Solution. All rights reserved.</span>
          <span className="emart-footer__trust">Secure payments · 7-day returns · CDAC Major Project</span>
        </div>
      </div>
    </footer>
  );
}
