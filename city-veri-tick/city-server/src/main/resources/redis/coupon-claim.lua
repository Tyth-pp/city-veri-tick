-- One Hash per coupon: all admission mutations are atomic and share one cluster slot.
local key = KEYS[1]
local op = ARGV[1]
local kind = redis.call('TYPE', key).ok
if kind ~= 'none' and kind ~= 'hash' then
    return -8
end
if op == 'init' then
    if kind == 'hash' then
        return 1
    end
    redis.call('HSET', key, 'generation', ARGV[2], 'stock', ARGV[3], 'limit', ARGV[4], 'start', ARGV[5], 'end', ARGV[6], 'active', ARGV[7])
    redis.call('PEXPIRE', key, ARGV[8])
    return 1
end
if kind == 'none' then
    return -1
end
if op == 'finish' then
    local owner = redis.call('HGET', key, 'r:' .. ARGV[2])
    if owner ~= ARGV[3] then
        return 0
    end
    if ARGV[4] == 'rollback' then
        local stock = tonumber(redis.call('HGET', key, 'stock'))
        local count = tonumber(redis.call('HGET', key, 'u:' .. owner))
        if not stock or not count or count < 1 then
            return -8
        end
        redis.call('HINCRBY', key, 'stock', 1)
        redis.call('HINCRBY', key, 'u:' .. owner, -1)
    end
    redis.call('HDEL', key, 'r:' .. ARGV[2])
    return 1
end
if op ~= 'reserve' then
    return -8
end
local stock = tonumber(redis.call('HGET', key, 'stock'))
local limit = tonumber(redis.call('HGET', key, 'limit'))
local first = tonumber(redis.call('HGET', key, 'start'))
local last = tonumber(redis.call('HGET', key, 'end'))
if not stock or not limit or limit < 0 or not first or not last then
    return -8
end
if redis.call('HGET', key, 'active') ~= '1' then
    return -5
end
local clock = redis.call('TIME')
local now = tonumber(clock[1]) * 1000 + math.floor(tonumber(clock[2]) / 1000)
if now < first then
    return -2
end
if now >= last then
    return -3
end
local user = 'u:' .. ARGV[2]
local count = tonumber(redis.call('HGET', key, user))
if limit > 0 and count and count >= limit then
    return -4
end
if stock <= 0 then
    return -7
end
if not count then
    local seed = tonumber(ARGV[4])
    if not seed or seed < 0 then
        return -6
    end
    count = seed
    if limit > 0 and count >= limit then
        redis.call('HSET', key, user, count);
        return -4
    end
end
-- Request ids are fresh UUIDs; retries must never increment the same reservation twice.
if redis.call('HEXISTS', key, 'r:' .. ARGV[3]) == 1 then
    return -8
end
redis.call('HSET', key, user, count + 1, 'stock', stock - 1, 'r:' .. ARGV[3], ARGV[2])
return 1
