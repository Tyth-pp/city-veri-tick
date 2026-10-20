-- Keys share an activity hash tag; write only while still owning the rebuild lease.
local operation = ARGV[1]
if operation == 'INVALIDATE' then
    return redis.call('DEL', KEYS[1], KEYS[2])
end
if redis.call('GET', KEYS[2]) ~= ARGV[2] then
    return 0
end
if operation == 'PUBLISH' then
    redis.call('SET', KEYS[1], ARGV[3], 'PX', ARGV[4])
    redis.call('DEL', KEYS[2])
    return 1
end
if operation == 'RELEASE' then
    return redis.call('DEL', KEYS[2])
end
return redis.error_reply('Unknown activity cache operation')
