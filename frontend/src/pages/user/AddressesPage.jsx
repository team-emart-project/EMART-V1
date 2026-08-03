import { useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { MapPin, Plus, Star, Trash2 } from 'lucide-react';
import { useDispatch } from 'react-redux';
import { Badge, Button, Card, EmptyState, Input, Modal, Skeleton } from '@/components/ui';
import AccountNav from './AccountNav';
import { useDelete, useFetch, usePost, usePut } from '@/hooks/useApi';
import endpoints from '@/api/endpoints';
import { toastError, toastSuccess } from '@/store/slices/uiSlice';

const BLANK = {
  addressLine1: '', addressLine2: '', city: '', state: '',
  zipCode: '', country: 'India', addressType: 'BOTH', isDefault: false,
};

export default function AddressesPage() {
  const dispatch = useDispatch();
  const { data: addresses, loading, refetch } = useFetch(endpoints.users.addresses);
  const { mutate: post, loading: creating } = usePost();
  const { mutate: put, loading: updating } = usePut();
  const { mutate: del } = useDelete();

  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(BLANK);

  const openNew = () => { setEditing(null); setForm(BLANK); setOpen(true); };
  const openEdit = (a) => { setEditing(a); setForm({ ...a }); setOpen(true); };

  const onChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm({ ...form, [name]: type === 'checkbox' ? checked : value });
  };

  const save = async (e) => {
    e.preventDefault();
    const { error } = editing
      ? await put(endpoints.users.addressById(editing.addressId), form)
      : await post(endpoints.users.addresses, form);

    if (error) { dispatch(toastError(error.message)); return; }
    dispatch(toastSuccess(editing ? 'Address updated' : 'Address added'));
    setOpen(false);
    refetch();
  };

  const remove = async (id) => {
    const { error } = await del(endpoints.users.addressById(id));
    if (error) dispatch(toastError(error.message));
    else { dispatch(toastSuccess('Address deleted')); refetch(); }
  };

  const makeDefault = async (id) => {
    const { error } = await put(endpoints.users.setDefaultAddress(id));
    if (error) dispatch(toastError(error.message));
    else { dispatch(toastSuccess('Default address updated')); refetch(); }
  };

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6 lg:px-8">
      <h1 className="mb-6 text-3xl font-bold text-slate-900">My account</h1>

      <div className="grid gap-6 lg:grid-cols-[220px_1fr]">
        <AccountNav />

        <div>
          <div className="mb-4 flex items-center justify-between">
            <h2 className="text-lg font-semibold text-slate-900">Saved addresses</h2>
            <Button size="sm" onClick={openNew}><Plus className="h-3.5 w-3.5" /> Add address</Button>
          </div>

          {loading ? (
            <Skeleton className="h-32 w-full" />
          ) : addresses?.length ? (
            <div className="grid gap-3 sm:grid-cols-2">
              <AnimatePresence mode="popLayout">
                {addresses.map((a) => (
                  <motion.div key={a.addressId} layout
                    initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }}>
                    <Card className="h-full">
                      <div className="mb-2 flex items-start justify-between">
                        <Badge tone="slate">{a.addressType}</Badge>
                        {a.isDefault && <Badge tone="brand">Default</Badge>}
                      </div>
                      <p className="text-sm font-medium text-slate-800">{a.addressLine1}</p>
                      {a.addressLine2 && <p className="text-sm text-slate-500">{a.addressLine2}</p>}
                      <p className="text-sm text-slate-500">
                        {a.city}, {a.state} {a.zipCode}
                      </p>
                      <p className="text-sm text-slate-500">{a.country}</p>

                      <div className="mt-4 flex flex-wrap gap-2">
                        <Button size="sm" variant="outline" onClick={() => openEdit(a)}>Edit</Button>
                        {!a.isDefault && (
                          <Button size="sm" variant="ghost" onClick={() => makeDefault(a.addressId)}>
                            <Star className="h-3.5 w-3.5" /> Make default
                          </Button>
                        )}
                        <Button size="sm" variant="ghost" className="text-rose-600"
                          onClick={() => remove(a.addressId)}>
                          <Trash2 className="h-3.5 w-3.5" />
                        </Button>
                      </div>
                    </Card>
                  </motion.div>
                ))}
              </AnimatePresence>
            </div>
          ) : (
            <EmptyState icon={MapPin} title="No addresses yet"
              message="Add one so you can check out."
              action={<Button onClick={openNew}>Add your first address</Button>} />
          )}
        </div>
      </div>

      <Modal open={open} onClose={() => setOpen(false)}
        title={editing ? 'Edit address' : 'Add address'}>
        <form onSubmit={save} className="space-y-4">
          <Input label="Address line 1" name="addressLine1" required
            value={form.addressLine1} onChange={onChange} />
          <Input label="Address line 2" name="addressLine2"
            value={form.addressLine2 || ''} onChange={onChange} />
          <div className="grid grid-cols-2 gap-4">
            <Input label="City" name="city" required value={form.city} onChange={onChange} />
            <Input label="State" name="state" required value={form.state} onChange={onChange} />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Input label="Zip code" name="zipCode" required value={form.zipCode} onChange={onChange} />
            <Input label="Country" name="country" required value={form.country} onChange={onChange} />
          </div>

          <div>
            <label className="mb-1.5 block text-sm font-medium text-slate-700">Address type</label>
            <select name="addressType" value={form.addressType} onChange={onChange}
              className="w-full rounded-xl border border-slate-300 bg-white px-3.5 py-2.5 text-sm">
              <option value="BOTH">Billing &amp; shipping</option>
              <option value="SHIPPING">Shipping only</option>
              <option value="BILLING">Billing only</option>
            </select>
          </div>

          <label className="flex items-center gap-2">
            <input type="checkbox" name="isDefault" checked={form.isDefault} onChange={onChange}
              className="h-4 w-4 rounded border-slate-300 text-brand-600" />
            <span className="text-sm text-slate-600">Make this my default address</span>
          </label>

          <Button type="submit" fullWidth loading={creating || updating}>
            {editing ? 'Save changes' : 'Add address'}
          </Button>
        </form>
      </Modal>
    </div>
  );
}
