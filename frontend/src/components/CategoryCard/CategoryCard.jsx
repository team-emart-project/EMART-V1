import './CategoryCard.css';

/**
 * CategoryCard
 * @param {object} category - { id, name, icon } (icon can later become an image URL from backend)
 */
export default function CategoryCard({ category }) {
  return (
    <a href={`/categories/${category.id}`} className="emart-category-card">
      <span className="emart-category-card__icon" aria-hidden="true">
        {category.icon}
      </span>
      <span className="emart-category-card__name">{category.name}</span>
    </a>
  );
}
