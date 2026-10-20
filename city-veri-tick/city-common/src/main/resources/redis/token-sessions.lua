-- Standalone Redis only: one user index plus dynamically named blacklist keys.
-- Preflight types before mutation. Lua isolation is not cross-system rollback.
local key = KEYS[1]
local op = ARGV[1]
local clock = redis.call('TIME')
local now = tonumber(clock[1]) * 1000 + math.floor(tonumber(clock[2]) / 1000)
local function kind(k) return redis.call('TYPE', k).ok end
local function indexType()
    local t = kind(key)
    if t ~= 'none' and t ~= 'set' and t ~= 'zset' then error('Invalid JWT session index type') end
    return t
end
local function expireIndex()
    local last = redis.call('ZREVRANGE', key, 0, 0, 'WITHSCORES')
    if #last > 0 then redis.call('PEXPIREAT', key, math.ceil(tonumber(last[2]))) end
end
local priority = {LOGOUT=1, FORCED_LOGOUT=2, ACCOUNT_DISABLED=3}
local function checkBlacklist(k)
    local t = kind(k)
    if t ~= 'none' and t ~= 'string' then error('Invalid JWT blacklist type') end
end
local function blacklist(k, reason, ttl)
    local previous = redis.call('GET', k)
    local previousTtl = redis.call('PTTL', k)
    if previous and (priority[previous] or 0) > (priority[reason] or 0) then reason = previous end
    ttl = math.max(1, math.ceil(ttl), previousTtl)
    redis.call('SET', k, reason, 'PX', ttl)
end

if op == 'REGISTER' then
    local t = indexType()
    local expiry = tonumber(ARGV[3])
    if expiry <= now then return 0 end
    if t == 'set' then
        local members = redis.call('SMEMBERS', key)
        local remaining = redis.call('PTTL', key)
        remaining = math.max(remaining, tonumber(ARGV[4]))
        redis.call('DEL', key)
        for _, jti in ipairs(members) do redis.call('ZADD', key, now + remaining, jti) end
    end
    redis.call('ZREMRANGEBYSCORE', key, '-inf', now)
    local existing = redis.call('ZSCORE', key, ARGV[2])
    if existing then expiry = math.max(expiry, tonumber(existing)) end
    redis.call('ZADD', key, expiry, ARGV[2])
    expireIndex()
    return 1
elseif op == 'REMOVE' then
    local t = indexType()
    if t == 'set' then
        redis.call('SREM', key, ARGV[2])
        if redis.call('PTTL', key) == -1 then redis.call('PEXPIRE', key, ARGV[3]) end
    elseif t == 'zset' then
        redis.call('ZREM', key, ARGV[2])
        redis.call('ZREMRANGEBYSCORE', key, '-inf', now)
        expireIndex()
    end
    return 1
elseif op == 'REVOKE' then
    checkBlacklist(key)
    blacklist(key, ARGV[2], tonumber(ARGV[3]))
    return 1
elseif op == 'REVOKE_ALL' then
    local t = indexType()
    local tokens = {}
    if t == 'set' then
        local remaining = redis.call('PTTL', key)
        if remaining < 1 then remaining = tonumber(ARGV[4]) end
        for _, jti in ipairs(redis.call('SMEMBERS', key)) do
            table.insert(tokens, jti)
            table.insert(tokens, now + math.max(remaining, tonumber(ARGV[4])))
        end
    elseif t == 'zset' then
        tokens = redis.call('ZRANGEBYSCORE', key, '(' .. now, '+inf', 'WITHSCORES')
    end
    -- A malformed blacklist must not leave half the batch revoked.
    for i=1,#tokens,2 do checkBlacklist(ARGV[2] .. tokens[i]) end
    for i=1,#tokens,2 do
        blacklist(ARGV[2] .. tokens[i], ARGV[3], tonumber(tokens[i+1]) - now)
    end
    redis.call('DEL', key)
    return #tokens / 2
end
return redis.error_reply('Unknown JWT session operation')
