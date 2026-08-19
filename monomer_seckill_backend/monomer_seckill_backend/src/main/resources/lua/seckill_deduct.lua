-- ============================================================
-- 秒杀扣库存 Lua 脚本（Redis 预扣库存 + 幂等防重复）
--
-- KEYS[1] = 库存 key：seckill:stock:{goodsId}
-- KEYS[2] = 已购用户集合 key：seckill:users:{goodsId}
-- ARGV[1] = userId
--
-- 返回值：
--   0  = 抢购成功（已原子扣减 1 个库存，并记录用户）
--  -1  = 库存不足（库存 key 不存在或已为 0）
--  -2  = 重复抢购（该用户已抢过此商品）
--
-- 说明：整个「判库存 → 判重复 → 扣库存 → 记用户」在一个 Lua 脚本内
--       由 Redis 单线程原子执行，杜绝并发下的超卖与重复下单。
-- ============================================================

local stock = tonumber(redis.call('GET', KEYS[1]))
if (not stock) or (stock <= 0) then
    return -1
end

local exists = redis.call('SISMEMBER', KEYS[2], ARGV[1])
if exists == 1 then
    return -2
end

redis.call('DECR', KEYS[1])
redis.call('SADD', KEYS[2], ARGV[1])
return 0
