-- All keys share a phone hash tag: code, cooldown, failures, reservation.
local op = ARGV[1]
for _, key in ipairs(KEYS) do
    local t = redis.call('TYPE', key).ok
    if t ~= 'none' and t ~= 'string' then
        return redis.error_reply('Invalid SMS state type')
    end
end
local failed = tonumber(redis.call('GET', KEYS[3]) or '0')
if op == 'SEND' then
    if failed >= tonumber(ARGV[6]) then
        return -2
    end
    if redis.call('EXISTS', KEYS[2]) == 1 then
        return -1
    end
    if redis.call('EXISTS', KEYS[4]) == 1 then
        return -3
    end
    redis.call('SET', KEYS[1], ARGV[2], 'EX', ARGV[3])
    redis.call('SET', KEYS[2], '1', 'EX', ARGV[4])
    return 1
elseif op == 'RESERVE' then
    if failed >= tonumber(ARGV[6]) then
        return -2
    end
    if redis.call('EXISTS', KEYS[4]) == 1 then
        return -3
    end
    local code = redis.call('GET', KEYS[1])
    if not code or code ~= ARGV[2] then
        local count = redis.call('INCR', KEYS[3])
        if count == 1 then
            redis.call('EXPIRE', KEYS[3], ARGV[5])
        end
        if count >= tonumber(ARGV[6]) then
            return -2
        end
        return 0
    end
    redis.call('SET', KEYS[4], ARGV[3], 'EX', ARGV[4])
    return 1
elseif op == 'COMPLETE' then
    if redis.call('GET', KEYS[4]) ~= ARGV[2] or redis.call('EXISTS', KEYS[1]) == 0 then
        return 0
    end
    redis.call('DEL', KEYS[1], KEYS[3], KEYS[4])
    return 1
elseif op == 'RELEASE' then
    if redis.call('GET', KEYS[4]) == ARGV[2] then
        redis.call('DEL', KEYS[4])
    end
    return 1
elseif op == 'DELIVERY_FAILED' then
    if redis.call('GET', KEYS[1]) == ARGV[2] and redis.call('EXISTS', KEYS[4]) == 0 then
        redis.call('DEL', KEYS[1], KEYS[2])
    end
    return 1
end
return redis.error_reply('Unknown SMS operation')
