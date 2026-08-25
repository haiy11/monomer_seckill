-- ============================================================
-- 秒杀压测后的数据正确性校验脚本（配合 wrk 压测使用）
--
-- 执行方式（二选一）：
--  1) WSL2 命令行：
--     docker exec -i mysql mysql -uroot -p123456hy --default-character-set=utf8mb4 monomer_seckill < /mnt/e/items/natherItems/monomer_seckill/docs/P2-check-data.sql
--  2) Navicat / DBeaver：连接 3307 后选中 monomer_seckill 库，分段执行。
--
-- 说明：P2 秒杀以 seckill_goods（秒杀商品）为粒度，Redis 预扣库存。
--   ⚠️ 关键：Redis 库存/已购集合是本轮的「事实来源」，SQL 只能操作 MySQL，无法直接操作 Redis。
--   所以「重置」必须分两步：先执行下面的 DB 重置 SQL，再把 Redis 同步成一致状态。
--   Redis 里需要处理两个 key，缺一不可：
--     1) 库存 key：seckill:stock:1      —— 重置为 1000（与 DB 一致）
--     2) 已购用户集合：seckill:users:1   —— 整体删除！否则复用同一批 token 时，
--        上一轮抢到库存的用户会被 Lua 误判为「重复抢购」，但 DB 订单已清空，造成数据不一致。
-- ============================================================

-- ============================================================
-- 【0】压测前：重置数据（每次压测前执行，保证初始状态一致）
--    把秒杀商品1的库存设成好算的数（这里 = 1000），并清空秒杀订单。
--    秒杀商品1 = 应用启动时 DataInitializer 播种的示例秒杀商品（id=1）。
-- ============================================================
DELETE FROM seckill_order;
UPDATE seckill_goods SET seckill_stock = 1000 WHERE id = 1;

-- 以上 SQL 只重置了 MySQL。还必须把 Redis 同步重置（SQL 做不到），且库存 key 与已购集合都要处理：
--   方式一（推荐，无需重启）：用管理员 token 调用管理接口
--     （接口内部会同时重置 seckill:stock:1 为 DB 当前库存，并清空 seckill:users:1 已购集合）
--     1) 登录拿管理员 token（默认账号 admin/admin123）：
--        curl -X POST http://localhost:7099/api/admin/login \
--             -H "Content-Type: application/json" \
--             -d '{"username":"admin","password":"admin123"}'
--        （返回体里的 data.token 即管理员 token）
--     2) 重置秒杀商品1的 Redis 库存并清空已购集合 seckill:users:1：
--        curl -X POST http://localhost:7099/api/admin/stock/reset/1 \
--             -H "Authorization: Bearer <管理员token>"
--   方式二：重启后端（启动时 DataInitializer 会自动预载全部秒杀库存到 Redis，并清空已购集合）。
--   方式三：直接用 redis-cli（两条都要执行，缺一不可）：
--        redis-cli SET seckill:stock:1 1000
--        redis-cli DEL seckill:users:1

-- ============================================================
-- 【压测后】逐项校验（核心）
-- ============================================================

-- 校验 1：Redis 剩余秒杀库存不能为负 —— 超卖的最直接证据（DB 层到 0 即停，不会为负）
-- 期望：GET seckill:stock:1 返回 >= 0；更建议配合校验 2 看「订单数是否 > 初始库存」。
-- Redis CLI 执行：GET seckill:stock:1
-- （可选，供人工核对；DB 层库存字段本身因原子扣减不会出现负数）

-- 校验 2：秒杀订单数 + 剩余秒杀库存 = 初始秒杀库存（不多卖、不少卖）
-- 以秒杀商品1初始 1000 为例，期望 total = 1000（前提：上面已把 DB 与 Redis 都重置为 1000）
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
-- 另可核对已购集合大小：SCARD seckill:users:1，应等于校验 4 中商品1的 sold_count。

-- ============================================================
-- 【压测后】重置：把环境恢复到干净状态，方便下一轮压测
-- （与【0】完全一致：先重置 DB，再重置 Redis 的库存 key 与已购用户集合）
-- ============================================================
DELETE FROM seckill_order;
UPDATE seckill_goods SET seckill_stock = 1000 WHERE id = 1;

-- 再执行一次 Redis 重置（SQL 做不到），库存与已购集合都要处理：
--   方式一：管理接口（推荐，内部同时重置库存并清空已购集合）
--     curl -X POST http://localhost:7099/api/admin/stock/reset/1 \
--          -H "Authorization: Bearer <管理员token>"
--   方式二：redis-cli（两条都要执行）
--     redis-cli SET seckill:stock:1 1000
--     redis-cli DEL seckill:users:1
