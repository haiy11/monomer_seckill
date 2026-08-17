-- ============================================================
-- 秒杀压测后的数据正确性校验脚本（配合 Apifox 压测使用）
--
-- 执行方式（二选一）：
--  1) WSL2 命令行（把 Windows 路径换成 /mnt/... 即可）：
--     docker exec -i mysql mysql -uroot -p123456hy --default-character-set=utf8mb4 monomer_seckill < /mnt/e/items/natherItems/monomer_seckill/docs/check-data.sql
--  2) Navicat / DBeaver：连接 3307 后选中 monomer_seckill 库，分段执行下面的语句。
-- ============================================================


-- ============================================================
-- 【0】压测前：重置数据（每次压测前执行，保证初始状态一致）
-- ============================================================
DELETE FROM seckill_order;                       -- 清空订单
UPDATE seckill_goods SET stock = 100 WHERE id = 1; -- 商品1 初始库存设为 100（可按需改）
UPDATE seckill_goods SET stock = 50  WHERE id = 2;
UPDATE seckill_goods SET stock = 30  WHERE id = 3;


-- ============================================================
-- 【压测后】逐项校验（核心）
-- ============================================================

-- 校验 1：库存不能为负 —— 超卖的最直接证据
-- 期望：返回空（没有任何一行 stock < 0）
SELECT id, name, stock FROM seckill_goods WHERE stock < 0;

-- 校验 2：订单数 + 剩余库存 = 初始库存（不多卖、不少卖）
-- 以商品1初始 100 为例，期望 total = 100
--   total > 100：说明超卖/多卖（订单多扣了库存）
--   total < 100：正常（并发请求里有一部分因"库存不足"失败）
SELECT
  (SELECT COUNT(*) FROM seckill_order WHERE goods_id = 1)
  + (SELECT stock FROM seckill_goods WHERE id = 1) AS total;

-- 校验 3：没有重复下单 —— 同一商品同一用户只允许一条订单
-- 期望：返回空（没有任何 (goods_id, user_id) 出现两次以上）
SELECT goods_id, user_id, COUNT(*) AS cnt
FROM seckill_order
GROUP BY goods_id, user_id
HAVING cnt > 1;

-- 校验 4：各商品实际卖出数量分布（对照 Apifox 报告的成功数）
SELECT goods_id, COUNT(*) AS sold_count FROM seckill_order GROUP BY goods_id;

-- 校验 5：订单总数
SELECT COUNT(*) AS total_orders FROM seckill_order;

-- 校验 6：抽查订单内容是否正常
SELECT * FROM seckill_order ORDER BY id DESC LIMIT 20;
