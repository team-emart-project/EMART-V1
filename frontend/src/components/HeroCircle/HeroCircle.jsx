import "./HeroCircle.css";
import { FaHome, FaCamera, FaMobileAlt, FaTshirt, FaShoppingBag } from "react-icons/fa";

function HeroCircle() {
  return (
    <div className="hero-circle-wrap">
      <div className="orbit orbit-1"></div>
      <div className="orbit orbit-2"></div>

      <div className="orbit-item item-top">
        <FaHome />
      </div>

      <div className="orbit-item item-left">
        <FaCamera />
      </div>

      <div className="orbit-item item-right">
        <FaMobileAlt />
      </div>

      <div className="orbit-item item-bottom">
        <FaTshirt />
      </div>

      <div className="hero-core">
        <FaShoppingBag />
      </div>
    </div>
  );
}

export default HeroCircle;