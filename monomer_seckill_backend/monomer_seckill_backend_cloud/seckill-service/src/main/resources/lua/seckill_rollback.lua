-- ============================================================
-- 秒杀回滚 Lua 脚本（下单失败 / 超时取消 / 手动取消时回补库存）
--
-- KEYS[1] = 库存 key：seckill:stock:{goodsId}
-- KEYS[2] = 已购用户集合 key：seckill:users:{goodsId}
-- ARGV[1] = userId
--
-- 返回值：
--   1  = 回滚成功（该用户此前确实在集合中，已移除并回补 1 个库存）
--   0  = 无需回滚（该用户不在集合中，幂等处理，不回补库存）
-- ============================================================

local removed = redis.call('SREM', KEYS[2], ARGV[1])
if removed == 1 then
    redis.call('INCR', KEYS[1])
    return 1
end
return 0
