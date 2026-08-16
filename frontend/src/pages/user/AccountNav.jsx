import { CreditCard, MapPin, Package, User } from 'lucide-react';
import { NavLink } from 'react-router-dom';
import { cn } from '@/utils/formatters';

const LINKS = [
  { to: '/account/profile', icon: User, label: 'Profile' },
  { to: '/account/addresses', icon: MapPin, label: 'Addresses' },
  { to: '/account/card', icon: CreditCard, label: 'e-MART card' },
  { to: '/account/orders', icon: Package, label: 'Orders' },
];

/** Shared side navigation for every account page — written once, used four times. */
export default function AccountNav() {
  return (
    <nav className="flex gap-1 overflow-x-auto rounded-2xl border border-slate-200 bg-white p-1.5 lg:flex-col lg:overflow-visible">
      {LINKS.map((l) => (
        <NavLink key={l.to} to={l.to}
          className={({ isActive }) => cn(
            'flex shrink-0 items-center gap-2 rounded-xl px-3 py-2.5 text-sm font-medium transition-colors',
<<<<<<< HEAD
            isActive ? 'bg-brand-600 text-white' : 'text-slate-600 hover:bg-slate-50'
=======
            isActive ? 'bg-brand-gradient text-white shadow-sm shadow-brand-600/25' : 'text-slate-600 hover:bg-slate-50'
>>>>>>> d5373e2ef28bd43e67b12b3e8d1dcff71723abeb
          )}>
          <l.icon className="h-4 w-4" />{l.label}
        </NavLink>
      ))}
    </nav>
  );
}
