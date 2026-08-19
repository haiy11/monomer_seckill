-- ============================================================
-- 秒杀压测后的数据正确性校验脚本（配合 wrk 压测使用）
--
-- 执行方式（二选一）：
--  1) WSL2 命令行：
--     docker exec -i mysql mysql -uroot -p123456hy --default-character-set=utf8mb4 monomer_seckill < /mnt/e/items/natherItems/monomer_seckill/docs/check-data.sql
--  2) Navicat / DBeaver：连接 3307 后选中 monomer_seckill 库，分段执行。
--
-- 说明：P2 秒杀以 seckill_goods（秒杀商品）为粒度，Redis 预扣库存。
--   压测前除重置 DB 外，还需重置 Redis 库存：
--   重启后端（启动时预载），或调用 POST /api/admin/stock/reset（需管理员 token）。
-- ============================================================

-- ============================================================
-- 【0】压测前：重置数据（每次压测前执行，保证初始状态一致）
--    把某个秒杀商品的库存设成好算的数，比如秒杀商品1 = 100，并清空秒杀订单。
-- ============================================================
DELETE FROM seckill_order;
UPDATE seckill_goods SET seckill_stock = 100 WHERE id = 1;

-- ============================================================
-- 【压测后】逐项校验（核心）
-- ============================================================

-- 校验 1：秒杀库存不能为负 —— 超卖的最直接证据
-- 期望：返回空
SELECT id, name, seckill_stock FROM seckill_goods WHERE seckill_stock < 0;

-- 校验 2：秒杀订单数 + 剩余秒杀库存 = 初始秒杀库存（不多卖、不少卖）
-- 以秒杀商品1初始 100 为例，期望 total = 100
SELECT
  (SELECT COUNT(*) FROM seckill_order WHERE seckill_goods_id = 1)
  + (SELECT seckill_stock FROM seckill_goods WHERE id = 1) AS total;

-- 校验 3：没有重复下单 —— 同一用户对同一秒杀商品只允许一条订单
-- 期望：返回空
SELECT seckill_goods_id, user_id, COUNT(*) AS cnt
FROM seckill_order
GROUP BY seckill_goods_id, user_id
HAVING cnt > 1;

-- 校验 4：各秒杀商品实际卖出数量分布
SELECT seckill_goods_id, COUNT(*) AS sold_count FROM seckill_order GROUP BY seckill_goods_id;

-- 校验 5：秒杀订单总数
SELECT COUNT(*) AS total_seckill_orders FROM seckill_order;

-- 校验 6：抽查秒杀订单内容
SELECT * FROM seckill_order ORDER BY id DESC LIMIT 20;

-- ============================================================
-- 【正常商品订单校验】（购物车批量下单）
-- ============================================================

-- 校验 7：正常商品库存不为负
SELECT id, name, stock FROM goods WHERE stock < 0;

-- 校验 8：订单总金额 = 明细小计之和（抽查最近一条订单）
SELECT
  o.order_no,
  o.total_amount,
  (SELECT SUM(oi.amount) FROM order_item oi WHERE oi.order_id = o.id) AS item_sum
FROM mall_order o
ORDER BY o.id DESC
LIMIT 5;

-- 校验 9（可选）：核对 Redis 剩余秒杀库存与 DB 是否一致
-- 在 Redis CLI 执行：GET seckill:stock:1，与上面 seckill_goods 表的 seckill_stock 对比，应相等。
