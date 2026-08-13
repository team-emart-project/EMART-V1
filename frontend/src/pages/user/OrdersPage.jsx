import { useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import { ChevronRight, Package } from 'lucide-react';
import { Link } from 'react-router-dom';
import { Badge, Button, Card, EmptyState, ListSkeleton, Pagination } from '@/components/ui';
import AccountNav from './AccountNav';
import { useFetch } from '@/hooks/useApi';
import endpoints from '@/api/endpoints';
import { formatPrice, formatDateTime, statusColor } from '@/utils/formatters';

export default function OrdersPage() {
  const [page, setPage] = useState(0);

  const url = useMemo(
    () => `${endpoints.orders.root}?page=${page}&size=10`,
    [page]
  );
  const { data, loading } = useFetch(url);

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6 lg:px-8">
      <h1 className="mb-6 text-3xl font-bold text-slate-900">My account</h1>

      <div className="grid gap-6 lg:grid-cols-[220px_1fr]">
        <AccountNav />

        <div>
          <h2 className="mb-4 text-lg font-semibold text-slate-900">Order history</h2>

          {loading ? (
            <ListSkeleton rows={3} />
          ) : data?.content?.length ? (
            <>
              <div className="space-y-3">
                {data.content.map((order, i) => (
                  <motion.div key={order.orderId}
                    initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: i * 0.05 }}>
                    <Link to={`/account/orders/${order.orderId}`}>
                      <Card hover className="flex items-center gap-4">
                        <div className="rounded-xl bg-brand-50 p-3">
                          <Package className="h-5 w-5 text-brand-600" />
                        </div>

                        <div className="min-w-0 flex-1">
                          <div className="flex flex-wrap items-center gap-2">
                            <p className="font-semibold text-slate-900">{order.orderNo}</p>
                            <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${statusColor(order.orderStatus)}`}>
                              {order.orderStatus}
                            </span>
                            <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${statusColor(order.paymentStatus)}`}>
                              {order.paymentStatus}
                            </span>
                          </div>
                          <p className="mt-1 text-sm text-slate-500">
                            {formatDateTime(order.orderDate)} · {order.items?.length || 0} item(s)
                          </p>
                          {order.pointsEarned > 0 && (
                            <Badge tone="accent" className="mt-1">
                              +{order.pointsEarned} e-Points
                            </Badge>
                          )}
                        </div>

                        <div className="text-right">
                          <p className="font-bold text-slate-900">{formatPrice(order.totalAmount)}</p>
                          <ChevronRight className="ml-auto mt-1 h-4 w-4 text-slate-400" />
                        </div>
                      </Card>
                    </Link>
                  </motion.div>
                ))}
              </div>
              <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
            </>
          ) : (
            <EmptyState icon={Package} title="No orders yet"
              message="When you place an order it will show up here."
              action={<Button as={Link} to="/products">Start shopping</Button>} />
          )}
        </div>
      </div>
    </div>
  );
}
