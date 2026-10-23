local stockKey = KEYS[1]
local userKey = KEYS[2]

if redis.call('EXISTS', stockKey) == 0 then
    return -2
end

if redis.call('EXISTS', userKey) == 1 then
    return 0
end

local stock = tonumber(redis.call('GET', stockKey))
if stock <= 0 then
    return -1
end

redis.call('DECR', stockKey)
redis.call('SET', userKey, '1')
return 1
