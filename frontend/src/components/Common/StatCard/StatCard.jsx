import "./StatCard.css";

function StatCard({ icon, value, label }) {
  return (
    <div className="stat-card">
      <span className="stat-icon">{icon}</span>

      <h2>{value}</h2>

      <p>{label}</p>
    </div>
  );
}

export default StatCard;