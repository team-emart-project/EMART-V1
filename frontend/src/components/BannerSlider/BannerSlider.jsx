import { useCallback, useEffect, useRef, useState } from 'react';
import { FaChevronLeft, FaChevronRight } from 'react-icons/fa';
import './BannerSlider.css';

/**
 * BannerSlider
 * Autoplaying hero carousel. Built manually (no bootstrap.bundle.js
 * dependency) so it works even in projects using only Bootstrap's CSS.
 *
 * @param {Array<{id:number,title:string,subtitle:string,ctaLabel:string,image:string,theme:string}>} slides
 * @param {number} [intervalMs=5000]
 */
export default function BannerSlider({ slides = [], intervalMs = 5000 }) {
  const [active, setActive] = useState(0);
  const timerRef = useRef(null);

  const goTo = useCallback(
    (index) => {
      setActive(((index % slides.length) + slides.length) % slides.length);
    },
    [slides.length]
  );

  const next = useCallback(() => goTo(active + 1), [active, goTo]);
  const prev = useCallback(() => goTo(active - 1), [active, goTo]);

  useEffect(() => {
    if (slides.length <= 1) return undefined;
    timerRef.current = setInterval(next, intervalMs);
    return () => clearInterval(timerRef.current);
  }, [next, intervalMs, slides.length]);

  const pause = () => clearInterval(timerRef.current);
  const resume = () => {
    clearInterval(timerRef.current);
    timerRef.current = setInterval(next, intervalMs);
  };

  if (!slides.length) return null;

  return (
    <div
      className="emart-banner-slider"
      onMouseEnter={pause}
      onMouseLeave={resume}
      role="region"
      aria-roledescription="carousel"
      aria-label="Promotional banners"
    >
      <div
        className="emart-banner-slider__track"
        style={{ transform: `translateX(-${active * 100}%)` }}
      >
        {slides.map((slide, i) => (
          <div
            className={`emart-banner-slide emart-banner-slide--${slide.theme}`}
            key={slide.id}
            aria-hidden={i !== active}
          >
            <img src={slide.image} alt="" className="emart-banner-slide__bg" />
            <div className="emart-banner-slide__scrim" />
            <div className="emart-banner-slide__content">
              <h2>{slide.title}</h2>
              <p>{slide.subtitle}</p>
              <button type="button" className="emart-banner-slide__cta">
                {slide.ctaLabel}
              </button>
            </div>
          </div>
        ))}
      </div>

      <button
        type="button"
        className="emart-banner-nav emart-banner-nav--prev"
        onClick={prev}
        aria-label="Previous banner"
      >
        <FaChevronLeft />
      </button>
      <button
        type="button"
        className="emart-banner-nav emart-banner-nav--next"
        onClick={next}
        aria-label="Next banner"
      >
        <FaChevronRight />
      </button>

      <div className="emart-banner-dots">
        {slides.map((slide, i) => (
          <button
            key={slide.id}
            type="button"
            className={`emart-banner-dot ${i === active ? 'emart-banner-dot--active' : ''}`}
            aria-label={`Go to slide ${i + 1}`}
            onClick={() => goTo(i)}
          />
        ))}
      </div>
    </div>
  );
}
