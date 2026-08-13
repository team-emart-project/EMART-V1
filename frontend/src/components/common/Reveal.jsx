import { motion } from 'framer-motion';

/**
 * Fades content in as it scrolls into view.
 *
 * `viewport={{ once: true }}` means it animates the first time only — replaying
 * on every scroll past is distracting.
 */
export default function Reveal({ children, delay = 0, y = 20, className }) {
  return (
    <motion.div
      initial={{ opacity: 0, y }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, margin: '-60px' }}
      transition={{ duration: 0.5, delay, ease: [0.16, 1, 0.3, 1] }}
      className={className}
    >
      {children}
    </motion.div>
  );
}
