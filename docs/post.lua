-- post.lua：wrk 压测秒杀接口用的 Lua 脚本
-- 用法：
--   wrk -t10 -c200 -d30s --latency -s post.lua http://localhost:7099
--
-- 作用：每个请求动态生成不同的 userId，避免全部被「重复抢购」拦截。

-- 注意事项：
--   userId 必须是纯整数（后端是 Long 类型）。
--   不要用 os.time() * 大数 的写法——那会产生浮点数/科学计数法，
--   拼进 URL 后后端会报 Failed to convert value ... For input string "1.7869546000382e15"。

-- 全局计数器：Lua 的 number 在 wrk 里是双精度，但 5000 万以内的整数都能精确表示，
-- 压测场景下的请求数远达不到溢出边界，纯累加足够安全。
counter = 0

request = function()
    counter = counter + 1
    local path = "/api/seckill/1?userId=" .. counter
    return wrk.format("POST", path)
end
