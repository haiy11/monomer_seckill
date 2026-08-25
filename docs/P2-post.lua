-- post.lua：wrk 压测秒杀接口用的 Lua 脚本
--
-- 用法（先准备 tokens.txt，每行一个登录 token）：
--   wrk -t10 -c200 -d30s --latency -s post.lua http://localhost:7099
--
-- 说明：
--   P2 秒杀接口 POST /api/seckill/{seckillGoodsId} 需要登录，
--   用户身份从 Authorization: Bearer <token> 解析，不再传 userId。
--   每个请求必须使用不同用户的 token，否则全部被「重复抢购」拦截。

-- 读取 tokens.txt（每行一个 token），压测时循环使用
tokens = {}
local i = 0
for line in io.lines("tokens.txt") do
    i = i + 1
    tokens[i] = line
end
if i == 0 then
    tokens[1] = "请替换为真实token"
end

idx = 0

request = function()
    idx = idx % #tokens + 1
    local token = tokens[idx]
    local path = "/api/seckill/1"
    return wrk.format("POST", path, { ["Authorization"] = "Bearer " .. token })
end
