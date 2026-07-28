import "./FloatingCard.css";

function FloatingCard({
  title,
  subtitle,
  price,
  className
}) {
  return (
    <div className={`floating-card ${className}`}>

      <h3>{title}</h3>

      <p>{subtitle}</p>

      {price && <span>{price}</span>}

    </div>
  );
}

export default FloatingCard;